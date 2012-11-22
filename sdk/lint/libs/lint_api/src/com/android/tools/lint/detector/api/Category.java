/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.tools.lint.detector.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.annotations.Beta;

/**
 * A category is a container for related issues.
 * <p/>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public final class Category implements Comparable<Category> {
    private final String mName;
    private final String mExplanation;
    private final int mPriority;
    private final Category mParent;

    /**
     * Creates a new {@link Category}.
     *
     * @param parent the name of a parent category, or null
     * @param name the name of the category
     * @param explanation an optional explanation of the category
     * @param priority a sorting priority, with higher being more important
     */
    private Category(
            @Nullable Category parent,
            @NonNull String name,
            @Nullable String explanation,
            int priority) {
        mParent = parent;
        mName = name;
        mExplanation = explanation;
        mPriority = priority;
    }

    /**
     * Creates a new top level {@link Category} with the given sorting priority.
     *
     * @param name the name of the category
     * @param priority a sorting priority, with higher being more important
     * @return a new category
     */
    @NonNull
    public static Category create(@NonNull String name, int priority) {
        return new Category(null, name, null, priority);
    }

    /**
     * Creates a new top level {@link Category} with the given sorting priority.
     *
     * @param parent the name of a parent category, or null
     * @param name the name of the category
     * @param explanation an optional explanation of the category
     * @param priority a sorting priority, with higher being more important
     * @return a new category
     */
    @NonNull
    public static Category create(
            @Nullable Category parent,
            @NonNull String name,
            @Nullable String explanation,
            int priority) {
        return new Category(parent, name, null, priority);
    }

    /**
     * Returns the parent category, or null if this is a top level category
     *
     * @return the parent category, or null if this is a top level category
     */
    public Category getParent() {
        return mParent;
    }

    /**
     * Returns the name of this category
     *
     * @return the name of this category
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns an explanation for this category, or null
     *
     * @return an explanation for this category, or null
     */
    public String getExplanation() {
        return mExplanation;
    }

    /**
     * Returns a full name for this category. For a top level category, this is just
     * the {@link #getName()} value, but for nested categories it will include the parent
     * names as well.
     *
     * @return a full name for this category
     */
    public String getFullName() {
        if (mParent != null) {
            return mParent.getFullName() + ':' + mName;
        } else {
            return mName;
        }
    }

    @Override
    public int compareTo(Category other) {
        if (other.mPriority == mPriority) {
            if (mParent == other) {
                return 1;
            } else if (other.mParent == this) {
                return -1;
            }
        }
        return other.mPriority - mPriority;
    }

    /** Issues related to running lint itself */
    public static final Category LINT = Category.create("Lint", 110);

    /** Issues related to correctness */
    public static final Category CORRECTNESS = Category.create("Correctness", 100);

    /** Issues related to security */
    public static final Category SECURITY = Category.create("Security", 90);

    /** Issues related to performance */
    public static final Category PERFORMANCE = Category.create("Performance", 80);

    /** Issues related to usability */
    public static final Category USABILITY = Category.create("Usability", 70);

    /** Issues related to accessibility */
    public static final Category A11Y = Category.create("Accessibility", 60);

    /** Issues related to internationalization */
    public static final Category I18N = Category.create("Internationalization", 50);

    // Sub categories

    /** Issues related to icons */
    public static final Category ICONS = Category.create(USABILITY, "Icons", null, 73);

    /** Issues related to typography */
    public static final Category TYPOGRAPHY = Category.create(USABILITY, "Typography", null, 76);

    /** Issues related to messages/strings */
    public static final Category MESSAGES = Category.create(CORRECTNESS, "Messages", null, 95);
}
