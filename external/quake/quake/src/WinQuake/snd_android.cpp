/*
 *  snd_android.c
 *  Android-specific sound interface
 *
 */

#include "quakedef.h"

#include <pthread.h>
#include <time.h>
#include <math.h>
#include <stdlib.h>
#include <unistd.h>

#include <android/log.h>
#include <SLES/OpenSLES.h>

#define LOG_TAG "Quake snd_android"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

const size_t SAMPLE_RATE = 11025;


const size_t BYTES_PER_SAMPLE = 2;
const size_t CHANNEL_COUNT = 2;
const size_t BITS_PER_SAMPLE = 8 * BYTES_PER_SAMPLE;

const size_t TOTAL_BUFFER_SIZE = 4 * 1024;

#define MAX_NUMBER_INTERFACES 3

/* Local storage for Audio data in 16 bit words */
#define AUDIO_DATA_STORAGE_SIZE (TOTAL_BUFFER_SIZE / 2)
/* Audio data buffer size in 16 bit words. 8 data segments are used in
this simple example */
#define AUDIO_DATA_BUFFER_SIZE (4096/8)

const size_t NUMBER_OF_BUFFERS = AUDIO_DATA_STORAGE_SIZE / AUDIO_DATA_BUFFER_SIZE;

/* Checks for error. If any errors exit the application! */
void CheckErr( SLresult res )
{
    if ( res != SL_RESULT_SUCCESS )
        {
            fprintf(stdout, "%u SL failure, exiting\n", res);
            exit(EXIT_FAILURE);
        }
    else {
        //fprintf(stdout, "%d SL success, proceeding...\n", res);
    }
}

/* Structure for passing information to callback function */
typedef struct CallbackCntxt_ {
    SLPlayItf  playItf;
    SLint16*   pDataBase;    // Base adress of local audio data storage
    SLint16*   pData;        // Current adress of local audio data storage
    SLuint32   size;
} CallbackCntxt;

/* Local storage for Audio data */
SLint16 pcmData[AUDIO_DATA_STORAGE_SIZE];

/* Callback for Buffer Queue events */
void BufferQueueCallback(
        SLBufferQueueItf queueItf,
        void *pContext)
{
    //fprintf(stdout, "BufferQueueCallback called\n");
    SLresult res;
    //fprintf(stdout, " pContext=%p\n", pContext);
    CallbackCntxt *pCntxt = (CallbackCntxt*)pContext;

    if (pCntxt->pData >= (pCntxt->pDataBase + pCntxt->size)) {
        pCntxt->pData = pCntxt->pDataBase;
    }
    {
        //fprintf(stdout, "callback: before enqueue\n");
        res = (*queueItf)->Enqueue(queueItf, (void*) pCntxt->pData,
                2 * AUDIO_DATA_BUFFER_SIZE); /* Size given in bytes. */
        CheckErr(res);
        /* Increase data pointer by buffer size */
        pCntxt->pData += AUDIO_DATA_BUFFER_SIZE;
    }
    //fprintf(stdout, "end of BufferQueueCallback()\n");
}

SLEngineItf                EngineItf;

SLint32                    numOutputs = 0;
SLuint32                   deviceID = 0;


SLDataSource               audioSource;
SLDataLocator_BufferQueue  bufferQueue;
SLDataFormat_PCM           pcm;

SLDataSink                 audioSink;
SLDataLocator_OutputMix    locator_outputmix;


SLVolumeItf                volumeItf;


SLboolean required[MAX_NUMBER_INTERFACES];
SLInterfaceID iidArray[MAX_NUMBER_INTERFACES];

/* Callback context for the buffer queue callback function */
CallbackCntxt cntxt;

static SLObjectItf                OutputMix;
static SLPlayItf                  playItf;
static SLObjectItf                player;
static SLBufferQueueItf           bufferQueueItf;
static SLBufferQueueState         state;

