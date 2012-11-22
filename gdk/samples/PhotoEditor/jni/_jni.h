extern "C" 
{
   void Backlight(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat backlight);
   void Blur(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale);
   void ColorTemp(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale);
   void Copy(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
   void CrossProcess(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
   void Duotone(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jint color1, jint color2);
   void Fisheye(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat focus_x, jfloat focus_y, jfloat scale);
   void FlipHorizontal(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
   void FlipVertical(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
   void FlipBoth(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
   void Grain(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat noise_scale);
   void Grayscale(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale);
   void HEQ(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale);
   void Negative(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
   void Quantize(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
   void RedEye(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jobjectArray redeye_positions, jfloat radius, jfloat intensity);
   void Saturation(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale);
   void Sepia(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
   void Sharpen(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale);
   void Tint(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jint tint);
   void Vignetting(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat range);
   void Warmify(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
   void WhiteBlack(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat white, jfloat black);
};
   
typedef void (*pBacklightType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat backlight);
typedef void (*pBlurType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale);
typedef void (*pColorTempType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale);
typedef void (*pCopyType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
typedef void (*pCrossProcessType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
typedef void (*pDuotoneType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jint color1, jint color2);
typedef void (*pFisheyeType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat focus_x, jfloat focus_y, jfloat scale);
typedef void (*pFlipHorizontalType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
typedef void (*pFlipVerticalType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
typedef void (*pFlipBothType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
typedef void (*pGrainType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat noise_scale);
typedef void (*pGrayscaleType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale);
typedef void (*pHEQType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale);
typedef void (*pNegativeType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
typedef void (*pQuantizeType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
typedef void (*pRedEyeType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jobjectArray redeye_positions, jfloat radius, jfloat intensity);
typedef void (*pSaturationType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale);
typedef void (*pSepiaType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
typedef void (*pSharpenType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat scale);
typedef void (*pTintType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jint tint);
typedef void (*pVignettingType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat range);
typedef void (*pWarmifyType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap);
typedef void (*pWhiteBlackType)(JNIEnv *env, jobject obj, jobject src_bitmap, jobject dst_bitmap, jfloat white, jfloat black);

#define DEFINE(f) JNI_ ## f,
enum
{
#include "_jnif.h"
   JNI_max
};
#undef DEFINE


typedef struct 
{
   const char *func_name;
   void *func_ptr;
} JNIFuncType;

extern JNIFuncType JNIFunc[JNI_max];


