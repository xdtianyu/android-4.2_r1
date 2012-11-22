/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.quicksearchbox;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import java.util.Collection;

/**
 * A corpus is a user-visible set of suggestions. A corpus gets suggestions from one
 * or more sources.
 *
 * Objects that implement this interface should override {@link Object#equals(Object)}
 * and {@link Object#hashCode()} so that they can be used as keys in hash maps.
 */
public interface Corpus extends SuggestionCursorProvider<CorpusResult> {

    /**
     * Gets the localized, human-readable label for this corpus.
     */
    CharSequence getLabel();

    /**
     * Gets the icon for this corpus.
     */
    Drawable getCorpusIcon();

    /**
     * Gets the icon URI for this corpus.
     */
    Uri getCorpusIconUri();

    /**
     * Gets the description to use for this corpus in system search settings.
     */
    CharSequence getSettingsDescription();

    /**
     * Gets the search hint text for this corpus.
     */
    CharSequence getHint();

    /**
     * @return The minimum query length for which this corpus should be queried.
     */
    int getQueryThreshold();

    boolean queryAfterZeroResults();

    boolean voiceSearchEnabled();

    Intent createSearchIntent(String query, Bundle appData);

    Intent createVoiceSearchIntent(Bundle appData);

    boolean isWebCorpus();

    /**
     * Gets the sources that this corpus uses.
     */
    Collection<Source> getSources();

    /**
     * Checks if this corpus is enabled by default.
     */
    boolean isCorpusDefaultEnabled();

    /**
     * Whether this corpus should be included in the blended All mode.
     */
    boolean includeInAll();

    /**
     * Checks if this corpus should be hidden from the corpus selector.
     */
    boolean isCorpusHidden();

}
