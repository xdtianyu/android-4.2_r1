/*
Copyright (C) 2007 The Android Open Source Project

*/

#include "quakedef.h"

unsigned short	d_8to16table[256];
unsigned	d_8to24table[256];

#ifdef SUPPORT_8BIT_MIPMAPGENERATION
unsigned char d_15to8table[65536];
#endif

cvar_t  mouse_button_commands[3] =
{
    CVAR2("mouse1","+attack"),
    CVAR2("mouse2","+strafe"),
    CVAR2("mouse3","+forward"),
};

static const int MOUSE_LEFTBUTTON = 1;
static const int MOUSE_MIDDLEBUTTON = 2;
static const int MOUSE_RIGHTBUTTON = 4;

bool     mouse_tap;
float   mouse_x, mouse_y;
float   old_mouse_x, old_mouse_y;
int     mx, my;
bool    mouse_buttonstate;
bool    mouse_oldbuttonstate;

cvar_t  m_filter = CVAR2("m_filter","1");

int scr_width, scr_height;

cvar_t	_windowed_mouse = CVAR3("_windowed_mouse","0", true);

/*-----------------------------------------------------------------------*/

//int		texture_mode = GL_NEAREST;
//int		texture_mode = GL_NEAREST_MIPMAP_NEAREST;
//int		texture_mode = GL_NEAREST_MIPMAP_LINEAR;
int		texture_mode = GL_LINEAR;
// int		texture_mode = GL_LINEAR_MIPMAP_NEAREST;
//int		texture_mode = GL_LINEAR_MIPMAP_LINEAR;

int		texture_extension_number = 1;

float		gldepthmin, gldepthmax;

cvar_t	gl_ztrick = CVAR2("gl_ztrick","0");

const char *gl_vendor;
const char *gl_renderer;
const char *gl_version;
const char *gl_extensions;

qboolean is8bit = false;
qboolean isPermedia = false;
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
  int dist, bestdist;
  static qboolean palflag = false;

  PMPBEGIN(("VID_SetPalette"));
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

#ifdef SUPPORT_8BIT_MIPMAPGENERATION

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
    PMPBEGIN(("Creating 15to8 palette"));
    for (i=0; i < (1<<15); i++) {
      /* Maps
       0000.0000.0000.0000
       0000.0000.0001.1111 = Red   = 0x001F
       0000.0011.1110.0000 = Green = 0x03E0
       0111.1100.0000.0000 = Blue  = 0x7C00
       */
       r = ((i & 0x1F) << 3)+4;
       g = ((i & 0x03E0) >> (5-3)) +4;
       b = ((i & 0x7C00) >> (10-3))+4;
      pal = (unsigned char *)d_8to24table;
      for (v=0,k=0,bestdist=0x7FFFFFFF; v<256; v++,pal+=4) {
         r1 = (int)r - (int)pal[0];
         g1 = (int)g - (int)pal[1];
         b1 = (int)b - (int)pal[2];
        dist = ((r1*r1)+(g1*g1)+(b1*b1));
        if (dist < bestdist) {
          k=v;
          bestdist = dist;
        }
      }
      d_15to8table[i]=k;
    }
    PMPEND(("Creating 15to8 palette"));
    sprintf(s, "%s/glquake", com_gamedir);
     Sys_mkdir (s);
    sprintf(s, "%s/glquake/15to8.pal", com_gamedir);
    if ((f = fopen(s, "wb")) != NULL) {
      fwrite(d_15to8table, 1<<15, 1, f);
      fclose(f);
    }
    else
    {
      Con_Printf("Could not write %s\n", s);
    }
  }

#endif // SUPPORT_8BIT_MIPMAPGENERATION
  PMPEND(("VID_SetPalette"));
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
  GLCHECK("glGetString");
  gl_renderer = (char*) glGetString (GL_RENDERER);
  Con_Printf ("GL_RENDERER: %s\n", gl_renderer);
  GLCHECK("glGetString");

  gl_version = (char*) glGetString (GL_VERSION);
  Con_Printf ("GL_VERSION: %s\n", gl_version);
  GLCHECK("glGetString");
  gl_extensions = (char*) glGetString (GL_EXTENSIONS);
  Con_Printf ("GL_EXTENSIONS: %s\n", gl_extensions);
  GLCHECK("glGetString");

