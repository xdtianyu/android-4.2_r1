/*
 * Copyright (c) 1989 The Regents of the University of California.
 * All rights reserved.
 *
 * This code is derived from software contributed to Berkeley by
 * Mike Muuss.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *	This product includes software developed by the University of
 *	California, Berkeley and its contributors.
 * 4. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#ifndef lint
char copyright[] =
"@(#) Copyright (c) 1989 The Regents of the University of California.\n\
 All rights reserved.\n";
#endif /* not lint */

/*
 *			P I N G . C
 *
 * Using the InterNet Control Message Protocol (ICMP) "ECHO" facility,
 * measure round-trip-delays and packet loss across network paths.
 *
 * Author -
 *	Mike Muuss
 *	U. S. Army Ballistic Research Laboratory
 *	December, 1983
 *
 * Status -
 *	Public Domain.  Distribution Unlimited.
 * Bugs -
 *	More statistics could always be gathered.
 *	This program has to run SUID to ROOT to access the ICMP socket.
 */

#include "ping_common.h"

#include <netinet/ip.h>
#include <linux/icmp.h>
#include <sched.h>
#include <sys/types.h>
#include <private/android_filesystem_config.h>

#define bzero(b,sz) memset(b, 0, sz)

/* PING COMMON */

int options;

int sndbuf;
int ttl;
int rtt;
int rtt_addend;
__u16 acked;

int mx_dup_ck = MAX_DUP_CHK;
char rcvd_tbl[MAX_DUP_CHK / 8];


/* counters */
long npackets;			/* max packets to transmit */
long nreceived;			/* # of packets we got back */
long nrepeats;			/* number of duplicates */
long ntransmitted;		/* sequence # for outbound packets = #sent */
long nchecksum;			/* replies with bad checksum */
long nerrors;			/* icmp errors */
int interval = 1000;		/* interval between packets (msec) */
int preload;
int deadline = 0;		/* time to die */
int lingertime = MAXWAIT*1000;
struct timeval start_time, cur_time;
volatile int exiting;
volatile int status_snapshot;
int confirm = 0;

/* Stupid workarounds for bugs/missing functionality in older linuces.
 * confirm_flag fixes refusing service of kernels without MSG_CONFIRM.
 * i.e. for linux-2.2 */
int confirm_flag = MSG_CONFIRM;
/* And this is workaround for bug in IP_RECVERR on raw sockets which is present
 * in linux-2.2.[0-19], linux-2.4.[0-7] */
int working_recverr;

/* timing */
int timing;			/* flag to do timing */
long tmin = LONG_MAX;		/* minimum round trip time */
long tmax;			/* maximum round trip time */
/* Message for rpm maintainers: have _shame_. If you want
 * to fix something send the patch to me for sanity checking.
 * "sparcfix" patch is a complete non-sense, apparenly the person
 * prepared it was stoned.
 */
long long tsum;			/* sum of all times, for doing average */
long long tsum2;
int  pipesize = -1;

int datalen = DEFDATALEN;

char *hostname;
int uid;
int ident;			/* process id to identify our packets */

static int screen_width = INT_MAX;

/* Fills all the outpack, excluding ICMP header, but _including_
 * timestamp area with supplied pattern.
 */
static void fill(char *patp)
{
	int ii, jj, kk;
	int pat[16];
	char *cp;
	char *bp = outpack+8;

	for (cp = patp; *cp; cp++) {
		if (!isxdigit(*cp)) {
			fprintf(stderr,
				"ping: patterns must be specified as hex digits.\n");
			exit(2);
		}
	}
	ii = sscanf(patp,
	    "%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x",
	    &pat[0], &pat[1], &pat[2], &pat[3], &pat[4], &pat[5], &pat[6],
	    &pat[7], &pat[8], &pat[9], &pat[10], &pat[11], &pat[12],
	    &pat[13], &pat[14], &pat[15]);

	if (ii > 0) {
		for (kk = 0; kk <= maxpacket - (8 + ii); kk += ii)
			for (jj = 0; jj < ii; ++jj)
				bp[jj + kk] = pat[jj];
	}
	if (!(options & F_QUIET)) {
		printf("PATTERN: 0x");
		for (jj = 0; jj < ii; ++jj)
			printf("%02x", bp[jj] & 0xFF);
		printf("\n");
	}
}

void common_options(int ch)
{
	switch(ch) {
	case 'a':
		options |= F_AUDIBLE;
		break;
	case 'A':
		options |= F_ADAPTIVE;
		break;
	case 'c':
		npackets = atoi(optarg);
		if (npackets <= 0) {
			fprintf(stderr, "ping: bad number of packets to transmit.\n");
			exit(2);
		}
		break;
	case 'd':
		options |= F_SO_DEBUG;
		break;
	case 'f':
		options |= F_FLOOD;
		//setbuf(stdout, (char *)NULL);
		break;
	case 'i':		/* wait between sending packets */
	{
		if (strchr(optarg, '.')) {
			float t;
			if (sscanf(optarg, "%f", &t) != 1) {
				fprintf(stderr, "ping: bad timing interval.\n");
				exit(2);
			}
			interval = (int)(t*1000);
		} else if (sscanf(optarg, "%d", &interval) == 1) {
			interval *= 1000;
		} else {
			fprintf(stderr, "ping: bad timing interval.\n");
			exit(2);
		}

		if (interval < 0) {
			fprintf(stderr, "ping: bad timing interval.\n");
			exit(2);
		}
		options |= F_INTERVAL;
		break;
	}
	case 'w':
		deadline = atoi(optarg);
		if (deadline < 0) {
			fprintf(stderr, "ping: bad wait time.\n");
			exit(2);
		}
		break;
	case 'l':
		preload = atoi(optarg);
		if (preload <= 0) {
			fprintf(stderr, "ping: bad preload value, should be 1..%d\n", mx_dup_ck);
			exit(2);
		}
		if (preload > mx_dup_ck)
			preload = mx_dup_ck;
		if (uid && preload > 3) {
			fprintf(stderr, "ping: cannot set preload to value > 3\n");
			exit(2);
		}
		break;
	case 'S':
		sndbuf = atoi(optarg);
		if (sndbuf <= 0) {
			fprintf(stderr, "ping: bad sndbuf value.\n");
			exit(2);
		}
		break;
	case 'n':
		options |= F_NUMERIC;
		break;
	case 'p':		/* fill buffer with user pattern */
		options |= F_PINGFILLED;
		fill(optarg);
		break;
	case 'q':
		options |= F_QUIET;
		break;
	case 'r':
		options |= F_SO_DONTROUTE;
		break;
	case 's':		/* size of packet to send */
		datalen = atoi(optarg);
		if (datalen < 0) {
			fprintf(stderr, "ping: illegal negative packet size %d.\n", datalen);
			exit(2);
		}
		break;
	case 'v':
		options |= F_VERBOSE;
		break;
	case 'L':
		options |= F_NOLOOP;
		break;
	case 't':
		options |= F_TTL;
		ttl = atoi(optarg);
		if (ttl < 0 || ttl > 255) {
			fprintf(stderr, "ping: ttl %u out of range\n", ttl);
			exit(2);
		}
		break;
	case 'U':
		options |= F_LATENCY;
		break;
	case 'B':
		options |= F_STRICTSOURCE;
		break;
	case 'W':
		lingertime = atoi(optarg);
		if (lingertime < 0 || lingertime > INT_MAX/1000000) {
			fprintf(stderr, "ping: bad linger time.\n");
			exit(2);
		}
		lingertime *= 1000;
		break;
	case 'V':
		printf("ping utility, iputils-ss\n");
		exit(0);
	default:
		abort();
	}
}


static void sigexit(int signo)
{
	exiting = 1;
}

static void sigstatus(int signo)
{
	status_snapshot = 1;
}


int __schedule_exit(int next)
{
	static unsigned long waittime;
	struct itimerval it;

	if (waittime)
		return next;

	if (nreceived) {
		waittime = 2 * tmax;
		if (waittime < 1000*interval)
			waittime = 1000*interval;
	} else
		waittime = lingertime*1000;

	if (next < 0 || next < waittime/1000)
		next = waittime/1000;

	it.it_interval.tv_sec = 0;
	it.it_interval.tv_usec = 0;
	it.it_value.tv_sec = waittime/1000000;
	it.it_value.tv_usec = waittime%1000000;
	setitimer(ITIMER_REAL, &it, NULL);
	return next;
}

