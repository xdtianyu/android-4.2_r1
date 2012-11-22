// Copyright (c) 2006-2008 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "base/file_util.h"

#include <fcntl.h>

#include <string>
#include <vector>

#include "base/eintr_wrapper.h"
#include "base/file_path.h"
#include "base/string_util.h"

// just lifted off bionic, no should find out why it doesn't get linked in

static int _gettemp(char *, int *, int, int);

extern uint32_t  arc4random();

int
mkstemps(char *path, int slen)
{
	int fd;

	return (_gettemp(path, &fd, 0, slen) ? fd : -1);
}

int
mkstemp(char *path)
{
	int fd;

	return (_gettemp(path, &fd, 0, 0) ? fd : -1);
}

char *
mkdtemp(char *path)
{
	return(_gettemp(path, (int *)NULL, 1, 0) ? path : (char *)NULL);
}

char *_mktemp(char *);

char *
_mktemp(char *path)
{
	return(_gettemp(path, (int *)NULL, 0, 0) ? path : (char *)NULL);
}

#ifdef __BIONIC__
__warn_references(mktemp,
    "warning: mktemp() possibly used unsafely; consider using mkstemp()");
#endif

char *
mktemp(char *path)
{
	return(_mktemp(path));
}


static int
_gettemp(char *path, int *doopen, int domkdir, int slen)
{
	char *start, *trv, *suffp;
	struct stat sbuf;
	int rval;
	pid_t pid;

	if (doopen && domkdir) {
		errno = EINVAL;
		return(0);
	}

	for (trv = path; *trv; ++trv)
		;
	trv -= slen;
	suffp = trv;
	--trv;
	if (trv < path) {
		errno = EINVAL;
		return (0);
	}
	pid = getpid();
	while (trv >= path && *trv == 'X' && pid != 0) {
		*trv-- = (pid % 10) + '0';
		pid /= 10;
	}
	while (trv >= path && *trv == 'X') {
		char c;

		pid = (arc4random() & 0xffff) % (26+26);
		if (pid < 26)
			c = pid + 'A';
		else
			c = (pid - 26) + 'a';
		*trv-- = c;
	}
	start = trv + 1;

	/*
	 * check the target directory; if you have six X's and it
	 * doesn't exist this runs for a *very* long time.
	 */
	if (doopen || domkdir) {
		for (;; --trv) {
			if (trv <= path)
				break;
			if (*trv == '/') {
				*trv = '\0';
				rval = stat(path, &sbuf);
				*trv = '/';
				if (rval != 0)
					return(0);
				if (!S_ISDIR(sbuf.st_mode)) {
					errno = ENOTDIR;
					return(0);
				}
				break;
			}
		}
	}

	for (;;) {
		if (doopen) {
			if ((*doopen =
			    open(path, O_CREAT|O_EXCL|O_RDWR, 0600)) >= 0)
				return(1);
			if (errno != EEXIST)
				return(0);
		} else if (domkdir) {
			if (mkdir(path, 0700) == 0)
				return(1);
			if (errno != EEXIST)
				return(0);
		} else if (lstat(path, &sbuf))
			return(errno == ENOENT ? 1 : 0);

		/* tricky little algorithm for backward compatibility */
		for (trv = start;;) {
			if (!*trv)
				return (0);
			if (*trv == 'Z') {
				if (trv == suffp)
					return (0);
				*trv++ = 'a';
			} else {
				if (isdigit(*trv))
					*trv = 'a';
				else if (*trv == 'z')	/* inc from z to A */
					*trv = 'A';
				else {
					if (trv == suffp)
						return (0);
					++*trv;
				}
				break;
			}
		}
	}
	/*NOTREACHED*/
}
