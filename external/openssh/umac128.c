/* In OpenSSH, umac.c is compiled twice, with different #defines set on the
 * command line. Since we don't want to stretch the Android build system, in
 * Android this file is duplicated as umac.c and umac128.c. The latter contains
 * the #defines (that were set in OpenSSH's Makefile) at the top of the
 * file and then #includes umac.c. */

#define UMAC_OUTPUT_LEN 16
#define umac_new umac128_new
#define umac_update umac128_update
#define umac_final umac128_final
#define umac_delete umac128_delete

#include "umac.c"
