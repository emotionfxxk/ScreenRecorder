#include "ScreenRecorder.h"

#define LOG_TAG "ScreenRecorder"
#define LOG_NDEBUG 0
#include <utils/Log.h>

#include <binder/IPCThreadState.h>
#include <utils/Errors.h>
#include <utils/Thread.h>
#include <utils/Timers.h>

#include <gui/Surface.h>
#include <gui/SurfaceComposerClient.h>
#include <gui/ISurfaceComposer.h>
#include <ui/DisplayInfo.h>
#include <media/openmax/OMX_IVCommon.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/MediaCodec.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaMuxer.h>
#include <media/ICrypto.h>

#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <stdio.h>
#include <fcntl.h>
#include <signal.h>
#include <getopt.h>
#include <sys/wait.h>

#include <pthread.h>
using namespace android;

static const uint32_t kMinBitRate = 100000;         // 0.1Mbps
static const uint32_t kMaxBitRate = 100 * 1000000;  // 100Mbps
static const uint32_t kMaxTimeLimitSec = 180;       // 3 minutes
static const uint32_t kFallbackWidth = 1280;        // 720p
static const uint32_t kFallbackHeight = 720;

// Command-line parameters.
//static bool gVerbose = false;               // chatty on stdout
//static bool gRotate = false;                // rotate 90 degrees
//static bool gSizeSpecified = false;         // was size explicitly requested?
//static uint32_t gVideoWidth = 0;            // default width+height
//static uint32_t gVideoHeight = 0;
//static uint32_t gBitRate = 4000000;         // 4Mbps
//static uint32_t gTimeLimitSec = kMaxTimeLimitSec;

// Set by signal handler to stop recording.
static bool gStopRequested;

// Previous signal handler state, restored after first hit.
static struct sigaction gOrigSigactionINT;
static struct sigaction gOrigSigactionHUP;

// single instance of screen recorder
static ScreenRecorder* sRecorder = NULL;

// params for recorder
static uint32_t sWidth = 0;
static uint32_t sHeight = 0;
static uint32_t sBitRate = 4000000;         // 4Mbps
static uint32_t sTimeLimitSec = kMaxTimeLimitSec;
static char* sFileName = NULL;
static bool sRotate = false;
static bool sIsRecorderRunning = false;

// inner methods declarations
static status_t recordScreen(const char* fileName);
//static status_t configureSignals();
//static void signalCatcher(int signum);
static bool isDeviceRotated(int orientation);
static status_t runEncoder(const sp<MediaCodec>& encoder, const sp<MediaMuxer>& muxer);
static status_t prepareVirtualDisplay(const DisplayInfo& mainDpyInfo,
        const sp<IGraphicBufferProducer>& bufferProducer,
        sp<IBinder>* pDisplayHandle);
static status_t prepareEncoder(float displayFps, sp<MediaCodec>* pCodec,
        sp<IGraphicBufferProducer>* pBufferProducer);

static void* recorder_thread_func(void* p_thread_data);

ScreenRecorder::ScreenRecorder() {
}

ScreenRecorder* ScreenRecorder::getRecorder() {
    if(sRecorder == NULL) {
        sRecorder = new ScreenRecorder();
    }
    return sRecorder;
}
bool ScreenRecorder::init(int width, int height, int bitrate, int timeLimit, bool rotate, const char* destFilePath) {
    if(sIsRecorderRunning) {
        ALOGW("Failed to init screen recorder, recorder is running now!");
        return false;
    }
    sWidth = width;
    sHeight = height;
    sBitRate = bitrate;
    sTimeLimitSec = timeLimit;
    if(sFileName != NULL) {
        free(sFileName);
        sFileName = NULL;
    }
    sFileName = strdup(destFilePath);
    sRotate = rotate;
    ALOGV("init WxH:[%d:%d], bitrate:[%d], time limit:[%d], rotate:[%d], dest file name:[%s]",
        sWidth, sHeight, sBitRate, sTimeLimitSec, sRotate, sFileName);
    return true;
}

bool ScreenRecorder::isRunning() {
    return sIsRecorderRunning;
}

bool ScreenRecorder::start() {
    if(sIsRecorderRunning) {
        ALOGW("Failed to start screen recorder, recorder is running now!");
        return false;
    }

    ALOGV("ScreenRecorder::start(), try to recorder screen to %s", sFileName);

    pthread_t thread;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    int res = pthread_create(&thread, &attr, recorder_thread_func, NULL);
    pthread_attr_destroy(&attr);

    sIsRecorderRunning = (res == NO_ERROR);
    if (!sIsRecorderRunning) {
        ALOGE("failed to create thread! res:%d",  res);
    }
    return sIsRecorderRunning;
}

