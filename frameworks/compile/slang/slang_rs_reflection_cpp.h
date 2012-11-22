/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_REFLECTION_CPP_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_REFLECTION_CPP_H_

#include "slang_rs_reflection_base.h"

namespace slang {

class RSReflectionCpp : public RSReflectionBase {
protected:


public:
    RSReflectionCpp(const RSContext *);
    virtual ~RSReflectionCpp();

    bool reflect(const std::string &OutputPathBase,
                 const std::string &InputFileName,
                 const std::string &OutputBCFileName);


private:
    bool makeHeader(const std::string &baseClass);
    bool makeImpl(const std::string &baseClass);
    bool writeBC();

    bool startScriptHeader();

};  // class RSReflection

}   // namespace slang

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_REFLECTION_CPP_H_  NOLINT