/* Play some audio from a buffer queue  */
void TestPlaySawtoothBufferQueue( SLObjectItf sl )
{
    SLresult                   res;
    int                        i;

    /* Get the SL Engine Interface which is implicit */
    res = (*sl)->GetInterface(sl, SL_IID_ENGINE, (void*)&EngineItf);
    CheckErr(res);

    /* Initialize arrays required[] and iidArray[] */
    for (i=0;i<MAX_NUMBER_INTERFACES;i++)
        {
            required[i] = SL_BOOLEAN_FALSE;
            iidArray[i] = SL_IID_NULL;
        }

    // Set arrays required[] and iidArray[] for VOLUME interface
    required[0] = SL_BOOLEAN_TRUE;
    iidArray[0] = SL_IID_VOLUME;
    // Create Output Mix object to be used by player
    res = (*EngineItf)->CreateOutputMix(EngineItf, &OutputMix, 0,
            iidArray, required); CheckErr(res);

    // Realizing the Output Mix object in synchronous mode.
    res = (*OutputMix)->Realize(OutputMix, SL_BOOLEAN_FALSE);
    CheckErr(res);

#if 0
    res = (*OutputMix)->GetInterface(OutputMix, SL_IID_VOLUME,
            (void*)&volumeItf); CheckErr(res);
#endif

    /* Setup the data source structure for the buffer queue */
    bufferQueue.locatorType = SL_DATALOCATOR_BUFFERQUEUE;
    bufferQueue.numBuffers = 4;  /* Four buffers in our buffer queue */

    /* Setup the format of the content in the buffer queue */
    pcm.formatType = SL_DATAFORMAT_PCM;
    pcm.numChannels = 2;
    pcm.samplesPerSec = SL_SAMPLINGRATE_11_025;
    pcm.bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_16;
    pcm.containerSize = 16;
    pcm.channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    pcm.endianness = SL_BYTEORDER_LITTLEENDIAN;

    audioSource.pFormat      = (void *)&pcm;
    audioSource.pLocator     = (void *)&bufferQueue;

    /* Setup the data sink structure */
    locator_outputmix.locatorType   = SL_DATALOCATOR_OUTPUTMIX;
    locator_outputmix.outputMix    = OutputMix;
    audioSink.pLocator           = (void *)&locator_outputmix;
    audioSink.pFormat            = NULL;

    /* Initialize the audio data to silence */
    memset(pcmData, 0, sizeof(pcmData));

    /* Initialize the context for Buffer queue callbacks */
    cntxt.pDataBase = /*(void*)&*/pcmData;
    cntxt.pData = cntxt.pDataBase;
    cntxt.size = sizeof(pcmData) / 2;

    /* Set arrays required[] and iidArray[] for SEEK interface
          (PlayItf is implicit) */
    required[0] = SL_BOOLEAN_TRUE;
    iidArray[0] = SL_IID_BUFFERQUEUE;

    /* Create the music player */
    res = (*EngineItf)->CreateAudioPlayer(EngineItf, &player,
            &audioSource, &audioSink, 1, iidArray, required); CheckErr(res);
    fprintf(stdout, "bufferQueue example: after CreateAudioPlayer\n");

    /* Realizing the player in synchronous mode. */
    res = (*player)->Realize(player, SL_BOOLEAN_FALSE); CheckErr(res);
    fprintf(stdout, "bufferQueue example: after Realize\n");

    /* Get seek and play interfaces */
    res = (*player)->GetInterface(player, SL_IID_PLAY, (void*)&playItf);
    CheckErr(res);
    fprintf(stdout, "bufferQueue example: after GetInterface(PLAY)\n");

    res = (*player)->GetInterface(player, SL_IID_BUFFERQUEUE,
            (void*)&bufferQueueItf); CheckErr(res);

    /* Setup to receive buffer queue event callbacks */
    res = (*bufferQueueItf)->RegisterCallback(bufferQueueItf,
            BufferQueueCallback, &cntxt); CheckErr(res);

#if 0
    /* Before we start set volume to -3dB (-300mB) */
    res = (*volumeItf)->SetVolumeLevel(volumeItf, -300); CheckErr(res);
#endif

    /* Enqueue a few buffers to get the ball rolling */
    res = (*bufferQueueItf)->Enqueue(bufferQueueItf, cntxt.pData,
            2 * AUDIO_DATA_BUFFER_SIZE); /* Size given in bytes. */
    CheckErr(res);
    cntxt.pData += AUDIO_DATA_BUFFER_SIZE;

    res = (*bufferQueueItf)->Enqueue(bufferQueueItf, cntxt.pData,
            2 * AUDIO_DATA_BUFFER_SIZE); /* Size given in bytes. */
    CheckErr(res);
    cntxt.pData += AUDIO_DATA_BUFFER_SIZE;

    res = (*bufferQueueItf)->Enqueue(bufferQueueItf, cntxt.pData,
            2 * AUDIO_DATA_BUFFER_SIZE); /* Size given in bytes. */
    CheckErr(res);
    cntxt.pData += AUDIO_DATA_BUFFER_SIZE;

    /* Play the PCM samples using a buffer queue */
    fprintf(stdout, "bufferQueue example: starting to play\n");
    res = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_PLAYING );
    CheckErr(res);

    /* Wait until the PCM data is done playing, the buffer queue callback
           will continue to queue buffers until the entire PCM data has been
           played. This is indicated by waiting for the count member of the
           SLBufferQueueState to go to zero.
     */
    res = (*bufferQueueItf)->GetState(bufferQueueItf, &state);
    CheckErr(res);