static inline void update_interval(void)
{
	int est = rtt ? rtt/8 : interval*1000; 

	interval = (est+rtt_addend+500)/1000;
	if (uid && interval < MINUSERINTERVAL)
		interval = MINUSERINTERVAL;
}

/*
 * pinger --
 * 	Compose and transmit an ICMP ECHO REQUEST packet.  The IP packet
 * will be added on by the kernel.  The ID field is our UNIX process ID,
 * and the sequence number is an ascending integer.  The first 8 bytes
 * of the data portion are used to hold a UNIX "timeval" struct in VAX
 * byte-order, to compute the round-trip time.
 */
int pinger(void)
{
	static int oom_count;
	static int tokens;
	int i;

	/* Have we already sent enough? If we have, return an arbitrary positive value. */ 
	if (exiting || (npackets && ntransmitted >= npackets && !deadline))
		return 1000;

	/* Check that packets < rate*time + preload */
	if (cur_time.tv_sec == 0) {
		gettimeofday(&cur_time, NULL);
		tokens = interval*(preload-1);
	} else {
		long ntokens;
		struct timeval tv;

		gettimeofday(&tv, NULL);
		ntokens = (tv.tv_sec - cur_time.tv_sec)*1000 +
			(tv.tv_usec-cur_time.tv_usec)/1000;
		if (!interval) {
			/* Case of unlimited flood is special;
			 * if we see no reply, they are limited to 100pps */
			if (ntokens < MININTERVAL && in_flight() >= preload)
				return MININTERVAL-ntokens;
		}
		ntokens += tokens;
		if (ntokens > interval*preload)
			ntokens = interval*preload;
		if (ntokens < interval)
			return interval - ntokens;

		cur_time = tv;
		tokens = ntokens - interval;
	}

resend:
	i = send_probe();

	if (i == 0) {
		oom_count = 0;
		advance_ntransmitted();
		if (!(options & F_QUIET) && (options & F_FLOOD)) {
			/* Very silly, but without this output with
			 * high preload or pipe size is very confusing. */
			if ((preload < screen_width && pipesize < screen_width) ||
			    in_flight() < screen_width)
				write(STDOUT_FILENO, ".", 1);
		}
		return interval - tokens;
	}

	/* And handle various errors... */
	if (i > 0) {
		/* Apparently, it is some fatal bug. */
		abort();
	} else if (errno == ENOBUFS || errno == ENOMEM) {
		int nores_interval;

		/* Device queue overflow or OOM. Packet is not sent. */
		tokens = 0;
		/* Slowdown. This works only in adaptive mode (option -A) */
		rtt_addend += (rtt < 8*50000 ? rtt/8 : 50000);
		if (options&F_ADAPTIVE)
			update_interval();
		nores_interval = SCHINT(interval/2);
		if (nores_interval > 500)
			nores_interval = 500;
		oom_count++;
		if (oom_count*nores_interval < lingertime)
			return nores_interval;
		i = 0;
		/* Fall to hard error. It is to avoid complete deadlock
		 * on stuck output device even when dealine was not requested.
		 * Expected timings are screwed up in any case, but we will
		 * exit some day. :-) */
	} else if (errno == EAGAIN) {
		/* Socket buffer is full. */
		tokens += interval;
		return MININTERVAL;
	} else {
		if ((i=receive_error_msg()) > 0) {
			/* An ICMP error arrived. */
			tokens += interval;
			return MININTERVAL;
		}
		/* Compatibility with old linuces. */
		if (i == 0 && confirm_flag && errno == EINVAL) {
			confirm_flag = 0;
			errno = 0;
		}
		if (!errno)
			goto resend;
	}

	/* Hard local error. Pretend we sent packet. */
	advance_ntransmitted();

	if (i == 0 && !(options & F_QUIET)) {
		if (options & F_FLOOD)
			write(STDOUT_FILENO, "E", 1);
		else
			perror("ping: sendmsg");
	}
	tokens = 0;
	return SCHINT(interval);
}

/* Set socket buffers, "alloc" is an estimate of memory taken by single packet. */

void sock_setbufs(int icmp_sock, int alloc)
{
	int rcvbuf, hold;
	int tmplen = sizeof(hold);

	if (!sndbuf)
		sndbuf = alloc;
	setsockopt(icmp_sock, SOL_SOCKET, SO_SNDBUF, (char *)&sndbuf, sizeof(sndbuf));

	rcvbuf = hold = alloc * preload;
	if (hold < 65536)
		hold = 65536;
	setsockopt(icmp_sock, SOL_SOCKET, SO_RCVBUF, (char *)&hold, sizeof(hold));
	if (getsockopt(icmp_sock, SOL_SOCKET, SO_RCVBUF, (char *)&hold, &tmplen) == 0) {
		if (hold < rcvbuf)
			fprintf(stderr, "WARNING: probably, rcvbuf is not enough to hold preload.\n");
	}
}

/* Protocol independent setup and parameter checks. */

void setup(int icmp_sock)
{
	int hold;
	struct timeval tv;

	if ((options & F_FLOOD) && !(options & F_INTERVAL))
		interval = 0;

	if (uid && interval < MINUSERINTERVAL) {
		fprintf(stderr, "ping: cannot flood; minimal interval, allowed for user, is %dms\n", MINUSERINTERVAL);
		exit(2);
	}

	if (interval >= INT_MAX/preload) {
		fprintf(stderr, "ping: illegal preload and/or interval\n");
		exit(2);
	}

	hold = 1;
	if (options & F_SO_DEBUG)
		setsockopt(icmp_sock, SOL_SOCKET, SO_DEBUG, (char *)&hold, sizeof(hold));
	if (options & F_SO_DONTROUTE)
		setsockopt(icmp_sock, SOL_SOCKET, SO_DONTROUTE, (char *)&hold, sizeof(hold));

#ifdef SO_TIMESTAMP
	if (!(options&F_LATENCY)) {
		int on = 1;
		if (setsockopt(icmp_sock, SOL_SOCKET, SO_TIMESTAMP, &on, sizeof(on)))
			fprintf(stderr, "Warning: no SO_TIMESTAMP support, falling back to SIOCGSTAMP\n");
	}
#endif

	/* Set some SNDTIMEO to prevent blocking forever
	 * on sends, when device is too slow or stalls. Just put limit
	 * of one second, or "interval", if it is less.
	 */
	tv.tv_sec = 1;
	tv.tv_usec = 0;
	if (interval < 1000) {
		tv.tv_sec = 0;
		tv.tv_usec = 1000 * SCHINT(interval);
	}
	setsockopt(icmp_sock, SOL_SOCKET, SO_SNDTIMEO, (char*)&tv, sizeof(tv));

	/* Set RCVTIMEO to "interval". Note, it is just an optimization
	 * allowing to avoid redundant poll(). */
	tv.tv_sec = SCHINT(interval)/1000;
	tv.tv_usec = 1000*(SCHINT(interval)%1000);
	if (setsockopt(icmp_sock, SOL_SOCKET, SO_RCVTIMEO, (char*)&tv, sizeof(tv)))
		options |= F_FLOOD_POLL;

	if (!(options & F_PINGFILLED)) {
		int i;
		char *p = outpack+8;

		/* Do not forget about case of small datalen,
		 * fill timestamp area too!
		 */
		for (i = 0; i < datalen; ++i)
			*p++ = i;
	}

	ident = getpid() & 0xFFFF;

	set_signal(SIGINT, sigexit);
	set_signal(SIGALRM, sigexit);
	set_signal(SIGQUIT, sigstatus);

	gettimeofday(&start_time, NULL);

	if (deadline) {
		struct itimerval it;

		it.it_interval.tv_sec = 0;
		it.it_interval.tv_usec = 0;
		it.it_value.tv_sec = deadline;
		it.it_value.tv_usec = 0;
		setitimer(ITIMER_REAL, &it, NULL);
	}

#if 0
	if (isatty(STDOUT_FILENO)) {
		struct winsize w;

		if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &w) != -1) {
			if (w.ws_col > 0)
				screen_width = w.ws_col;
		}
	}
#endif
}

