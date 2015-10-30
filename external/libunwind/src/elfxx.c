/* libunwind - a platform-independent unwind library
   Copyright (C) 2003-2005 Hewlett-Packard Co
   Copyright (C) 2007 David Mosberger-Tang
	Contributed by David Mosberger-Tang <dmosberger@gmail.com>

This file is part of libunwind.

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.  */

#include "libunwind_i.h"

#include <stdio.h>
#include <sys/param.h>

#ifdef HAVE_LZMA
#include <lzma.h>
#endif /* HAVE_LZMA */

// --------------------------------------------------------------------------
// Functions to read elf data from memory.
// --------------------------------------------------------------------------
extern size_t elf_w (memory_read) (
    struct elf_image* ei, unw_word_t addr, uint8_t* buffer, size_t bytes, bool string_read) {
  struct map_info* map = ei->u.memory.map;
  unw_accessors_t* a = unw_get_accessors (ei->u.memory.as);
  if (map->end - addr < bytes) {
    bytes = map->end - addr;
  }
  size_t bytes_read = 0;
  unw_word_t data_word;
  size_t align_bytes = addr & (sizeof(unw_word_t) - 1);
  if (align_bytes != 0) {
    if ((*a->access_mem) (ei->u.memory.as, addr & ~(sizeof(unw_word_t) - 1), &data_word,
                          0, ei->u.memory.as_arg) != 0) {
      return 0;
    }
    size_t copy_bytes = MIN(sizeof(unw_word_t) - align_bytes, bytes);
    memcpy (buffer, (uint8_t*) (&data_word) + align_bytes, copy_bytes);
    if (string_read) {
      // Check for nul terminator.
      uint8_t* nul_terminator = memchr (buffer, '\0', copy_bytes);
      if (nul_terminator != NULL) {
        return nul_terminator - buffer;
      }
    }

    addr += copy_bytes;
    bytes_read += copy_bytes;
    bytes -= copy_bytes;
    buffer += copy_bytes;
  }

  size_t num_words = bytes / sizeof(unw_word_t);
  size_t i;
  for (i = 0; i < num_words; i++) {
    if ((*a->access_mem) (ei->u.memory.as, addr, &data_word, 0, ei->u.memory.as_arg) != 0) {
      return bytes_read;
    }

    memcpy (buffer, &data_word, sizeof(unw_word_t));
    if (string_read) {
      // Check for nul terminator.
      uint8_t* nul_terminator = memchr (buffer, '\0', sizeof(unw_word_t));
      if (nul_terminator != NULL) {
        return nul_terminator - buffer + bytes_read;
      }
    }

    addr += sizeof(unw_word_t);
    bytes_read += sizeof(unw_word_t);
    buffer += sizeof(unw_word_t);
  }

  size_t left_over = bytes & (sizeof(unw_word_t) - 1);
  if (left_over) {
    if ((*a->access_mem) (ei->u.memory.as, addr, &data_word, 0, ei->u.memory.as_arg) != 0) {
      return bytes_read;
    }

    memcpy (buffer, &data_word, left_over);
    if (string_read) {
      // Check for nul terminator.
      uint8_t* nul_terminator = memchr (buffer, '\0', sizeof(unw_word_t));
      if (nul_terminator != NULL) {
        return nul_terminator - buffer + bytes_read;
      }
    }

    bytes_read += left_over;
  }
  return bytes_read;
}

static bool elf_w (section_table_offset) (struct elf_image* ei, Elf_W(Ehdr)* ehdr, Elf_W(Off)* offset) {
  GET_EHDR_FIELD(ei, ehdr, e_shoff, true);
  GET_EHDR_FIELD(ei, ehdr, e_shentsize, true);
  GET_EHDR_FIELD(ei, ehdr, e_shnum, true);

  uintptr_t size = ei->u.memory.map->end - ei->u.memory.map->start;
  if (ehdr->e_shoff + ehdr->e_shnum * ehdr->e_shentsize > size) {
    Debug (1, "section table outside of image? (%lu > %lu)\n",
           (unsigned long) (ehdr->e_shoff + ehdr->e_shnum * ehdr->e_shentsize),
           (unsigned long) size);
    return false;
  }

  *offset = ehdr->e_shoff;
  return true;
}

