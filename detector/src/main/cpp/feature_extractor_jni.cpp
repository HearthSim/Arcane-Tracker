//
// Created by martin on 9/14/17.
//
#include "jni.h"
#include "lodepng.h"
#include <malloc.h>
#include <android/log.h>


extern "C" {

void save_png(uint8_t *image, int w, int h, const char *name, int index);
void computeHaar(double *pDouble, double *pDouble1);
double *scaledIntegralImage(uint8_t *in, int pixel_stride, int stride, jdouble in_x, jdouble in_y,
                            jdouble in_w, jdouble in_h,
                            int out_w, int out_h);

#define FEATURES_PER_CHANNEL 5
#define HAAR_SCALED_SIZE 32
#define PHASH_SCALED_SIZE 8

#define DUMP_PNG 0
#define DBG if (0)

#define TAG "JNI"


JNIEXPORT jdoubleArray JNICALL
Java_net_mbonnin_arcanetracker_detector_FeatureExtractor_getFeatures(JNIEnv *env, jobject obj,
                                                                     jobject byteBuffer,
                                                                     jint stride, jdouble x,
                                                                     jdouble y, jdouble w,
                                                                     jdouble h) {

    jdoubleArray vector;
    vector = env->NewDoubleArray(3 * FEATURES_PER_CHANNEL);
    if (vector == NULL) {
        return NULL;  /* out of memory error thrown */
    }

    uint8_t *buf = (uint8_t *) env->GetDirectBufferAddress(byteBuffer);

    double *v = (double *) malloc(3 * FEATURES_PER_CHANNEL * sizeof(double));
    for (int i = 0; i < 3; i++) {
        double *integralImage = scaledIntegralImage(buf + i, 4, stride, x, y, w, h,
                                                    HAAR_SCALED_SIZE,
                                                    HAAR_SCALED_SIZE);

        computeHaar(integralImage, v + i * FEATURES_PER_CHANNEL);
        free(integralImage);
    }

    for (int i = 0; i < 3 * FEATURES_PER_CHANNEL; i++) {
        v[i] = v[i] / (HAAR_SCALED_SIZE * HAAR_SCALED_SIZE);
    }

    env->SetDoubleArrayRegion(vector, 0, 3 * FEATURES_PER_CHANNEL, v);

    free(v);
    return vector;
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
    return integralImage[(x - 1) + (y - 1) * HAAR_SCALED_SIZE];
}

void computeHaar(double *integralImage, double *vector) {
    *vector++ =
            getPoint(integralImage, 3 * HAAR_SCALED_SIZE / 4, HAAR_SCALED_SIZE)
            - getPoint(integralImage, 2 * HAAR_SCALED_SIZE / 4, HAAR_SCALED_SIZE)
            + getPoint(integralImage, 1 * HAAR_SCALED_SIZE / 4, HAAR_SCALED_SIZE);

    *vector++ = getPoint(integralImage, HAAR_SCALED_SIZE, HAAR_SCALED_SIZE)
                - 2 * getPoint(integralImage, HAAR_SCALED_SIZE / 2, HAAR_SCALED_SIZE);

    *vector++ = getPoint(integralImage, HAAR_SCALED_SIZE, HAAR_SCALED_SIZE)
                - 2 * getPoint(integralImage, HAAR_SCALED_SIZE, HAAR_SCALED_SIZE / 2);

    *vector++ = -getPoint(integralImage, HAAR_SCALED_SIZE, HAAR_SCALED_SIZE)
                + 2 * getPoint(integralImage, 3 * HAAR_SCALED_SIZE / 4, HAAR_SCALED_SIZE)
                - 2 * getPoint(integralImage, HAAR_SCALED_SIZE / 4, HAAR_SCALED_SIZE);

    *vector++ = -getPoint(integralImage, HAAR_SCALED_SIZE, HAAR_SCALED_SIZE)
                + 2 * getPoint(integralImage, HAAR_SCALED_SIZE, 3 * HAAR_SCALED_SIZE / 4)
                - 2 * getPoint(integralImage, HAAR_SCALED_SIZE, HAAR_SCALED_SIZE / 4);
}

uint8_t *scaleImage(uint8_t *in, int pixel_stride, int stride, jdouble in_x, jdouble in_y,
                    jdouble in_w, jdouble in_h,
                    int out_w, int out_h) {

    DBG __android_log_print(ANDROID_LOG_DEBUG, TAG, "scale: %dx%d -> %dx%d", (int) in_w, (int) in_h,
                        out_w, out_h);

    if (1 && in_w / out_w > 2) {
        int intermediate_w = (int) (in_w / 2 + 1);
        int intermediate_h = (int) (in_h / 2 + 1);

        uint8_t *intermediate = scaleImage(in, pixel_stride, stride, in_x, in_y, in_w, in_h,
                                           intermediate_w,
                                           intermediate_h);
        uint8_t *ret = scaleImage(intermediate, 1, intermediate_w, 0, 0, intermediate_w,
                                  intermediate_h, out_w, out_h);

        free(intermediate);
        return ret;
    }


    uint8_t *out = (uint8_t *) malloc(out_w * out_h * sizeof(uint8_t));
    uint8_t *out_ptr = out;
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

            double v0 = (x1 - x) * X0Y0 + (x - x0) * X1Y0;
            double v1 = (x1 - x) * X0Y1 + (x - x0) * X1Y1;
            double v = (y1 - y) * v0 + (y - y0) * v1;

            *out_ptr++ = (uint8_t) (((int) v) & 0xff);
        }
    }
    DBG __android_log_print(ANDROID_LOG_DEBUG, TAG, "done %dx%d (ax=%f, ay=%f)", out_w, out_h, ax, ay);
    return out;
}