#if 0
    // while (state.playIndex < 100) {
    while (state.count) {
        usleep(10000);
        (*bufferQueueItf)->GetState(bufferQueueItf, &state);
    }

 #endif
}

SLObjectItf gSoundEngine;

int startAndroidSound()
{
    SLresult    res;

    SLEngineOption EngineOption[] = {
            {(SLuint32) SL_ENGINEOPTION_THREADSAFE,
            (SLuint32) SL_BOOLEAN_TRUE}};

    res = slCreateEngine( &gSoundEngine, 1, EngineOption, 0, NULL, NULL);
    CheckErr(res);
    /* Realizing the SL Engine in synchronous mode. */
    res = (*gSoundEngine)->Realize(gSoundEngine, SL_BOOLEAN_FALSE); CheckErr(res);

    /* Run the test */
    TestPlaySawtoothBufferQueue(gSoundEngine);
    return EXIT_SUCCESS;
}

void finishAndroidSound()
{
    SLresult                   res;

    if (gSoundEngine == NULL) {
        return;
    }

    /* Make sure player is stopped */
    if (playItf != NULL) {
         res = (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_STOPPED);
         CheckErr(res);
         playItf = NULL;
    }

    if (player != NULL) {
         /* Destroy the player */
         (*player)->Destroy(player);
         player = NULL;
    }

    if (OutputMix != NULL) {
        /* Destroy Output Mix object */
        (*OutputMix)->Destroy(OutputMix);
        OutputMix = NULL;
    }

    /* Shutdown OpenSL ES */
    (*gSoundEngine)->Destroy(gSoundEngine);
    gSoundEngine = NULL;
}

#if 1

/*
==================
SNDDMA_Init

Try to find a sound device to mix for.
Returns false if nothing is found.
==================
*/
qboolean SNDDMA_Init(void)
{
    // Initialize Quake's idea of a DMA buffer.

    shm = &sn;
    memset((void*)&sn, 0, sizeof(sn));

    shm->splitbuffer = false;	// Not used.
    shm->samplebits = 16;
    shm->speed = 11025;
    shm->channels = 2;
    shm->samples = TOTAL_BUFFER_SIZE / BYTES_PER_SAMPLE;
    shm->samplepos = 0; // Not used.
    shm->buffer = (unsigned char*) pcmData;
    shm->submission_chunk = 1; // Not used.

    shm->soundalive = true;

    if ( (shm->samples & 0x1ff) != 0 ) {
      LOGE("SNDDDMA_Init: samples must be power of two.");
      return false;
    }

    if ( shm->buffer == 0 ) {
      LOGE("SNDDDMA_Init: Could not allocate sound buffer.");
      return false;
    }

    int result = startAndroidSound();
    return result == EXIT_SUCCESS;
}