static bool elf_w (string_table_offset) (
    struct elf_image* ei, int section, Elf_W(Ehdr)* ehdr, Elf_W(Off)* offset) {
  GET_EHDR_FIELD(ei, ehdr, e_shoff, true);
  GET_EHDR_FIELD(ei, ehdr, e_shentsize, true);
  unw_word_t str_soff = ehdr->e_shoff + (section * ehdr->e_shentsize);
  uintptr_t size = ei->u.memory.map->end - ei->u.memory.map->start;
  if (str_soff + ehdr->e_shentsize > size) {
    Debug (1, "string shdr table outside of image? (%lu > %lu)\n",
           (unsigned long) (str_soff + ehdr->e_shentsize),
           (unsigned long) size);
    return false;
  }

  Elf_W(Shdr) shdr;
  GET_SHDR_FIELD(ei, str_soff, &shdr, sh_offset);
  GET_SHDR_FIELD(ei, str_soff, &shdr, sh_size);
  if (shdr.sh_offset + shdr.sh_size > size) {
    Debug (1, "string table outside of image? (%lu > %lu)\n",
           (unsigned long) (shdr.sh_offset + shdr.sh_size),
           (unsigned long) size);
    return false;
  }

  Debug (16, "strtab=0x%lx\n", (long) shdr.sh_offset);
  *offset = shdr.sh_offset;
  return true;
}

static bool elf_w (lookup_symbol_memory) (
    unw_addr_space_t as, unw_word_t ip, struct elf_image* ei, Elf_W(Addr) load_offset,
    char* buf, size_t buf_len, unw_word_t* offp, Elf_W(Ehdr)* ehdr) {
  Elf_W(Off) shdr_offset;
  if (!elf_w (section_table_offset) (ei, ehdr, &shdr_offset)) {
    return false;
  }

  GET_EHDR_FIELD(ei, ehdr, e_shnum, true);
  GET_EHDR_FIELD(ei, ehdr, e_shentsize, true);
  int i;
  for (i = 0; i < ehdr->e_shnum; ++i) {
    Elf_W(Shdr) shdr;
    GET_SHDR_FIELD(ei, shdr_offset, &shdr, sh_type);
    switch (shdr.sh_type) {
      case SHT_SYMTAB:
      case SHT_DYNSYM:
      {
        GET_SHDR_FIELD(ei, shdr_offset, &shdr, sh_link);

        Elf_W(Off) strtab_offset;
        if (!elf_w (string_table_offset) (ei, shdr.sh_link, ehdr, &strtab_offset)) {
          continue;
        }

        GET_SHDR_FIELD(ei, shdr_offset, &shdr, sh_offset);
        GET_SHDR_FIELD(ei, shdr_offset, &shdr, sh_size);
        GET_SHDR_FIELD(ei, shdr_offset, &shdr, sh_entsize);

        Debug (16, "symtab=0x%lx[%d]\n", (long) shdr.sh_offset, shdr.sh_type);

        unw_word_t sym_offset;
        unw_word_t symtab_end = shdr.sh_offset + shdr.sh_size;
        for (sym_offset = shdr.sh_offset;
             sym_offset < symtab_end;
             sym_offset += shdr.sh_entsize) {
          Elf_W(Sym) sym;
          GET_SYM_FIELD(ei, sym_offset, &sym, st_info);
          GET_SYM_FIELD(ei, sym_offset, &sym, st_shndx);

          if (ELF_W (ST_TYPE) (sym.st_info) == STT_FUNC && sym.st_shndx != SHN_UNDEF) {
            GET_SYM_FIELD(ei, sym_offset, &sym, st_value);
            Elf_W(Addr) val;
            if (tdep_get_func_addr (as, sym.st_value, &val) < 0) {
              continue;
            }
            if (sym.st_shndx != SHN_ABS) {
              val += load_offset;
            }
            Debug (16, "0x%016lx info=0x%02x\n", (long) val, sym.st_info);

            GET_SYM_FIELD(ei, sym_offset, &sym, st_size);
            if (ip >= val && (Elf_W(Addr)) (ip - val) < sym.st_size) {
              GET_SYM_FIELD(ei, sym_offset, &sym, st_name);
              uintptr_t size = ei->u.memory.map->end - ei->u.memory.map->start;
              Elf_W(Off) strname_offset = strtab_offset + sym.st_name;
              if (strname_offset > size || strname_offset < strtab_offset) {
                // Malformed elf symbol table.
                break;
              }

              size_t bytes_read = elf_w (memory_read) (
                  ei, ei->u.memory.map->start + strname_offset,
                  (uint8_t*) buf, buf_len, true);
              if (bytes_read == 0) {
                // Empty name, so keep checking the other symbol tables
                // for a possible match.
                break;
              }
              // Ensure the string is nul terminated, it is assumed that
              // sizeof(buf) >= buf_len + 1.
              buf[buf_len] = '\0';

              if (offp != NULL) {
                *offp = ip - val;
              }
              return true;
            }
          }
        }
        break;
      }

      default:
        break;
    }
    shdr_offset += ehdr->e_shentsize;
  }
  return false;
}

