//
// Created by martin on 9/14/17.
//
#include "jni.h"
#include <malloc.h>

extern "C" {

void computeHaar(double *pDouble, double *pDouble1);
double *scaledIntegralImage(uint8_t *in, int pixel_stride, int stride, jdouble in_x, jdouble in_y,
                            jdouble in_w, jdouble in_h,
                            int out_w, int out_h);

#define FEATURES_PER_CHANNEL 5
#define SCALED_SIZE 32

JNIEXPORT jdoubleArray JNICALL
Java_net_mbonnin_arcanetracker_detector_FeatureExtractor_allocateVector(JNIEnv *env, jobject obj) {
    jdoubleArray result;
    result = env->NewDoubleArray(3 * FEATURES_PER_CHANNEL);
    if (result == NULL) {
        return NULL;  /* out of memory error thrown */
    }

    return result;
}

JNIEXPORT void JNICALL
Java_net_mbonnin_arcanetracker_detector_FeatureExtractor_getFeatures(JNIEnv *env, jobject obj,
                                                                     jobject byteBuffer,
                                                                     jint stride, jdouble x,
                                                                     jdouble y, jdouble w,
                                                                     jdouble h,
                                                                     jdoubleArray vector) {

    uint8_t *buf = (uint8_t *) env->GetDirectBufferAddress(byteBuffer);

    double *v = (double *) malloc(3 * FEATURES_PER_CHANNEL * sizeof(double));
    for (int i = 0; i < 3; i++) {
        double *integralImage = scaledIntegralImage(buf + i, 4, stride, x, y, w, h, SCALED_SIZE,
                                                    SCALED_SIZE);

        computeHaar(integralImage, v + i * FEATURES_PER_CHANNEL);
        free(integralImage);
    }

    for (int i = 0; i < 3 * FEATURES_PER_CHANNEL; i++) {
        v[i] = v[i] / (SCALED_SIZE * SCALED_SIZE);
    }

    env->SetDoubleArrayRegion(vector, 0, 3 * FEATURES_PER_CHANNEL, v);

    free(v);
}


double *scaledIntegralImage(uint8_t *in, int pixel_stride, int stride, jdouble in_x, jdouble in_y,
                            jdouble in_w, jdouble in_h,
                            int out_w, int out_h) {
    double *out = (double *) malloc(out_w * out_h * sizeof(double));
    double *out_ptr = out;
    double ax = out_w / in_w;
    double ay = out_h / in_h;

    for (int j = 0; j < out_h; j++) {
        for (int i = 0; i < out_w; i++) {
            double x = in_x + i / ax;
            double y = in_y + j / ay;


            int x0 = (int) x;
            int y0 = (int) y;
            int x1 = x0 + 1;
            int y1 = y0 + 1;

            double X0Y0 = in[x0 * pixel_stride + y0 * stride];
            double X1Y0 = in[x1 * pixel_stride + y0 * stride];
            double X0Y1 = in[x0 * pixel_stride + y1 * stride];
            double X1Y1 = in[x1 * pixel_stride + y1 * stride];

            double v0 = (x - x0) * X0Y0 + (x1 - x) * X1Y0;
            double v1 = (x - x0) * X0Y1 + (x1 - x) * X1Y1;
            double v = (y - y0) * v0 + (y1 - y) * v1;

            if (j > 0) {
                v += out[i + (j - 1) * out_w];
            }
            if (i > 0) {
                v += out[i - 1 + j * out_w];
            }
            if (i > 0 && j > 0) {
                v -= out[i - 1 + (j - 1) * out_w];
            }
            *out_ptr++ = v;
        }
    }

    return out;
}

static inline double getPoint(double *integralImage, int x, int y) {
    return integralImage[(x - 1) + (y - 1) * SCALED_SIZE];
}

void computeHaar(double *integralImage, double *vector) {
    *vector++ =
            getPoint(integralImage, SCALED_SIZE, SCALED_SIZE) - 128.0 * SCALED_SIZE * SCALED_SIZE;

    *vector++ = getPoint(integralImage, SCALED_SIZE, SCALED_SIZE)
                - 2 * getPoint(integralImage, SCALED_SIZE / 2, SCALED_SIZE);

    *vector++ = getPoint(integralImage, SCALED_SIZE, SCALED_SIZE)
                - 2 * getPoint(integralImage, SCALED_SIZE, SCALED_SIZE / 2);

    *vector++ = -getPoint(integralImage, SCALED_SIZE, SCALED_SIZE)
                + 2 * getPoint(integralImage, 3 * SCALED_SIZE / 4, SCALED_SIZE)
                - 2 * getPoint(integralImage, SCALED_SIZE / 4, SCALED_SIZE);

    *vector++ = -getPoint(integralImage, SCALED_SIZE, SCALED_SIZE)
                + 2 * getPoint(integralImage, SCALED_SIZE, 3 * SCALED_SIZE / 4)
                - 2 * getPoint(integralImage, SCALED_SIZE, SCALED_SIZE / 4);
}

}

