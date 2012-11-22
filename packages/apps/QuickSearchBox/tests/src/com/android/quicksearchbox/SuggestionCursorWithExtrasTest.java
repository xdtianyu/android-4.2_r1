/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.SearchManager;
import android.database.Cursor;
import android.test.AndroidTestCase;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Tests to verify that custom columns from a suggestion cursor get propagated through QSB properly.
 */
public class SuggestionCursorWithExtrasTest extends AndroidTestCase {

    public void testSuggestionCursorExtraRowString() {
        checkSuggestionCursorExtraColumnValue("extra_text", "Extra text");
    }

    public void testSuggestionCursorExtraRowInteger() {
        checkSuggestionCursorExtraColumnValue("extra_int", 42);
    }

    public void testSuggestionCursorExtraRowFloat() {
        checkSuggestionCursorExtraColumnValue("extra_float", new Float(Math.E));
    }

    public void testSuggestionCursorExtraRowNull() {
        checkSuggestionCursorExtraColumnValue("extra_null", null);
    }

    public void testCursorExtraRowString() {
        checkExtraRowString("stringy-string");
        checkExtraRowString("");
        checkExtraRowString(null);
    }

    private void checkExtraRowString(String value) {
        String column = "extra_string";
        Cursor c = createCursorWithExtras(column, value);
        assertEquals("Extra column value", value, c.getString(c.getColumnIndex(column)));
    }

    public void testCursorExtraRowInt() {
        checkCursorExtraInt(42);
        checkCursorExtraInt(0);
        checkCursorExtraInt(-42);
        checkCursorExtraInt(Integer.MAX_VALUE);
        checkCursorExtraInt(Integer.MIN_VALUE);
    }

    public void checkCursorExtraInt(int value) {
        String column = "extra_int";
        Cursor c = createCursorWithExtras(column, value);
        assertEquals("Extra column value", value, c.getInt(c.getColumnIndex(column)));
    }

    public void testCursorExtraRowDouble() {
        checkCursorExtraRowDouble(Math.PI);
        checkCursorExtraRowDouble(-Math.PI);
        checkCursorExtraRowDouble(0);
        checkCursorExtraRowDouble(Double.MAX_VALUE);
        checkCursorExtraRowDouble(Double.MIN_VALUE);
    }

    public void checkCursorExtraRowDouble(double value) {
        String column = "extra_double";
        Cursor c = createCursorWithExtras(column, value);
        assertEquals("Extra column value", value, c.getDouble(c.getColumnIndex(column)));
    }

    public void testCursorExtraRowFloat() {
        checkCursorExtraRowFloat((float) Math.E);
        checkCursorExtraRowFloat((float) -Math.E);
        checkCursorExtraRowFloat(0f);
        checkCursorExtraRowFloat(Float.MAX_VALUE);
        checkCursorExtraRowFloat(Float.MIN_VALUE);
    }

    public void checkCursorExtraRowFloat(float value) {
        String column = "extra_float";
        Cursor c = createCursorWithExtras(column, value);
        assertEquals("Extra column value", value, c.getFloat(c.getColumnIndex(column)));
    }

    public void testCursorExtraRowLong() {
        checkExtraRowLong(0xfeed0beefl);
        checkExtraRowLong(-0xfeed0beefl);
        checkExtraRowLong(0);
        checkExtraRowLong(Long.MIN_VALUE);
        checkExtraRowLong(Long.MAX_VALUE);
    }

    private void checkExtraRowLong(long value) {
        String column = "extra_long";
        Cursor c = createCursorWithExtras(column, value);
        assertEquals("Extra column value", value, c.getLong(c.getColumnIndex(column)));
    }

    public void testCursorExtraRowShort() {
        checkCursorExtraRowShort((short) 0xabc);
        checkCursorExtraRowShort((short) -0xabc);
        checkCursorExtraRowShort((short) 0);
        checkCursorExtraRowShort(Short.MAX_VALUE);
        checkCursorExtraRowShort(Short.MIN_VALUE);
    }

    private void checkCursorExtraRowShort(short value) {
        String column = "extra_short";
        Cursor c = createCursorWithExtras(column, value);
        assertEquals("Extra column value", value, c.getShort(c.getColumnIndex(column)));
    }

    private Cursor createCursorWithExtras(String columnName, Object columnValue) {
        MockSuggestionProviderCursor expectedCursor = new MockSuggestionProviderCursor(
                new String[]{"_id",   SearchManager.SUGGEST_COLUMN_TEXT_1, columnName });
        expectedCursor.addRow(       0,       "Text 1",                            columnValue);

        // this roughly approcimates what happens to suggestions
        CursorBackedSourceResult suggestions = new CursorBackedSourceResult(
                MockSource.SOURCE_1, "", expectedCursor);
        assertEquals("Number of suggestions", 1, suggestions.getCount());
        suggestions.moveTo(0);
        SuggestionCursorBackedCursor observedCursor = new SuggestionCursorBackedCursor(suggestions);

        assertEquals("Cursor rows", 1, observedCursor.getCount());
        HashSet<String> rows = new HashSet<String>();
        rows.addAll(Arrays.asList(observedCursor.getColumnNames()));
        assertTrue("Extra column present in cursor", rows.contains(columnName));
        observedCursor.moveToFirst();
        return observedCursor;
    }

    private void checkSuggestionCursorExtraColumnValue(String columnName, Object columnValue) {
        MockSuggestionProviderCursor cursor = new MockSuggestionProviderCursor(
                new String[]{"_id",   SearchManager.SUGGEST_COLUMN_TEXT_1, columnName });
        cursor.addRow(       0,       "Text 1",                            columnValue);

        CursorBackedSourceResult suggestions = new CursorBackedSourceResult(
                MockSource.SOURCE_1, "", cursor);

        assertEquals("Suggestions count matches cursor count",
                cursor.getCount(), suggestions.getCount());

        cursor.moveToFirst();
        suggestions.moveTo(0);

        SuggestionExtras extras = suggestions.getExtras();
        assertNotNull("Suggestions extras", extras);
        assertTrue("Extra column missing", extras.getExtraColumnNames().contains(columnName));
        assertTrue("Spurious columns", extras.getExtraColumnNames().size() == 1);
        Object extraValue = extras.getExtra(columnName);
        if (columnValue == null) {
            assertEquals("Wrong extra value", columnValue, extraValue);
        } else {
            assertNotNull("Extra value null", extraValue);
            if (columnValue == null) {
                assertEquals("Extra value wrong", columnValue, extraValue);
            } else {
                assertEquals("Extra value wrong", columnValue.toString(), extraValue);
            }
        }
    }

}
