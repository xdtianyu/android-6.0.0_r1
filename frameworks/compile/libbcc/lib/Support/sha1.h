/*
 * See "sha1.c" for author info.
 */
#ifndef _DALVIK_SHA1
#define _DALVIK_SHA1

#include <stdint.h>

typedef struct {
    uint32_t state[5];
    uint32_t count[2];
    uint8_t buffer[64];
} SHA1_CTX;

#define HASHSIZE 20

#if defined(__cplusplus)
extern "C" {
#endif

void SHA1Init(SHA1_CTX* context);
void SHA1Update(SHA1_CTX* context, const uint8_t* data, uint32_t len);
void SHA1Final(uint8_t digest[HASHSIZE], SHA1_CTX* context);

#if defined(__cplusplus)
}
#endif

#endif /*_DALVIK_SHA1*/