void main_loop(int icmp_sock, __u8 *packet, int packlen)
{
	char addrbuf[128];
	char ans_data[4096];
	struct iovec iov;
	struct msghdr msg;
	struct cmsghdr *c;
	int cc;
	int next;
	int polling;

	iov.iov_base = (char *)packet;

	for (;;) {
		/* Check exit conditions. */
		if (exiting)
			break;
		if (npackets && nreceived + nerrors >= npackets)
			break;
		if (deadline && nerrors)
			break;
		/* Check for and do special actions. */
		if (status_snapshot)
			status();

		/* Send probes scheduled to this time. */
		do {
			next = pinger();
			next = schedule_exit(next);
		} while (next <= 0);

		/* "next" is time to send next probe, if positive.
		 * If next<=0 send now or as soon as possible. */

		/* Technical part. Looks wicked. Could be dropped,
		 * if everyone used the newest kernel. :-) 
		 * Its purpose is:
		 * 1. Provide intervals less than resolution of scheduler.
		 *    Solution: spinning.
		 * 2. Avoid use of poll(), when recvmsg() can provide
		 *    timed waiting (SO_RCVTIMEO). */
		polling = 0;
		if ((options & (F_ADAPTIVE|F_FLOOD_POLL)) || next<SCHINT(interval)) {
			int recv_expected = in_flight();

			/* If we are here, recvmsg() is unable to wait for
			 * required timeout. */ 
			if (1000*next <= 1000000/(int)HZ) {
				/* Very short timeout... So, if we wait for
				 * something, we sleep for MININTERVAL.
				 * Otherwise, spin! */
				if (recv_expected) {
					next = MININTERVAL;
				} else {
					next = 0;
					/* When spinning, no reasons to poll.
					 * Use nonblocking recvmsg() instead. */
					polling = MSG_DONTWAIT;
					/* But yield yet. */
					sched_yield();
				}
			}

			if (!polling &&
			    ((options & (F_ADAPTIVE|F_FLOOD_POLL)) || interval)) {
				struct pollfd pset;
				pset.fd = icmp_sock;
				pset.events = POLLIN|POLLERR;
				pset.revents = 0;
				if (poll(&pset, 1, next) < 1 ||
				    !(pset.revents&(POLLIN|POLLERR)))
					continue;
				polling = MSG_DONTWAIT;
			}
		}

		for (;;) {
			struct timeval *recv_timep = NULL;
			struct timeval recv_time;
			int not_ours = 0; /* Raw socket can receive messages
					   * destined to other running pings. */

			iov.iov_len = packlen;
			msg.msg_name = addrbuf;
			msg.msg_namelen = sizeof(addrbuf);
			msg.msg_iov = &iov;
			msg.msg_iovlen = 1;
			msg.msg_control = ans_data;
			msg.msg_controllen = sizeof(ans_data);

			cc = recvmsg(icmp_sock, &msg, polling);
			polling = MSG_DONTWAIT;

			if (cc < 0) {
				if (errno == EAGAIN || errno == EINTR)
					break;
				if (!receive_error_msg()) {
					if (errno) {
						perror("ping: recvmsg");
						break;
					}
					not_ours = 1;
				}
			} else {

#ifdef SO_TIMESTAMP
				for (c = CMSG_FIRSTHDR(&msg); c; c = CMSG_NXTHDR(&msg, c)) {
					if (c->cmsg_level != SOL_SOCKET ||
					    c->cmsg_type != SO_TIMESTAMP)
						continue;
					if (c->cmsg_len < CMSG_LEN(sizeof(struct timeval)))
						continue;
					recv_timep = (struct timeval*)CMSG_DATA(c);
				}
#endif

				if ((options&F_LATENCY) || recv_timep == NULL) {
					if ((options&F_LATENCY) ||
					    ioctl(icmp_sock, SIOCGSTAMP, &recv_time))
						gettimeofday(&recv_time, NULL);
					recv_timep = &recv_time;
				}

				not_ours = parse_reply(&msg, cc, addrbuf, recv_timep);
			}

			/* See? ... someone runs another ping on this host. */ 
			if (not_ours)
				install_filter();

			/* If nothing is in flight, "break" returns us to pinger. */
			if (in_flight() == 0)
				break;

			/* Otherwise, try to recvmsg() again. recvmsg()
			 * is nonblocking after the first iteration, so that
			 * if nothing is queued, it will receive EAGAIN
			 * and return to pinger. */
		}
	}
	finish();
}

int gather_statistics(__u8 *ptr, int cc, __u16 seq, int hops,
		      int csfailed, struct timeval *tv, char *from)
{
	int dupflag = 0;
	long triptime = 0;

	++nreceived;
	if (!csfailed)
		acknowledge(seq);

	if (timing && cc >= 8+sizeof(struct timeval)) {
		struct timeval tmp_tv;
		memcpy(&tmp_tv, ptr, sizeof(tmp_tv));

restamp:
		tvsub(tv, &tmp_tv);
		triptime = tv->tv_sec * 1000000 + tv->tv_usec;
		if (triptime < 0) {
			fprintf(stderr, "Warning: time of day goes back (%ldus), taking countermeasures.\n", triptime);
			triptime = 0;
			if (!(options & F_LATENCY)) {
				gettimeofday(tv, NULL);
				options |= F_LATENCY;
				goto restamp;
			}
		}
		if (!csfailed) {
			tsum += triptime;
			tsum2 += (long long)triptime * (long long)triptime;
			if (triptime < tmin)
				tmin = triptime;
			if (triptime > tmax)
				tmax = triptime;
			if (!rtt)
				rtt = triptime*8;
			else
				rtt += triptime-rtt/8;
			if (options&F_ADAPTIVE)
				update_interval();
		}
	}

	if (csfailed) {
		++nchecksum;
		--nreceived;
	} else if (TST(seq % mx_dup_ck)) {
		++nrepeats;
		--nreceived;
		dupflag = 1;
	} else {
		SET(seq % mx_dup_ck);
		dupflag = 0;
	}
	confirm = confirm_flag;

	if (options & F_QUIET)
		return 1;

	if (options & F_FLOOD) {
		if (!csfailed)
			write(STDOUT_FILENO, "\b \b", 3);
		else
			write(STDOUT_FILENO, "\bC", 1);
	} else {
		int i;
		__u8 *cp, *dp;
		printf("%d bytes from %s: icmp_seq=%u", cc, from, seq);

		if (hops >= 0)
			printf(" ttl=%d", hops);

		if (cc < datalen+8) {
			printf(" (truncated)\n");
			return 1;
		}
		if (timing) {
			if (triptime >= 100000)
				printf(" time=%ld ms", triptime/1000);
			else if (triptime >= 10000)
				printf(" time=%ld.%01ld ms", triptime/1000,
				       (triptime%1000)/100);
			else if (triptime >= 1000)
				printf(" time=%ld.%02ld ms", triptime/1000,
				       (triptime%1000)/10);
			else
				printf(" time=%ld.%03ld ms", triptime/1000,
				       triptime%1000);
		}
		if (dupflag)
			printf(" (DUP!)");
		if (csfailed)
			printf(" (BAD CHECKSUM!)");

		/* check the data */
		cp = ((u_char*)ptr) + sizeof(struct timeval);
		dp = &outpack[8 + sizeof(struct timeval)];
		for (i = sizeof(struct timeval); i < datalen; ++i, ++cp, ++dp) {
			if (*cp != *dp) {
				printf("\nwrong data byte #%d should be 0x%x but was 0x%x",
				       i, *dp, *cp);
				cp = (u_char*)ptr + sizeof(struct timeval);
				for (i = sizeof(struct timeval); i < datalen; ++i, ++cp) {
					if ((i % 32) == sizeof(struct timeval))
						printf("\n#%d\t", i);
					printf("%x ", *cp);
				}
				break;
			}
		}
	}
	return 0;
}

static long llsqrt(long long a)
{
	long long prev = ~((long long)1 << 63);
	long long x = a;

	if (x > 0) {
		while (x < prev) {
			prev = x;
			x = (x+(a/x))/2;
		}
	}

	return (long)x;
}

/*
 * finish --
 *	Print out statistics, and give up.
 */
