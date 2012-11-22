/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (c) 2011-2012, Code Aurora Forum. All rights reserved.
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

#define LOG_TAG "AudioPolicyManagerALSA"
//#define LOG_NDEBUG 0
#define LOG_NDDEBUG 0
#include <utils/Log.h>

#include "AudioPolicyManagerALSA.h"
#include <media/mediarecorder.h>

namespace android_audio_legacy {

// ----------------------------------------------------------------------------
// AudioPolicyManagerALSA
// ----------------------------------------------------------------------------

//Compiling error seen (only) on mako-pdk project if AudioParamer doesn't exist in this file
//No issue is seen on QCOM jb-mailine if remvong this line
//AudioParameter param;

// ---  class factory


extern "C" AudioPolicyInterface* createAudioPolicyManager(AudioPolicyClientInterface *clientInterface)
{
    return new AudioPolicyManager(clientInterface);
}

extern "C" void destroyAudioPolicyManager(AudioPolicyInterface *interface)
{
    delete interface;
}

}; // namespace androidi_audio_legacy