bool ScreenRecorder::stop() {
    if(!sIsRecorderRunning) {
        ALOGW("Failed to stop screen recorder, recorder is not running now!");
        return false;
    }
    return true;
}


/*
 * Main "do work" method.
 *
 * Configures codec, muxer, and virtual display, then starts moving bits
 * around.
 */
static status_t recordScreen(const char* fileName) {
    status_t err;

    // TODO: we don't need signal handler here??? maybe
    /*
    // Configure signal handler.
    err = configureSignals();
    if (err != NO_ERROR) return err;
    */

    // Start Binder thread pool.  MediaCodec needs to be able to receive
    // messages from mediaserver.
    sp<ProcessState> self = ProcessState::self();
    self->startThreadPool();

    // Get main display parameters.
    sp<IBinder> mainDpy = SurfaceComposerClient::getBuiltInDisplay(
            ISurfaceComposer::eDisplayIdMain);
    DisplayInfo mainDpyInfo;
    err = SurfaceComposerClient::getDisplayInfo(mainDpy, &mainDpyInfo);
    if (err != NO_ERROR) {
        ALOGE("recordScreen: unable to get display characteristics");
        return err;
    }

    ALOGD("recordScreen: Main display is %dx%d @%.2ffps (orientation=%u)",
            mainDpyInfo.w, mainDpyInfo.h, mainDpyInfo.fps,
            mainDpyInfo.orientation);

    bool rotated = isDeviceRotated(mainDpyInfo.orientation);
    if (sWidth == 0) {
        sWidth = rotated ? mainDpyInfo.h : mainDpyInfo.w;
    }
    if (sHeight == 0) {
        sHeight = rotated ? mainDpyInfo.w : mainDpyInfo.h;
    }

    // Configure and start the encoder.
    sp<MediaCodec> encoder;
    sp<IGraphicBufferProducer> bufferProducer;
    err = prepareEncoder(mainDpyInfo.fps, &encoder, &bufferProducer);

    if (err != NO_ERROR /*&& !gSizeSpecified*/) {
        // fallback is defined for landscape; swap if we're in portrait
        bool needSwap = sWidth < sHeight;
        uint32_t newWidth = needSwap ? kFallbackHeight : kFallbackWidth;
        uint32_t newHeight = needSwap ? kFallbackWidth : kFallbackHeight;
        if (sWidth != newWidth && sHeight != newHeight) {
            ALOGV("recordScreen: Retrying with 720p");
            ALOGW("recordScreen: WARNING: failed at %dx%d, retrying at %dx%d",
                    sWidth, sHeight, newWidth, newHeight);
            sWidth = newWidth;
            sHeight = newHeight;
            err = prepareEncoder(mainDpyInfo.fps, &encoder, &bufferProducer);
        }
    }
    if (err != NO_ERROR) {
        return err;
    }

    // Configure virtual display.
    sp<IBinder> dpy;
    err = prepareVirtualDisplay(mainDpyInfo, bufferProducer, &dpy);
    if (err != NO_ERROR) {
        encoder->release();
        encoder.clear();
        ALOGE("recordScreen: failed to prepare virtual display with err=%d", err);
        return err;
    }

    // Configure, but do not start, muxer.
    sp<MediaMuxer> muxer = new MediaMuxer(fileName,
            MediaMuxer::OUTPUT_FORMAT_MPEG_4);
    if (sRotate) {
        muxer->setOrientationHint(90);
    }

    // Main encoder loop.
    err = runEncoder(encoder, muxer);
    if (err != NO_ERROR) {
        encoder->release();
        encoder.clear();
        ALOGE("recordScreen: failed to run encoder with err=%d", err);
        return err;
    }

    ALOGV("Stopping encoder and muxer");

    // Shut everything down, starting with the producer side.
    bufferProducer = NULL;
    SurfaceComposerClient::destroyDisplay(dpy);

    encoder->stop();
    muxer->stop();
    encoder->release();

    return 0;
}

