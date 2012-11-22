/*
 * Copyright (C) 2010 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#include <list>

namespace android {

void ListNodeBase::swap(ListNodeBase& a, ListNodeBase& b) {
    if (a.mNext != &a) {
        if (b.mNext != &b) {
            // a and b are not empty.
            std::swap(a.mNext, b.mNext);
            std::swap(a.mPrev, b.mPrev);
            a.mNext->mPrev = a.mPrev->mNext = &a;
            b.mNext->mPrev = b.mPrev->mNext = &b;
        } else {
            // a not empty but b is.
            b.mNext = a.mNext;
            b.mPrev = a.mPrev;
            b.mNext->mPrev = b.mPrev->mNext = &b;
            a.mNext = a.mPrev = &a;  // empty a
        }
    } else if (b.mNext != &b) {
        // a is empty but b is not.
        a.mNext = b.mNext;
        a.mPrev = b.mPrev;
        a.mNext->mPrev = a.mPrev->mNext = &a;
        b.mNext = b.mPrev = &b;  // empty b
    }
}

void ListNodeBase::hook(ListNodeBase *const pos) {
    mNext = pos;
    mPrev = pos->mPrev;
    pos->mPrev->mNext = this;
    pos->mPrev = this;
}

void ListNodeBase::unhook() {
    ListNodeBase *const next = mNext;
    ListNodeBase *const prev = mPrev;
    prev->mNext = next;
    next->mPrev = prev;
}

}  // namespace android