void finish(void)
{
	struct timeval tv = cur_time;

	tvsub(&tv, &start_time);

	putchar('\n');
	fflush(stdout);
	printf("--- %s ping statistics ---\n", hostname);
	printf("%ld packets transmitted, ", ntransmitted);
	printf("%ld received", nreceived);
	if (nrepeats)
		printf(", +%ld duplicates", nrepeats);
	if (nchecksum)
		printf(", +%ld corrupted", nchecksum);
	if (nerrors)
		printf(", +%ld errors", nerrors);
	if (ntransmitted) {
		printf(", %d%% packet loss",
		       (int) ((((long long)(ntransmitted - nreceived)) * 100) /
			      ntransmitted));
		printf(", time %ldms", 1000*tv.tv_sec+tv.tv_usec/1000);
	}
	putchar('\n');

	if (nreceived && timing) {
		long tmdev;

		tsum /= nreceived + nrepeats;
		tsum2 /= nreceived + nrepeats;
		tmdev = llsqrt(tsum2 - tsum * tsum);

		printf("rtt min/avg/max/mdev = %ld.%03ld/%lu.%03ld/%ld.%03ld/%ld.%03ld ms",
		       tmin/1000, tmin%1000,
		       (unsigned long)(tsum/1000), (long)(tsum%1000),
		       tmax/1000, tmax%1000,
		       tmdev/1000, tmdev%1000
		       );
	}
	if (pipesize > 1)
		printf(", pipe %d", pipesize);
	if (ntransmitted > 1 && (!interval || (options&(F_FLOOD|F_ADAPTIVE)))) {
		int ipg = (1000000*(long long)tv.tv_sec+tv.tv_usec)/(ntransmitted-1);
		printf(", ipg/ewma %d.%03d/%d.%03d ms",
		       ipg/1000, ipg%1000, rtt/8000, (rtt/8)%1000);
	}
	putchar('\n');
	exit(!nreceived || (deadline && nreceived < npackets));
}


void status(void)
{
	int loss = 0;
	long tavg = 0;

	status_snapshot = 0;

	if (ntransmitted)
		loss = (((long long)(ntransmitted - nreceived)) * 100) / ntransmitted;

	fprintf(stderr, "\r%ld/%ld packets, %d%% loss", ntransmitted, nreceived, loss);

	if (nreceived && timing) {
		tavg = tsum / (nreceived + nrepeats);

		fprintf(stderr, ", min/avg/ewma/max = %ld.%03ld/%lu.%03ld/%d.%03d/%ld.%03ld ms",
		       tmin/1000, tmin%1000,
		       tavg/1000, tavg%1000,
		       rtt/8000, (rtt/8)%1000,
		       tmax/1000, tmax%1000
		       );
	}
	fprintf(stderr, "\n");
}

/* PING COMMON */

#define	MAXIPLEN	60
#define	MAXICMPLEN	76
#define	NROUTES		9		/* number of record route slots */
#define TOS_MAX		255		/* 8-bit TOS field */


static int ts_type;
static int nroute = 0;
static __u32 route[10];



struct sockaddr_in whereto;	/* who to ping */
int optlen = 0;
int settos = 0;			/* Set TOS, Precendence or other QOS options */
int icmp_sock;			/* socket file descriptor */
u_char outpack[0x10000];
int maxpacket = sizeof(outpack);

static int broadcast_pings = 0;

static char *pr_addr(__u32);
static void pr_options(unsigned char * cp, int hlen);
static void pr_iph(struct iphdr *ip);
static void usage(void) __attribute__((noreturn));
static u_short in_cksum(const u_short *addr, int len, u_short salt);
static void pr_icmph(__u8 type, __u8 code, __u32 info, struct icmphdr *icp);
static int parsetos(char *str);

static struct {
	struct cmsghdr cm;
	struct in_pktinfo ipi;
} cmsg = { {sizeof(struct cmsghdr) + sizeof(struct in_pktinfo), SOL_IP, IP_PKTINFO},
	   {0, }};
int cmsg_len;

struct sockaddr_in source;
char *device;
int pmtudisc = -1;

int receive_error_msg()
{
	int res;
	char cbuf[512];
	struct iovec  iov;
	struct msghdr msg;
	struct cmsghdr *cmsg;
	struct sock_extended_err *e;
	struct icmphdr icmph;
	struct sockaddr_in target;
	int net_errors = 0;
	int local_errors = 0;
	int saved_errno = errno;

	iov.iov_base = &icmph;
	iov.iov_len = sizeof(icmph);
	msg.msg_name = (void*)&target;
	msg.msg_namelen = sizeof(target);
	msg.msg_iov = &iov;
	msg.msg_iovlen = 1;
	msg.msg_flags = 0;
	msg.msg_control = cbuf;
	msg.msg_controllen = sizeof(cbuf);

	res = recvmsg(icmp_sock, &msg, MSG_ERRQUEUE|MSG_DONTWAIT);
	if (res < 0)
		goto out;

	e = NULL;
	for (cmsg = CMSG_FIRSTHDR(&msg); cmsg; cmsg = CMSG_NXTHDR(&msg, cmsg)) {
		if (cmsg->cmsg_level == SOL_IP) {
			if (cmsg->cmsg_type == IP_RECVERR)
				e = (struct sock_extended_err *)CMSG_DATA(cmsg);
		}
	}
	if (e == NULL)
		abort();

	if (e->ee_origin == SO_EE_ORIGIN_LOCAL) {
		local_errors++;
		if (options & F_QUIET)
			goto out;
		if (options & F_FLOOD)
			write(STDOUT_FILENO, "E", 1);
		else if (e->ee_errno != EMSGSIZE)
			fprintf(stderr, "ping: local error: %s\n", strerror(e->ee_errno));
		else
			fprintf(stderr, "ping: local error: Message too long, mtu=%u\n", e->ee_info);
		nerrors++;
	} else if (e->ee_origin == SO_EE_ORIGIN_ICMP) {
		struct sockaddr_in *sin = (struct sockaddr_in*)(e+1);

		if (res < sizeof(icmph) ||
		    target.sin_addr.s_addr != whereto.sin_addr.s_addr ||
		    icmph.type != ICMP_ECHO ||
		    icmph.un.echo.id != ident) {
			/* Not our error, not an error at all. Clear. */
			saved_errno = 0;
			goto out;
		}

		acknowledge(ntohs(icmph.un.echo.sequence));

		if (!working_recverr) {
			struct icmp_filter filt;
			working_recverr = 1;
			/* OK, it works. Add stronger filter. */
			filt.data = ~((1<<ICMP_SOURCE_QUENCH)|
				      (1<<ICMP_REDIRECT)|
				      (1<<ICMP_ECHOREPLY));
			if (setsockopt(icmp_sock, SOL_RAW, ICMP_FILTER, (char*)&filt, sizeof(filt)) == -1)
				perror("\rWARNING: setsockopt(ICMP_FILTER)");
		}

		net_errors++;
		nerrors++;
		if (options & F_QUIET)
			goto out;
		if (options & F_FLOOD) {
			write(STDOUT_FILENO, "\bE", 2);
		} else {
			printf("From %s icmp_seq=%u ", pr_addr(sin->sin_addr.s_addr), ntohs(icmph.un.echo.sequence));
			pr_icmph(e->ee_type, e->ee_code, e->ee_info, NULL);
			fflush(stdout);
		}
	}

out:
	errno = saved_errno;
	return net_errors ? : -local_errors;
}

/*
 * pinger --
 * 	Compose and transmit an ICMP ECHO REQUEST packet.  The IP packet
 * will be added on by the kernel.  The ID field is our UNIX process ID,
 * and the sequence number is an ascending integer.  The first 8 bytes
 * of the data portion are used to hold a UNIX "timeval" struct in VAX
 * byte-order, to compute the round-trip time.
 */
int send_probe()
{
	struct icmphdr *icp;
	int cc;
	int i;

	icp = (struct icmphdr *)outpack;
	icp->type = ICMP_ECHO;
	icp->code = 0;
	icp->checksum = 0;
	icp->un.echo.sequence = htons(ntransmitted+1);
	icp->un.echo.id = ident;			/* ID */

	CLR((ntransmitted+1) % mx_dup_ck);

	if (timing) {
		if (options&F_LATENCY) {
			static volatile int fake_fucked_egcs = sizeof(struct timeval);
			struct timeval tmp_tv;
			gettimeofday(&tmp_tv, NULL);
			/* egcs is crap or glibc is crap, but memcpy 
			   does not copy anything, if len is constant! */
			memcpy(icp+1, &tmp_tv, fake_fucked_egcs);
		} else {
			memset(icp+1, 0, sizeof(struct timeval));
		}
	}

	cc = datalen + 8;			/* skips ICMP portion */

	/* compute ICMP checksum here */
	icp->checksum = in_cksum((u_short *)icp, cc, 0);

	if (timing && !(options&F_LATENCY)) {
		static volatile int fake_fucked_egcs = sizeof(struct timeval);
	        struct timeval tmp_tv;
		gettimeofday(&tmp_tv, NULL);
		/* egcs is crap or glibc is crap, but memcpy 
		   does not copy anything, if len is constant! */
		memcpy(icp+1, &tmp_tv, fake_fucked_egcs);
		icp->checksum = in_cksum((u_short *)(icp+1), fake_fucked_egcs, ~icp->checksum);
	}

        do {
		static struct iovec iov = {outpack, 0};
		static struct msghdr m = { &whereto, sizeof(whereto),
						   &iov, 1, &cmsg, 0, 0 };
		m.msg_controllen = cmsg_len;
		iov.iov_len = cc;

		i = sendmsg(icmp_sock, &m, confirm);
		confirm = 0;
	} while (0);

	return (cc == i ? 0 : i);
}