JNIEXPORT jlong JNICALL
Java_net_mbonnin_arcanetracker_detector_FeatureExtractor_getHash(JNIEnv *env, jobject obj,
                                                                 jobject byteBuffer,
                                                                 jint stride, jdouble x,
                                                                 jdouble y, jdouble w,
                                                                 jdouble h) {

    uint8_t *buf = (uint8_t *) env->GetDirectBufferAddress(byteBuffer);
    static int index = 0;

    uint8_t *red = scaleImage(buf, 4, stride, x, y, w, h, PHASH_SCALED_SIZE, PHASH_SCALED_SIZE);
    uint8_t *green = scaleImage(buf + 1, 4, stride, x, y, w, h, PHASH_SCALED_SIZE,
                                PHASH_SCALED_SIZE);
    uint8_t *blue = scaleImage(buf + 2, 4, stride, x, y, w, h, PHASH_SCALED_SIZE,
                               PHASH_SCALED_SIZE);

    uint8_t *acc = (uint8_t *) malloc(PHASH_SCALED_SIZE * PHASH_SCALED_SIZE);
    uint8_t *p = acc;
    uint8_t *r = red;
    uint8_t *g = green;
    uint8_t *b = blue;

    double sum = 0;
    for (int i = 0; i < PHASH_SCALED_SIZE * PHASH_SCALED_SIZE; i++) {
        *p = (uint8_t) ((*r++ + *g++ + (int) *b++) / 3 & 0xff);
        sum += *p;
        p++;
    }

    sum /= (PHASH_SCALED_SIZE * PHASH_SCALED_SIZE);
    DBG __android_log_print(ANDROID_LOG_DEBUG, TAG, "average: %f", sum);

    jlong ret = 0;
    p = acc;
    for (int i = 0; i < PHASH_SCALED_SIZE * PHASH_SCALED_SIZE; i++) {
        if (*p > sum) {
            ret |= 1L << i;
        }
        p++;
    }

    if (0) {
        uint8_t *tmp = (uint8_t *) malloc((size_t) ((int) w * (int) h));
        uint8_t *t = tmp;
        for (int j = 0; j < (int) h; j++) {
            for (int i = 0; i < (int) w; i++) {
                *t++ = buf[4 * (i + (int) x) + (j + (int) y) * stride];
            }
        }
        save_png(tmp, (int) w, (int) h, "original", index);

        uint8_t *tmp2 = scaleImage(tmp, 1, (int)w, 0, 0, (int)w, (int)h, w, h);
        save_png(tmp2, w, h, "noresize", index);
        free(tmp2);
        free(tmp);

        save_png(acc, PHASH_SCALED_SIZE, PHASH_SCALED_SIZE, "acc", index);

        tmp = (uint8_t *) malloc(PHASH_SCALED_SIZE * PHASH_SCALED_SIZE);
        t = tmp;
        p = acc;
        for (int i = 0; i < PHASH_SCALED_SIZE * PHASH_SCALED_SIZE; i++) {
            if (*p > sum) {
                *t = 255;
            } else {
                *t = 0;
            }
            t++;
            p++;
        }
        save_png(tmp, PHASH_SCALED_SIZE, PHASH_SCALED_SIZE, "vector", index);
        free(tmp);

    }

    index++;

    free(acc);
    free(red);
    free(green);
    free(blue);

    DBG __android_log_print(ANDROID_LOG_DEBUG, TAG, "ret=%ld", ret);

    return ret;
}

void save_png(uint8_t *image, int w, int h, const char *name, int index) {
    lodepng::State state;

    state.info_raw.colortype = LCT_GREY;
    state.info_raw.bitdepth = 8;

    state.info_png.color.colortype = LCT_GREY;
    state.info_png.color.bitdepth = 8;
    state.encoder.auto_convert = 0; // without this, it would ignore the output color type specified above and choose an optimal one instead

    std::vector<unsigned char> buffer;
    lodepng::encode(buffer, image, w, h, state);
    char buf[1024];
    snprintf(buf, 1024, "sdcard/%s-%d.png", name, index);
    lodepng::save_file(buffer, buf);
}

}

