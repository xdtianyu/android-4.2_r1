/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _PIXELFLINGER2_CONSTANTS_H_
#define _PIXELFLINGER2_CONSTANTS_H_

#define GGL_MAXVERTEXATTRIBS 8           
#define GGL_MAXVERTEXUNIFORMVECTORS 128 
#define GGL_MAXVARYINGVECTORS 8           
#define GGL_MAXVERTEXTEXTUREIMAGEUNITS 8  
#define GGL_MAXCOMBINEDTEXTUREIMAGEUNITS 16 /* samplers used in vertex + fragment */
#define GGL_MAXTEXTUREIMAGEUNITS 8 /* samplers used in fragment only */      
#define GGL_MAXFRAGMENTUNIFORMVECTORS 16
#define GGL_MAXDRAWBUFFERS 2

// these describe the layout of VertexOut when fed to fs, 
// it must NOT change and match VertexOut in pixelflinger_2.h
#define GGL_VS_OUTPUT_OFFSET            0
#define GGL_VS_OUTPUT_POSITION_INDEX    1

#define GGL_FS_INPUT_OFFSET             1 // vector4 index of first fs input in VertexOut
#define GGL_FS_INPUT_FRAGCOORD_INDEX    0
#define GGL_FS_INPUT_VARYINGS_INDEX     (GGL_FS_INPUT_FRAGCOORD_INDEX + 1)
#define GGL_FS_INPUT_FRONTFACINGPOINTCOORD_INDEX (GGL_FS_INPUT_VARYINGS_INDEX + GGL_MAXVARYINGVECTORS)

#define GGL_FS_OUTPUT_OFFSET            (GGL_FS_INPUT_OFFSET + GGL_FS_INPUT_FRONTFACINGPOINTCOORD_INDEX + 1)
#define GGL_FS_OUTPUT_FRAGCOLOR_INDEX   0

#define GGL_MAX_VIEWPORT_DIMS           4096

#endif // _PIXELFLINGER2_CONSTANTS_H_
