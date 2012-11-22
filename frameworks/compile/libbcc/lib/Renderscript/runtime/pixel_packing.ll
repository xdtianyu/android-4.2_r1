target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7-none-linux-gnueabi"

@fc_255.0 = internal constant <4 x float> <float 255.0, float 255.0, float 255.0, float 255.0>, align 16
@fc_0.5 = internal constant <4 x float> <float 0.5, float 0.5, float 0.5, float 0.5>, align 16

declare <4 x i8> @_Z14convert_uchar4Dv4_f(<4 x float> %in) nounwind readnone
declare <4 x float> @_Z14convert_float4Dv4_h(<4 x i8> %in) nounwind readnone

; uchar4 __attribute__((overloadable)) rsPackColorTo8888(float4 color)
define <4 x i8> @_Z17rsPackColorTo8888Dv4_f(<4 x float> %color) nounwind readnone {
    %f255 = load <4 x float>* @fc_255.0, align 16
    %f05 = load <4 x float>* @fc_0.5, align 16
    %v1 = fmul <4 x float> %f255, %color
    %v2 = fadd <4 x float> %f05, %v1
    %v3 = tail call <4 x i8> @_Z14convert_uchar4Dv4_f(<4 x float> %v2) nounwind readnone
    ret <4 x i8> %v3
}

; uchar4 __attribute__((overloadable)) rsPackColorTo8888(float3 color)
define <4 x i8> @_Z17rsPackColorTo8888Dv3_f(<3 x float> %color) nounwind readnone {
    %1 = shufflevector <3 x float> %color, <3 x float> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 3>
    %2 = insertelement <4 x float> %1, float 1.0, i32 3
    %3 = tail call <4 x i8> @_Z17rsPackColorTo8888Dv4_f(<4 x float> %2) nounwind readnone
    ret <4 x i8> %3
}

; uchar4 __attribute__((overloadable)) rsPackColorTo8888(float r, float g, float b)
define <4 x i8> @_Z17rsPackColorTo8888fff(float %r, float %g, float %b) nounwind readnone {
    %1 = insertelement <4 x float> undef, float %r, i32 0
    %2 = insertelement <4 x float> %1, float %g, i32 1
    %3 = insertelement <4 x float> %2, float %b, i32 2
    %4 = insertelement <4 x float> %3, float 1.0, i32 3
    %5 = tail call <4 x i8> @_Z17rsPackColorTo8888Dv4_f(<4 x float> %4) nounwind readnone
    ret <4 x i8> %5
}

; uchar4 __attribute__((overloadable)) rsPackColorTo8888(float r, float g, float b, float a)
define <4 x i8> @_Z17rsPackColorTo8888ffff(float %r, float %g, float %b, float %a) nounwind readnone {
    %1 = insertelement <4 x float> undef, float %r, i32 0
    %2 = insertelement <4 x float> %1, float %g, i32 1
    %3 = insertelement <4 x float> %2, float %b, i32 2
    %4 = insertelement <4 x float> %3, float %a, i32 3
    %5 = tail call <4 x i8> @_Z17rsPackColorTo8888Dv4_f(<4 x float> %4) nounwind readnone
    ret <4 x i8> %5
}

