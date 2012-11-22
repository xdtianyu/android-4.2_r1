/******************************************************************************
 *
 *  Copyright (C) 2001-2012 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

/******************************************************************************
 *
 *  Critical section definitions for btdisp functions.
 *
 ******************************************************************************/

#ifndef BTDISP_LOCK
#define BTDISP_LOCK


#ifdef TESTER

#ifdef __cplusplus
extern "C" {
#endif

// External function declaration
extern void btdisp_lock();
extern void btdisp_unlock();
extern void btdisp_init();
extern void btdisp_uninit();

#ifdef __cplusplus
}
#endif

// Lock Macros
#define BTDISP_LOCK_LOG()           btdisp_lock();
#define BTDISP_UNLOCK_LOG()         btdisp_unlock();
#define BTDISP_INIT_LOCK()          btdisp_init();
#define BTDISP_UNINIT_LOCK()        btdisp_uninit();

#else

#define BTDISP_LOCK_LOG()
#define BTDISP_UNLOCK_LOG()
#define BTDISP_INIT_LOCK()
#define BTDISP_UNINIT_LOCK()

#endif



#endif // BTDISP_LOCK
