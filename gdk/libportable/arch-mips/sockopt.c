
#include <sys/types.h>
#include <sys/socket.h>

/* Derived from android-3/arch-arm/include/asm/socket.h */

#define SOL_SOCKET_PORTABLE	1

#define SO_DEBUG_PORTABLE	1
#define SO_REUSEADDR_PORTABLE	2
#define SO_TYPE_PORTABLE	3
#define SO_ERROR_PORTABLE	4
#define SO_DONTROUTE_PORTABLE	5
#define SO_BROADCAST_PORTABLE	6
#define SO_SNDBUF_PORTABLE	7
#define SO_RCVBUF_PORTABLE	8
#define SO_SNDBUFFORCE_PORTABLE	32
#define SO_RCVBUFFORCE_PORTABLE	33
#define SO_KEEPALIVE_PORTABLE	9
#define SO_OOBINLINE_PORTABLE	10
#define SO_NO_CHECK_PORTABLE	11
#define SO_PRIORITY_PORTABLE	12
#define SO_LINGER_PORTABLE	13
#define SO_BSDCOMPAT_PORTABLE	14

#define SO_PASSCRED_PORTABLE	16
#define SO_PEERCRED_PORTABLE	17
#define SO_RCVLOWAT_PORTABLE	18
#define SO_SNDLOWAT_PORTABLE	19
#define SO_RCVTIMEO_PORTABLE	20
#define SO_SNDTIMEO_PORTABLE	21

#define SO_SECURITY_AUTHENTICATION_PORTABLE 22
#define SO_SECURITY_ENCRYPTION_TRANSPORT_PORTABLE 23
#define SO_SECURITY_ENCRYPTION_NETWORK_PORTABLE 24

#define SO_BINDTODEVICE_PORTABLE 25

#define SO_ATTACH_FILTER_PORTABLE 26
#define SO_DETACH_FILTER_PORTABLE 27

#define SO_PEERNAME_PORTABLE	28
#define SO_TIMESTAMP_PORTABLE	29
#define SCM_TIMESTAMP_PORTABLE SO_TIMESTAMP_PORTABLE

#define SO_ACCEPTCONN_PORTABLE	30

#define SO_PEERSEC_PORTABLE	31
#define SO_PASSSEC_PORTABLE	34

#if SOL_SOCKET_PORTABLE==SOL_SOCKET
#error Build environment
#endif

static inline int mips_change_level(int level)
{
    switch (level) {
    case SOL_SOCKET_PORTABLE:
	level = SOL_SOCKET;
	break;
    }
    return level;
}


static inline int mips_change_optname(int optname)
{
    switch (optname) {
    case SO_DEBUG_PORTABLE:
	return SO_DEBUG;
    case SO_REUSEADDR_PORTABLE:
	return SO_REUSEADDR;
    case SO_TYPE_PORTABLE:
	return SO_TYPE;
    case SO_ERROR_PORTABLE:
	return SO_ERROR;
    case SO_DONTROUTE_PORTABLE:
	return SO_DONTROUTE;
    case SO_BROADCAST_PORTABLE:
	return SO_BROADCAST;
    case SO_SNDBUF_PORTABLE:
	return SO_SNDBUF;
    case SO_RCVBUF_PORTABLE:
	return SO_RCVBUF;
    case SO_SNDBUFFORCE_PORTABLE:
	return SO_SNDBUFFORCE;
    case SO_RCVBUFFORCE_PORTABLE:
	return SO_RCVBUFFORCE;
    case SO_KEEPALIVE_PORTABLE:
	return SO_KEEPALIVE;
    case SO_OOBINLINE_PORTABLE:
	return SO_OOBINLINE;
    case SO_NO_CHECK_PORTABLE:
	return SO_NO_CHECK;
    case SO_PRIORITY_PORTABLE:
	return SO_PRIORITY;
    case SO_LINGER_PORTABLE:
	return SO_LINGER;
    case SO_BSDCOMPAT_PORTABLE:
	return SO_BSDCOMPAT;
    case SO_PASSCRED_PORTABLE:
	return SO_PASSCRED;
    case SO_PEERCRED_PORTABLE:
	return SO_PEERCRED;
    case SO_RCVLOWAT_PORTABLE:
	return SO_RCVLOWAT;
    case SO_SNDLOWAT_PORTABLE:
	return SO_SNDLOWAT;
    case SO_RCVTIMEO_PORTABLE:
	return SO_RCVTIMEO;
    case SO_SNDTIMEO_PORTABLE:
	return SO_SNDTIMEO;
    case SO_SECURITY_AUTHENTICATION_PORTABLE:
	return SO_SECURITY_AUTHENTICATION;
    case SO_SECURITY_ENCRYPTION_TRANSPORT_PORTABLE:
	return SO_SECURITY_ENCRYPTION_TRANSPORT;
    case SO_SECURITY_ENCRYPTION_NETWORK_PORTABLE:
	return SO_SECURITY_ENCRYPTION_NETWORK;
    case SO_BINDTODEVICE_PORTABLE:
	return SO_BINDTODEVICE;
    case SO_ATTACH_FILTER_PORTABLE:
	return SO_ATTACH_FILTER;
    case SO_DETACH_FILTER_PORTABLE:
	return SO_DETACH_FILTER;
    case SO_PEERNAME_PORTABLE:
	return SO_PEERNAME;
    case SO_TIMESTAMP_PORTABLE:
	return SO_TIMESTAMP;
    case SO_ACCEPTCONN_PORTABLE:
	return SO_ACCEPTCONN;
    case SO_PEERSEC_PORTABLE:
	return SO_PEERSEC;
    case SO_PASSSEC_PORTABLE:
	return SO_PASSSEC;
    }
    return optname;
}

extern int setsockopt(int, int, int, const void *, socklen_t);
int setsockopt_portable(int s, int level, int optname, const void *optval, socklen_t optlen)
{
    return setsockopt(s, mips_change_level(level), mips_change_optname(optname), optval, optlen);
}

extern int getsockopt (int, int, int, void *, socklen_t *);
int getsockopt_portable(int s, int level, int optname, void *optval, socklen_t *optlen)
{
    return getsockopt(s, mips_change_level(level), mips_change_optname(optname), optval, optlen);
}