static bool elf_w (get_load_offset_memory) (
    struct elf_image* ei, unsigned long segbase, unsigned long mapoff,
    Elf_W(Ehdr)* ehdr, Elf_W(Addr)* load_offset) {
  GET_EHDR_FIELD(ei, ehdr, e_phoff, true);
  GET_EHDR_FIELD(ei, ehdr, e_phnum, true);

  unw_word_t offset = ehdr->e_phoff;
  int i;
  for (i = 0; i < ehdr->e_phnum; ++i) {
    Elf_W(Phdr) phdr;
    GET_PHDR_FIELD(ei, offset, &phdr, p_type);
    if (phdr.p_type == PT_LOAD) {
      GET_PHDR_FIELD(ei, offset, &phdr, p_offset);
      if (phdr.p_offset == mapoff) {
        GET_PHDR_FIELD(ei, offset, &phdr, p_vaddr);
        *load_offset = segbase - phdr.p_vaddr;
        return true;
      }
    }
    offset += sizeof(Elf_W(Phdr));
  }
  return false;
}

// --------------------------------------------------------------------------
// Functions to read elf data from the mapped elf image.
// --------------------------------------------------------------------------
static Elf_W(Shdr)* elf_w (section_table) (struct elf_image* ei) {
  Elf_W(Ehdr)* ehdr = ei->u.mapped.image;
  Elf_W(Off) soff = ehdr->e_shoff;
  if (soff + ehdr->e_shnum * ehdr->e_shentsize > ei->u.mapped.size) {
    Debug (1, "section table outside of image? (%lu > %lu)\n",
           (unsigned long) (soff + ehdr->e_shnum * ehdr->e_shentsize),
           (unsigned long) ei->u.mapped.size);
    return NULL;
  }

  return (Elf_W(Shdr) *) ((char *) ei->u.mapped.image + soff);
}

static char* elf_w (string_table) (struct elf_image* ei, int section) {
  Elf_W(Ehdr)* ehdr = ei->u.mapped.image;
  Elf_W(Off) str_soff = ehdr->e_shoff + (section * ehdr->e_shentsize);
  if (str_soff + ehdr->e_shentsize > ei->u.mapped.size) {
    Debug (1, "string shdr table outside of image? (%lu > %lu)\n",
           (unsigned long) (str_soff + ehdr->e_shentsize),
           (unsigned long) ei->u.mapped.size);
    return NULL;
  }
  Elf_W(Shdr)* str_shdr = (Elf_W(Shdr) *) ((char *) ei->u.mapped.image + str_soff);

  if (str_shdr->sh_offset + str_shdr->sh_size > ei->u.mapped.size) {
    Debug (1, "string table outside of image? (%lu > %lu)\n",
           (unsigned long) (str_shdr->sh_offset + str_shdr->sh_size),
           (unsigned long) ei->u.mapped.size);
    return NULL;
  }

  Debug (16, "strtab=0x%lx\n", (long) str_shdr->sh_offset);
  return (char*) ((uintptr_t) ei->u.mapped.image + str_shdr->sh_offset);
}

