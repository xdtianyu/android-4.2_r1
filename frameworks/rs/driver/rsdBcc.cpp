/*
 * Copyright (C) 2011-2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "rsdCore.h"

#include <bcc/BCCContext.h>
#include <bcc/Renderscript/RSCompilerDriver.h>
#include <bcc/Renderscript/RSExecutable.h>
#include <bcc/Renderscript/RSInfo.h>

#include "rsdBcc.h"
#include "rsdRuntime.h"
#include "rsdAllocation.h"
#include "rsdIntrinsics.h"

#include "rsContext.h"
#include "rsElement.h"
#include "rsScriptC.h"

#include "utils/Vector.h"
#include "utils/Timers.h"
#include "utils/StopWatch.h"

using namespace android;
using namespace android::renderscript;


static Script * setTLS(Script *sc) {
    ScriptTLSStruct * tls = (ScriptTLSStruct *)pthread_getspecific(rsdgThreadTLSKey);
    rsAssert(tls);
    Script *old = tls->mScript;
    tls->mScript = sc;
    return old;
}


bool rsdScriptInit(const Context *rsc,
                     ScriptC *script,
                     char const *resName,
                     char const *cacheDir,
                     uint8_t const *bitcode,
                     size_t bitcodeSize,
                     uint32_t flags) {
    //ALOGE("rsdScriptCreate %p %p %p %p %i %i %p", rsc, resName, cacheDir, bitcode, bitcodeSize, flags, lookupFunc);
    //ALOGE("rsdScriptInit %p %p", rsc, script);

    pthread_mutex_lock(&rsdgInitMutex);

    bcc::RSExecutable *exec;
    const bcc::RSInfo *info;
    DrvScript *drv = (DrvScript *)calloc(1, sizeof(DrvScript));
    if (drv == NULL) {
        goto error;
    }
    script->mHal.drv = drv;

    drv->mCompilerContext = NULL;
    drv->mCompilerDriver = NULL;
    drv->mExecutable = NULL;

    drv->mCompilerContext = new bcc::BCCContext();
    if (drv->mCompilerContext == NULL) {
        ALOGE("bcc: FAILS to create compiler context (out of memory)");
        goto error;
    }

    drv->mCompilerDriver = new bcc::RSCompilerDriver();
    if (drv->mCompilerDriver == NULL) {
        ALOGE("bcc: FAILS to create compiler driver (out of memory)");
        goto error;
    }

    script->mHal.info.isThreadable = true;

    drv->mCompilerDriver->setRSRuntimeLookupFunction(rsdLookupRuntimeStub);
    drv->mCompilerDriver->setRSRuntimeLookupContext(script);

    exec = drv->mCompilerDriver->build(*drv->mCompilerContext,
                                       cacheDir, resName,
                                       (const char *)bitcode, bitcodeSize);

    if (exec == NULL) {
        ALOGE("bcc: FAILS to prepare executable for '%s'", resName);
        goto error;
    }

    drv->mExecutable = exec;

    exec->setThreadable(script->mHal.info.isThreadable);
    if (!exec->syncInfo()) {
        ALOGW("bcc: FAILS to synchronize the RS info file to the disk");
    }

    drv->mRoot = reinterpret_cast<int (*)()>(exec->getSymbolAddress("root"));
    drv->mRootExpand =
        reinterpret_cast<int (*)()>(exec->getSymbolAddress("root.expand"));
    drv->mInit = reinterpret_cast<void (*)()>(exec->getSymbolAddress("init"));
    drv->mFreeChildren =
        reinterpret_cast<void (*)()>(exec->getSymbolAddress(".rs.dtor"));

    info = &drv->mExecutable->getInfo();
    // Copy info over to runtime
    script->mHal.info.exportedFunctionCount = info->getExportFuncNames().size();
    script->mHal.info.exportedVariableCount = info->getExportVarNames().size();
    script->mHal.info.exportedPragmaCount = info->getPragmas().size();
    script->mHal.info.exportedPragmaKeyList =
        const_cast<const char**>(exec->getPragmaKeys().array());
    script->mHal.info.exportedPragmaValueList =
        const_cast<const char**>(exec->getPragmaValues().array());

    if (drv->mRootExpand) {
        script->mHal.info.root = drv->mRootExpand;
    } else {
        script->mHal.info.root = drv->mRoot;
    }

    if (script->mHal.info.exportedVariableCount) {
        drv->mBoundAllocs = new Allocation *[script->mHal.info.exportedVariableCount];
        memset(drv->mBoundAllocs, 0, sizeof(void *) * script->mHal.info.exportedVariableCount);
    }

    pthread_mutex_unlock(&rsdgInitMutex);
    return true;

error:

    pthread_mutex_unlock(&rsdgInitMutex);
    if (drv) {
        delete drv->mCompilerContext;
        delete drv->mCompilerDriver;
        delete drv->mExecutable;
        delete[] drv->mBoundAllocs;
        free(drv);
    }
    script->mHal.drv = NULL;
    return false;

}

bool rsdInitIntrinsic(const Context *rsc, Script *s, RsScriptIntrinsicID iid, Element *e) {
    pthread_mutex_lock(&rsdgInitMutex);

    DrvScript *drv = (DrvScript *)calloc(1, sizeof(DrvScript));
    if (drv == NULL) {
        goto error;
    }
    s->mHal.drv = drv;
    drv->mIntrinsicID = iid;
    drv->mIntrinsicData = rsdIntrinsic_Init(rsc, s, iid, &drv->mIntrinsicFuncs);
    s->mHal.info.isThreadable = true;

    pthread_mutex_unlock(&rsdgInitMutex);
    return true;

error:
    pthread_mutex_unlock(&rsdgInitMutex);
    return false;
}

typedef void (*rs_t)(const void *, void *, const void *, uint32_t, uint32_t, uint32_t, uint32_t);

static void wc_xy(void *usr, uint32_t idx) {
    MTLaunchStruct *mtls = (MTLaunchStruct *)usr;
    RsForEachStubParamStruct p;
    memcpy(&p, &mtls->fep, sizeof(p));
    RsdHal * dc = (RsdHal *)mtls->rsc->mHal.drv;
    uint32_t sig = mtls->sig;

#if defined(ARCH_ARM_RS_USE_CACHED_SCANLINE_WRITE)
    unsigned char buf[1024 * 8];
#endif

    outer_foreach_t fn = (outer_foreach_t) mtls->kernel;
    while (1) {
        uint32_t slice = (uint32_t)android_atomic_inc(&mtls->mSliceNum);
        uint32_t yStart = mtls->yStart + slice * mtls->mSliceSize;
        uint32_t yEnd = yStart + mtls->mSliceSize;
        yEnd = rsMin(yEnd, mtls->yEnd);
        if (yEnd <= yStart) {
            return;
        }

        //ALOGE("usr idx %i, x %i,%i  y %i,%i", idx, mtls->xStart, mtls->xEnd, yStart, yEnd);
        //ALOGE("usr ptr in %p,  out %p", mtls->fep.ptrIn, mtls->fep.ptrOut);

#if defined(ARCH_ARM_RS_USE_CACHED_SCANLINE_WRITE)
        if (mtls->fep.yStrideOut < sizeof(buf)) {
            p.out = buf;
            for (p.y = yStart; p.y < yEnd; p.y++) {
                p.in = mtls->fep.ptrIn + (mtls->fep.yStrideIn * p.y);
                fn(&p, mtls->xStart, mtls->xEnd, mtls->fep.eStrideIn, mtls->fep.eStrideOut);
                memcpy(mtls->fep.ptrOut + (mtls->fep.yStrideOut * p.y), buf, mtls->fep.yStrideOut);
            }
        } else
#endif
            {
            for (p.y = yStart; p.y < yEnd; p.y++) {
                p.out = mtls->fep.ptrOut + (mtls->fep.yStrideOut * p.y) +
                        (mtls->fep.eStrideOut * mtls->xStart);
                p.in = mtls->fep.ptrIn + (mtls->fep.yStrideIn * p.y) +
                       (mtls->fep.eStrideIn * mtls->xStart);
                fn(&p, mtls->xStart, mtls->xEnd, mtls->fep.eStrideIn, mtls->fep.eStrideOut);
            }
        }
    }
}

static void wc_x(void *usr, uint32_t idx) {
    MTLaunchStruct *mtls = (MTLaunchStruct *)usr;
    RsForEachStubParamStruct p;
    memcpy(&p, &mtls->fep, sizeof(p));
    RsdHal * dc = (RsdHal *)mtls->rsc->mHal.drv;
    uint32_t sig = mtls->sig;

    outer_foreach_t fn = (outer_foreach_t) mtls->kernel;
    while (1) {
        uint32_t slice = (uint32_t)android_atomic_inc(&mtls->mSliceNum);
        uint32_t xStart = mtls->xStart + slice * mtls->mSliceSize;
        uint32_t xEnd = xStart + mtls->mSliceSize;
        xEnd = rsMin(xEnd, mtls->xEnd);
        if (xEnd <= xStart) {
            return;
        }

        //ALOGE("usr slice %i idx %i, x %i,%i", slice, idx, xStart, xEnd);
        //ALOGE("usr ptr in %p,  out %p", mtls->fep.ptrIn, mtls->fep.ptrOut);

        p.out = mtls->fep.ptrOut + (mtls->fep.eStrideOut * xStart);
        p.in = mtls->fep.ptrIn + (mtls->fep.eStrideIn * xStart);
        fn(&p, xStart, xEnd, mtls->fep.eStrideIn, mtls->fep.eStrideOut);
    }
}

void rsdScriptInvokeForEachMtlsSetup(const Context *rsc,
                                     const Allocation * ain,
                                     Allocation * aout,
                                     const void * usr,
                                     uint32_t usrLen,
                                     const RsScriptCall *sc,
                                     MTLaunchStruct *mtls) {

    memset(mtls, 0, sizeof(MTLaunchStruct));

    if (ain) {
        mtls->fep.dimX = ain->getType()->getDimX();
        mtls->fep.dimY = ain->getType()->getDimY();
        mtls->fep.dimZ = ain->getType()->getDimZ();
        //mtls->dimArray = ain->getType()->getDimArray();
    } else if (aout) {
        mtls->fep.dimX = aout->getType()->getDimX();
        mtls->fep.dimY = aout->getType()->getDimY();
        mtls->fep.dimZ = aout->getType()->getDimZ();
        //mtls->dimArray = aout->getType()->getDimArray();
    } else {
        rsc->setError(RS_ERROR_BAD_SCRIPT, "rsForEach called with null allocations");
        return;
    }

    if (!sc || (sc->xEnd == 0)) {
        mtls->xEnd = mtls->fep.dimX;
    } else {
        rsAssert(sc->xStart < mtls->fep.dimX);
        rsAssert(sc->xEnd <= mtls->fep.dimX);
        rsAssert(sc->xStart < sc->xEnd);
        mtls->xStart = rsMin(mtls->fep.dimX, sc->xStart);
        mtls->xEnd = rsMin(mtls->fep.dimX, sc->xEnd);
        if (mtls->xStart >= mtls->xEnd) return;
    }

    if (!sc || (sc->yEnd == 0)) {
        mtls->yEnd = mtls->fep.dimY;
    } else {
        rsAssert(sc->yStart < mtls->fep.dimY);
        rsAssert(sc->yEnd <= mtls->fep.dimY);
        rsAssert(sc->yStart < sc->yEnd);
        mtls->yStart = rsMin(mtls->fep.dimY, sc->yStart);
        mtls->yEnd = rsMin(mtls->fep.dimY, sc->yEnd);
        if (mtls->yStart >= mtls->yEnd) return;
    }

    mtls->xEnd = rsMax((uint32_t)1, mtls->xEnd);
    mtls->yEnd = rsMax((uint32_t)1, mtls->yEnd);
    mtls->zEnd = rsMax((uint32_t)1, mtls->zEnd);
    mtls->arrayEnd = rsMax((uint32_t)1, mtls->arrayEnd);

    rsAssert(!ain || (ain->getType()->getDimZ() == 0));

    Context *mrsc = (Context *)rsc;
    mtls->rsc = mrsc;
    mtls->ain = ain;
    mtls->aout = aout;
    mtls->fep.usr = usr;
    mtls->fep.usrLen = usrLen;
    mtls->mSliceSize = 10;
    mtls->mSliceNum = 0;

    mtls->fep.ptrIn = NULL;
    mtls->fep.eStrideIn = 0;

    if (ain) {
        DrvAllocation *aindrv = (DrvAllocation *)ain->mHal.drv;
        mtls->fep.ptrIn = (const uint8_t *)aindrv->lod[0].mallocPtr;
        mtls->fep.eStrideIn = ain->getType()->getElementSizeBytes();
        mtls->fep.yStrideIn = aindrv->lod[0].stride;
    }

    mtls->fep.ptrOut = NULL;
    mtls->fep.eStrideOut = 0;
    if (aout) {
        DrvAllocation *aoutdrv = (DrvAllocation *)aout->mHal.drv;
        mtls->fep.ptrOut = (uint8_t *)aoutdrv->lod[0].mallocPtr;
        mtls->fep.eStrideOut = aout->getType()->getElementSizeBytes();
        mtls->fep.yStrideOut = aoutdrv->lod[0].stride;
    }
}

void rsdScriptLaunchThreads(const Context *rsc,
                            Script *s,
                            uint32_t slot,
                            const Allocation * ain,
                            Allocation * aout,
                            const void * usr,
                            uint32_t usrLen,
                            const RsScriptCall *sc,
                            MTLaunchStruct *mtls) {

    Script * oldTLS = setTLS(s);
    Context *mrsc = (Context *)rsc;
    RsdHal * dc = (RsdHal *)mtls->rsc->mHal.drv;

    if ((dc->mWorkers.mCount > 1) && s->mHal.info.isThreadable && !dc->mInForEach) {
        dc->mInForEach = true;
        if (mtls->fep.dimY > 1) {
            mtls->mSliceSize = mtls->fep.dimY / (dc->mWorkers.mCount * 4);
            if(mtls->mSliceSize < 1) {
                mtls->mSliceSize = 1;
            }

            rsdLaunchThreads(mrsc, wc_xy, mtls);
        } else {
            mtls->mSliceSize = mtls->fep.dimX / (dc->mWorkers.mCount * 4);
            if(mtls->mSliceSize < 1) {
                mtls->mSliceSize = 1;
            }

            rsdLaunchThreads(mrsc, wc_x, mtls);
        }
        dc->mInForEach = false;

        //ALOGE("launch 1");
    } else {
        RsForEachStubParamStruct p;
        memcpy(&p, &mtls->fep, sizeof(p));
        uint32_t sig = mtls->sig;

        //ALOGE("launch 3");
        outer_foreach_t fn = (outer_foreach_t) mtls->kernel;
        for (p.ar[0] = mtls->arrayStart; p.ar[0] < mtls->arrayEnd; p.ar[0]++) {
            for (p.z = mtls->zStart; p.z < mtls->zEnd; p.z++) {
                for (p.y = mtls->yStart; p.y < mtls->yEnd; p.y++) {
                    uint32_t offset = mtls->fep.dimY * mtls->fep.dimZ * p.ar[0] +
                                      mtls->fep.dimY * p.z + p.y;
                    p.out = mtls->fep.ptrOut + (mtls->fep.yStrideOut * offset) +
                            (mtls->fep.eStrideOut * mtls->xStart);
                    p.in = mtls->fep.ptrIn + (mtls->fep.yStrideIn * offset) +
                           (mtls->fep.eStrideIn * mtls->xStart);
                    fn(&p, mtls->xStart, mtls->xEnd, mtls->fep.eStrideIn, mtls->fep.eStrideOut);
                }
            }
        }
    }

    setTLS(oldTLS);
}

void rsdScriptInvokeForEach(const Context *rsc,
                            Script *s,
                            uint32_t slot,
                            const Allocation * ain,
                            Allocation * aout,
                            const void * usr,
                            uint32_t usrLen,
                            const RsScriptCall *sc) {

    RsdHal * dc = (RsdHal *)rsc->mHal.drv;

    MTLaunchStruct mtls;
    rsdScriptInvokeForEachMtlsSetup(rsc, ain, aout, usr, usrLen, sc, &mtls);
    mtls.script = s;
    mtls.fep.slot = slot;

    DrvScript *drv = (DrvScript *)s->mHal.drv;
    if (drv->mIntrinsicID) {
        mtls.kernel = (void (*)())drv->mIntrinsicFuncs.root;
        mtls.fep.usr = drv->mIntrinsicData;
    } else {
        rsAssert(slot < drv->mExecutable->getExportForeachFuncAddrs().size());
        mtls.kernel = reinterpret_cast<ForEachFunc_t>(
                          drv->mExecutable->getExportForeachFuncAddrs()[slot]);
        rsAssert(mtls.kernel != NULL);
        mtls.sig = drv->mExecutable->getInfo().getExportForeachFuncs()[slot].second;
    }


    rsdScriptLaunchThreads(rsc, s, slot, ain, aout, usr, usrLen, sc, &mtls);
}


int rsdScriptInvokeRoot(const Context *dc, Script *script) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;

    Script * oldTLS = setTLS(script);
    int ret = drv->mRoot();
    setTLS(oldTLS);

    return ret;
}

void rsdScriptInvokeInit(const Context *dc, Script *script) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;

    if (drv->mInit) {
        drv->mInit();
    }
}

void rsdScriptInvokeFreeChildren(const Context *dc, Script *script) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;

    if (drv->mFreeChildren) {
        drv->mFreeChildren();
    }
}

void rsdScriptInvokeFunction(const Context *dc, Script *script,
                            uint32_t slot,
                            const void *params,
                            size_t paramLength) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;
    //ALOGE("invoke %p %p %i %p %i", dc, script, slot, params, paramLength);

    Script * oldTLS = setTLS(script);
    reinterpret_cast<void (*)(const void *, uint32_t)>(
        drv->mExecutable->getExportFuncAddrs()[slot])(params, paramLength);
    setTLS(oldTLS);
}

void rsdScriptSetGlobalVar(const Context *dc, const Script *script,
                           uint32_t slot, void *data, size_t dataLength) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;
    //rsAssert(!script->mFieldIsObject[slot]);
    //ALOGE("setGlobalVar %p %p %i %p %i", dc, script, slot, data, dataLength);

    if (drv->mIntrinsicID) {
        drv->mIntrinsicFuncs.setVar(dc, script, drv->mIntrinsicData, slot, data, dataLength);
        return;
    }

    int32_t *destPtr = reinterpret_cast<int32_t *>(
                          drv->mExecutable->getExportVarAddrs()[slot]);
    if (!destPtr) {
        //ALOGV("Calling setVar on slot = %i which is null", slot);
        return;
    }

    memcpy(destPtr, data, dataLength);
}

void rsdScriptSetGlobalVarWithElemDims(
        const android::renderscript::Context *dc,
        const android::renderscript::Script *script,
        uint32_t slot, void *data, size_t dataLength,
        const android::renderscript::Element *elem,
        const size_t *dims, size_t dimLength) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;

    int32_t *destPtr = reinterpret_cast<int32_t *>(
        drv->mExecutable->getExportVarAddrs()[slot]);
    if (!destPtr) {
        //ALOGV("Calling setVar on slot = %i which is null", slot);
        return;
    }

    // We want to look at dimension in terms of integer components,
    // but dimLength is given in terms of bytes.
    dimLength /= sizeof(int);

    // Only a single dimension is currently supported.
    rsAssert(dimLength == 1);
    if (dimLength == 1) {
        // First do the increment loop.
        size_t stride = elem->getSizeBytes();
        char *cVal = reinterpret_cast<char *>(data);
        for (size_t i = 0; i < dims[0]; i++) {
            elem->incRefs(cVal);
            cVal += stride;
        }

        // Decrement loop comes after (to prevent race conditions).
        char *oldVal = reinterpret_cast<char *>(destPtr);
        for (size_t i = 0; i < dims[0]; i++) {
            elem->decRefs(oldVal);
            oldVal += stride;
        }
    }

    memcpy(destPtr, data, dataLength);
}

void rsdScriptSetGlobalBind(const Context *dc, const Script *script, uint32_t slot, Allocation *data) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;

    //rsAssert(!script->mFieldIsObject[slot]);
    //ALOGE("setGlobalBind %p %p %i %p", dc, script, slot, data);

    rsAssert(!drv->mIntrinsicID);

    int32_t *destPtr = reinterpret_cast<int32_t *>(
                          drv->mExecutable->getExportVarAddrs()[slot]);
    if (!destPtr) {
        //ALOGV("Calling setVar on slot = %i which is null", slot);
        return;
    }

    void *ptr = NULL;
    drv->mBoundAllocs[slot] = data;
    if(data) {
        DrvAllocation *allocDrv = (DrvAllocation *)data->mHal.drv;
        ptr = allocDrv->lod[0].mallocPtr;
    }
    memcpy(destPtr, &ptr, sizeof(void *));
}

void rsdScriptSetGlobalObj(const Context *dc, const Script *script, uint32_t slot, ObjectBase *data) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;
    //rsAssert(script->mFieldIsObject[slot]);
    //ALOGE("setGlobalObj %p %p %i %p", dc, script, slot, data);

    if (drv->mIntrinsicID) {
        drv->mIntrinsicFuncs.setVarObj(dc, script, drv->mIntrinsicData, slot,
                                       static_cast<Allocation *>(data));
        return;
    }

    int32_t *destPtr = reinterpret_cast<int32_t *>(
                          drv->mExecutable->getExportVarAddrs()[slot]);
    if (!destPtr) {
        //ALOGV("Calling setVar on slot = %i which is null", slot);
        return;
    }

    rsrSetObject(dc, script, (ObjectBase **)destPtr, data);
}

void rsdScriptDestroy(const Context *dc, Script *script) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;

    if (drv == NULL) {
        return;
    }

    if (drv->mExecutable) {
        Vector<void *>::const_iterator var_addr_iter =
            drv->mExecutable->getExportVarAddrs().begin();
        Vector<void *>::const_iterator var_addr_end =
            drv->mExecutable->getExportVarAddrs().end();

        bcc::RSInfo::ObjectSlotListTy::const_iterator is_object_iter =
            drv->mExecutable->getInfo().getObjectSlots().begin();
        bcc::RSInfo::ObjectSlotListTy::const_iterator is_object_end =
            drv->mExecutable->getInfo().getObjectSlots().end();

        while ((var_addr_iter != var_addr_end) &&
               (is_object_iter != is_object_end)) {
            // The field address can be NULL if the script-side has optimized
            // the corresponding global variable away.
            ObjectBase **obj_addr =
                reinterpret_cast<ObjectBase **>(*var_addr_iter);
            if (*is_object_iter) {
                if (*var_addr_iter != NULL) {
                    rsrClearObject(dc, script, obj_addr);
                }
            }
            var_addr_iter++;
            is_object_iter++;
        }
    }

    delete drv->mCompilerContext;
    delete drv->mCompilerDriver;
    delete drv->mExecutable;
    delete[] drv->mBoundAllocs;
    free(drv);
    script->mHal.drv = NULL;
}

Allocation * rsdScriptGetAllocationForPointer(const android::renderscript::Context *dc,
                                              const android::renderscript::Script *sc,
                                              const void *ptr) {
    DrvScript *drv = (DrvScript *)sc->mHal.drv;
    if (!ptr) {
        return NULL;
    }

    for (uint32_t ct=0; ct < sc->mHal.info.exportedVariableCount; ct++) {
        Allocation *a = drv->mBoundAllocs[ct];
        if (!a) continue;
        DrvAllocation *adrv = (DrvAllocation *)a->mHal.drv;
        if (adrv->lod[0].mallocPtr == ptr) {
            return a;
        }
    }
    ALOGE("rsGetAllocation, failed to find %p", ptr);
    return NULL;
}

