#
# Copyright (C) 2012 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#

LOCAL_PATH := $(call my-dir)

#=====================================================================
# Device Static Library: libcompiler-rt-builtins
#=====================================================================


include $(CLEAR_VARS)

LOCAL_MODULE := libcompiler-rt-builtins
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := STATIC_LIBRARIES
LOCAL_CLANG := true
#LOCAL_CFLAGS := -integrated-as

# Skip atomic.c since it needs to be built separately according to the docs.
# Skip clear_cache.c since it redefines a system function on Android.
LOCAL_SRC_FILES += \
  absvdi2.c \
  absvsi2.c \
  absvti2.c \
  adddf3.c \
  addsf3.c \
  addvdi3.c \
  addvsi3.c \
  addvti3.c \
  apple_versioning.c \
  ashldi3.c \
  ashlti3.c \
  ashrdi3.c \
  ashrti3.c \
  clzdi2.c \
  clzsi2.c \
  clzti2.c \
  cmpdi2.c \
  cmpti2.c \
  comparedf2.c \
  comparesf2.c \
  ctzdi2.c \
  ctzsi2.c \
  ctzti2.c \
  divdc3.c \
  divdf3.c \
  divdi3.c \
  divmoddi4.c \
  divmodsi4.c \
  divsc3.c \
  divsf3.c \
  divsi3.c \
  divti3.c \
  divxc3.c \
  enable_execute_stack.c \
  eprintf.c \
  extendsfdf2.c \
  ffsdi2.c \
  ffsti2.c \
  fixdfdi.c \
  fixdfsi.c \
  fixdfti.c \
  fixsfdi.c \
  fixsfsi.c \
  fixsfti.c \
  fixunsdfdi.c \
  fixunsdfsi.c \
  fixunsdfti.c \
  fixunssfdi.c \
  fixunssfsi.c \
  fixunssfti.c \
  fixunsxfdi.c \
  fixunsxfsi.c \
  fixunsxfti.c \
  fixxfdi.c \
  fixxfti.c \
  floatdidf.c \
  floatdisf.c \
  floatdixf.c \
  floatsidf.c \
  floatsisf.c \
  floattidf.c \
  floattisf.c \
  floattixf.c \
  floatundidf.c \
  floatundisf.c \
  floatundixf.c \
  floatunsidf.c \
  floatunsisf.c \
  floatuntidf.c \
  floatuntisf.c \
  floatuntixf.c \
  gcc_personality_v0.c \
  int_util.c \
  lshrdi3.c \
  lshrti3.c \
  moddi3.c \
  modsi3.c \
  modti3.c \
  muldc3.c \
  muldf3.c \
  muldi3.c \
  mulosi4.c \
  muloti4.c \
  mulsc3.c \
  mulsf3.c \
  multi3.c \
  mulvdi3.c \
  mulvsi3.c \
  mulvti3.c \
  mulxc3.c \
  negdf2.c \
  negdi2.c \
  negsf2.c \
  negti2.c \
  negvdi2.c \
  negvsi2.c \
  negvti2.c \
  paritydi2.c \
  paritysi2.c \
  parityti2.c \
  popcountdi2.c \
  popcountsi2.c \
  popcountti2.c \
  powidf2.c \
  powisf2.c \
  powitf2.c \
  powixf2.c \
  subdf3.c \
  subsf3.c \
  subvdi3.c \
  subvsi3.c \
  subvti3.c \
  trampoline_setup.c \
  truncdfsf2.c \
  ucmpdi2.c \
  ucmpti2.c \
  udivdi3.c \
  udivmoddi4.c \
  udivmodsi4.c \
  udivmodti4.c \
  udivsi3.c \
  udivti3.c \
  umoddi3.c \
  umodsi3.c \
  umodti3.c

include $(BUILD_STATIC_LIBRARY)


#=====================================================================
# Device Static Library: libcompiler-rt-extras
#=====================================================================

# These are functions that are not available in libgcc.a, so we potentially
# need them when using a Clang-built component (i.e. -ftrapv with 64-bit
# integer multiplies).
include $(CLEAR_VARS)

LOCAL_MODULE := libcompiler-rt-extras
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := STATIC_LIBRARIES
LOCAL_CLANG := true

LOCAL_SRC_FILES += \
  mulodi4.c

include $(BUILD_STATIC_LIBRARY)


