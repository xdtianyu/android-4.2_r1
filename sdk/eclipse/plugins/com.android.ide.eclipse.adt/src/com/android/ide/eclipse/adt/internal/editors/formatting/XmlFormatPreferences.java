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
package com.android.ide.eclipse.adt.internal.editors.formatting;

import com.android.annotations.VisibleForTesting;
import com.android.ide.eclipse.adt.internal.preferences.AdtPrefs;
import com.android.ide.eclipse.adt.internal.preferences.AttributeSortOrder;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceNames;

/**
 * Formatting preferences used by the Android XML formatter.
 */
public class XmlFormatPreferences {
    /** Use the Eclipse indent (tab/space, indent size) settings? */
    public boolean useEclipseIndent = false;

    /** Remove empty lines in all cases? */
    public boolean removeEmptyLines = false;

    /** Reformat the text and comment blocks? */
    public boolean reflowText = false;

    /** Join lines when reformatting text and comment blocks? */
    public boolean joinLines = false;

    /** Can attributes appear on the same line as the opening line if there is just one of them? */
    public boolean oneAttributeOnFirstLine = true;

    /** The sorting order to use when formatting */
    public AttributeSortOrder sortAttributes = AttributeSortOrder.LOGICAL;

    /** Should there be a space before the closing > or /> ? */
    public boolean spaceBeforeClose = true;

    /** The string to insert for each indentation level */
    private String mOneIndentUnit = "    "; //$NON-NLS-1$

    /** Tab width (number of spaces to display for a tab) */
    private int mTabWidth = -1; // -1: uninitialized

    @VisibleForTesting
    protected XmlFormatPreferences() {
    }

    /**
     * Creates a new {@link XmlFormatPreferences} based on the current settings
     * in {@link AdtPrefs}
     *
     * @return an {@link XmlFormatPreferences} object
     */
    public static XmlFormatPreferences create() {
        XmlFormatPreferences p = new XmlFormatPreferences();
        AdtPrefs prefs = AdtPrefs.getPrefs();

        p.useEclipseIndent = prefs.isUseEclipseIndent();
        p.removeEmptyLines = prefs.isRemoveEmptyLines();
        p.oneAttributeOnFirstLine = prefs.isOneAttributeOnFirstLine();
        p.sortAttributes = prefs.getAttributeSort();
        p.spaceBeforeClose = prefs.isSpaceBeforeClose();

        return p;
    }

    // The XML module settings do not have a public API. We should replace this with JDT
    // settings anyway since that's more likely what users have configured and want applied
    // to their XML files
    /**
     * Returns the string to use to indent one indentation level
     *
     * @return the string used to indent one indentation level
     */
    @SuppressWarnings({
            "restriction", "deprecation"
    })
    public String getOneIndentUnit() {
        if (useEclipseIndent) {
            // Look up Eclipse indent preferences
            // TODO: Use the JDT preferences instead, which make more sense
            Preferences preferences = XMLCorePlugin.getDefault().getPluginPreferences();
            int indentationWidth = preferences.getInt(XMLCorePreferenceNames.INDENTATION_SIZE);
            String indentCharPref = preferences.getString(XMLCorePreferenceNames.INDENTATION_CHAR);
            boolean useSpaces = XMLCorePreferenceNames.SPACE.equals(indentCharPref);

            StringBuilder indentString = new StringBuilder();
            for (int j = 0; j < indentationWidth; j++) {
                if (useSpaces) {
                    indentString.append(' ');
                } else {
                    indentString.append('\t');
                }
            }
            mOneIndentUnit = indentString.toString();
        }

        return mOneIndentUnit;
    }

    /**
     * Returns the number of spaces used to display a single tab character
     *
     * @return the number of spaces used to display a single tab character
     */
    @SuppressWarnings("restriction") // Editor settings
    public int getTabWidth() {
        if (mTabWidth == -1) {
            String key = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH;
            try {
                IPreferenceStore prefs = EditorsPlugin.getDefault().getPreferenceStore();
                mTabWidth = prefs.getInt(key);
            } catch (Throwable t) {
                // Pass: We'll pick a suitable default instead below
            }
            if (mTabWidth <= 0) {
                mTabWidth = 4;
            }
        }

        return mTabWidth;
    }
}
