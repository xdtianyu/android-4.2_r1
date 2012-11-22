/*
Copyright (C) 2007 The Android Open Source Project

*/

#include "quakedef.h"

unsigned short	d_8to16table[256];
unsigned	d_8to24table[256];
unsigned char d_15to8table[65536];

int scr_width, scr_height;

cvar_t	_windowed_mouse = CVAR3("_windowed_mouse","0", true);

/*-----------------------------------------------------------------------*/

//int		texture_mode = GL_NEAREST;
//int		texture_mode = GL_NEAREST_MIPMAP_NEAREST;
//int		texture_mode = GL_NEAREST_MIPMAP_LINEAR;
int		texture_mode = GL_LINEAR;
//int		texture_mode = GL_LINEAR_MIPMAP_NEAREST;
//int		texture_mode = GL_LINEAR_MIPMAP_LINEAR;

int		texture_extension_number = 1;

float		gldepthmin, gldepthmax;

cvar_t	gl_ztrick = CVAR2("gl_ztrick","1");

const char *gl_vendor;
const char *gl_renderer;
const char *gl_version;
const char *gl_extensions;

qboolean is8bit = false;
qboolean gl_mtexable = false;

/*-----------------------------------------------------------------------*/
void D_BeginDirectRect (int x, int y, byte *pbitmap, int width, int height)
{
}

void D_EndDirectRect (int x, int y, int width, int height)
{
}

void VID_Shutdown(void)
{
}


void VID_ShiftPalette(unsigned char *p)
{
//	VID_SetPalette(p);
}

void	VID_SetPalette (unsigned char *palette)
{
	byte	*pal;
	unsigned r,g,b;
	unsigned v;
	int     r1,g1,b1;
	int		k;
	unsigned short i;
	unsigned	*table;
	FILE *f;
	char s[255];
	float dist, bestdist;
	static qboolean palflag = false;

//
// 8 8 8 encoding
//
	Con_Printf("Converting 8to24\n");

	pal = palette;
	table = d_8to24table;
	for (i=0 ; i<256 ; i++)
	{
		r = pal[0];
		g = pal[1];
		b = pal[2];
		pal += 3;
		
//		v = (255<<24) + (r<<16) + (g<<8) + (b<<0);
//		v = (255<<0) + (r<<8) + (g<<16) + (b<<24);
		v = (255<<24) + (r<<0) + (g<<8) + (b<<16);
		*table++ = v;
	}
	d_8to24table[255] &= 0xffffff;	// 255 is transparent

	// JACK: 3D distance calcs - k is last closest, l is the distance.
	// FIXME: Precalculate this and cache to disk.
	if (palflag)
		return;
	palflag = true;

	COM_FOpenFile("glquake/15to8.pal", &f);
	if (f) {
		fread(d_15to8table, 1<<15, 1, f);
		fclose(f);
	} else {
		for (i=0; i < (1<<15); i++) {
			/* Maps
 			000000000000000
 			000000000011111 = Red  = 0x1F
 			000001111100000 = Blue = 0x03E0
 			111110000000000 = Grn  = 0x7C00
 			*/
 			r = ((i & 0x1F) << 3)+4;
 			g = ((i & 0x03E0) >> 2)+4;
 			b = ((i & 0x7C00) >> 7)+4;
			pal = (unsigned char *)d_8to24table;
			for (v=0,k=0,bestdist=10000.0; v<256; v++,pal+=4) {
 				r1 = (int)r - (int)pal[0];
 				g1 = (int)g - (int)pal[1];
 				b1 = (int)b - (int)pal[2];
				dist = sqrt(((r1*r1)+(g1*g1)+(b1*b1)));
				if (dist < bestdist) {
					k=v;
					bestdist = dist;
				}
			}
			d_15to8table[i]=k;
		}
		sprintf(s, "%s/glquake", com_gamedir);
 		Sys_mkdir (s);
		sprintf(s, "%s/glquake/15to8.pal", com_gamedir);
		if ((f = fopen(s, "wb")) != NULL) {
			fwrite(d_15to8table, 1<<15, 1, f);
			fclose(f);
		}
	}
}

