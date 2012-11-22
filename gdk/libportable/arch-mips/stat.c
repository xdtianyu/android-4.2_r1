#include <sys/stat.h>

/* It's easy to change kernel to support stat */

struct stat_portable {
    unsigned long long  st_dev;
    unsigned char       __pad0[4];

    unsigned long       __st_ino;
    unsigned int        st_mode;
    unsigned int        st_nlink;

    unsigned long       st_uid;
    unsigned long       st_gid;

    unsigned long long  st_rdev;
    unsigned char       __pad3[4];

    long long           st_size;
    unsigned long       st_blksize;
    unsigned long long  st_blocks;

    unsigned long       st_atime;
    unsigned long       st_atime_nsec;

    unsigned long       st_mtime;
    unsigned long       st_mtime_nsec;

    unsigned long       st_ctime;
    unsigned long       st_ctime_nsec;

    unsigned long long  st_ino;
};

/*
The MIPS Version is
struct stat {
    unsigned long       st_dev;
    unsigned long       __pad0[3];

    unsigned long long  st_ino;

    unsigned int        st_mode;
    unsigned int        st_nlink;

    unsigned long       st_uid;
    unsigned long       st_gid;

    unsigned long       st_rdev;
    unsigned long       __pad1[3];

    long long           st_size;

    unsigned long       st_atime;
    unsigned long       st_atime_nsec;

    unsigned long       st_mtime;
    unsigned long       st_mtime_nsec;

    unsigned long       st_ctime;
    unsigned long       st_ctime_nsec;

    unsigned long       st_blksize;
    unsigned long       __pad2;

    unsigned long long  st_blocks;
};
*/

/* Real Stat Syscall */
extern int stat(const char *, struct stat *);

/* Note: The Portable Header will define stat to stat_portable */
int stat_portable(const char *path, struct stat_portable *s)
{
   struct stat mips_stat;
   int ret = stat(path,&mips_stat);
   s->st_dev = mips_stat.st_dev;
   s->__st_ino = mips_stat.st_ino;
   s->st_mode = mips_stat.st_mode;
   s->st_nlink = mips_stat.st_nlink;
   s->st_uid = mips_stat.st_uid;
   s->st_gid = mips_stat.st_gid;
   s->st_rdev = mips_stat.st_rdev;
   s->st_size = mips_stat.st_size;
   s->st_blksize = mips_stat.st_blksize;
   s->st_blocks = mips_stat.st_blocks;
   s->st_atime = mips_stat.st_atime;
   s->st_atime_nsec = mips_stat.st_atime_nsec;
   s->st_mtime = mips_stat.st_mtime;
   s->st_mtime_nsec = mips_stat.st_mtime_nsec;
   s->st_ctime = mips_stat.st_ctime;
   s->st_ctime_nsec = mips_stat.st_ctime_nsec;
   s->st_ino =  mips_stat.st_ino;
   return ret;
}