//	Con_Printf ("%s %s\n", gl_renderer, gl_version);

  // Enable/disable multitexture:

  gl_mtexable = true;

  glClearColor (1,0,0,0);
  glCullFace(GL_FRONT);
  glEnable(GL_TEXTURE_2D);

  glEnable(GL_ALPHA_TEST);
  glAlphaFunc(GL_GREATER, 0.666);

#ifdef USE_OPENGLES
#else
  glPolygonMode (GL_FRONT_AND_BACK, GL_FILL);
#endif
  glShadeModel(GL_FLAT);
    glDisable(GL_DITHER);

    // perspective correction don't work well currently
    glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);

  glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
  glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
  glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
  glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

  glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

//	glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
  glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

#ifdef USE_OPENGLES
  glEnableClientState(GL_VERTEX_ARRAY);
  glEnableClientState(GL_TEXTURE_COORD_ARRAY);
#endif
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
  //glFlush();
  // !!! Swap buffers.
}

void Init_KBD(void)
{
}

// This function controls whether or not 8-bit paletted textures are used:

qboolean VID_Is8bit(void)
{
  return 0;
}

static void Check_Gamma (unsigned char *pal)
{
    float vid_gamma;
  float	f, inf;
  unsigned char	palette[768];
  int		i;

  if ((i = COM_CheckParm("-gamma")) == 0) {
    vid_gamma = 0.5;	// brighten up game.
  } else
    vid_gamma = Q_atof(com_argv[i+1]);

  if(vid_gamma != 1)
  {
    for (i=0 ; i<768 ; i++)
    {
      f = pow ( (pal[i]+1)/256.0 , vid_gamma );
      inf = f*255 + 0.5;
      if (inf < 0)
        inf = 0;
      if (inf > 255)
        inf = 255;
      palette[i] = (unsigned char) inf;
    }
  }

  memcpy (pal, palette, sizeof(palette));
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

  Check_Gamma(palette);
  VID_SetPalette(palette);

  Con_SafePrintf ("Video mode %dx%d initialized.\n", width, height);

  vid.recalc_refdef = 1;				// force a surface cache flush
}

// Android Key event codes. Some of these are
// only generated from the simulator.
// Not all Android devices can generate all codes.

byte scantokey[] =
{
    '$', K_ESCAPE, '$', '$',  K_ESCAPE, '$', '$', '0', //  0.. 7
    '1', '2', '3', '4',  '5', '6', '7', '8', //  8..15
    '9', '$', '$', K_UPARROW,  K_DOWNARROW, K_LEFTARROW, K_RIGHTARROW, K_ENTER, // 16..23
    '$', '$', '$', '$',  '$', 'a', 'b', 'c', // 24..31

    'd', 'e', 'f', 'g',  'h', 'i', 'j', 'k', // 32..39
    'l', 'm', 'n', 'o',  'p', 'q', 'r', 's', // 40..47
    't', 'u', 'v', 'w',  'x', 'y', 'z', ',', // 48..55
    '.', K_CTRL, K_SHIFT, K_TAB,  ' ', '$', '$', '$', // 56..63
  '$', '$', K_ENTER, K_BACKSPACE, '`', '-',  '=', '[', // 64..71
  ']', '\\', ';', '\'', '/', '@',  '#', '$', // 72..79
  '$', '$', K_ESCAPE, '$'                       // 80..83
};