static bool elf_w (lookup_symbol_mapped) (
    unw_addr_space_t as, unw_word_t ip, struct elf_image* ei, Elf_W(Addr) load_offset,
    char* buf, size_t buf_len, unw_word_t* offp) {

  Elf_W(Shdr)* shdr = elf_w (section_table) (ei);
  if (!shdr) {
    return false;
  }

  Elf_W(Ehdr)* ehdr = ei->u.mapped.image;
  int i;
  for (i = 0; i < ehdr->e_shnum; ++i) {
    switch (shdr->sh_type) {
      case SHT_SYMTAB:
      case SHT_DYNSYM:
      {
        Elf_W(Sym)* symtab = (Elf_W(Sym) *) ((char *) ei->u.mapped.image + shdr->sh_offset);
        Elf_W(Sym)* symtab_end = (Elf_W(Sym) *) ((char *) symtab + shdr->sh_size);

        char* strtab = elf_w (string_table) (ei, shdr->sh_link);
        if (!strtab) {
          continue;
        }

        Debug (16, "symtab=0x%lx[%d]\n", (long) shdr->sh_offset, shdr->sh_type);

        Elf_W(Sym)* sym;
        for (sym = symtab;
             sym < symtab_end;
             sym = (Elf_W(Sym) *) ((char *) sym + shdr->sh_entsize)) {
          if (ELF_W (ST_TYPE) (sym->st_info) == STT_FUNC && sym->st_shndx != SHN_UNDEF) {
            Elf_W(Addr) val;
            if (tdep_get_func_addr (as, sym->st_value, &val) < 0) {
              continue;
            }
            if (sym->st_shndx != SHN_ABS) {
              val += load_offset;
            }
            Debug (16, "0x%016lx info=0x%02x\n", (long) val, sym->st_info);
            if (ip >= val && (Elf_W(Addr)) (ip - val) < sym->st_size) {
              char* str_name = strtab + sym->st_name;
              if (str_name > (char*) ei->u.mapped.image + ei->u.mapped.size ||
                  str_name < strtab) {
                // Malformed elf symbol table.
                break;
              }

              // Make sure we don't try and read past the end of the image.
              uintptr_t max_size = (uintptr_t) str_name - (uintptr_t) ei->u.mapped.image;
              if (buf_len > max_size) {
                buf_len = max_size;
              }

              strncpy (buf, str_name, buf_len);
              // Ensure the string is nul terminated, it is assumed that
              // sizeof(buf) >= buf_len + 1.
              buf[buf_len] = '\0';
              if (buf[0] == '\0') {
                // Empty name, so keep checking the other symbol tables
                // for a possible match.
                break;
              }
              if (offp != NULL) {
                *offp = ip - val;
              }
              return true;
            }
          }
        }
        break;
      }

      default:
        break;
    }
    shdr = (Elf_W(Shdr) *) (((char *) shdr) + ehdr->e_shentsize);
  }
  return false;
}

static bool elf_w (get_load_offset_mapped) (
    struct elf_image *ei, unsigned long segbase, unsigned long mapoff, Elf_W(Addr)* load_offset) {
  Elf_W(Ehdr) *ehdr = ei->u.mapped.image;
  Elf_W(Phdr) *phdr = (Elf_W(Phdr) *) ((char *) ei->u.mapped.image + ehdr->e_phoff);

  int i;
  for (i = 0; i < ehdr->e_phnum; ++i) {
    if (phdr[i].p_type == PT_LOAD && phdr[i].p_offset == mapoff) {
      *load_offset = segbase - phdr[i].p_vaddr;
      return true;
    }
  }
  return false;
}

// --------------------------------------------------------------------------

static inline bool elf_w (lookup_symbol) (
    unw_addr_space_t as, unw_word_t ip, struct elf_image *ei, Elf_W(Addr) load_offset,
    char *buf, size_t buf_len, unw_word_t* offp, Elf_W(Ehdr)* ehdr) {
  if (!ei->valid)
    return false;

  if (buf_len <= 1) {
    Debug (1, "lookup_symbol called with a buffer too small to hold a name %zu\n", buf_len);
    return false;
  }

  // Leave enough space for the nul terminator.
  buf_len--;

  if (ei->mapped) {
    return elf_w (lookup_symbol_mapped) (as, ip, ei, load_offset, buf, buf_len, offp);
  } else {
    return elf_w (lookup_symbol_memory) (as, ip, ei, load_offset, buf, buf_len, offp, ehdr);
  }
}

