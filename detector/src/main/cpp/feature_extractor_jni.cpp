//
// Created by martin on 9/14/17.
//
#include "jni.h"
#include <android/log.h>


extern "C" {

JNIEXPORT void JNICALL
Java_net_mbonnin_arcanetracker_detector_FeatureExtractor_sayHello(JNIEnv *env, jobject obj) {
    __android_log_print(ANDROID_LOG_VERBOSE, "toto", "The value of 1 + 1 is %d", 1 + 1);
}

}
