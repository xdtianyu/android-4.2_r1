/*
 * Misc system wide definitions
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
 * $Id: bcmdefs.h,v 13.68.2.8 2011-01-08 04:04:19 Exp $
 */

#ifndef	_bcmdefs_h_
#define	_bcmdefs_h_



#define bcmreclaimed 		0
#define _data	_data
#define _fn	_fn
#define _data	_data
#define _fn		_fn
#define _fn	_fn
#define CONST	const
#define BCMFASTPATH




#define _data	_data
#define _fn		_fn
#define _fn	_fn
#define STATIC	static


#define	SI_BUS			0
#define	PCI_BUS			1
#define	PCMCIA_BUS		2
#define SDIO_BUS		3
#define JTAG_BUS		4
#define USB_BUS			5
#define SPI_BUS			6
#define RPC_BUS			7


#ifdef BCMBUSTYPE
#define BUSTYPE(bus) 	(BCMBUSTYPE)
#else
#define BUSTYPE(bus) 	(bus)
#endif


#ifdef BCMCHIPTYPE
#define CHIPTYPE(bus) 	(BCMCHIPTYPE)
#else
#define CHIPTYPE(bus) 	(bus)
#endif



#if defined(BCMSPROMBUS)
#define SPROMBUS	(BCMSPROMBUS)
#elif defined(SI_PCMCIA_SROM)
#define SPROMBUS	(PCMCIA_BUS)
#else
#define SPROMBUS	(PCI_BUS)
#endif


#ifdef BCMCHIPID
#define CHIPID(chip)	(BCMCHIPID)
#else
#define CHIPID(chip)	(chip)
#endif

#ifdef BCMCHIPREV
#define CHIPREV(rev)	(BCMCHIPREV)
#else
#define CHIPREV(rev)	(rev)
#endif


#define DMADDR_MASK_32 0x0
#define DMADDR_MASK_30 0xc0000000
#define DMADDR_MASK_0  0xffffffff

#define	DMADDRWIDTH_30  30
#define	DMADDRWIDTH_32  32
#define	DMADDRWIDTH_63  63
#define	DMADDRWIDTH_64  64

#ifdef BCMDMA64OSL
typedef struct {
	uint32 loaddr;
	uint32 hiaddr;
} dma64addr_t;

typedef dma64addr_t dmaaddr_t;
#define PHYSADDRHI(_pa) ((_pa).hiaddr)
#define PHYSADDRHISET(_pa, _val) \
	do { \
		(_pa).hiaddr = (_val);		\
	} while (0)
#define PHYSADDRLO(_pa) ((_pa).loaddr)
#define PHYSADDRLOSET(_pa, _val) \
	do { \
		(_pa).loaddr = (_val);		\
	} while (0)

#else
typedef unsigned long dmaaddr_t;
#define PHYSADDRHI(_pa) (0)
#define PHYSADDRHISET(_pa, _val)
#define PHYSADDRLO(_pa) ((_pa))
#define PHYSADDRLOSET(_pa, _val) \
	do { \
		(_pa) = (_val);			\
	} while (0)
#endif


typedef struct  {
	dmaaddr_t addr;
	uint32	  length;
} hnddma_seg_t;

#define MAX_DMA_SEGS 4


typedef struct {
	void *oshdmah;
	uint origsize;
	uint nsegs;
	hnddma_seg_t segs[MAX_DMA_SEGS];
} hnddma_seg_map_t;




#if defined(BCM_RPC_NOCOPY) || defined(BCM_RCP_TXNOCOPY)

#define BCMEXTRAHDROOM 220
#else
#define BCMEXTRAHDROOM 172
#endif


#define BCMDONGLEHDRSZ 12
#define BCMDONGLEPADSZ 16

#define BCMDONGLEOVERHEAD	(BCMDONGLEHDRSZ + BCMDONGLEPADSZ)


#if defined(BCMASSERT_LOG)
#define BCMASSERT_SUPPORT
#endif


#define BITFIELD_MASK(width) \
		(((unsigned)1 << (width)) - 1)
#define GFIELD(val, field) \
		(((val) >> field ## _S) & field ## _M)
#define SFIELD(val, field, bits) \
		(((val) & (~(field ## _M << field ## _S))) | \
		 ((unsigned)(bits) << field ## _S))


#ifdef BCMSMALL
#undef	BCMSPACE
#define bcmspace	FALSE
#else
#define	BCMSPACE
#define bcmspace	TRUE
#endif


#define	MAXSZ_NVRAM_VARS	4096

#define LOCATOR_EXTERN static

#endif
