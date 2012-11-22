/*
 * Copyright (C) 2012 The Android Open Source Project
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

#ifndef __ANDROID_BASE_OBJ_H__
#define __ANDROID_BASE_OBJ_H__


#include "utils/RefBase.h"
#include <pthread.h>
#include <rs.h>

#include "RenderScript.h"

namespace android {
namespace renderscriptCpp {


class BaseObj : public android::LightRefBase<BaseObj> {
protected:
    friend class Element;
    friend class Type;
    friend class Allocation;
    friend class Script;
    friend class ScriptC;

    void *mID;
    RenderScript *mRS;
    android::String8 mName;

    void * getID() const;

    BaseObj(void *id, RenderScript *rs);
    void checkValid();

    static void * getObjID(sp<const BaseObj> o);

public:

    virtual ~BaseObj();
    virtual void updateFromNative();
    virtual bool equals(const BaseObj *obj);
};

}
}
#endif
