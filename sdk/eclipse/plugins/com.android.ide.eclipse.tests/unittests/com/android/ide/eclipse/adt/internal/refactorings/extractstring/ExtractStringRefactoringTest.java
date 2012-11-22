/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.refactorings.extractstring;

import junit.framework.TestCase;

public class ExtractStringRefactoringTest extends TestCase {

    public void testEscapeStringShouldEscapeXmlSpecialCharacters() throws Exception {
        assertEquals("&lt;", escape("<")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("&amp;", escape("&")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testEscapeStringShouldEscapeQuotes() throws Exception {
        assertEquals("\\'", escape("'")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\\\"", escape("\"")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\" ' \"", escape(" ' ")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testEscapeStringShouldPreserveWhitespace() throws Exception {
        assertEquals("\"at end  \"", escape("at end  ")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\"  at begin\"", escape("  at begin")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testEscapeStringShouldEscapeAtSignAndQuestionMarkOnlyAtBeginning()
            throws Exception {
        assertEquals("\\@text", escape("@text")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("a@text", escape("a@text")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\\?text", escape("?text")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("a?text", escape("a?text")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\" ?text\"", escape(" ?text")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testEscapeStringShouldEscapeJavaEscapeSequences() throws Exception {
        assertEquals("\\n", escape("\n")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\\t", escape("\t")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\\\\", escape("\\")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static String escape(String unescaped) {
        return ExtractStringRefactoring.escapeString(unescaped);
    }
}