byte scantokeyAlt[] =
{
    0, 0, 0, 0,  0, 0, 0, 0, //  0.. 7
    0, 0, 0, 0,  0, 0, 0, 0, //  8..15
    0, 0, 0, 0,  0, 0, 0, 0, // 16..23
    0, 0, 0, 0,  0, '%', '=', '8', // 24..31

    '5', '2', '6', '-',  '[', '$', ']', '"', // 32..39
    '\'', '>', '<', '(',  ')', '*', '3', '4', // 40..47
    '+', '&', '9', '1',  '7', '!', '#', ';', // 48..55
    ':', 0, 0, 0,  K_TAB, 0, 0, 0, // 56..63
  0, 0, 0, 0,  0, 0, 0, 0, // 64..71
  0, 0, '?', '0',  0, 0, 0, 0, // 72..79
  0, 0, K_ESCAPE, 0                       // 80..83
};

#if 0

byte scantokeyCap[] =
{
    0, 0, 0, 0,  0, 0, 0, 0, //  0.. 7
    0, 0, 0, 0,  0, 0, 0, 0, //  8..15
    0, 0, 0, 0,  0, 0, 0, 0, // 16..23
    0, 0, 0, 0,  0, 'A', 'B', 'C', // 24..31

    'D', 'E', 'F', 'G',  'H', 'I', 'J', 'K', // 32..39
    'L', 'M', 'N', 'O',  'P', 'Q', 'R', 'S', // 40..47
    'T', 'U', 'V', 'W',  'X', 'Y', 'Z', 0, // 48..55
    0, 0, 0, 0,  0, 0, 0, 0, // 56..63
  0, 0, 0, 0,  0, 0, 0, 0, // 64..71
  0, 0, 0, 0,  0, 0, 0, 0, // 72..79
  0, 0, K_ESCAPE, 0                       // 80..83
};

#endif

byte scantokeySym[] =
{
    0, 0, 0, 0,  0, 0, 0, 0, //  0.. 7
    0, 0, 0, 0,  0, 0, 0, 0, //  8..15
    0, 0, 0, 0,  0, 0, 0, 0, // 16..23
    0, 0, 0, 0,  0, 0, 0, K_F8, // 24..31

    K_F5, K_F2, K_F6, '_',  0, 0, 0, 0, // 32..39
    0, 0, 0, 0,  0, 0, K_F3, K_F4, // 40..47
    K_F11, 0, K_F9, K_F1,  K_F7, K_F12, K_PAUSE, 0, // 48..55
    0, 0, 0, 0,  0, 0, 0, 0, // 56..63
  0, 0, 0, 0,  0, 0, 0, 0, // 64..71
  0, 0, '`', K_F10,  0, 0, 0, 0, // 72..79
  0, 0, K_ESCAPE, 0                       // 80..83
};

#define ALT_KEY_VALUE 57
// #define CAPS_KEY_VALUE 58
#define SYM_KEY_VALUE 61

byte modifierKeyInEffect;

qboolean symKeyDown;
byte symKeyCode;

// Called from stand-alone main() function to process an event.
// Return non-zero if the event is handled.

int AndroidEvent(int type, int value)
{
  if(value >= 0 && value < (int) sizeof(scantokey))
  {
    byte key;
    qboolean isPress = type != 0;

    qboolean isModifier = value == ALT_KEY_VALUE || value == SYM_KEY_VALUE;

    if(isModifier)
    {
      if(isPress)
      {
        if(modifierKeyInEffect == value)
        {
          // Press modifier twice to cancel modifier
          modifierKeyInEffect = 0;
        }
        else
        {
          // Most recent modifier key wins
          modifierKeyInEffect = value;
        }
      }
      return 1;
    }
    else
    {
      switch(modifierKeyInEffect)
      {
        default:	        key = scantokey[value]; break;
        case ALT_KEY_VALUE: key = scantokeyAlt[value]; break;
        // case CAP_KEY_VALUE: key = scantokeyCap[value]; break;
        case SYM_KEY_VALUE: key = scantokeySym[value]; break;
      }
      if(!key)
      {
        key = scantokey[value];
      }

      // Hack: Remap @ and / to K_CTRL in game mode
      if(key_dest == key_game && ! modifierKeyInEffect && (key == '@' || key == '/'))
      {
        key = K_CTRL;
      }

      if(!isPress)
      {
        modifierKeyInEffect = 0;
      }
    }

    Key_Event(key, type);
    // PMPLOG(("type: %d, value: %d -> %d '%c'\n", type, value, key, key));

    return 1;
  }
  else
  {
    PMPWARNING(("unexpected event type: %d, value: %d\n", type, value));
  }
  return 0;
}

