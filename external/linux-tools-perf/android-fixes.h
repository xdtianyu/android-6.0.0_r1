/* Turn on our Android-specific changes until we can upstream them. */
#define ANDROID_PATCHES

#include <stddef.h>

/* In libcxxabi. */
extern char* __cxa_demangle(const char*, char*, size_t*, int*);

/* So we can say HAVE_CPLUS_DEMANGLE (since we don't have libbfd). */
static inline char* cplus_demangle(const char* c, int i) {
  return __cxa_demangle(c, 0, 0, 0);
}