/*
 * Configures signal handlers.  The previous handlers are saved.
 *
 * If the command is run from an interactive adb shell, we get SIGINT
 * when Ctrl-C is hit.  If we're run from the host, the local adb process
 * gets the signal, and we get a SIGHUP when the terminal disconnects.
 */
 /*
static status_t configureSignals()
{
    struct sigaction act;
    memset(&act, 0, sizeof(act));
    act.sa_handler = signalCatcher;
    if (sigaction(SIGINT, &act, &gOrigSigactionINT) != 0) {
        status_t err = -errno;
        fprintf(stderr, "Unable to configure SIGINT handler: %s\n",
                strerror(errno));
        return err;
    }
    if (sigaction(SIGHUP, &act, &gOrigSigactionHUP) != 0) {
        status_t err = -errno;
        fprintf(stderr, "Unable to configure SIGHUP handler: %s\n",
                strerror(errno));
        return err;
    }
    return NO_ERROR;
}*/

/*
 * Catch keyboard interrupt signals.  On receipt, the "stop requested"
 * flag is raised, and the original handler is restored (so that, if
 * we get stuck finishing, a second Ctrl-C will kill the process).
 */
 /*
static void signalCatcher(int signum)
{
    gStopRequested = true;
    switch (signum) {
    case SIGINT:
    case SIGHUP:
        sigaction(SIGINT, &gOrigSigactionINT, NULL);
        sigaction(SIGHUP, &gOrigSigactionHUP, NULL);
        break;
    default:
        abort();
        break;
    }
}*/

/*
 * Returns "true" if the device is rotated 90 degrees.
 */
static bool isDeviceRotated(int orientation) {
    return orientation != DISPLAY_ORIENTATION_0 &&
            orientation != DISPLAY_ORIENTATION_180;
}

/*
 * Configures and starts the MediaCodec encoder.  Obtains an input surface
 * from the codec.
 */
static status_t prepareEncoder(float displayFps, sp<MediaCodec>* pCodec,
        sp<IGraphicBufferProducer>* pBufferProducer) {
    status_t err;
    ALOGV("prepareEncoder: Configuring recorder for %dx%d video at %.2fMbps",
            sWidth, sHeight, sBitRate / 1000000.0);

    sp<AMessage> format = new AMessage;
    format->setInt32("width", sWidth);
    format->setInt32("height", sHeight);
    format->setString("mime", "video/avc");
    format->setInt32("color-format", OMX_COLOR_FormatAndroidOpaque);
    format->setInt32("bitrate", sBitRate);
    format->setFloat("frame-rate", displayFps);
    format->setInt32("i-frame-interval", 10);

    sp<ALooper> looper = new ALooper;
    looper->setName("screenrecord_looper");
    looper->start();
    ALOGV("prepareEncoder: Creating codec");
    sp<MediaCodec> codec = MediaCodec::CreateByType(looper, "video/avc", true);
    if (codec == NULL) {
        ALOGE("prepareEncoder: ERROR: unable to create video/avc codec instance");
        return UNKNOWN_ERROR;
    }
    err = codec->configure(format, NULL, NULL,
            MediaCodec::CONFIGURE_FLAG_ENCODE);
    if (err != NO_ERROR) {
        codec->release();
        codec.clear();

        ALOGE("prepareEncoder: ERROR: unable to configure codec (err=%d)", err);
        return err;
    }

    ALOGV("prepareEncoder: Creating buffer producer");
    sp<IGraphicBufferProducer> bufferProducer;
    err = codec->createInputSurface(&bufferProducer);
    if (err != NO_ERROR) {
        codec->release();
        codec.clear();

        ALOGE("prepareEncoder: ERROR: unable to create encoder input surface (err=%d)", err);
        return err;
    }

    ALOGV("prepareEncoder: Starting codec");
    err = codec->start();
    if (err != NO_ERROR) {
        codec->release();
        codec.clear();

        ALOGE("prepareEncoder: ERROR: unable to start codec (err=%d)\n", err);
        return err;
    }

    ALOGV("prepareEncoder: Codec prepared");
    *pCodec = codec;
    *pBufferProducer = bufferProducer;
    return 0;
}

/*
 * Configures the virtual display.  When this completes, virtual display
 * frames will start being sent to the encoder's surface.
 */
