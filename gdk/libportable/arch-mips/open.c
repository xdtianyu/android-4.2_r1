#include <unistd.h>
#include <fcntl.h>
#include <stdarg.h>

/*
 * Although these definitions are called  *_PORTABLE
 * they are actually the ARM definitions
 */

/* Derived from development/ndk/platforms/android-3/arch-arm/include/asm/fcntl.h */
/* NB x86 does not have these and only uses the generic definitions */
#define O_DIRECTORY_PORTABLE	040000
#define O_NOFOLLOW_PORTABLE	0100000
#define O_DIRECT_PORTABLE	0200000
#define O_LARGEFILE_PORTABLE	0400000

/* Derived from development/ndk/platforms/android-3/include/asm-generic/fcntl.h */
#define O_ACCMODE_PORTABLE	00000003
#define O_RDONLY_PORTABLE	00000000
#define O_WRONLY_PORTABLE	00000001
#define O_RDWR_PORTABLE		00000002
#ifndef O_CREAT_PORTABLE
#define O_CREAT_PORTABLE	00000100
#endif
#ifndef O_EXCL_PORTABLE
#define O_EXCL_PORTABLE		00000200
#endif
#ifndef O_NOCTTY_PORTABLE
#define O_NOCTTY_PORTABLE	00000400
#endif
#ifndef O_TRUNC_PORTABLE
#define O_TRUNC_PORTABLE	00001000
#endif
#ifndef O_APPEND_PORTABLE
#define O_APPEND_PORTABLE	00002000
#endif
#ifndef O_NONBLOCK_PORTABLE
#define O_NONBLOCK_PORTABLE	00004000
#endif
#ifndef O_SYNC_PORTABLE
#define O_SYNC_PORTABLE		00010000
#endif
#ifndef FASYNC_PORTABLE
#define FASYNC_PORTABLE		00020000
#endif
#ifndef O_DIRECT_PORTABLE
#define O_DIRECT_PORTABLE	00040000
#endif
#ifndef O_LARGEFILE_PORTABLE
#define O_LARGEFILE_PORTABLE	00100000
#endif
#ifndef O_DIRECTORY_PORTABLE
#define O_DIRECTORY_PORTABLE	00200000
#endif
#ifndef O_NOFOLLOW_PORTABLE
#define O_NOFOLLOW_PORTABLE	00400000
#endif
#ifndef O_NOATIME_PORTABLE
#define O_NOATIME_PORTABLE	01000000
#endif
#ifndef O_NDELAY_PORTABLE
#define O_NDELAY_PORTABLE	O_NONBLOCK_PORTABLE
#endif

#if O_CREAT_PORTABLE==O_CREAT
#error Bad build environment
#endif

static inline int mips_change_flags(int flags)
{
    int mipsflags = flags & O_ACCMODE_PORTABLE;
    if (flags & O_CREAT_PORTABLE)
	mipsflags |= O_CREAT;
    if (flags & O_EXCL_PORTABLE)
	mipsflags |= O_EXCL;
    if (flags & O_NOCTTY_PORTABLE)
	mipsflags |= O_NOCTTY;
    if (flags & O_TRUNC_PORTABLE)
	mipsflags |= O_TRUNC;
    if (flags & O_APPEND_PORTABLE)
	mipsflags |= O_APPEND;
    if (flags & O_NONBLOCK_PORTABLE)
	mipsflags |= O_NONBLOCK;
    if (flags & O_SYNC_PORTABLE)
	mipsflags |= O_SYNC;
    if (flags & FASYNC_PORTABLE)
	mipsflags |= FASYNC;
    if (flags & O_DIRECT_PORTABLE)
	mipsflags |= O_DIRECT;
    if (flags & O_LARGEFILE_PORTABLE)
	mipsflags |= O_LARGEFILE;
    if (flags & O_DIRECTORY_PORTABLE)
	mipsflags |= O_DIRECTORY;
    if (flags & O_NOFOLLOW_PORTABLE)
	mipsflags |= O_NOFOLLOW;
    if (flags & O_NOATIME_PORTABLE)
	mipsflags |= O_NOATIME;
    if (flags & O_NDELAY_PORTABLE)
	mipsflags |= O_NDELAY;

    return mipsflags;
}

extern int  __open(const char*, int, int);
int open(const char *pathname, int flags, ...)
{
    mode_t  mode = 0;
    flags |= O_LARGEFILE;

    if (flags & O_CREAT)
    {
        va_list  args;

        va_start(args, flags);
        mode = (mode_t) va_arg(args, int);
        va_end(args);
    }

    return __open(pathname, mips_change_flags(flags), mode);
}
