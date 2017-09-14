//
// Created by martin on 9/14/17.
//
#include "jni.h"
#include <android/log.h>


extern "C" {

JNIEXPORT jdoubleArray JNICALL
Java_net_mbonnin_arcanetracker_detector_FeatureExtractor_getFeatures(JNIEnv *env, jobject obj, jobject byteBuffer, jint w, jint h, jint stride) {
    __android_log_print(ANDROID_LOG_VERBOSE, "toto", "The value of 1 + 1 is %d", w);

    jdoubleArray result;
    result = env->NewDoubleArray(2);
    if (result == NULL) {
        return NULL; /* out of memory error thrown */
    }

    env->SetDoubleArrayRegion()
    return result;
}

}