static status_t prepareVirtualDisplay(const DisplayInfo& mainDpyInfo,
        const sp<IGraphicBufferProducer>& bufferProducer,
        sp<IBinder>* pDisplayHandle) {
    status_t err;

    // Set the region of the layer stack we're interested in, which in our
    // case is "all of it".  If the app is rotated (so that the width of the
    // app is based on the height of the display), reverse width/height.
    bool deviceRotated = isDeviceRotated(mainDpyInfo.orientation);
    ALOGV("prepareVirtualDisplay deviceRotated:%d", deviceRotated);
    uint32_t sourceWidth, sourceHeight;
    if (!deviceRotated) {
        sourceWidth = mainDpyInfo.w;
        sourceHeight = mainDpyInfo.h;
    } else {
        sourceHeight = mainDpyInfo.w;
        sourceWidth = mainDpyInfo.h;
    }
    Rect layerStackRect(sourceWidth, sourceHeight);

    // We need to preserve the aspect ratio of the display.
    float displayAspect = (float) sourceHeight / (float) sourceWidth;


    // Set the way we map the output onto the display surface (which will
    // be e.g. 1280x720 for a 720p video).  The rect is interpreted
    // post-rotation, so if the display is rotated 90 degrees we need to
    // "pre-rotate" it by flipping width/height, so that the orientation
    // adjustment changes it back.
    //
    // We might want to encode a portrait display as landscape to use more
    // of the screen real estate.  (If players respect a 90-degree rotation
    // hint, we can essentially get a 720x1280 video instead of 1280x720.)
    // In that case, we swap the configured video width/height and then
    // supply a rotation value to the display projection.
    uint32_t videoWidth, videoHeight;
    uint32_t outWidth, outHeight;
    if (!sRotate) {
        videoWidth = sWidth;
        videoHeight = sHeight;
    } else {
        videoWidth = sHeight;
        videoHeight = sWidth;
    }
    if (videoHeight > (uint32_t)(videoWidth * displayAspect)) {
        // limited by narrow width; reduce height
        outWidth = videoWidth;
        outHeight = (uint32_t)(videoWidth * displayAspect);
    } else {
        // limited by short height; restrict width
        outHeight = videoHeight;
        outWidth = (uint32_t)(videoHeight / displayAspect);
    }
    uint32_t offX, offY;
    offX = (videoWidth - outWidth) / 2;
    offY = (videoHeight - outHeight) / 2;
    Rect displayRect(offX, offY, offX + outWidth, offY + outHeight);

    if (sRotate) {
        ALOGV("prepareVirtualDisplay: Rotated content area is %ux%u at offset x=%d y=%d",
                outHeight, outWidth, offY, offX);
    } else {
        ALOGV("prepareVirtualDisplay: Content area is %ux%u at offset x=%d y=%d",
                outWidth, outHeight, offX, offY);
    }

    sp<IBinder> dpy = SurfaceComposerClient::createDisplay(
            String8("ScreenRecorder"), false /* secure */);

    SurfaceComposerClient::openGlobalTransaction();
    SurfaceComposerClient::setDisplaySurface(dpy, bufferProducer);
    SurfaceComposerClient::setDisplayProjection(dpy,
            sRotate ? DISPLAY_ORIENTATION_90 : DISPLAY_ORIENTATION_0,
            layerStackRect, displayRect);
    SurfaceComposerClient::setDisplayLayerStack(dpy, 0);    // default stack
    SurfaceComposerClient::closeGlobalTransaction();

    *pDisplayHandle = dpy;

    return NO_ERROR;
}

/*
 * Runs the MediaCodec encoder, sending the output to the MediaMuxer.  The
 * input frames are coming from the virtual display as fast as SurfaceFlinger
 * wants to send them.
 *
 * The muxer must *not* have been started before calling.
 */
