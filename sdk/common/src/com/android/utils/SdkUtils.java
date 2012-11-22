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

package com.android.utils;

import com.android.annotations.NonNull;

public class SdkUtils {
    /**
     * Returns true if the given string ends with the given suffix, using a
     * case-insensitive comparison.
     *
     * @param string the full string to be checked
     * @param suffix the suffix to be checked for
     * @return true if the string case-insensitively ends with the given suffix
     */
    public static boolean endsWithIgnoreCase(String string, String suffix) {
        return string.regionMatches(true /* ignoreCase */, string.length() - suffix.length(),
                suffix, 0, suffix.length());
    }

    /**
     * Returns true if the given sequence ends with the given suffix (case
     * sensitive).
     *
     * @param sequence the character sequence to be checked
     * @param suffix the suffix to look for
     * @return true if the given sequence ends with the given suffix
     */
    public static boolean endsWith(CharSequence sequence, CharSequence suffix) {
        return endsWith(sequence, sequence.length(), suffix);
    }

    /**
     * Returns true if the given sequence ends at the given offset with the given suffix (case
     * sensitive)
     *
     * @param sequence the character sequence to be checked
     * @param endOffset the offset at which the sequence is considered to end
     * @param suffix the suffix to look for
     * @return true if the given sequence ends with the given suffix
     */
    public static boolean endsWith(CharSequence sequence, int endOffset, CharSequence suffix) {
        if (endOffset < suffix.length()) {
            return false;
        }

        for (int i = endOffset - 1, j = suffix.length() - 1; j >= 0; i--, j--) {
            if (sequence.charAt(i) != suffix.charAt(j)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if the given string starts with the given prefix, using a
     * case-insensitive comparison.
     *
     * @param string the full string to be checked
     * @param prefix the prefix to be checked for
     * @return true if the string case-insensitively starts with the given prefix
     */
    public static boolean startsWithIgnoreCase(String string, String prefix) {
        return string.regionMatches(true /* ignoreCase */, 0, prefix, 0, prefix.length());
    }

    /**
     * Returns true if the given string starts at the given offset with the
     * given prefix, case insensitively.
     *
     * @param string the full string to be checked
     * @param offset the offset in the string to start looking
     * @param prefix the prefix to be checked for
     * @return true if the string case-insensitively starts at the given offset
     *         with the given prefix
     */
    public static boolean startsWith(String string, int offset, String prefix) {
        return string.regionMatches(true /* ignoreCase */, offset, prefix, 0, prefix.length());
    }

    /**
     * Strips the whitespace from the given string
     *
     * @param string the string to be cleaned up
     * @return the string, without whitespace
     */
    public static String stripWhitespace(String string) {
        StringBuilder sb = new StringBuilder(string.length());
        for (int i = 0, n = string.length(); i < n; i++) {
            char c = string.charAt(i);
            if (!Character.isWhitespace(c)) {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /** For use by {@link #getLineSeparator()} */
    private static String sLineSeparator;

    /**
     * Returns the default line separator to use.
     * <p>
     * NOTE: If you have an associated {@link IDocument}, it is better to call
     * {@link TextUtilities#getDefaultLineDelimiter(IDocument)} since that will
     * allow (for example) editing a \r\n-delimited document on a \n-delimited
     * platform and keep a consistent usage of delimiters in the file.
     *
     * @return the delimiter string to use
     */
    @NonNull
    public static String getLineSeparator() {
        if (sLineSeparator == null) {
            // This is guaranteed to exist:
            sLineSeparator = System.getProperty("line.separator"); //$NON-NLS-1$
        }

        return sLineSeparator;
    }
}