// Called from Java to process an event.
// Return non-zero if the event is handled.

int AndroidEvent2(int type, int value)
{
  Key_Event(value, type);
  return 0;
}

static const int MOTION_DOWN = 0;
static const int MOTION_UP = 1;
static const int MOTION_MOVE = 2;
static const int MOTION_CANCEL = 3;

class GestureDetector {
private:
    bool mIsScroll;
    bool mIsTap;
    bool mIsDoubleTap;

    float mScrollX;
    float mScrollY;

    static const unsigned long long TAP_TIME_MS = 200;
    static const unsigned long long DOUBLE_TAP_TIME_MS = 400;
    static const int TAP_REGION_MANHATTAN_DISTANCE = 10;

    bool mAlwaysInTapRegion;
    float mDownX;
    float mDownY;
    unsigned long long mDownTime;
    unsigned long long mPreviousDownTime;

    /**
     * Position of the last motion event.
     */
    float mLastMotionY;
    float mLastMotionX;

public:
    /**
     * Analyze a motion event. Call this once for each motion event
     * that is received by a view.
     * @param ev the motion event to analyze.
     */
    void analyze(unsigned long long eventTime, int action,
            float x, float y, float pressure, float size, int deviceId) {
        mIsScroll = false;
        mIsTap = false;
        mIsDoubleTap = false;

        switch (action) {
          case MOTION_DOWN:
            printf("Down");
            // Remember where the motion event started
            mLastMotionX = x;
            mLastMotionY = y;
            mDownX = x;
            mDownY = y;
            mPreviousDownTime = mDownTime;
            mDownTime = eventTime;
            mAlwaysInTapRegion = true;
            break;

          case MOTION_MOVE:
          {
            mIsScroll = true;
            mScrollX = mLastMotionX - x;
            mScrollY = mLastMotionY - y;
            mLastMotionX = x;
            mLastMotionY = y;
            int manhattanTapDistance = (int) (absf(x - mDownX) +
                    absf(y - mDownY));
            if (manhattanTapDistance > TAP_REGION_MANHATTAN_DISTANCE) {
                mAlwaysInTapRegion = false;
            }
          }
          break;

          case MOTION_UP:
          {
              unsigned long long doubleTapDelta =
                  eventTime - mPreviousDownTime;
              unsigned long long singleTapDelta =
                  eventTime - mDownTime;

              if (mAlwaysInTapRegion) {
                  if (doubleTapDelta < DOUBLE_TAP_TIME_MS) {
                      mIsDoubleTap = true;
                  }
                  else if (singleTapDelta < TAP_TIME_MS) {
                      mIsTap = true;
                  }
              }
          }
          break;
        }
    }

    /**
     * @return true if the current motion event is a scroll
     * event.
     */
    bool isScroll() {
        return mIsScroll;
    }

    /**
     * This value is only defined if {@link #isScroll} is true.
     * @return the X position of the current scroll event.
     * event.
     */
    float scrollX() {
        return mScrollX;
    }

    /**
     * This value is only defined if {@link #isScroll} is true.
     * @return the Y position of the current scroll event.
     * event.
     */
    float scrollY() {
        return mScrollY;
    }

    /**
     * @return true if the current motion event is a single-tap
     * event.
     */
    bool isTap() {
        return mIsTap;
    }

    /**
     * This value is only defined if either {@link #isTap} or
     * {@link #isDoubleTap} is true.
     * @return the X position of the current tap event.
     * event.
     */
    float tapX() {
        return mDownX;
    }