static bool elf_w (get_load_offset) (
    struct elf_image* ei, unsigned long segbase, unsigned long mapoff,
    Elf_W(Ehdr)* ehdr, Elf_W(Addr)* load_offset) {
  if (ei->mapped) {
    return elf_w (get_load_offset_mapped) (ei, segbase, mapoff, load_offset);
  } else {
    return elf_w (get_load_offset_memory) (ei, segbase, mapoff, ehdr, load_offset);
  }
}

#if HAVE_LZMA
static size_t xz_uncompressed_size (uint8_t* compressed, size_t length) {
  uint64_t memlimit = UINT64_MAX;
  size_t ret = 0, pos = 0;
  lzma_stream_flags options;
  lzma_index *index;

  if (length < LZMA_STREAM_HEADER_SIZE) {
    return 0;
  }

  uint8_t *footer = compressed + length - LZMA_STREAM_HEADER_SIZE;
  if (lzma_stream_footer_decode (&options, footer) != LZMA_OK) {
    return 0;
  }

  if (length < LZMA_STREAM_HEADER_SIZE + options.backward_size) {
    return 0;
  }

  uint8_t* indexdata = footer - options.backward_size;
  if (lzma_index_buffer_decode (&index, &memlimit, NULL, indexdata,
                                &pos, options.backward_size) != LZMA_OK) {
    return 0;
  }

  if (lzma_index_size (index) == options.backward_size) {
    ret = lzma_index_uncompressed_size (index);
  }

  lzma_index_end (index, NULL);
  return ret;
}

static bool elf_w (extract_minidebuginfo) (struct elf_image* ei, struct elf_image* mdi, Elf_W(Ehdr)* ehdr) {
  Elf_W(Ehdr)* ehdr = ei->image;
  Elf_W(Shdr)* shdr;
  char *strtab;
  int i;
  uint8_t *compressed = NULL;
  uint64_t memlimit = UINT64_MAX; /* no memory limit */
  size_t compressed_len, uncompressed_len;

  if (!ei->valid) {
    return false;
  }

  shdr = elf_w (section_table) (ei);
  if (!shdr) {
    return false;
  }

  strtab = elf_w (string_table) (ei, ehdr->e_shstrndx);
  if (!strtab) {
    return false;
  }

  for (i = 0; i < ehdr->e_shnum; ++i) {
    if (strcmp (strtab + shdr->sh_name, ".gnu_debugdata") == 0) {
      if (shdr->sh_offset + shdr->sh_size > ei->size) {
        Debug (1, ".gnu_debugdata outside image? (0x%lu > 0x%lu)\n",
               (unsigned long) shdr->sh_offset + shdr->sh_size,
               (unsigned long) ei->size);
        return false;
      }

      Debug (16, "found .gnu_debugdata at 0x%lx\n",
             (unsigned long) shdr->sh_offset);
             compressed = ((uint8_t *) ei->image) + shdr->sh_offset;
             compressed_len = shdr->sh_size;
      break;
    }

    shdr = (Elf_W(Shdr) *) (((char *) shdr) + ehdr->e_shentsize);
  }

  /* not found */
  if (!compressed) {
    return false;
  }

  uncompressed_len = xz_uncompressed_size (compressed, compressed_len);
  if (uncompressed_len == 0) {
    Debug (1, "invalid .gnu_debugdata contents\n");
    return false;
  }

  mdi->size = uncompressed_len;
  mdi->image = mmap (NULL, uncompressed_len, PROT_READ|PROT_WRITE,
                     MAP_PRIVATE|MAP_ANONYMOUS, -1, 0);

  if (mdi->image == MAP_FAILED) {
    return false;
  }

  size_t in_pos = 0, out_pos = 0;
  lzma_ret lret;
  lret = lzma_stream_buffer_decode (&memlimit, 0, NULL,
                                    compressed, &in_pos, compressed_len,
                                    mdi->image, &out_pos, mdi->size);
  if (lret != LZMA_OK) {
    Debug (1, "LZMA decompression failed: %d\n", lret);
    munmap (mdi->image, mdi->size);
    return false;
  }

  return true;
}
#else
static bool elf_w (extract_minidebuginfo) (struct elf_image* ei, struct elf_image* mdi, Elf_W(Ehdr)* ehdr) {
  return false;
}
#endif /* !HAVE_LZMA */

