#!/bin/sh

# Usually, manually running build_clcore.sh shouldn't be needed. build_clcore.mk should
# kick in automatically during Android build process. 

# Generate rs_cl.bc
# =================

scriptc_path=../../../../base/libs/rs/scriptc
clang_header_path=../../../../../external/clang/lib/Headers

clang -ccc-host-triple armv7-none-linux-gnueabi -I${scriptc_path} -I${clang_header_path} -c -std=c99 -O3 rs_cl.c -emit-llvm -o rs_cl.bc

# Generate rs_core.bc
# ===================

clang -ccc-host-triple armv7-none-linux-gnueabi -I${scriptc_path} -I${clang_header_path} -c -std=c99 -O3 rs_core.c -emit-llvm -o rs_core.bc

# Link everything together
# ========================

llvm-link rs_cl.bc rs_core.bc -o libclcore.bc
