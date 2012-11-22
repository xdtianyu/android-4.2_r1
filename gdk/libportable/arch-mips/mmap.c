#include <unistd.h>
#include <stdio.h>
#include <errno.h>
#include <sys/mman.h>

/* Derived from development/ndk/platforms/android-3/include/asm-generic/mman.h */
#define PROT_READ_PORTABLE     0x1
#define PROT_WRITE_PORTABLE    0x2
#define PROT_EXEC_PORTABLE     0x4
#define PROT_SEM_PORTABLE      0x8
#define PROT_NONE_PORTABLE     0x0
#define PROT_GROWSDOWN_PORTABLE        0x01000000
#define PROT_GROWSUP_PORTABLE  0x02000000

#define MAP_SHARED_PORTABLE    0x01
#define MAP_PRIVATE_PORTABLE   0x02
#define MAP_TYPE_PORTABLE      0x0f
#define MAP_FIXED_PORTABLE     0x10
#define MAP_ANONYMOUS_PORTABLE 0x20

#define MS_ASYNC_PORTABLE      1
#define MS_INVALIDATE_PORTABLE 2
#define MS_SYNC_PORTABLE       4

#define MADV_NORMAL_PORTABLE   0
#define MADV_RANDOM_PORTABLE   1
#define MADV_SEQUENTIAL_PORTABLE 2
#define MADV_WILLNEED_PORTABLE 3
#define MADV_DONTNEED_PORTABLE 4

#define MADV_REMOVE_PORTABLE   9
#define MADV_DONTFORK_PORTABLE 10
#define MADV_DOFORK_PORTABLE   11

#define MAP_ANON_PORTABLE      MAP_ANONYMOUS_PORTABLE
#define MAP_FILE_PORTABLE      0

/* Derived from development/ndk/platforms/android-3/include/asm-generic/mman.h */
#define MAP_GROWSDOWN_PORTABLE 0x0100
#define MAP_DENYWRITE_PORTABLE 0x0800
#define MAP_EXECUTABLE_PORTABLE        0x1000
#define MAP_LOCKED_PORTABLE    0x2000
#define MAP_NORESERVE_PORTABLE 0x4000
#define MAP_POPULATE_PORTABLE  0x8000
#define MAP_NONBLOCK_PORTABLE  0x10000

#define MCL_CURRENT_PORTABLE   1
#define MCL_FUTURE_PORTABLE    2


#if MAP_ANONYMOUS_PORTABLE==MAP_ANONYMOUS
#error Bad build environment
#endif

static inline int mips_change_prot(int prot)
{
    /* Only PROT_SEM is different */
    if (prot & PROT_SEM_PORTABLE) {
        prot &= ~PROT_SEM_PORTABLE;
        prot |= PROT_SEM;
    }

    return prot;
}

static inline int mips_change_flags(int flags)
{
    int mipsflags = 0;
    /* These are the documented flags for mmap */
    if (flags & MAP_SHARED_PORTABLE)
       mipsflags |= MAP_SHARED;
    if (flags & MAP_PRIVATE_PORTABLE)
       mipsflags |= MAP_PRIVATE;
#if defined(MAP_32BIT_PORTABLE) && defined(MAP_32BIT)
    if (flags & MAP_32BIT_PORTABLE)
       mipsflags |= MAP_32BIT;
#endif
    if (flags & MAP_ANONYMOUS_PORTABLE)
       mipsflags |= MAP_ANONYMOUS;
    if (flags & MAP_FIXED_PORTABLE)
       mipsflags |= MAP_FIXED;
    if (flags & MAP_GROWSDOWN_PORTABLE)
       mipsflags |= MAP_GROWSDOWN;
#if defined(MAP_HUGETLB_PORTABLE) && defined(MAP_HUGETLB)
    if (flags & MAP_HUGETLB_PORTABLE)
       mipsflags |= MAP_HUGETLB;
#endif
    if (flags & MAP_LOCKED_PORTABLE)
       mipsflags |= MAP_LOCKED;
    if (flags & MAP_NONBLOCK_PORTABLE)
       mipsflags |= MAP_NONBLOCK;
    if (flags & MAP_NORESERVE_PORTABLE)
       mipsflags |= MAP_NORESERVE;
    if (flags & MAP_POPULATE_PORTABLE)
       mipsflags |= MAP_POPULATE;
#if defined(MAP_STACK_PORTABLE) && defined(MAP_STACK)
    if (flags & MAP_STACK_PORTABLE)
       mipsflags |= MAP_STACK;
#endif

    return mipsflags;
}

#define  MMAP2_SHIFT  12
extern void *__mmap2(void *, size_t, int, int, int, size_t);
void *mmap(void *addr, size_t size, int prot, int flags, int fd, long offset)
{
    if ( offset & ((1UL << MMAP2_SHIFT)-1) ) {
        errno = EINVAL;
        return MAP_FAILED;
    }

    return __mmap2(addr, size, mips_change_prot(prot), mips_change_flags(flags),
                   fd, (size_t)offset >> MMAP2_SHIFT);
}