// Find the ELF image that contains IP and return the procedure name from
// the symbol table that matches the IP.
HIDDEN bool elf_w (get_proc_name_in_image) (
    unw_addr_space_t as, struct elf_image* ei, unsigned long segbase, unsigned long mapoff,
    unw_word_t ip, char* buf, size_t buf_len, unw_word_t* offp) {
  Elf_W(Ehdr) ehdr;
  memset(&ehdr, 0, sizeof(ehdr));
  Elf_W(Addr) load_offset;
  if (!elf_w (get_load_offset) (ei, segbase, mapoff, &ehdr, &load_offset)) {
    return false;
  }

  if (elf_w (lookup_symbol) (as, ip, ei, load_offset, buf, buf_len, offp, &ehdr) != 0) {
    return true;
  }

  // If the ELF image doesn't contain a match, look up the symbol in
  // the MiniDebugInfo.
  struct elf_image mdi;
  if (elf_w (extract_minidebuginfo) (ei, &mdi, &ehdr)) {
    if (!elf_w (get_load_offset) (&mdi, segbase, mapoff, &ehdr, &load_offset)) {
      return false;
    }
    if (elf_w (lookup_symbol) (as, ip, &mdi, load_offset, buf, buf_len, offp, &ehdr)) {
      munmap (mdi.u.mapped.image, mdi.u.mapped.size);
      return true;
    }
    return false;
  }
  return false;
}

HIDDEN bool elf_w (get_proc_name) (
    unw_addr_space_t as, pid_t pid, unw_word_t ip, char* buf, size_t buf_len,
    unw_word_t* offp, void* as_arg) {
  unsigned long segbase, mapoff;
  struct elf_image ei;

  if (tdep_get_elf_image(as, &ei, pid, ip, &segbase, &mapoff, NULL, as_arg) < 0) {
    return false;
  }

  return elf_w (get_proc_name_in_image) (as, &ei, segbase, mapoff, ip, buf, buf_len, offp);
}

HIDDEN bool elf_w (get_load_base) (struct elf_image* ei, unw_word_t mapoff, unw_word_t* load_base) {
  if (!ei->valid) {
    return false;
  }

  if (ei->mapped) {
    Elf_W(Ehdr)* ehdr = ei->u.mapped.image;
    Elf_W(Phdr)* phdr = (Elf_W(Phdr)*) ((char*) ei->u.mapped.image + ehdr->e_phoff);
    int i;
    for (i = 0; i < ehdr->e_phnum; ++i) {
      if (phdr[i].p_type == PT_LOAD && phdr[i].p_offset == mapoff) {
        *load_base = phdr[i].p_vaddr;
        return true;
      }
    }
    return false;
  } else {
    Elf_W(Ehdr) ehdr;
    GET_EHDR_FIELD(ei, &ehdr, e_phnum, false);
    GET_EHDR_FIELD(ei, &ehdr, e_phoff, false);
    int i;
    unw_word_t offset = ehdr.e_phoff;
    for (i = 0; i < ehdr.e_phnum; ++i) {
      Elf_W(Phdr) phdr;
      GET_PHDR_FIELD(ei, offset, &phdr, p_type);
      GET_PHDR_FIELD(ei, offset, &phdr, p_offset);
      if (phdr.p_type == PT_LOAD && phdr.p_offset == mapoff) {
        GET_PHDR_FIELD(ei, offset, &phdr, p_vaddr);
        *load_base = phdr.p_vaddr;
        return true;
      }
      offset += sizeof(phdr);
    }
    return false;
  }
  return false;
}