/*
===============
GL_Init
===============
*/
void GL_Init (void)
{
	gl_vendor = (char*) glGetString (GL_VENDOR);
	Con_Printf ("GL_VENDOR: %s\n", gl_vendor);
	gl_renderer = (char*) glGetString (GL_RENDERER);
	Con_Printf ("GL_RENDERER: %s\n", gl_renderer);

	gl_version = (char*) glGetString (GL_VERSION);
	Con_Printf ("GL_VERSION: %s\n", gl_version);
	gl_extensions = (char*) glGetString (GL_EXTENSIONS);
	Con_Printf ("GL_EXTENSIONS: %s\n", gl_extensions);

//	Con_Printf ("%s %s\n", gl_renderer, gl_version);

	glClearColor (1,0,0,0);
	glCullFace(GL_FRONT);
	glEnable(GL_TEXTURE_2D);

	glEnable(GL_ALPHA_TEST);
	glAlphaFunc(GL_GREATER, 0.666);

#ifdef USE_OPENGLES
#else
	glPolygonMode (GL_FRONT_AND_BACK, GL_FILL);
#endif
	glShadeModel (GL_FLAT);

	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

	glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

//	glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
	glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
}

/*
=================
GL_BeginRendering

=================
*/
void GL_BeginRendering (int *x, int *y, int *width, int *height)
{
	extern cvar_t gl_clear;

	*x = *y = 0;
	*width = scr_width;
	*height = scr_height;

//    if (!wglMakeCurrent( maindc, baseRC ))
//		Sys_Error ("wglMakeCurrent failed");

//	glViewport (*x, *y, *width, *height);
}


void GL_EndRendering (void)
{
	glFlush();
	// !!! Swap buffers.
}

void Init_KBD(void)
{
}

qboolean VID_Is8bit(void)
{
	return 0;
}

void VID_Init(unsigned char *palette)
{
	int i;
	GLint attribs[32];
	char	gldir[MAX_OSPATH];
	int width = scr_width, height = scr_height;

	S_Init();

	Init_KBD();

	Cvar_RegisterVariable (&gl_ztrick);
	
	vid.maxwarpwidth = scr_width;
	vid.maxwarpheight = height;
	vid.colormap = host_colormap;
	vid.fullbright = 0xffff;
	vid.aspect = (float) scr_width / (float) scr_height;
	vid.numpages = 2;
	vid.rowbytes = 2 * scr_width;
	vid.width = scr_width;
	vid.height = scr_height;
	
	vid.conwidth = scr_width;
	vid.conheight = scr_height;

// interpret command-line params

// set vid parameters

	GL_Init();

	sprintf (gldir, "%s/glquake", com_gamedir);
	Sys_mkdir (gldir);

	VID_SetPalette(palette);

	Con_SafePrintf ("Video mode %dx%d initialized.\n", width, height);

	vid.recalc_refdef = 1;				// force a surface cache flush
}

// Android Key event codes. These are from the simulator.
// Not all Android devices can generate all codes.

byte scantokey[] =
{
    '$', '$', '$', '$',  '$', '$', '$', '0', //  0.. 7
    '1', '2', '3', '4',  '5', '6', '7', '8', //  8..15
    '9', '$', '$', K_UPARROW,  K_DOWNARROW, K_LEFTARROW, K_RIGHTARROW, K_ENTER, // 16..23
    '$', '$', '$', '$',  '$', 'a', 'b', 'c', // 24..31

    'd', 'e', 'f', 'g',  'h', 'i', 'j', 'k', // 32..39
    'l', 'm', 'n', 'o',  'p', 'q', 'r', 's', // 40..47
    't', 'u', 'v', 'w',  'x', 'y', 'z', ',', // 48..55
    '.', K_CTRL, K_SHIFT, K_TAB,  ' ', '$', '$', '$', // 56..63
	K_ENTER, K_BACKSPACE, '`', '-',  '=', '$', '$', '$', // 64..71
	'$', '$', '$', '$',  '$', '$', '$', '/', // 72..79
};
	
// return non-zero if the event is handled

int AndroidEvent(int type, int value)
{
	if(value >= 0 && value < (int) sizeof(scantokey))
	{
		byte key = scantokey[value];
		Key_Event(key, type);
		// printf("type: %d, value: %d -> %d '%c'\n", type, value, key, key);
		return 1;
	}
	else
	{
		printf("unexpected event type: %d, value: %d\n", type, value);
	}
	return 0;
}

void Sys_SendKeyEvents(void)
{
}

void Force_CenterView_f (void)
{
	cl.viewangles[PITCH] = 0;
}

void IN_Init(void)
{
}

void IN_Shutdown(void)
{
}

/*
===========
IN_Commands
===========
*/
void IN_Commands (void)
{
}

/*
===========
IN_Move
===========
*/
void IN_MouseMove (usercmd_t *cmd)
{

}

void IN_Move (usercmd_t *cmd)
{
	IN_MouseMove(cmd);
}

void VID_UnlockBuffer() {}
void VID_LockBuffer() {}