/*
 * parse_reply --
 *	Print out the packet, if it came from us.  This logic is necessary
 * because ALL readers of the ICMP socket get a copy of ALL ICMP packets
 * which arrive ('tis only fair).  This permits multiple copies of this
 * program to be run without having intermingled output (or statistics!).
 */
int
parse_reply(struct msghdr *msg, int cc, void *addr, struct timeval *tv)
{
	struct sockaddr_in *from = addr;
	__u8 *buf = msg->msg_iov->iov_base;
	struct icmphdr *icp;
	struct iphdr *ip;
	int hlen;
	int csfailed;

	/* Check the IP header */
	ip = (struct iphdr *)buf;
	hlen = ip->ihl*4;
	if (cc < hlen + 8 || ip->ihl < 5) {
		if (options & F_VERBOSE)
			fprintf(stderr, "ping: packet too short (%d bytes) from %s\n", cc,
				pr_addr(from->sin_addr.s_addr));
		return 1;
	}

	/* Now the ICMP part */
	cc -= hlen;
	icp = (struct icmphdr *)(buf + hlen);
	csfailed = in_cksum((u_short *)icp, cc, 0);

	if (icp->type == ICMP_ECHOREPLY) {
		if (icp->un.echo.id != ident)
			return 1;			/* 'Twas not our ECHO */
		if (gather_statistics((__u8*)(icp+1), cc,
				      ntohs(icp->un.echo.sequence),
				      ip->ttl, 0, tv, pr_addr(from->sin_addr.s_addr)))
			return 0;
	} else {
		/* We fall here when a redirect or source quench arrived.
		 * Also this branch processes icmp errors, when IP_RECVERR
		 * is broken. */
		   
	        switch (icp->type) {
		case ICMP_ECHO:
			/* MUST NOT */
			return 1;
		case ICMP_SOURCE_QUENCH:
		case ICMP_REDIRECT:
		case ICMP_DEST_UNREACH:
		case ICMP_TIME_EXCEEDED:
		case ICMP_PARAMETERPROB:
			{
				struct iphdr * iph = (struct  iphdr *)(&icp[1]);
				struct icmphdr *icp1 = (struct icmphdr*)((unsigned char *)iph + iph->ihl*4);
				int error_pkt;
				if (cc < 8+sizeof(struct iphdr)+8 ||
				    cc < 8+iph->ihl*4+8)
					return 1;
				if (icp1->type != ICMP_ECHO ||
				    iph->daddr != whereto.sin_addr.s_addr ||
				    icp1->un.echo.id != ident)
					return 1;
				error_pkt = (icp->type != ICMP_REDIRECT &&
					     icp->type != ICMP_SOURCE_QUENCH);
				if (error_pkt) {
					acknowledge(ntohs(icp1->un.echo.sequence));
					if (working_recverr) {
						return 0;
					} else {
						static int once;
						/* Sigh, IP_RECVERR for raw socket
						 * was broken until 2.4.9. So, we ignore
						 * the first error and warn on the second.
						 */
						if (once++ == 1)
							fprintf(stderr, "\rWARNING: kernel is not very fresh, upgrade is recommended.\n");
						if (once == 1)
							return 0;
					}
				}
				nerrors+=error_pkt;
				if (options&F_QUIET)
					return !error_pkt;
				if (options & F_FLOOD) {
					if (error_pkt)
						write(STDOUT_FILENO, "\bE", 2);
					return !error_pkt;
				}
				printf("From %s: icmp_seq=%u ",
				       pr_addr(from->sin_addr.s_addr),
				       ntohs(icp1->un.echo.sequence));
				if (csfailed)
					printf("(BAD CHECKSUM)");
				pr_icmph(icp->type, icp->code, ntohl(icp->un.gateway), icp);
				return !error_pkt;
			}
	        default:
			/* MUST NOT */
			break;
		}
		if ((options & F_FLOOD) && !(options & (F_VERBOSE|F_QUIET))) {
			if (!csfailed)
				write(STDOUT_FILENO, "!E", 2);
			else
				write(STDOUT_FILENO, "!EC", 3);
			return 0;
		}
		if (!(options & F_VERBOSE) || uid)
			return 0;
		printf("From %s: ", pr_addr(from->sin_addr.s_addr));
		if (csfailed) {
			printf("(BAD CHECKSUM)\n");
			return 0;
		}
		pr_icmph(icp->type, icp->code, ntohl(icp->un.gateway), icp);
		return 0;
	}

	if (!(options & F_FLOOD)) {
		pr_options(buf + sizeof(struct iphdr), hlen);

		if (options & F_AUDIBLE)
			putchar('\a');
		putchar('\n');
		fflush(stdout);
	}
	return 0;
}

u_short
in_cksum(const u_short *addr, register int len, u_short csum)
{
	register int nleft = len;
	const u_short *w = addr;
	register u_short answer;
	register int sum = csum;

	/*
	 *  Our algorithm is simple, using a 32 bit accumulator (sum),
	 *  we add sequential 16 bit words to it, and at the end, fold
	 *  back all the carry bits from the top 16 bits into the lower
	 *  16 bits.
	 */
	while (nleft > 1)  {
		sum += *w++;
		nleft -= 2;
	}

	/* mop up an odd byte, if necessary */
	if (nleft == 1)
		sum += htons(*(u_char *)w << 8);

	/*
	 * add back carry outs from top 16 bits to low 16 bits
	 */
	sum = (sum >> 16) + (sum & 0xffff);	/* add hi 16 to low 16 */
	sum += (sum >> 16);			/* add carry */
	answer = ~sum;				/* truncate to 16 bits */
	return (answer);
}

/*
 * pr_icmph --
 *	Print a descriptive string about an ICMP header.
 */
void pr_icmph(__u8 type, __u8 code, __u32 info, struct icmphdr *icp)
{
	switch(type) {
	case ICMP_ECHOREPLY:
		printf("Echo Reply\n");
		/* XXX ID + Seq + Data */
		break;
	case ICMP_DEST_UNREACH:
		switch(code) {
		case ICMP_NET_UNREACH:
			printf("Destination Net Unreachable\n");
			break;
		case ICMP_HOST_UNREACH:
			printf("Destination Host Unreachable\n");
			break;
		case ICMP_PROT_UNREACH:
			printf("Destination Protocol Unreachable\n");
			break;
		case ICMP_PORT_UNREACH:
			printf("Destination Port Unreachable\n");
			break;
		case ICMP_FRAG_NEEDED:
			printf("Frag needed and DF set (mtu = %u)\n", info);
			break;
		case ICMP_SR_FAILED:
			printf("Source Route Failed\n");
			break;
		case ICMP_PKT_FILTERED:
			printf("Packet filtered\n");
			break;
		default:
			printf("Dest Unreachable, Bad Code: %d\n", code);
			break;
		}
		if (icp && (options & F_VERBOSE))
			pr_iph((struct iphdr*)(icp + 1));
		break;
	case ICMP_SOURCE_QUENCH:
		printf("Source Quench\n");
		if (icp && (options & F_VERBOSE))
			pr_iph((struct iphdr*)(icp + 1));
		break;
	case ICMP_REDIRECT:
		switch(code) {
		case ICMP_REDIR_NET:
			printf("Redirect Network");
			break;
		case ICMP_REDIR_HOST:
			printf("Redirect Host");
			break;
		case ICMP_REDIR_NETTOS:
			printf("Redirect Type of Service and Network");
			break;
		case ICMP_REDIR_HOSTTOS:
			printf("Redirect Type of Service and Host");
			break;
		default:
			printf("Redirect, Bad Code: %d", code);
			break;
		}
		if (icp)
			printf("(New nexthop: %s)\n", pr_addr(icp->un.gateway));
		if (icp && (options & F_VERBOSE))
			pr_iph((struct iphdr*)(icp + 1));
		break;
	case ICMP_ECHO:
		printf("Echo Request\n");
		/* XXX ID + Seq + Data */
		break;
	case ICMP_TIME_EXCEEDED:
		switch(code) {
		case ICMP_EXC_TTL:
			printf("Time to live exceeded\n");
			break;
		case ICMP_EXC_FRAGTIME:
			printf("Frag reassembly time exceeded\n");
			break;
		default:
			printf("Time exceeded, Bad Code: %d\n", code);
			break;
		}
		if (icp && (options & F_VERBOSE))
			pr_iph((struct iphdr*)(icp + 1));
		break;
	case ICMP_PARAMETERPROB:
		printf("Parameter problem: pointer = %u\n", icp ? (ntohl(icp->un.gateway)>>24) : info);
		if (icp && (options & F_VERBOSE))
			pr_iph((struct iphdr*)(icp + 1));
		break;
	case ICMP_TIMESTAMP:
		printf("Timestamp\n");
		/* XXX ID + Seq + 3 timestamps */
		break;
	case ICMP_TIMESTAMPREPLY:
		printf("Timestamp Reply\n");
		/* XXX ID + Seq + 3 timestamps */
		break;
	case ICMP_INFO_REQUEST:
		printf("Information Request\n");
		/* XXX ID + Seq */
		break;
	case ICMP_INFO_REPLY:
		printf("Information Reply\n");
		/* XXX ID + Seq */
		break;
#ifdef ICMP_MASKREQ
	case ICMP_MASKREQ:
		printf("Address Mask Request\n");
		break;
#endif
#ifdef ICMP_MASKREPLY
	case ICMP_MASKREPLY:
		printf("Address Mask Reply\n");
		break;
#endif
	default:
		printf("Bad ICMP type: %d\n", type);
	}
}

