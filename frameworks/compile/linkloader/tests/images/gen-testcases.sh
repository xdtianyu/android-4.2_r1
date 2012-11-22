#!/bin/bash -e


# Copyright (C) 2011-2012 The Android Open Source Project
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


CYAN='\033[1;36m'
RESET='\033[m'

echo -e "${CYAN}Generating bitcode ...${RESET}"
clang -emit-llvm -std=c89 -Wall -c test.c -o test.bc
clang -emit-llvm -std=c89 -Wall -c simple-test.c -o simple-test.bc
clang -emit-llvm -std=c89 -Wall -c rodata-test.c -o rodata-test.bc

function gen_test_cases {
  echo -e "${CYAN}Generating for $1 ...${RESET}"
  llc -filetype=obj -relocation-model=static -mtriple $2 $3 test.bc -o test-$1.o
  llc -filetype=obj -relocation-model=static -mtriple $2 $3 simple-test.bc -o simple-test-$1.o
  llc -filetype=obj -relocation-model=static -mtriple $2 $3 rodata-test.bc -o rodata-test-$1.o
}

gen_test_cases arm    armv7-none-linux-gnueabi
gen_test_cases tegra2 armv7-none-linux-gnueabi '-mcpu=cortex-a9 -mattr=+vfp3'
gen_test_cases thumb2 thumb-none-linux-gnueabi '-march=thumb -mattr=+thumb2'
gen_test_cases thumb2lc thumb-none-linux-gnueabi '-mattr=+thumb2,+neonfp,+vfp3 -arm-long-calls'
gen_test_cases thumb2lc-xoom thumb-none-linux-gnueabi '-mattr=+thumb2 -arm-long-calls'
gen_test_cases x86_32 i686-none-linux
gen_test_cases x86_64 x86_64-none-linux
gen_test_cases mipsel mipsel-none-linux-gnueabi