static status_t runEncoder(const sp<MediaCodec>& encoder,
        const sp<MediaMuxer>& muxer) {
    static int kTimeout = 250000;   // be responsive on signal
    status_t err;
    ssize_t trackIdx = -1;
    uint32_t debugNumFrames = 0;
    int64_t startWhenNsec = systemTime(CLOCK_MONOTONIC);
    int64_t endWhenNsec = startWhenNsec + seconds_to_nanoseconds(sTimeLimitSec);

    Vector<sp<ABuffer> > buffers;
    err = encoder->getOutputBuffers(&buffers);
    if (err != NO_ERROR) {
        ALOGE("runEncoder: Unable to get output buffers (err=%d)", err);
        return err;
    }

    // This is set by the signal handler.
    gStopRequested = false;

    // Run until we're signaled.
    while (!gStopRequested) {
        size_t bufIndex, offset, size;
        int64_t ptsUsec;
        uint32_t flags;

        if (systemTime(CLOCK_MONOTONIC) > endWhenNsec) {
            ALOGV("runEncoder: Time limit reached");
            break;
        }

        ALOGV("runEncoder: Calling dequeueOutputBuffer");
        err = encoder->dequeueOutputBuffer(&bufIndex, &offset, &size, &ptsUsec,
                &flags, kTimeout);
        ALOGV("runEncoder: dequeueOutputBuffer returned %d", err);
        switch (err) {
        case NO_ERROR:
            // got a buffer
            if ((flags & MediaCodec::BUFFER_FLAG_CODECCONFIG) != 0) {
                // ignore this -- we passed the CSD into MediaMuxer when
                // we got the format change notification
                ALOGV("runEncoder: Got codec config buffer (%u bytes); ignoring", size);
                size = 0;
            }
            if (size != 0) {
                ALOGV("runEncoder: Got data in buffer %d, size=%d, pts=%lld",
                        bufIndex, size, ptsUsec);
                CHECK(trackIdx != -1);

                // If the virtual display isn't providing us with timestamps,
                // use the current time.
                if (ptsUsec == 0) {
                    ptsUsec = systemTime(SYSTEM_TIME_MONOTONIC) / 1000;
                }

                // The MediaMuxer docs are unclear, but it appears that we
                // need to pass either the full set of BufferInfo flags, or
                // (flags & BUFFER_FLAG_SYNCFRAME).
                err = muxer->writeSampleData(buffers[bufIndex], trackIdx,
                        ptsUsec, flags);
                if (err != NO_ERROR) {
                    ALOGE("runEncoder: Failed writing data to muxer (err=%d)",
                            err);
                    return err;
                }
                debugNumFrames++;
            }
            err = encoder->releaseOutputBuffer(bufIndex);
            if (err != NO_ERROR) {
                ALOGE("runEncoder: Unable to release output buffer (err=%d)",
                        err);
                return err;
            }
            if ((flags & MediaCodec::BUFFER_FLAG_EOS) != 0) {
                // Not expecting EOS from SurfaceFlinger.  Go with it.
                ALOGD("runEncoder: Received end-of-stream");
                gStopRequested = false;
            }
            break;
        case -EAGAIN:                       // INFO_TRY_AGAIN_LATER
            ALOGV("runEncoder: Got -EAGAIN, looping");
            break;
        case INFO_FORMAT_CHANGED:           // INFO_OUTPUT_FORMAT_CHANGED
            {
                // format includes CSD, which we must provide to muxer
                ALOGV("runEncoder: Encoder format changed");
                sp<AMessage> newFormat;
                encoder->getOutputFormat(&newFormat);
                trackIdx = muxer->addTrack(newFormat);
                ALOGV("runEncoder: Starting muxer");
                err = muxer->start();
                if (err != NO_ERROR) {
                    ALOGE("runEncoder: Unable to start muxer (err=%d)", err);
                    return err;
                }
            }
            break;
        case INFO_OUTPUT_BUFFERS_CHANGED:   // INFO_OUTPUT_BUFFERS_CHANGED
            // not expected for an encoder; handle it anyway
            ALOGV("runEncoder: Encoder buffers changed");
            err = encoder->getOutputBuffers(&buffers);
            if (err != NO_ERROR) {
                ALOGE("runEncoder: Unable to get new output buffers (err=%d)", err);
                return err;
            }
            break;
        case INVALID_OPERATION:
            ALOGE("runEncoder: Request for encoder buffer failed");
            return err;
        default:
            ALOGE("runEncoder: Got weird result %d from dequeueOutputBuffer", err);
            return err;
        }
    }

    ALOGV("runEncoder: Encoder stopping (req=%d)", gStopRequested);
    ALOGV("runEncoder: Encoder stopping; recorded %u frames in %lld seconds",
            debugNumFrames,
            nanoseconds_to_seconds(systemTime(CLOCK_MONOTONIC) - startWhenNsec));

    return NO_ERROR;
}

static void* recorder_thread_func(void* p_thread_data) {
    ALOGV("recorder_thread_func: entering thread ......");

    while(true) {
        // MediaMuxer tries to create the file in the constructor, but we don't
        // learn about the failure until muxer.start(), which returns a generic
        // error code without logging anything.  We attempt to create the file
        // now for better diagnostics.
        int fd = open(sFileName, O_CREAT | O_RDWR, 0644);
        if (fd < 0) {
            ALOGE("Unable to open '%s': %s", sFileName, strerror(errno));
            break;
        }
        close(fd);

        status_t err = recordScreen(sFileName);
        // TODO: notify media scanner in java layer
        /*
        if (err == NO_ERROR) {
            // Try to notify the media scanner.  Not fatal if this fails.
            notifyMediaScanner(fileName);
        }*/
        ALOGD("ScreenRecorder::start() %s", (err == NO_ERROR ? "success" : "failed"));
        break;
    }

    // reset running flag
    if(sIsRecorderRunning) {
        sIsRecorderRunning = false;
    }
    ALOGV("recorder_thread_func: leaving thread ......");
    return NULL;
}