void pr_options(unsigned char * cp, int hlen)
{
	int i, j;
	int optlen, totlen;
	unsigned char * optptr;
	static int old_rrlen;
	static char old_rr[MAX_IPOPTLEN];

	totlen = hlen-sizeof(struct iphdr);
	optptr = cp;

	while (totlen > 0) {
		if (*optptr == IPOPT_EOL)
			break;
		if (*optptr == IPOPT_NOP) {
			totlen--;
			optptr++;
			printf("\nNOP");
			continue;
		}
		cp = optptr;
		optlen = optptr[1];
		if (optlen < 2 || optlen > totlen)
			break;

		switch (*cp) {
		case IPOPT_SSRR:
		case IPOPT_LSRR:
			printf("\n%cSRR: ", *cp==IPOPT_SSRR ? 'S' : 'L');
			j = *++cp;
			i = *++cp;
			i -= 4;
			cp++;
			if (j > IPOPT_MINOFF) {
				for (;;) {
					__u32 address;
					memcpy(&address, cp, 4);
					cp += 4;
					if (address == 0)
						printf("\t0.0.0.0");
					else
						printf("\t%s", pr_addr(address));
					j -= 4;
					putchar('\n');
					if (j <= IPOPT_MINOFF)
						break;
				}
			}
			break;
		case IPOPT_RR:
			j = *++cp;		/* get length */
			i = *++cp;		/* and pointer */
			if (i > j)
				i = j;
			i -= IPOPT_MINOFF;
			if (i <= 0)
				continue;
			if (i == old_rrlen
			    && !memcmp((char *)cp, old_rr, i)
			    && !(options & F_FLOOD)) {
				printf("\t(same route)");
				i = ((i + 3) / 4) * 4;
				cp += i;
				break;
			}
			old_rrlen = i;
			memcpy((char *)cp, old_rr, i);
			printf("\nRR: ");
			cp++;
			for (;;) {
				__u32 address;
				memcpy(&address, cp, 4);
				cp += 4;
				if (address == 0)
					printf("\t0.0.0.0");
				else
					printf("\t%s", pr_addr(address));
				i -= 4;
				putchar('\n');
				if (i <= 0)
					break;
			}
			break;
		case IPOPT_TS:
		{
			int stdtime = 0, nonstdtime = 0;
			__u8 flags;
			j = *++cp;		/* get length */
			i = *++cp;		/* and pointer */
			if (i > j)
				i = j;
			i -= 5;
			if (i <= 0)
				continue;
			flags = *++cp;
			printf("\nTS: ");
			cp++;
			for (;;) {
				long l;

				if ((flags&0xF) != IPOPT_TS_TSONLY) {
					__u32 address;
					memcpy(&address, cp, 4);
					cp += 4;
					if (address == 0)
						printf("\t0.0.0.0");
					else
						printf("\t%s", pr_addr(address));
					i -= 4;
					if (i <= 0)
						break;
				}
				l = *cp++;
				l = (l<<8) + *cp++;
				l = (l<<8) + *cp++;
				l = (l<<8) + *cp++;

				if  (l & 0x80000000) {
					if (nonstdtime==0)
						printf("\t%ld absolute not-standard", l&0x7fffffff);
					else
						printf("\t%ld not-standard", (l&0x7fffffff) - nonstdtime);
					nonstdtime = l&0x7fffffff;
				} else {
					if (stdtime==0)
						printf("\t%ld absolute", l);
					else
						printf("\t%ld", l - stdtime);
					stdtime = l;
				}
				i -= 4;
				putchar('\n');
				if (i <= 0)
					break;
			}
			if (flags>>4)
				printf("Unrecorded hops: %d\n", flags>>4);
			break;
		}
		default:
			printf("\nunknown option %x", *cp);
			break;
		}
		totlen -= optlen;
		optptr += optlen;
	}
}


/*
 * pr_iph --
 *	Print an IP header with options.
 */
void pr_iph(struct iphdr *ip)
{
	int hlen;
	u_char *cp;

	hlen = ip->ihl << 2;
	cp = (u_char *)ip + 20;		/* point to options */

	printf("Vr HL TOS  Len   ID Flg  off TTL Pro  cks      Src      Dst Data\n");
	printf(" %1x  %1x  %02x %04x %04x",
	       ip->version, ip->ihl, ip->tos, ip->tot_len, ip->id);
	printf("   %1x %04x", ((ip->frag_off) & 0xe000) >> 13,
	       (ip->frag_off) & 0x1fff);
	printf("  %02x  %02x %04x", ip->ttl, ip->protocol, ip->check);
	printf(" %s ", inet_ntoa(*(struct in_addr *)&ip->saddr));
	printf(" %s ", inet_ntoa(*(struct in_addr *)&ip->daddr));
	printf("\n");
	pr_options(cp, hlen);
}

/*
 * pr_addr --
 *	Return an ascii host address as a dotted quad and optionally with
 * a hostname.
 */
char *
pr_addr(__u32 addr)
{
	struct hostent *hp;
	static char buf[4096];

	if ((options & F_NUMERIC) ||
	    !(hp = gethostbyaddr((char *)&addr, 4, AF_INET)))
		sprintf(buf, "%s", inet_ntoa(*(struct in_addr *)&addr));
	else
		snprintf(buf, sizeof(buf), "%s (%s)", hp->h_name,
			 inet_ntoa(*(struct in_addr *)&addr));
	return(buf);
}


/* Set Type of Service (TOS) and other Quality of Service relating bits */
int parsetos(char *str)
{
        const char *cp;
        int tos;
        char *ep;

        /* handle both hex and decimal values */
        if (str[0] == '0' && (str[1] == 'x' || str[1] == 'X')) {
		cp = str + 2;
		tos = (int)strtol(cp, &ep, 16);
        } else
                tos = (int)strtol(str, &ep, 10);

        /* doesn't look like decimal or hex, eh? */
        if (*ep != '\0') {
        	fprintf(stderr, "ping: \"%s\" bad value for TOS\n", str);
        	exit(2);
        }

        if (tos > TOS_MAX) {
        	fprintf(stderr, "ping: the decimal value of TOS bits must be 0-254 (or zero)\n");
        	exit(2);
        }
	return(tos);
}

#include <linux/filter.h>

void install_filter(void)
{
	static int once;
	static struct sock_filter insns[] = {
		BPF_STMT(BPF_LDX|BPF_B|BPF_MSH, 0), /* Skip IP header. F..g BSD... Look into ping6. */
		BPF_STMT(BPF_LD|BPF_H|BPF_IND, 4), /* Load icmp echo ident */
		BPF_JUMP(BPF_JMP|BPF_JEQ|BPF_K, 0xAAAA, 0, 1), /* Ours? */
		BPF_STMT(BPF_RET|BPF_K, ~0U), /* Yes, it passes. */
		BPF_STMT(BPF_LD|BPF_B|BPF_IND, 0), /* Load icmp type */
		BPF_JUMP(BPF_JMP|BPF_JEQ|BPF_K, ICMP_ECHOREPLY, 1, 0), /* Echo? */
		BPF_STMT(BPF_RET|BPF_K, 0xFFFFFFF), /* No. It passes. */
		BPF_STMT(BPF_RET|BPF_K, 0) /* Echo with wrong ident. Reject. */
	};
	static struct sock_fprog filter = {
		sizeof insns / sizeof(insns[0]),
		insns
	};

	if (once)
		return;
	once = 1;

	/* Patch bpflet for current identifier. */
	insns[2] = (struct sock_filter)BPF_JUMP(BPF_JMP|BPF_JEQ|BPF_K, __constant_htons(ident), 0, 1);

	if (setsockopt(icmp_sock, SOL_SOCKET, SO_ATTACH_FILTER, &filter, sizeof(filter)))
		perror("WARNING: failed to install socket filter\n");
}


