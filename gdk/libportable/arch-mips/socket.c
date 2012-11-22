#include <unistd.h>
#include <sys/socket.h>
#include <sys/linux-syscalls.h>

/* From ndk/platforms/android-3/include/sys/socket.h */
#define SOCK_STREAM_PORTABLE   1
#define SOCK_DGRAM_PORTABLE    2
#define SOCK_RAW_PORTABLE      3
#define SOCK_RDM_PORTABLE      4
#define SOCK_SEQPACKET_PORTABLE        5
#define SOCK_PACKET_PORTABLE   10

#if SOCK_STREAM==SOCK_STREAM_PORTABLE
#error Bad build environment
#endif

static inline int mips_change_type(int type)
{
    switch (type) {
      case SOCK_STREAM_PORTABLE: return SOCK_STREAM;
      case SOCK_DGRAM_PORTABLE: return SOCK_DGRAM;
      case SOCK_RAW_PORTABLE: return SOCK_RAW;
      case SOCK_RDM_PORTABLE: return SOCK_RDM;
      case SOCK_SEQPACKET_PORTABLE: return SOCK_SEQPACKET;
      case SOCK_PACKET_PORTABLE: return SOCK_PACKET;
    }
    return type;
}

extern int socket(int, int, int);

int socket_portable(int domain, int type, int protocol) {
    return socket(domain, mips_change_type(type), protocol);
}
