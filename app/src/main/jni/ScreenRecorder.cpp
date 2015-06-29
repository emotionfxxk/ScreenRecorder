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

using namespace android;


static ScreenRecorder* sRecorder = NULL;

ScreenRecorder::ScreenRecorder() {
}

ScreenRecorder* ScreenRecorder::getRecorder() {
    if(sRecorder == NULL) {
        sRecorder = new ScreenRecorder();
    }
    return sRecorder;
}
bool ScreenRecorder::init(int width, int height, int bitrate, int timeLimit, bool rotate, const char* destFilePath) {
    return true;
}
bool ScreenRecorder::start() {
    return true;
}
bool ScreenRecorder::stop() {
    return true;
}