void usage(void)
{
	fprintf(stderr,
"Usage: ping [-LRUbdfnqrvVaA] [-c count] [-i interval] [-w deadline]\n"
"            [-p pattern] [-s packetsize] [-t ttl] [-I interface or address]\n"
"            [-M mtu discovery hint] [-S sndbuf]\n"
"            [ -T timestamp option ] [ -Q tos ] [hop1 ...] destination\n");
	exit(2);
}

int isInSupplementaryGroup(gid_t group) {
	long ngroups_max;
	gid_t empty[0];
	gid_t *groups;
	int ngroups;
	int i;

	if (getuid() == 0) {
		// root is presumed to be in every group
		return 1;
	}

	ngroups = getgroups(0, empty);
	if (ngroups < 0) {
		perror("ping: call to getgroups for sizing failed");
		exit(2);
	}
	groups = (gid_t *) malloc((ngroups * sizeof(gid_t)));
	if (groups == NULL) {
		fprintf(stderr, "ping: unable to allocate memory for %d groups.  Aborting\n", ngroups);
		exit(2);
	}
	ngroups = getgroups(ngroups, groups);
	if (ngroups < 0) {
		perror("ping: getgroups failed");
		exit(2);
	}

	for (i = 0; i < ngroups; i++) {
		if (group == groups[i]) {
			free(groups);
			return 1;
		}
	}

	free(groups);
	return 0;
}

