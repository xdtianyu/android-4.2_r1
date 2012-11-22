/*
** Copyright 2008, The Android Open-Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#include <math.h>

//#define LOG_NDEBUG 0
#define LOG_TAG "AudioHardware"

#include <utils/Log.h>
#include <utils/String8.h>
//#include <hardware_legacy/power.h>

#include <stdio.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <dlfcn.h>
#include <fcntl.h>

#include "AudioHardware.h"
#include <media/AudioRecord.h>

extern "C" {
#include "msm_audio.h"
}


namespace android {
// ----------------------------------------------------------------------------

AudioHardware::AudioHardware() :
    mInit(false), mMicMute(true), mOutput(0)
{
    mInit = true;
}

AudioHardware::~AudioHardware()
{
    closeOutputStream((AudioStreamOut*)mOutput);
    mInit = false;
}

status_t AudioHardware::initCheck()
{
    return mInit ? NO_ERROR : NO_INIT;
}

AudioStreamOut* AudioHardware::openOutputStream(
        uint32_t devices, int *format, uint32_t *channels, uint32_t *sampleRate, status_t *status)
{
    { // scope for the lock
        Mutex::Autolock lock(mLock);

        // only one output stream allowed
        if (mOutput) {
            if (status) {
                *status = INVALID_OPERATION;
            }
            return 0;
        }

        AudioStreamOutQ5V2* out = new AudioStreamOutQ5V2();

        status_t rc = out->set(this, devices, format, channels, sampleRate);
        if (rc) {
            *status = rc;
        }
        if (rc == NO_ERROR) {
            mOutput = out;
        } else {
            delete out;
        }
    }
    return mOutput;
}

void AudioHardware::closeOutputStream(AudioStreamOut* out) {
    Mutex::Autolock lock(mLock);
    if (mOutput == 0 || mOutput != out) {
        ALOGW("Attempt to close invalid output stream");
    }
    else {
        delete mOutput;
        mOutput = 0;
    }
}

AudioStreamIn* AudioHardware::openInputStream(
        uint32_t devices, int *format, uint32_t *channels, uint32_t *sampleRate, status_t *status,
        AudioSystem::audio_in_acoustics acoustic_flags)
{
    return 0;
}

void AudioHardware::closeInputStream(AudioStreamIn* in) {
}

status_t AudioHardware::setMode(int mode)
{
    return NO_ERROR;
}

status_t AudioHardware::setMicMute(bool state)
{
    return NO_ERROR;
}

status_t AudioHardware::getMicMute(bool* state)
{
    *state = mMicMute;
    return NO_ERROR;
}

status_t AudioHardware::setParameters(const String8& keyValuePairs)
{
    return NO_ERROR;
}

String8 AudioHardware::getParameters(const String8& keys)
{
    AudioParameter request = AudioParameter(keys);
    AudioParameter reply = AudioParameter();

    ALOGV("getParameters() %s", keys.string());

    return reply.toString();
}

size_t AudioHardware::getInputBufferSize(uint32_t sampleRate, int format, int channelCount)
{
    return 4096;
}

status_t AudioHardware::setVoiceVolume(float v)
{
    return NO_ERROR;
}

status_t AudioHardware::setMasterVolume(float v)
{
    ALOGI("Set master volume to %f.\n", v);
    // We return an error code here to let the audioflinger do in-software
    // volume on top of the maximum volume that we set through the SND API.
    // return error - software mixer will handle it
    return -1;
}

status_t AudioHardware::dump(int fd, const Vector<String16>& args)
{
    return NO_ERROR;
}

AudioHardware::AudioStreamOutQ5V2::AudioStreamOutQ5V2() :
    mHardware(0), mFd(-1), mStartCount(0), mRetryCount(0), mStandby(true),
    mDevices(0), mChannels(AUDIO_HW_OUT_CHANNELS), mSampleRate(AUDIO_HW_OUT_SAMPLERATE),
    mBufferSize(AUDIO_HW_OUT_BUFSZ)
{
}

status_t AudioHardware::AudioStreamOutQ5V2::set(
        AudioHardware* hw, uint32_t devices, int *pFormat, uint32_t *pChannels, uint32_t *pRate)
{
    int lFormat = pFormat ? *pFormat : 0;
    uint32_t lChannels = pChannels ? *pChannels : 0;
    uint32_t lRate = pRate ? *pRate : 0;

    mHardware = hw;
    mDevices = devices;

    // fix up defaults
    if (lFormat == 0) lFormat = format();
    if (lChannels == 0) lChannels = channels();
    if (lRate == 0) lRate = sampleRate();

    // check values
    if ((lFormat != format()) ||
        (lChannels != channels()) ||
        (lRate != sampleRate())) {
        if (pFormat) *pFormat = format();
        if (pChannels) *pChannels = channels();
        if (pRate) *pRate = sampleRate();
        return BAD_VALUE;
    }

    if (pFormat) *pFormat = lFormat;
    if (pChannels) *pChannels = lChannels;
    if (pRate) *pRate = lRate;

    mChannels = lChannels;
    mSampleRate = lRate;
    mBufferSize = 4096;

    return NO_ERROR;
}

AudioHardware::AudioStreamOutQ5V2::~AudioStreamOutQ5V2()
{
    standby();
}

ssize_t AudioHardware::AudioStreamOutQ5V2::write(const void* buffer, size_t bytes)
{
    // ALOGD("AudioStreamOutQ5V2::write(%p, %u)", buffer, bytes);
    status_t status = NO_INIT;
    size_t count = bytes;
    const uint8_t* p = static_cast<const uint8_t*>(buffer);

    if (mStandby) {
        ALOGV("open pcm_out driver");
        status = ::open("/dev/msm_pcm_out", O_RDWR);
        if (status < 0) {
                ALOGE("Cannot open /dev/msm_pcm_out errno: %d", errno);
            goto Error;
        }
        mFd = status;

        // configuration
        ALOGV("get config");
        struct msm_audio_config config;
        status = ioctl(mFd, AUDIO_GET_CONFIG, &config);
        if (status < 0) {
            ALOGE("Cannot read pcm_out config");
            goto Error;
        }

        ALOGV("set pcm_out config");
        config.channel_count = AudioSystem::popCount(channels());
        config.sample_rate = mSampleRate;
        config.buffer_size = mBufferSize;
        config.buffer_count = AUDIO_HW_NUM_OUT_BUF;
//        config.codec_type = CODEC_TYPE_PCM;
        status = ioctl(mFd, AUDIO_SET_CONFIG, &config);
        if (status < 0) {
            ALOGE("Cannot set config");
            goto Error;
        }

        ALOGV("buffer_size: %u", config.buffer_size);
        ALOGV("buffer_count: %u", config.buffer_count);
        ALOGV("channel_count: %u", config.channel_count);
        ALOGV("sample_rate: %u", config.sample_rate);

#if 0
        status = ioctl(mFd, AUDIO_START, &acdb_id);
        if (status < 0) {
            ALOGE("Cannot start pcm playback");
            goto Error;
        }

        status = ioctl(mFd, AUDIO_SET_VOLUME, &stream_volume);
        if (status < 0) {
            ALOGE("Cannot start pcm playback");
            goto Error;
        }
#endif
        mStandby = false;
    }

    while (count) {
        ssize_t written = ::write(mFd, p, count);
        if (written >= 0) {
            count -= written;
            p += written;
        } else {
            if (errno != EAGAIN) return written;
            mRetryCount++;
            ALOGW("EAGAIN - retry");
        }
    }

    return bytes;

Error:
    if (mFd >= 0) {
        ::close(mFd);
        mFd = -1;
    }
    // Simulate audio output timing in case of error
    usleep(bytes * 1000000 / frameSize() / sampleRate());

    return status;
}

status_t AudioHardware::AudioStreamOutQ5V2::standby()
{
    status_t status = NO_ERROR;
    if (!mStandby && mFd >= 0) {
        ::close(mFd);
        mFd = -1;
    }
    mStandby = true;
    ALOGI("AudioHardware pcm playback is going to standby.");
    return status;
}

status_t AudioHardware::AudioStreamOutQ5V2::dump(int fd, const Vector<String16>& args)
{
    return NO_ERROR;
}

bool AudioHardware::AudioStreamOutQ5V2::checkStandby()
{
    return mStandby;
}

status_t AudioHardware::AudioStreamOutQ5V2::setParameters(const String8& keyValuePairs)
{
    return NO_ERROR;
}

String8 AudioHardware::AudioStreamOutQ5V2::getParameters(const String8& keys)
{
    AudioParameter param = AudioParameter(keys);
    ALOGV("AudioStreamOutQ5V2::getParameters() %s", param.toString().string());
    return param.toString();
}

status_t AudioHardware::AudioStreamOutQ5V2::getRenderPosition(uint32_t *dspFrames)
{
    return INVALID_OPERATION;
}

extern "C" AudioHardwareInterface* createAudioHardware(void) {
    return new AudioHardware();
}

}; // namespace android