/*
==============
SNDDMA_GetDMAPos

return the current sample position (in mono samples read)
inside the recirculating dma buffer, so the mixing code will know
how many sample are required to fill it up.
===============
*/
int SNDDMA_GetDMAPos(void)
{
    SLresult                   res;
    if (bufferQueueItf != NULL) {
        res = (*bufferQueueItf)->GetState(bufferQueueItf, &state);
        CheckErr(res);
        // Index of the currently playing or filling buffer.
        SLuint32 playIndex = state.playIndex;
        int ringIndex = playIndex % NUMBER_OF_BUFFERS;
        return ringIndex * AUDIO_DATA_BUFFER_SIZE;
    }
    return 0;
}

/*
===============
SNDDMA_ReportWrite

Report valid data being written into the DMA buffer by the sound mixing code.
This is an Android specific API.
================
*/
void SNDDMA_ReportWrite(size_t lengthBytes) {
    // TODO: keep track of how much of the sound ring buffer has sound in it,
    // detect starvation, and mix silence when we are starving.
}

/*
==============
SNDDMA_Submit

Send sound to device if buffer isn't really the dma buffer
===============
*/
void SNDDMA_Submit(void)
{
}

/*
==============
SNDDMA_Shutdown

Reset the sound device for exiting
===============
*/
void SNDDMA_Shutdown(void)
{
    finishAndroidSound();
}


#else

// Written by the callback function running in an audio thread.
// index in bytes of where we last read.

static volatile size_t gDMAByteIndex;


// Written by main thread
static size_t gAvailableBytes;
static bool gSoundMixingStarted;

// The condition is "new data is now available"

static pthread_mutex_t condition_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  condition_cond  = PTHREAD_COND_INITIALIZER;

/*
==================
SNDDMA_Init

Try to find a sound device to mix for.
Returns false if nothing is found.
==================
*/


const size_t SAMPLE_RATE = 11025;


const size_t BYTES_PER_SAMPLE = 2;
const size_t CHANNEL_COUNT = 2;
const size_t BITS_PER_SAMPLE = 8 * BYTES_PER_SAMPLE;

const size_t TOTAL_BUFFER_SIZE = 16 * 1024;

static size_t min(size_t a, size_t b) {
  return a < b ? a : b;
}

static size_t mod(size_t value, size_t mod) {
  return value % mod;
}

static size_t next(size_t value, size_t mod) {
  value = value + 1;
  if ( value >= mod ) {
    value = 0;
  }
  return value;
}

static size_t prev(size_t value, size_t mod) {
  if ( value <= 0 ) {
    value = mod;
  }
  return value - 1;
}


static bool enableSound() {

    if (COM_CheckParm("-nosound"))
        return false;

  return true;
}

// Choose one:

// #define GENERATE_SINE_WAVE
#define NORMAL_SOUND

#ifdef GENERATE_SINE_WAVE

static const float p = 2 * M_PI * 440.0f / SAMPLE_RATE;
static float left = 0.0f;
static float right = 0.0f;

static float sinef(float x)
{
    const float A =   1.0f / (2.0f*M_PI);
    const float B = -16.0f;
    const float C =   8.0f;

    // scale angle for easy argument reduction
    x *= A;

    if (fabsf(x) >= 0.5f) {
        // Argument reduction
        x = x - ceilf(x + 0.5f) + 1.0f;
    }

    const float y = B*x*fabsf(x) + C*x;
    return 0.2215f * (y*fabsf(y) - y) + y;
}

