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

package android.text.format.cts;

import android.content.Context;
import android.test.AndroidTestCase;
import android.text.format.DateUtils;
import dalvik.annotation.KnownFailure;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtilsTest extends AndroidTestCase {

    private static final long MIN_DURATION = 1000;
    private static final long MINUTE_DURATION = 42 * 60 * 1000;
    private static final long HOUR_DURATION = 2 * 60 * 60 * 1000;
    private static final long DAY_DURATION = 5 * 24 * 60 * 60 * 1000;
    private long mBaseTime;
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        mBaseTime = System.currentTimeMillis();
    }

    public void testGetDayOfWeekString() {
        if (!LocaleUtils.isCurrentLocale(mContext, Locale.US)) {
            return;
        }

        assertEquals("Sunday",
                DateUtils.getDayOfWeekString(Calendar.SUNDAY, DateUtils.LENGTH_LONG));
        assertEquals("Sun",
                DateUtils.getDayOfWeekString(Calendar.SUNDAY, DateUtils.LENGTH_MEDIUM));
        assertEquals("Sun",
                DateUtils.getDayOfWeekString(Calendar.SUNDAY, DateUtils.LENGTH_SHORT));
        assertEquals("Sun",
                DateUtils.getDayOfWeekString(Calendar.SUNDAY, DateUtils.LENGTH_SHORTER));
        assertEquals("S",
                DateUtils.getDayOfWeekString(Calendar.SUNDAY, DateUtils.LENGTH_SHORTEST));
        // Other abbrev
        assertEquals("Sun",
                DateUtils.getDayOfWeekString(Calendar.SUNDAY, 60));
    }

    public void testGetMonthString() {
        if (!LocaleUtils.isCurrentLocale(mContext, Locale.US)) {
            return;
        }
        assertEquals("January", DateUtils.getMonthString(Calendar.JANUARY, DateUtils.LENGTH_LONG));
        assertEquals("Jan",
                DateUtils.getMonthString(Calendar.JANUARY, DateUtils.LENGTH_MEDIUM));
        assertEquals("Jan", DateUtils.getMonthString(Calendar.JANUARY, DateUtils.LENGTH_SHORT));
        assertEquals("Jan",
                DateUtils.getMonthString(Calendar.JANUARY, DateUtils.LENGTH_SHORTER));
        assertEquals("J",
                DateUtils.getMonthString(Calendar.JANUARY, DateUtils.LENGTH_SHORTEST));
        // Other abbrev
        assertEquals("Jan", DateUtils.getMonthString(Calendar.JANUARY, 60));
    }

    public void testGetAMPMString() {
        if (!LocaleUtils.isCurrentLocale(mContext, Locale.US)) {
            return;
        }
        assertEquals("AM", DateUtils.getAMPMString(Calendar.AM));
        assertEquals("PM", DateUtils.getAMPMString(Calendar.PM));
    }


    public void testGetSpanString() {
        if (!LocaleUtils.isCurrentLocale(mContext, Locale.US)) {
            return;
        }
        assertEquals("0 minutes ago",
                DateUtils.getRelativeTimeSpanString(mBaseTime - MIN_DURATION).toString());
        assertEquals("in 0 minutes",
                DateUtils.getRelativeTimeSpanString(mBaseTime + MIN_DURATION).toString());

        assertEquals("42 minutes ago", DateUtils.getRelativeTimeSpanString(
                mBaseTime - MINUTE_DURATION, mBaseTime, DateUtils.MINUTE_IN_MILLIS).toString());
        assertEquals("in 42 minutes", DateUtils.getRelativeTimeSpanString(
                mBaseTime + MINUTE_DURATION, mBaseTime, DateUtils.MINUTE_IN_MILLIS).toString());

        assertEquals("2 hours ago", DateUtils.getRelativeTimeSpanString(mBaseTime - HOUR_DURATION,
                mBaseTime, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_NUMERIC_DATE).toString());
        assertEquals("in 2 hours", DateUtils.getRelativeTimeSpanString(mBaseTime + HOUR_DURATION,
                mBaseTime, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_NUMERIC_DATE).toString());
        assertEquals("in 42 mins", DateUtils.getRelativeTimeSpanString(mBaseTime + MINUTE_DURATION,
                mBaseTime, DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE).toString());

        assertNotNull(DateUtils.getRelativeDateTimeString(mContext,
                mBaseTime - DAY_DURATION, DateUtils.MINUTE_IN_MILLIS, DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_NUMERIC_DATE).toString());
        assertNotNull(DateUtils.getRelativeTimeSpanString(mContext,
                mBaseTime - DAY_DURATION, true).toString());
        assertNotNull(DateUtils.getRelativeTimeSpanString(mContext,
                mBaseTime - DAY_DURATION).toString());
    }

    @SuppressWarnings("deprecation")
    public void testFormatMethods() {
        if (!LocaleUtils.isCurrentLocale(mContext, Locale.US)) {
            return;
        }

        long elapsedTime = 2 * 60 * 60;
        String expected = "2:00:00";
        assertEquals(expected, DateUtils.formatElapsedTime(elapsedTime));
        StringBuilder sb = new StringBuilder();
        assertEquals(expected, DateUtils.formatElapsedTime(sb, elapsedTime));
        assertEquals(expected, sb.toString());

        Date date = new Date(109, 0, 19, 3, 30, 15);
        long fixedTime = date.getTime();

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        Date dateWithCurrentYear = new Date(currentYear - 1900, 0, 19, 3, 30, 15);
        long timeWithCurrentYear = dateWithCurrentYear.getTime();

        assertEquals("Saturday, January 24, 2009", DateUtils.formatSameDayTime(
                fixedTime + DAY_DURATION, fixedTime, java.text.DateFormat.FULL,
                java.text.DateFormat.FULL).toString());
        assertEquals("Jan 24, 2009", DateUtils.formatSameDayTime(fixedTime + DAY_DURATION,
                fixedTime, java.text.DateFormat.DEFAULT, java.text.DateFormat.FULL).toString());
        assertEquals("January 24, 2009", DateUtils.formatSameDayTime(fixedTime + DAY_DURATION,
                fixedTime, java.text.DateFormat.LONG, java.text.DateFormat.FULL).toString());
        assertEquals("Jan 24, 2009", DateUtils.formatSameDayTime(fixedTime + DAY_DURATION,
                fixedTime, java.text.DateFormat.MEDIUM, java.text.DateFormat.FULL).toString());
        assertEquals("1/24/09", DateUtils.formatSameDayTime(fixedTime + DAY_DURATION,
                fixedTime, java.text.DateFormat.SHORT, java.text.DateFormat.FULL).toString());

        assertEquals("5:30:15 AM GMT", DateUtils.formatSameDayTime(fixedTime + HOUR_DURATION,
                fixedTime, java.text.DateFormat.FULL, java.text.DateFormat.FULL).toString());
        assertEquals("5:30:15 AM", DateUtils.formatSameDayTime(fixedTime + HOUR_DURATION,
                fixedTime, java.text.DateFormat.FULL, java.text.DateFormat.DEFAULT).toString());
        assertEquals("5:30:15 AM GMT", DateUtils.formatSameDayTime(fixedTime + HOUR_DURATION,
                fixedTime, java.text.DateFormat.FULL, java.text.DateFormat.LONG).toString());
        assertEquals("5:30:15 AM", DateUtils.formatSameDayTime(fixedTime + HOUR_DURATION,
                fixedTime, java.text.DateFormat.FULL, java.text.DateFormat.MEDIUM).toString());
        assertEquals("5:30 AM", DateUtils.formatSameDayTime(fixedTime + HOUR_DURATION,
                fixedTime, java.text.DateFormat.FULL, java.text.DateFormat.SHORT).toString());

        long noonDuration = (8 * 60 + 30) * 60 * 1000 - 15 * 1000;
        long midnightDuration = (3 * 60 + 30) * 60 * 1000 + 15 * 1000;
        long integralDuration = 30 * 60 * 1000 + 15 * 1000;
        assertEquals("Monday", DateUtils.formatDateRange(mContext, fixedTime, fixedTime
                + HOUR_DURATION, DateUtils.FORMAT_SHOW_WEEKDAY));
        assertEquals("January 19", DateUtils.formatDateRange(mContext, timeWithCurrentYear,
                timeWithCurrentYear + HOUR_DURATION, DateUtils.FORMAT_SHOW_DATE));
        assertEquals("3:30AM", DateUtils.formatDateRange(mContext, fixedTime, fixedTime,
                DateUtils.FORMAT_SHOW_TIME));
        assertEquals("January 19, 2009", DateUtils.formatDateRange(mContext, fixedTime,
                fixedTime + HOUR_DURATION, DateUtils.FORMAT_SHOW_YEAR));
        assertEquals("January 19", DateUtils.formatDateRange(mContext, timeWithCurrentYear,
                timeWithCurrentYear + HOUR_DURATION, DateUtils.FORMAT_NO_YEAR));
        assertEquals("January", DateUtils.formatDateRange(mContext, timeWithCurrentYear,
                timeWithCurrentYear + HOUR_DURATION, DateUtils.FORMAT_NO_MONTH_DAY));
        assertEquals("3:30AM", DateUtils.formatDateRange(mContext, fixedTime, fixedTime,
                DateUtils.FORMAT_12HOUR | DateUtils.FORMAT_SHOW_TIME));
        assertEquals("03:30", DateUtils.formatDateRange(mContext, fixedTime, fixedTime,
                DateUtils.FORMAT_24HOUR | DateUtils.FORMAT_SHOW_TIME));
        assertEquals("3:30AM", DateUtils.formatDateRange(mContext, fixedTime, fixedTime,
                DateUtils.FORMAT_12HOUR | DateUtils.FORMAT_CAP_AMPM | DateUtils.FORMAT_SHOW_TIME));
        assertEquals("noon", DateUtils.formatDateRange(mContext, fixedTime + noonDuration,
                fixedTime + noonDuration, DateUtils.FORMAT_12HOUR | DateUtils.FORMAT_SHOW_TIME));
        assertEquals("Noon", DateUtils.formatDateRange(mContext, fixedTime + noonDuration,
                fixedTime + noonDuration,
                DateUtils.FORMAT_12HOUR | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_CAP_NOON));
        assertEquals("12:00PM", DateUtils.formatDateRange(mContext, fixedTime + noonDuration,
                fixedTime + noonDuration,
                DateUtils.FORMAT_12HOUR | DateUtils.FORMAT_NO_NOON | DateUtils.FORMAT_SHOW_TIME));
        assertEquals("12:00AM", DateUtils.formatDateRange(mContext, fixedTime - midnightDuration,
                fixedTime - midnightDuration,
                DateUtils.FORMAT_12HOUR | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_NO_MIDNIGHT));
        assertEquals("3:30AM", DateUtils.formatDateRange(mContext, fixedTime, fixedTime,
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_UTC));
        assertEquals("3am", DateUtils.formatDateRange(mContext, fixedTime - integralDuration,
                fixedTime - integralDuration,
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_TIME));
        assertEquals("Mon", DateUtils.formatDateRange(mContext, fixedTime,
                fixedTime + HOUR_DURATION,
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY));
        assertEquals("Jan 19", DateUtils.formatDateRange(mContext, timeWithCurrentYear,
                timeWithCurrentYear + HOUR_DURATION,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH));
        assertEquals("Jan 19", DateUtils.formatDateRange(mContext, timeWithCurrentYear,
                timeWithCurrentYear + HOUR_DURATION,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL));
        String actual = DateUtils.formatDateRange(mContext, fixedTime,
                fixedTime + HOUR_DURATION,
                DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE);
        // accept with leading zero or without
        assertTrue("1/19/2009".equals(actual) || "01/19/2009".equals(actual));
    }

    public void testIsToday() {
        assertTrue(DateUtils.isToday(mBaseTime));
        assertFalse(DateUtils.isToday(mBaseTime - DAY_DURATION));
    }

    /**
     * DateUtils is broken beyond Integer.MAX_VALUE seconds of 1970.
     * http://code.google.com/p/android/issues/detail?id=13050
     */
    public void test2038() {
        assertEquals("00:00, Thursday, January 1, 1970", formatFull(0L));
    }

    private String formatFull(long millis) {
        Formatter formatter = new Formatter();
        int flags = DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_WEEKDAY
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_24HOUR;
        DateUtils.formatDateRange(null, formatter, millis, millis, flags, "UTC");
        return formatter.toString();
    }
}
