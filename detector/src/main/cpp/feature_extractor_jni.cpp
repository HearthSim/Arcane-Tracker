//
// Created by martin on 9/14/17.
//
#include "jni.h"
#include <malloc.h>

#include <android/log.h>

extern "C" {

void computeHaar(double *pDouble, double *pDouble1);
double *scaledIntegralImage(uint8_t *in, int stride, jdouble in_x, jdouble in_y, jdouble in_w, jdouble in_h,
                            int out_w, int out_h);

#define FEATURES_PER_CHANNEL 5
#define SCALED_SIZE 48

JNIEXPORT jdoubleArray JNICALL
Java_net_mbonnin_arcanetracker_detector_FeatureExtractor_getFeatures(JNIEnv *env, jobject obj,
                                                                     jobject byteBuffer, jdouble x,
                                                                     jdouble y, jdouble w, jdouble h,
                                                                     jint stride) {
    jdoubleArray result;
    result = env->NewDoubleArray(3 * FEATURES_PER_CHANNEL);
    if (result == NULL) {
        return NULL; /* out of memory error thrown */
    }

    uint8_t *buf = (uint8_t *) env->GetDirectBufferAddress(byteBuffer);

    double *vector = (double*) malloc(3 * FEATURES_PER_CHANNEL * sizeof(double));
    for (int i = 0; i < 3; i++) {
        double *integralImage = scaledIntegralImage(buf + i, stride, x, y, w, h, SCALED_SIZE, SCALED_SIZE);

        computeHaar(integralImage, vector + i * FEATURES_PER_CHANNEL);
        free(integralImage);
    }

    __android_log_print(ANDROID_LOG_DEBUG, "feature", "average color: #%02x%02x%02x",
                        (unsigned int) (vector[0 * FEATURES_PER_CHANNEL] / (SCALED_SIZE * SCALED_SIZE)),
                        (unsigned int) (vector[1 * FEATURES_PER_CHANNEL] / (SCALED_SIZE * SCALED_SIZE)),
                        (unsigned int) (vector[2 * FEATURES_PER_CHANNEL] / (SCALED_SIZE * SCALED_SIZE)));

    env->SetDoubleArrayRegion(result, 0, 3 * FEATURES_PER_CHANNEL, vector);

    free(vector);
    return result;
}


double *scaledIntegralImage(uint8_t *in, int stride, jdouble in_x, jdouble in_y, jdouble in_w, jdouble in_h,
                            int out_w, int out_h) {
    double *out = (double *) malloc(out_w * out_h * sizeof(double));
    double *out_ptr = out;
    double acc = 0;
    double ax = out_w / in_w;
    double ay = out_h / in_h;

    for (int i = 0; i < out_w; i++) {
        for (int j = 0; j < out_h; j++) {
            double x = in_x + i / ax;
            double y = in_y + j / ay;


            int x0 = (int)x;
            int y0 = (int)y;
            int x1 = x0 + 1;
            int y1 = y0 + 1;

            double X0Y0 = in[x0 + y0 * stride];
            double X1Y0 = in[x1 + y0 * stride];
            double X0Y1 = in[x0 + y1 * stride];
            double X1Y1 = in[x1 + y1 * stride];

            double v0 = (x - x0) * X0Y0 + (x1 - x) * X1Y0;
            double v1 = (x - x0) * X0Y1 + (x1 - x) * X1Y1;
            double v = (y - y0) * v0 + (y1 - y) * v1;

            acc += v;
            *out_ptr++ = acc;
        }
    }

    return out;
}

static inline double getPoint(double *integralImage, int x, int y) {
    return integralImage[(x - 1) + (y - 1) * SCALED_SIZE];
}

void computeHaar(double *integralImage, double *vector) {
    *vector++ = getPoint(integralImage, SCALED_SIZE, SCALED_SIZE);
    *vector++ = getPoint(integralImage, SCALED_SIZE, SCALED_SIZE)
            - 2 * getPoint(integralImage, SCALED_SIZE / 2, SCALED_SIZE);
    *vector++ = getPoint(integralImage, SCALED_SIZE, SCALED_SIZE)
                - 2 * getPoint(integralImage, SCALED_SIZE, SCALED_SIZE / 2);
    *vector++ = - getPoint(integralImage, SCALED_SIZE, SCALED_SIZE)
                + 2 * getPoint(integralImage, 2 * SCALED_SIZE / 3, SCALED_SIZE)
                - 2 * getPoint(integralImage, SCALED_SIZE / 3, SCALED_SIZE);
    *vector++ = - getPoint(integralImage, SCALED_SIZE, SCALED_SIZE)
                + 2 * getPoint(integralImage, SCALED_SIZE, 2 * SCALED_SIZE / 3)
                - 2 * getPoint(integralImage, SCALED_SIZE, SCALED_SIZE / 3);
}

}

