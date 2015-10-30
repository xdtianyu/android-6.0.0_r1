/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include <linux/futex.h>
#include <sys/types.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <sys/prctl.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/utsname.h>
#include <fcntl.h>
#include <cutils/log.h>
#include <linux/perf_event.h>
#include <errno.h>
#include <inttypes.h>
#include <linux/sysctl.h>
#include <arpa/inet.h>

/*
 * Returns true iff this device is vulnerable to CVE-2013-2094.
 * A patch for CVE-2013-2094 can be found at
 * http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=8176cced706b5e5d15887584150764894e94e02f
 */
static jboolean android_security_cts_NativeCodeTest_doPerfEventTest(JNIEnv* env, jobject thiz)
{
    uint64_t attr[10] = { 0x4800000001, (uint32_t) -1, 0, 0, 0, 0x300 };

    int fd = syscall(__NR_perf_event_open, attr, 0, -1, -1, 0);
    jboolean result = (fd != -1);

    if (fd != -1) {
        close(fd);
    }

    return result;
}

/*
 * Detects if the following patch is present.
 * http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=c95eb3184ea1a3a2551df57190c81da695e2144b
 *
 * Returns true if the patch is applied, or crashes the system otherwise.
 *
 * While you're at it, you want to apply the following patch too.
 * http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=b88a2595b6d8aedbd275c07dfa784657b4f757eb
 * This test doesn't cover the above patch. TODO write a new test.
 *
 * Credit: https://github.com/deater/perf_event_tests/blob/master/exploits/arm_perf_exploit.c
 */
static jboolean android_security_cts_NativeCodeTest_doPerfEventTest2(JNIEnv* env, jobject thiz)
{
    struct perf_event_attr pe[2];
    int fd[2];
    memset(pe, 0, sizeof(pe));
    pe[0].type = 2;
    pe[0].config = 72;
    pe[0].size = 80;
    pe[1].type = PERF_TYPE_RAW;
    pe[1].size = 80;
    fd[0]=syscall(__NR_perf_event_open, &pe[0], 0, 0, -1, 0);
    fd[1]=syscall(__NR_perf_event_open, &pe[1], 0, 0, fd[0], 0);
    close(fd[0]);
    close(fd[1]);
    return true;
}

/*
 * Prior to https://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/arch/arm/include/asm/uaccess.h?id=8404663f81d212918ff85f493649a7991209fa04
 * there was a flaw in the kernel's handling of get_user and put_user
 * requests. Normally, get_user and put_user are supposed to guarantee
 * that reads/writes outside the process's address space are not
 * allowed.
 *
 * In this test, we use sysctl to force a read from an address outside
 * of our address space (but in the kernel's address space). Without the
 * patch applied, this read succeeds, because sysctl uses the
 * vulnerable get_user call.
 *
 * This function returns true if the patch above is applied, or false
 * otherwise.
 *
 * Credit: https://twitter.com/grsecurity/status/401443359912239105
 */
static jboolean android_security_cts_NativeCodeTest_doVrootTest(JNIEnv*, jobject)
{
#ifdef __arm__
    ALOGE("Starting doVrootTest");

    struct __sysctl_args args;
    char osname[100];
    int name[] = { CTL_KERN, KERN_OSTYPE };

    memset(&args, 0, sizeof(struct __sysctl_args));
    args.name = name;
    args.nlen = sizeof(name)/sizeof(name[0]);
    args.oldval = osname;
    args.oldlenp = (size_t *) 0xc0000000; // PAGE_OFFSET

    int result = syscall(__NR__sysctl, &args);
    return ((result == -1) && (errno == EFAULT));
#else
    return true;
#endif
}

static void* mmap_syscall(void* addr, size_t len, int prot, int flags, int fd, off_t offset)
{
#ifdef __LP64__
    return mmap(addr, len, prot, flags, fd, offset);
#else
    return (void*) syscall(__NR_mmap2, addr, len, prot, flags, fd, offset);
#endif
}

#define KBASE_REG_COOKIE_TB         2
#define KBASE_REG_COOKIE_MTP        3

/*
 * Returns true if the device is immune to CVE-2014-1710,
 * false if the device is vulnerable.
 */