int main(int argc, char *argv[])
{
	struct hostent *hp;
	int ch, hold, packlen;
	int socket_errno;
	u_char *packet;
	char *target, hnamebuf[MAXHOSTNAMELEN];
	char rspace[3 + 4 * NROUTES + 1];	/* record route space */

	icmp_sock = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);
	socket_errno = errno;

	/* if we were setuid root, undo that */
	if (setuid(getuid())) return -1;

	if (!isInSupplementaryGroup(AID_INET)) {
		fprintf(stderr, "You must have internet permissions to use ping.  Aborting.\n");
		exit(2);
	}

	source.sin_family = AF_INET;

	preload = 1;
	while ((ch = getopt(argc, argv, COMMON_OPTSTR "bRT:")) != EOF) {
		switch(ch) {
		case 'b':
		        broadcast_pings = 1;
			break;
		case 'Q':
			settos = parsetos(optarg);
			if (settos &&
			    (setsockopt(icmp_sock, IPPROTO_IP, IP_TOS,
					(char *)&settos, sizeof(int)) < 0)) {
				perror("ping: error setting QOS sockopts");
				exit(2);
			}
			break;
		case 'R':
			if (options & F_TIMESTAMP) {
				fprintf(stderr, "Only one of -T or -R may be used\n");
				exit(2);
			}
			options |= F_RROUTE;
			break;
		case 'T':
			if (options & F_RROUTE) {
				fprintf(stderr, "Only one of -T or -R may be used\n");
				exit(2);
			}
			options |= F_TIMESTAMP;
			if (strcmp(optarg, "tsonly") == 0)
				ts_type = IPOPT_TS_TSONLY;
			else if (strcmp(optarg, "tsandaddr") == 0)
				ts_type = IPOPT_TS_TSANDADDR;
			else if (strcmp(optarg, "tsprespec") == 0)
				ts_type = IPOPT_TS_PRESPEC;
			else {
				fprintf(stderr, "Invalid timestamp type\n");
				exit(2);
			}
			break;
		case 'I':
		{
			char dummy;
			int i1, i2, i3, i4;

			if (sscanf(optarg, "%u.%u.%u.%u%c",
				   &i1, &i2, &i3, &i4, &dummy) == 4) {
				__u8 *ptr;
				ptr = (__u8*)&source.sin_addr;
				ptr[0] = i1;
				ptr[1] = i2;
				ptr[2] = i3;
				ptr[3] = i4;
				options |= F_STRICTSOURCE;
			} else {
				device = optarg;
			}
			break;
		}
		case 'M':
			if (strcmp(optarg, "do") == 0)
				pmtudisc = IP_PMTUDISC_DO;
			else if (strcmp(optarg, "dont") == 0)
				pmtudisc = IP_PMTUDISC_DONT;
			else if (strcmp(optarg, "want") == 0)
				pmtudisc = IP_PMTUDISC_WANT;
			else {
				fprintf(stderr, "ping: wrong value for -M: do, dont, want are valid ones.\n");
				exit(2);
			}
			break;
		case 'V':
			printf("ping utility, iputils-ss\n");
			exit(0);
		COMMON_OPTIONS
			common_options(ch);
			break;
		default:
			usage();
		}
	}
	argc -= optind;
	argv += optind;

	if (argc == 0) 
		usage();
	if (argc > 1) {
		if (options & F_RROUTE)
			usage();
		else if (options & F_TIMESTAMP) {
			if (ts_type != IPOPT_TS_PRESPEC)
				usage();
			if (argc > 5)
				usage();
		} else {
			if (argc > 10)
				usage();
			options |= F_SOURCEROUTE;
		}
	}
	while (argc > 0) {
		target = *argv;

		bzero((char *)&whereto, sizeof(whereto));
		whereto.sin_family = AF_INET;
		if (inet_aton(target, &whereto.sin_addr) == 1) {
			hostname = target;
			if (argc == 1)
				options |= F_NUMERIC;
		} else {
			hp = gethostbyname(target);
			hp = gethostbyname(target);
			hp = gethostbyname(target);
			if (!hp) {
				fprintf(stderr, "ping: unknown host %s\n", target);
				exit(2);
			}
			memcpy(&whereto.sin_addr, hp->h_addr, 4);
			strncpy(hnamebuf, hp->h_name, sizeof(hnamebuf) - 1);
			hnamebuf[sizeof(hnamebuf) - 1] = 0;
			hostname = hnamebuf;
		}
		if (argc > 1)
			route[nroute++] = whereto.sin_addr.s_addr;
		argc--;
		argv++;
	}

	if (source.sin_addr.s_addr == 0) {
		int alen;
		struct sockaddr_in dst = whereto;
		int probe_fd = socket(AF_INET, SOCK_DGRAM, 0);

		if (probe_fd < 0) {
			perror("socket");
			exit(2);
		}
		if (device) {
			struct ifreq ifr;
			memset(&ifr, 0, sizeof(ifr));
			strncpy(ifr.ifr_name, device, IFNAMSIZ-1);
			if (setsockopt(probe_fd, SOL_SOCKET, SO_BINDTODEVICE, device, strlen(device)+1) == -1) {
				if (IN_MULTICAST(ntohl(dst.sin_addr.s_addr))) {
					struct ip_mreqn imr;
					if (ioctl(probe_fd, SIOCGIFINDEX, &ifr) < 0) {
						fprintf(stderr, "ping: unknown iface %s\n", device);
						exit(2);
					}
					memset(&imr, 0, sizeof(imr));
					imr.imr_ifindex = ifr.ifr_ifindex;
					if (setsockopt(probe_fd, SOL_IP, IP_MULTICAST_IF, &imr, sizeof(imr)) == -1) {
						perror("ping: IP_MULTICAST_IF");
						exit(2);
					}
				}
			}
		}

		if (settos &&
		    setsockopt(probe_fd, IPPROTO_IP, IP_TOS, (char *)&settos, sizeof(int)) < 0)
			perror("Warning: error setting QOS sockopts");

		dst.sin_port = htons(1025);
		if (nroute)
			dst.sin_addr.s_addr = route[0];
		if (connect(probe_fd, (struct sockaddr*)&dst, sizeof(dst)) == -1) {
			if (errno == EACCES) {
				if (broadcast_pings == 0) {
					fprintf(stderr, "Do you want to ping broadcast? Then -b\n");
					exit(2);
				}
				fprintf(stderr, "WARNING: pinging broadcast address\n");
				if (setsockopt(probe_fd, SOL_SOCKET, SO_BROADCAST,
					       &broadcast_pings, sizeof(broadcast_pings)) < 0) {
					perror ("can't set broadcasting");
					exit(2);
				}
				if (connect(probe_fd, (struct sockaddr*)&dst, sizeof(dst)) == -1) {
					perror("connect");
					exit(2);
				}
			} else {
				perror("connect");
				exit(2);
			}
		}
		alen = sizeof(source);
		if (getsockname(probe_fd, (struct sockaddr*)&source, &alen) == -1) {
			perror("getsockname");
			exit(2);
		}
		source.sin_port = 0;
		close(probe_fd);
	} while (0);

	if (whereto.sin_addr.s_addr == 0)
		whereto.sin_addr.s_addr = source.sin_addr.s_addr;

	if (icmp_sock < 0) {
		errno = socket_errno;
		perror("ping: icmp open socket");
		exit(2);
	}

	if (device) {
		struct ifreq ifr;

		memset(&ifr, 0, sizeof(ifr));
		strncpy(ifr.ifr_name, device, IFNAMSIZ-1);
		if (ioctl(icmp_sock, SIOCGIFINDEX, &ifr) < 0) {
			fprintf(stderr, "ping: unknown iface %s\n", device);
			exit(2);
		}
		cmsg.ipi.ipi_ifindex = ifr.ifr_ifindex;
		cmsg_len = sizeof(cmsg);
	}

	if (broadcast_pings || IN_MULTICAST(ntohl(whereto.sin_addr.s_addr))) {
		if (uid) {
			if (interval < 1000) {
				fprintf(stderr, "ping: broadcast ping with too short interval.\n");
				exit(2);
			}
			if (pmtudisc >= 0 && pmtudisc != IP_PMTUDISC_DO) {
				fprintf(stderr, "ping: broadcast ping does not fragment.\n");
				exit(2);
			}
		}
		if (pmtudisc < 0)
			pmtudisc = IP_PMTUDISC_DO;
	}

	if (pmtudisc >= 0) {
		if (setsockopt(icmp_sock, SOL_IP, IP_MTU_DISCOVER, &pmtudisc, sizeof(pmtudisc)) == -1) {
			perror("ping: IP_MTU_DISCOVER");
			exit(2);
		}
	}

	if ((options&F_STRICTSOURCE) &&
	    bind(icmp_sock, (struct sockaddr*)&source, sizeof(source)) == -1) {
		perror("bind");
		exit(2);
	}

	if (1) {
		struct icmp_filter filt;
		filt.data = ~((1<<ICMP_SOURCE_QUENCH)|
			      (1<<ICMP_DEST_UNREACH)|
			      (1<<ICMP_TIME_EXCEEDED)|
			      (1<<ICMP_PARAMETERPROB)|
			      (1<<ICMP_REDIRECT)|
			      (1<<ICMP_ECHOREPLY));
		if (setsockopt(icmp_sock, SOL_RAW, ICMP_FILTER, (char*)&filt, sizeof(filt)) == -1)
			perror("WARNING: setsockopt(ICMP_FILTER)");
	}

	hold = 1;
	if (setsockopt(icmp_sock, SOL_IP, IP_RECVERR, (char *)&hold, sizeof(hold)))
		fprintf(stderr, "WARNING: your kernel is veeery old. No problems.\n");

	/* record route option */
	if (options & F_RROUTE) {
	        bzero(rspace, sizeof(rspace));
		rspace[0] = IPOPT_NOP;
		rspace[1+IPOPT_OPTVAL] = IPOPT_RR;
		rspace[1+IPOPT_OLEN] = sizeof(rspace)-1;
		rspace[1+IPOPT_OFFSET] = IPOPT_MINOFF;
		optlen = 40;
		if (setsockopt(icmp_sock, IPPROTO_IP, IP_OPTIONS, rspace, sizeof(rspace)) < 0) {
			perror("ping: record route");
			exit(2);
		}
	}
	if (options & F_TIMESTAMP) {
	        bzero(rspace, sizeof(rspace));
		rspace[0] = IPOPT_TIMESTAMP;
		rspace[1] = (ts_type==IPOPT_TS_TSONLY ? 40 : 36);
		rspace[2] = 5;
		rspace[3] = ts_type;
		if (ts_type == IPOPT_TS_PRESPEC) {
			int i;
			rspace[1] = 4+nroute*8;
			for (i=0; i<nroute; i++)
				*(__u32*)&rspace[4+i*8] = route[i];
		}
		if (setsockopt(icmp_sock, IPPROTO_IP, IP_OPTIONS, rspace, rspace[1]) < 0) {
			rspace[3] = 2;
			if (setsockopt(icmp_sock, IPPROTO_IP, IP_OPTIONS, rspace, rspace[1]) < 0) {
				perror("ping: ts option");
				exit(2);
			}
		}
		optlen = 40;
	}
	if (options & F_SOURCEROUTE) {
	        int i;
	        bzero(rspace, sizeof(rspace));
		rspace[0] = IPOPT_NOOP;
		rspace[1+IPOPT_OPTVAL] = (options & F_SO_DONTROUTE) ? IPOPT_SSRR
			: IPOPT_LSRR;
		rspace[1+IPOPT_OLEN] = 3 + nroute*4;
		rspace[1+IPOPT_OFFSET] = IPOPT_MINOFF;
		for (i=0; i<nroute; i++)
			*(__u32*)&rspace[4+i*4] = route[i];
		
		if (setsockopt(icmp_sock, IPPROTO_IP, IP_OPTIONS, rspace, 4 + nroute*4) < 0) {
			perror("ping: record route");
			exit(2);
		}
		optlen = 40;
	}

	/* Estimate memory eaten by single packet. It is rough estimate.
	 * Actually, for small datalen's it depends on kernel side a lot. */
	hold = datalen + 8;
	hold += ((hold+511)/512)*(optlen + 20 + 16 + 64 + 160);
	sock_setbufs(icmp_sock, hold);

	if (broadcast_pings) {
		if (setsockopt(icmp_sock, SOL_SOCKET, SO_BROADCAST,
			       &broadcast_pings, sizeof(broadcast_pings)) < 0) {
			perror ("ping: can't set broadcasting");
			exit(2);
		}
        }

	if (options & F_NOLOOP) {
		int loop = 0;
		if (setsockopt(icmp_sock, IPPROTO_IP, IP_MULTICAST_LOOP,
							&loop, 1) == -1) {
			perror ("ping: can't disable multicast loopback");
			exit(2);
		}
	}
	if (options & F_TTL) {
		int ittl = ttl;
		if (setsockopt(icmp_sock, IPPROTO_IP, IP_MULTICAST_TTL,
							&ttl, 1) == -1) {
			perror ("ping: can't set multicast time-to-live");
			exit(2);
		}
		if (setsockopt(icmp_sock, IPPROTO_IP, IP_TTL,
							&ittl, sizeof(ittl)) == -1) {
			perror ("ping: can't set unicast time-to-live");
			exit(2);
		}
	}

	if (datalen > 0xFFFF - 8 - optlen - 20) {
		if (uid || datalen > sizeof(outpack)-8) {
			fprintf(stderr, "Error: packet size %d is too large. Maximum is %d\n", datalen, 0xFFFF-8-20-optlen);
			exit(2);
		}
		/* Allow small oversize to root yet. It will cause EMSGSIZE. */
		fprintf(stderr, "WARNING: packet size %d is too large. Maximum is %d\n", datalen, 0xFFFF-8-20-optlen);
	}

	if (datalen >= sizeof(struct timeval))	/* can we time transfer */
		timing = 1;
	packlen = datalen + MAXIPLEN + MAXICMPLEN;
	if (!(packet = (u_char *)malloc((u_int)packlen))) {
		fprintf(stderr, "ping: out of memory.\n");
		exit(2);
	}

	printf("PING %s (%s) ", hostname, inet_ntoa(whereto.sin_addr));
	if (device || (options&F_STRICTSOURCE))
		printf("from %s %s: ", inet_ntoa(source.sin_addr), device ?: "");
	printf("%d(%d) bytes of data.\n", datalen, datalen+8+optlen+20);

	setup(icmp_sock);

	main_loop(icmp_sock, packet, packlen);
	return 0;
}

void *dlopen(const char *filename, int flag) { return 0; }
char *dlerror(void) { return 0; }
void *dlsym(void *handle, const char *symbol) { return 0; }
int dlclose(void *handle) { return 0; }
