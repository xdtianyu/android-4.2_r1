/*
 * Definitions for DHD command-line utility
 *
 * Copyright (C) 1999-2011, Broadcom Corporation
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION
 * OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 * $Id: dhdu.h,v 1.7 2009-05-22 19:23:29 Exp $
 */

#ifndef _dhdu_h_
#define _dhdu_h_

#include "dhdu_cmd.h"

extern char *dhdu_av0;

/* parse common option */
extern int dhd_option(char ***pargv, char **pifname, int *phelp);
extern void dhd_cmd_init(void);

/* print usage */
extern void dhd_cmd_usage(cmd_t *cmd);
extern void dhd_usage(cmd_t *port_cmds);
extern void dhd_cmds_usage(cmd_t *port_cmds);

/* print helpers */
extern void dhd_printlasterror(void *dhd);
extern void dhd_printint(int val);

/* check driver version */
extern int dhd_check(void *dhd);

/* utility functions */
struct ipv4_addr;
int dhd_ether_atoe(const char *a, struct ether_addr *n);
int dhd_atoip(const char *a, struct ipv4_addr *n);
/* useful macros */
#define ARRAYSIZE(a)  (sizeof(a)/sizeof(a[0]))

#define USAGE_ERROR    -1	/* Error code for Usage */
#define IOCTL_ERROR    -2	/* Error code for ioctl failure */
#define COMMAND_ERROR  -3	/* Error code for general command failure */

/* integer output format */
#define INT_FMT_DEC	0	/* signed integer */
#define INT_FMT_UINT	1	/* unsigned integer */
#define INT_FMT_HEX	2	/* hexdecimal */

/* command line argument usage */
#define CMD_ERR		-1	/* Error for command */
#define CMD_OPT		0	/* a command line option */
#define CMD_DHD		1	/* the start of a dhd command */

#endif /* _dhdu_h_ */