static jboolean android_security_cts_NativeCodeTest_doCVE20141710Test(JNIEnv*, jobject)
{
    jboolean result = false;
    int fd = open("/dev/mali0", O_RDWR);
    if (fd < 0) {
        return true; /* not vulnerable */
    }

    void* a = mmap_syscall(NULL, 0x1000, PROT_READ, MAP_SHARED, fd, KBASE_REG_COOKIE_MTP);
    void* b = mmap_syscall(NULL, 0x1000, PROT_READ, MAP_SHARED, fd, KBASE_REG_COOKIE_TB);

    if (a == MAP_FAILED) {
        result = true; /* assume not vulnerable */
        goto done;
    }

    if (b == MAP_FAILED) {
        result = true; /* assume not vulnerable */
        goto done;
    }

    /* mprotect should return an error if not vulnerable */
    result = (mprotect(b, 0x1000, PROT_READ | PROT_WRITE) == -1);

 done:
    if (a != MAP_FAILED) {
        munmap(a, 0x1000);
    }
    if (b != MAP_FAILED) {
        munmap(b, 0x1000);
    }
    close(fd);
    return result;
}

static inline int futex_syscall(volatile int* uaddr, int op, int val, const struct timespec* ts,
                                volatile int* uaddr2, int val3) {
    return syscall(__NR_futex, uaddr, op, val, ts, uaddr2, val3);
}

/*
 * Test for vulnerability to CVE-2014-3153, a bug in the futex() syscall that can
 * lead to privilege escalation and was used by the towelroot exploit. Returns true
 * if device is patched, false if still vulnerable.
 */
static jboolean android_security_cts_NativeCodeTest_doFutexTest(JNIEnv*, jobject)
{
    jboolean result = false;

    int futex = 1;
    int ret;

    /* The patch will reject FUTEX_CMP_REQUEUE_PI calls where addr == addr2, so
     * that's what we're checking for - they're both &futex. Patched systems will
     * return -1 and set errno to 22 (EINVAL), vulnerable systems will return 0.
     */
    ret = futex_syscall(&futex, FUTEX_CMP_REQUEUE_PI, 1, NULL, &futex, 0);
    return (ret == -1 && errno == EINVAL);
}

static jboolean android_security_cts_NativeCodeTest_doNvmapIocFromIdTest(JNIEnv*, jobject)
{
    /*
     * IOCTL code specified from the original notification.
     * Also available in:
     *     .../kernel/tegra/drivers/video/tegra/nvmap/nvmap_ioctl.h
     * #define NVMAP_IOC_MAGIC 'N'
     * #define NVMAP_IOC_FROM_ID _IOWR(NVMAP_IOC_MAGIC, 2, struct nvmap_create_handle)
     */
    const int NVMAP_IOC_FROM_ID = 0xc0084e02;
    int       nvmap = open("/dev/nvmap", O_RDWR | O_CLOEXEC, 0);
    bool      vulnerable = false;

    if (nvmap >= 0) {
        if (0 == ioctl(nvmap, NVMAP_IOC_FROM_ID)) {
            /* IOCTL succeeded */
            vulnerable = true;
        }
        else if (errno != ENOTTY) {
            /* IOCTL failed, but provided the wrong error number */
            vulnerable = true;
        }

        close(nvmap);
    }

    return !vulnerable;
}

static jboolean android_security_cts_NativeCodeTest_doPingPongRootTest(JNIEnv*, jobject)
{
    int icmp_sock;
    struct sockaddr sock_addr;

    memset(&sock_addr, 0, sizeof(sock_addr));
    icmp_sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_ICMP);
    sock_addr.sa_family = AF_INET;

    /* first connect */
    connect(icmp_sock, &sock_addr, sizeof(sock_addr));

    /* disconnect */
    sock_addr.sa_family = AF_UNSPEC;
    connect(icmp_sock, &sock_addr, sizeof(sock_addr));

    /* second disconnect -> crash */
    sock_addr.sa_family = AF_UNSPEC;
    connect(icmp_sock, &sock_addr, sizeof(sock_addr));

    return true;
}

static JNINativeMethod gMethods[] = {
    {  "doPerfEventTest", "()Z",
            (void *) android_security_cts_NativeCodeTest_doPerfEventTest },
    {  "doPerfEventTest2", "()Z",
            (void *) android_security_cts_NativeCodeTest_doPerfEventTest2 },
    {  "doVrootTest", "()Z",
            (void *) android_security_cts_NativeCodeTest_doVrootTest },
    {  "doCVE20141710Test", "()Z",
            (void *) android_security_cts_NativeCodeTest_doCVE20141710Test },
    {  "doFutexTest", "()Z",
            (void *) android_security_cts_NativeCodeTest_doFutexTest },
    {  "doNvmapIocFromIdTest", "()Z",
            (void *) android_security_cts_NativeCodeTest_doNvmapIocFromIdTest },
    {  "doPingPongRootTest", "()Z",
            (void *) android_security_cts_NativeCodeTest_doPingPongRootTest },
};

int register_android_security_cts_NativeCodeTest(JNIEnv* env)
{
    jclass clazz = env->FindClass("android/security/cts/NativeCodeTest");
    return env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(JNINativeMethod));
}

