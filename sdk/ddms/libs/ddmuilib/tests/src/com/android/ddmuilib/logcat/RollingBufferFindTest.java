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

package com.android.ddmuilib.logcat;

import com.android.ddmuilib.AbstractBufferFindTarget;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

public class RollingBufferFindTest extends TestCase {
    public class FindTarget extends AbstractBufferFindTarget {
        private int mSelectedItem = -1;
        private int mItemReadCount = 0;
        private List<String> mItems = Arrays.asList(
                "abc",
                "def",
                "abc",
                null,
                "xyz"
        );

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public String getItem(int index) {
            mItemReadCount++;
            return mItems.get(index);
        }

        @Override
        public void selectAndReveal(int index) {
            mSelectedItem = index;
        }

        @Override
        public int getStartingIndex() {
            return mItems.size() - 1;
        }
    }
    FindTarget mFindTarget = new FindTarget();

    public void testMultipleMatch() {
        mFindTarget.mSelectedItem = -1;

        String text = "abc";
        int lastIndex = mFindTarget.mItems.lastIndexOf(text);
        int firstIndex = mFindTarget.mItems.indexOf(text);

        // the first time we search through the buffer we should hit the item at lastIndex
        assertTrue(mFindTarget.findAndSelect(text, true, false));
        assertEquals(lastIndex, mFindTarget.mSelectedItem);

        // subsequent search should hit the item at first index
        assertTrue(mFindTarget.findAndSelect(text, false, false));
        assertEquals(firstIndex, mFindTarget.mSelectedItem);

        // search again should roll over and hit the last index
        assertTrue(mFindTarget.findAndSelect(text, false, false));
        assertEquals(lastIndex, mFindTarget.mSelectedItem);
    }

    public void testMissingItem() {
        mFindTarget.mSelectedItem = -1;
        mFindTarget.mItemReadCount = 0;

        // should not match
        assertFalse(mFindTarget.findAndSelect("nonexistent", true, false));

        // no item should be selected
        assertEquals(-1, mFindTarget.mSelectedItem);

        // but all items should have been read in once
        assertEquals(mFindTarget.getItemCount(), mFindTarget.mItemReadCount);
    }

    public void testSearchDirection() {
        String text = "abc";
        int lastIndex = mFindTarget.mItems.lastIndexOf(text);
        int firstIndex = mFindTarget.mItems.indexOf(text);

        // the first time we search through the buffer we should hit the "abc" from the last
        assertTrue(mFindTarget.findAndSelect(text, true, false));
        assertEquals(lastIndex, mFindTarget.mSelectedItem);

        // searching forward from there should also hit the first index
        assertTrue(mFindTarget.findAndSelect(text, false, true));
        assertEquals(firstIndex, mFindTarget.mSelectedItem);
    }
}
