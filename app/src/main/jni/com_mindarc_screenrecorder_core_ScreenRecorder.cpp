#include "com_mindarc_screenrecorder_core_ScreenRecorder.h"
#include "ScreenRecorder.h"

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_mindarc_screenrecorder_core_ScreenRecorder
 * Method:    init
 * Signature: (IIIIZLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_mindarc_screenrecorder_core_ScreenRecorder_init
  (JNIEnv *env, jclass clazz, jint width, jint height, jint bitrate, jint timeLimit, jboolean rotate, jstring destFile) {
    return true;
}

/*
 * Class:     com_mindarc_screenrecorder_core_ScreenRecorder
 * Method:    start
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_mindarc_screenrecorder_core_ScreenRecorder_start
  (JNIEnv *env, jclass clazz) {
    return ScreenRecorder::getRecorder()->start();
}

/*
 * Class:     com_mindarc_screenrecorder_core_ScreenRecorder
 * Method:    stop
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_mindarc_screenrecorder_core_ScreenRecorder_stop
  (JNIEnv *env, jclass clazz) {
    return ScreenRecorder::getRecorder()->stop();
}

#ifdef __cplusplus
}
#endif
