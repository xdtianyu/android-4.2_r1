target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7-none-linux-gnueabi"

declare float @llvm.sqrt.f32(float)
declare float @llvm.pow.f32(float, float)

define float @_Z4sqrtf(float %v) {
  %1 = tail call float @llvm.sqrt.f32(float %v)
  ret float %1
}

define float @_Z3powf(float %v1, float %v2) {
  %1 = tail call float @llvm.pow.f32(float  %v1, float %v2)
  ret float %1
}

