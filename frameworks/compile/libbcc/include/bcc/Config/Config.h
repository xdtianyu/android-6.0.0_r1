#ifndef BCC_CONFIG_CONFIG_H
#define BCC_CONFIG_CONFIG_H

//---------------------------------------------------------------------------
// Configuration for Disassembler
//---------------------------------------------------------------------------

#if DEBUG_MC_DISASSEMBLER
#define USE_DISASSEMBLER 1
#else
#define USE_DISASSEMBLER 0
#endif

#if defined(__HOST__)
#define DEBUG_DISASSEMBLER_FILE "/tmp/mc-dis.s"
#else
#define DEBUG_DISASSEMBLER_FILE "/data/local/tmp/mc-dis.s"
#endif // defined(__HOST__)

//---------------------------------------------------------------------------
// Configuration for CodeGen and CompilerRT
//---------------------------------------------------------------------------

#if defined(FORCE_ARM_CODEGEN)
  #define PROVIDE_ARM_CODEGEN
  #define DEFAULT_ARM_CODEGEN

#elif defined(FORCE_ARM64_CODEGEN)
  #define PROVIDE_ARM_CODEGEN
  #define PROVIDE_ARM64_CODEGEN
  #define DEFAULT_ARM64_CODEGEN

#elif defined(FORCE_MIPS_CODEGEN)
  #define PROVIDE_MIPS_CODEGEN
  #define DEFAULT_MIPS_CODEGEN

#elif defined(FORCE_MIPS64_CODEGEN)
  #define PROVIDE_MIPS_CODEGEN
  #define PROVIDE_MIPS64_CODEGEN
  #define DEFAULT_MIPS64_CODEGEN

#elif defined(FORCE_X86_CODEGEN)
  #define PROVIDE_X86_CODEGEN
  #define DEFAULT_X86_CODEGEN

#elif defined(FORCE_X86_64_CODEGEN)
  // There is no separate X86_64 code generation target. It is all part of X86.
  #define PROVIDE_X86_CODEGEN
  #define DEFAULT_X86_64_CODEGEN

#else
  #define PROVIDE_ARM_CODEGEN
  #define PROVIDE_ARM64_CODEGEN
  #define PROVIDE_MIPS_CODEGEN
  #define PROVIDE_MIPS64_CODEGEN
  #define PROVIDE_X86_CODEGEN
  #define PROVIDE_X86_64_CODEGEN

  #if defined(__arm__)
    #define DEFAULT_ARM_CODEGEN
  #elif defined(__aarch64__)
    #define DEFAULT_ARM64_CODEGEN
  #elif defined(__mips__)
    #if defined(__LP64__)
      #define DEFAULT_MIPS64_CODEGEN
    #else
      #define DEFAULT_MIPS_CODEGEN
    #endif
  #elif defined(__i386__)
    #define DEFAULT_X86_CODEGEN
  #elif defined(__x86_64__)
    #define DEFAULT_X86_64_CODEGEN
  #endif
#endif

#define DEFAULT_ARM_TRIPLE_STRING      "armv7-none-linux-gnueabi"
#define DEFAULT_THUMB_TRIPLE_STRING    "thumbv7-none-linux-gnueabi"
#define DEFAULT_ARM64_TRIPLE_STRING    "aarch64-none-linux-gnueabi"
#define DEFAULT_MIPS_TRIPLE_STRING     "mipsel-none-linux-gnueabi"
#define DEFAULT_MIPS64_TRIPLE_STRING   "mips64el-none-linux-gnueabi"
#define DEFAULT_X86_TRIPLE_STRING      "i686-unknown-linux"
#define DEFAULT_X86_64_TRIPLE_STRING   "x86_64-unknown-linux"

#if defined(DEFAULT_ARM_CODEGEN)
  #define DEFAULT_TARGET_TRIPLE_STRING DEFAULT_ARM_TRIPLE_STRING
#elif defined(DEFAULT_ARM64_CODEGEN)
  #define DEFAULT_TARGET_TRIPLE_STRING DEFAULT_ARM64_TRIPLE_STRING
#elif defined(DEFAULT_MIPS_CODEGEN)
  #define DEFAULT_TARGET_TRIPLE_STRING DEFAULT_MIPS_TRIPLE_STRING
#elif defined(DEFAULT_MIPS64_CODEGEN)
  #define DEFAULT_TARGET_TRIPLE_STRING DEFAULT_MIPS64_TRIPLE_STRING
#elif defined(DEFAULT_X86_CODEGEN)
  #define DEFAULT_TARGET_TRIPLE_STRING DEFAULT_X86_TRIPLE_STRING
#elif defined(DEFAULT_X86_64_CODEGEN)
  #define DEFAULT_TARGET_TRIPLE_STRING DEFAULT_X86_64_TRIPLE_STRING
#endif

#if (defined(__VFP_FP__) && !defined(__SOFTFP__))
  #define ARM_USE_VFP
#endif

//---------------------------------------------------------------------------

#endif // BCC_CONFIG_CONFIG_H