static
void AndroidQuakeSoundCallback(int event, void* user, void *info) {

    if (event != AudioTrack::EVENT_MORE_DATA) return;

    const AudioTrack::Buffer *buffer = static_cast<const AudioTrack::Buffer *>(info);
    size_t bytesToCopy = buffer->size;
    size_t framesToCopy = buffer->size / (BYTES_PER_SAMPLE * CHANNEL_COUNT);
    short* pData = buffer->i16;

    for(size_t frame = 0; frame < framesToCopy; frame++) {
        short leftSample = (short) (32767.0f * sinef(left));
        left += p;
        if (left > 2*M_PI) {
            left -= 2*M_PI;
        }
        pData[frame * CHANNEL_COUNT] = leftSample;

        short rightSample = (short) (32767.0f * sinef(right));
        right += 2 * p;
        if (right > 2*M_PI) {
            right -= 2*M_PI;
        }
        pData[1 + frame * CHANNEL_COUNT] = rightSample;
    }

    gDMAByteIndex = mod(gDMAByteIndex + bytesToCopy, TOTAL_BUFFER_SIZE);
    asm volatile ("":::"memory");
}

#endif

#ifdef NORMAL_SOUND

static bool gWaitingForMixerToRestart;

// Assumes the mutex is acquired.
// Waits until audio is available or a time period has elapsed.
static bool shouldMixSilence() {
  if (!gSoundMixingStarted) {
    return true;
  }
    while (gAvailableBytes == 0) {
      if (gWaitingForMixerToRestart) {
        return true;
      }
        timeval tp;
        if (gettimeofday(&tp, NULL)) {
          return true;
        }
     const long WAIT_NS = 40 * 1000 * 1000;
     const long NS_PER_SECOND = 1000 * 1000 * 1000;
     timespec ts;
     ts.tv_sec  = tp.tv_sec;
     ts.tv_nsec = tp.tv_usec * 1000 + WAIT_NS;
     if (ts.tv_nsec >= NS_PER_SECOND) {
       ts.tv_nsec -= NS_PER_SECOND;
       ts.tv_sec += 1;
     }
     if (ETIMEDOUT == pthread_cond_timedwait( &condition_cond,  &condition_mutex, &ts)) {
       gWaitingForMixerToRestart = true;
       return true;
     }
    }
    gWaitingForMixerToRestart = false;
    return false;
}

static
void AndroidQuakeSoundCallback(int event, void* user, void *info) {

    if (event != AudioTrack::EVENT_MORE_DATA) return;

    const AudioTrack::Buffer *buffer = static_cast<const AudioTrack::Buffer *>(info);
    size_t dmaByteIndex = gDMAByteIndex;
    size_t size = buffer->size;
    unsigned char* pDestBuffer = (unsigned char*) buffer->raw;

    if (size == 0) return;

    if ( ! shm ) {
        memset(pDestBuffer, 0, size);
        return;
    }

    const unsigned char* pSrcBuffer = shm->buffer;

    while(size > 0) {
        pthread_mutex_lock( &condition_mutex );

        if (shouldMixSilence()) {
          memset(pDestBuffer, 0, size);
          pthread_mutex_unlock( &condition_mutex );
          return;
        }

        size_t chunkSize = min(gAvailableBytes, min(TOTAL_BUFFER_SIZE-dmaByteIndex, size));
        gAvailableBytes -= chunkSize;

        pthread_mutex_unlock( &condition_mutex );

    memcpy(pDestBuffer, pSrcBuffer + dmaByteIndex, chunkSize);
    size -= chunkSize;
    pDestBuffer += chunkSize;
    dmaByteIndex += chunkSize;
    if (dmaByteIndex >= TOTAL_BUFFER_SIZE) {
      dmaByteIndex = 0;
    }
  }

  gDMAByteIndex = dmaByteIndex;
  asm volatile ("":::"memory");
}

#endif