    /**
     * This value is only defined if either {@link #isTap} or
     * {@link #isDoubleTap} is true.
     * @return the Y position of the current tap event.
     * event.
     */
    float tapY() {
        return mDownY;
    }

    /**
     * @return true if the current motion event is a double-tap
     * event.
     */
    bool isDoubleTap() {
        return mIsDoubleTap;
    }

private:
    inline float absf(float a) {
        return a >= 0.0f ? a : -a;
    }
};

GestureDetector gGestureDetector;

int AndroidMotionEvent(unsigned long long eventTime, int action,
        float x, float y, float pressure, float size, int deviceId)
{
    gGestureDetector.analyze(eventTime, action, x, y, pressure, size, deviceId);

    if (gGestureDetector.isTap()) {
        mouse_tap = true;
    }
    else if (gGestureDetector.isScroll()) {
        mx += (int) gGestureDetector.scrollX();
        my += (int) gGestureDetector.scrollY();
    }

    return true;
}

int AndroidTrackballEvent(unsigned long long eventTime, int action,
        float x, float y)
{
    switch (action ) {
    case MOTION_DOWN:
      mouse_buttonstate = true;
      break;
    case MOTION_UP:
      mouse_buttonstate = false;
      break;
    case MOTION_MOVE:
      mx += (int) (20.0f * x);
      my += (int) (20.0f * y);
      break;
    }

    return true;
}

void Sys_SendKeyEvents(void)
{
  // Used to poll keyboards on systems that need to poll keyboards.
}

void Force_CenterView_f (void)
{
  cl.viewangles[PITCH] = 0;
}

void IN_Init(void)
{
    Cvar_RegisterVariable (&mouse_button_commands[0]);
    Cvar_RegisterVariable (&mouse_button_commands[1]);
    Cvar_RegisterVariable (&mouse_button_commands[2]);
    Cmd_AddCommand ("force_centerview", Force_CenterView_f);

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
    // perform button actions
    if (mouse_tap) {
        Key_Event (K_MOUSE1, true);
        Key_Event (K_MOUSE1, false);
        mouse_tap = false;
    }
    if (mouse_buttonstate != mouse_oldbuttonstate) {
      Key_Event (K_MOUSE1, mouse_buttonstate ? true : false);
      mouse_oldbuttonstate = mouse_buttonstate;
    }
}

/*
===========
IN_Move
===========
*/
void IN_MouseMove (usercmd_t *cmd)
{
#if 0
    if (m_filter.value)
    {
        mouse_x = (mx + old_mouse_x) * 0.5;
        mouse_y = (my + old_mouse_y) * 0.5;
    }
    else
#endif
    {
        mouse_x = mx;
        mouse_y = my;
    }
    old_mouse_x = mx;
    old_mouse_y = my;
    mx = my = 0; // clear for next update

    mouse_x *= 5.0f * sensitivity.value;
    mouse_y *= 5.0f * sensitivity.value;

// add mouse X/Y movement to cmd
    if ( (in_strafe.state & 1) || (lookstrafe.value && (in_mlook.state & 1) ))
        cmd->sidemove += m_side.value * mouse_x;
    else
        cl.viewangles[YAW] -= m_yaw.value * mouse_x;

    if (in_mlook.state & 1)
        V_StopPitchDrift ();

    if ( (in_mlook.state & 1) && !(in_strafe.state & 1))
    {
        cl.viewangles[PITCH] += m_pitch.value * mouse_y;
        if (cl.viewangles[PITCH] > 80)
            cl.viewangles[PITCH] = 80;
        if (cl.viewangles[PITCH] < -70)
            cl.viewangles[PITCH] = -70;
    }
    else
    {
        if ((in_strafe.state & 1) && noclip_anglehack)
            cmd->upmove -= m_forward.value * mouse_y;
        else
            cmd->forwardmove -= m_forward.value * mouse_y;
    }
}

void IN_Move (usercmd_t *cmd)
{
  IN_MouseMove(cmd);
}