qboolean SNDDMA_Init(void)
{
  if ( ! enableSound() ) {
    return false;
  }

  gDMAByteIndex = 0;

  // Initialize the AudioTrack.

  status_t result = gAudioTrack.set(
    AUDIO_STREAM_DEFAULT, // stream type
    SAMPLE_RATE,   // sample rate
    BITS_PER_SAMPLE == 16 ? AUDIO_FORMAT_PCM_16_BIT : AUDIO_FORMAT_PCM_8_BIT,      // format (8 or 16)
    (CHANNEL_COUNT > 1) ? AUDIO_CHANNEL_OUT_STEREO : AUDIO_CHANNEL_OUT_MONO,       // channel mask
    0,       // default buffer size
    (audio_output_flags_t) 0, // AUDIO_OUTPUT_FLAG_NONE
    AndroidQuakeSoundCallback, // callback
    0,  // user
    0); // default notification size

  LOGI("AudioTrack status  = %d (%s)\n", result, result == NO_ERROR ? "success" : "error");

  if ( result == NO_ERROR ) {
    LOGI("AudioTrack latency = %u ms\n", gAudioTrack.latency());
    LOGI("AudioTrack format = %u bits\n", gAudioTrack.format() == AUDIO_FORMAT_PCM_16_BIT ? 16 : 8);
    LOGI("AudioTrack sample rate = %u Hz\n", gAudioTrack.getSampleRate());
    LOGI("AudioTrack frame count = %d\n", int(gAudioTrack.frameCount()));
    LOGI("AudioTrack channel count = %d\n", gAudioTrack.channelCount());

    // Initialize Quake's idea of a DMA buffer.

    shm = &sn;
    memset((void*)&sn, 0, sizeof(sn));

    shm->splitbuffer = false;	// Not used.
    shm->samplebits = gAudioTrack.format() == AUDIO_FORMAT_PCM_16_BIT ? 16 : 8;
    shm->speed = gAudioTrack.getSampleRate();
    shm->channels = gAudioTrack.channelCount();
    shm->samples = TOTAL_BUFFER_SIZE / BYTES_PER_SAMPLE;
    shm->samplepos = 0; // Not used.
    shm->buffer = (unsigned char*) Hunk_AllocName(TOTAL_BUFFER_SIZE, (char*) "shmbuf");
    shm->submission_chunk = 1; // Not used.

    shm->soundalive = true;

    if ( (shm->samples & 0x1ff) != 0 ) {
      LOGE("SNDDDMA_Init: samples must be power of two.");
      return false;
    }

    if ( shm->buffer == 0 ) {
      LOGE("SNDDDMA_Init: Could not allocate sound buffer.");
      return false;
    }

    gAudioTrack.setVolume(1.0f, 1.0f);
    gAudioTrack.start();
  }

  return result == NO_ERROR;
}

/*
==============
SNDDMA_GetDMAPos

return the current sample position (in mono samples read)
inside the recirculating dma buffer, so the mixing code will know
how many sample are required to fill it up.
===============
*/
int SNDDMA_GetDMAPos(void)
{
  int dmaPos = gDMAByteIndex / BYTES_PER_SAMPLE;
  asm volatile ("":::"memory");
  return dmaPos;
}

/*
===============
SNDDMA_ReportWrite

Report valid data being written into the DMA buffer by the sound mixing code.
This is an Android specific API.
================
*/
void SNDDMA_ReportWrite(size_t lengthBytes) {
    pthread_mutex_lock( &condition_mutex );
    gSoundMixingStarted = true;
    if (gAvailableBytes == 0) {
        pthread_cond_signal( &condition_cond );
    }
    gAvailableBytes += lengthBytes;
    pthread_mutex_unlock( &condition_mutex );
}

/*
==============
SNDDMA_Submit

Send sound to device if buffer isn't really the dma buffer
===============
*/
void SNDDMA_Submit(void)
{
}

/*
==============
SNDDMA_Shutdown

Reset the sound device for exiting
===============
*/
void SNDDMA_Shutdown(void)
{
  gAudioTrack.stop();
}

#endif
