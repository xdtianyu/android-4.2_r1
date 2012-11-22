/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.editors.layout.configuration;

import com.google.common.collect.Sets;

import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class LocaleManagerTest extends TestCase {
    public void testIntegrity() {
        LocaleManager localeManager = LocaleManager.get();
        assertSame(localeManager, LocaleManager.get());

        Map<String, String> languageToCountry = LocaleManager.getLanguageToCountryMap();
        Map<String, String> languageNames = LocaleManager.getLanguageNamesMap();
        Map<String, String> regionNames = LocaleManager.getRegionNamesMap();
        assertEquals("Make sure to update initial capacity in declaration after editing map",
                177, languageToCountry.size());
        assertEquals("Make sure to update initial capacity in declaration after editing map",
                187, languageNames.size());
        assertEquals("Make sure to update initial capacity in declaration after editing map",
                249, regionNames.size());

        assertTrue(Sets.difference(languageToCountry.keySet(),
                languageNames.keySet()).isEmpty());
        for (Map.Entry<String, String> entry : languageToCountry.entrySet()) {
            assertTrue(entry.getValue(), entry.getKey().length() > 0);
            assertTrue(entry.getKey(), entry.getValue().length() > 0);
        }
        for (Map.Entry<String, String> entry : regionNames.entrySet()) {
            assertTrue(entry.getValue(), entry.getKey().length() > 0);
            assertTrue(entry.getKey(), entry.getValue().length() > 0);
        }
        for (Map.Entry<String, String> entry : languageNames.entrySet()) {
            assertTrue(entry.getValue(), entry.getKey().length() > 0);
            assertTrue(entry.getKey(), entry.getValue().length() > 0);
        }
        for (String s : languageToCountry.keySet()) {
            assertTrue(s, s.length() == 2 && s.equals(s.toLowerCase(Locale.US)));
        }
        for (String s : languageNames.keySet()) {
            assertTrue(s, s.length() == 2 && s.equals(s.toLowerCase(Locale.US)));
        }
        for (String s : languageNames.values()) {
            assertTrue(s, s.length() > 2 && Character.isUpperCase(s.charAt(0)));
        }
        for (String s : languageToCountry.values()) {
            assertTrue(s, s.length() == 2 && s.equals(s.toUpperCase(Locale.US)));
        }
        for (String s : regionNames.keySet()) {
            assertTrue(s, s.length() == 2 && s.equals(s.toUpperCase(Locale.US)));
        }
        for (String s : regionNames.values()) {
            assertTrue(s, s.length() > 2 && Character.isUpperCase(s.charAt(0)));
        }
        assertNull(languageToCountry.get(""));
        assertNull(languageNames.get(""));
        assertTrue(Sets.difference(languageToCountry.keySet(),
                languageNames.keySet()).isEmpty());
    }

    public void testGetLanguageNames() throws Exception {
        assertEquals("English", LocaleManager.getLanguageName("en"));
        assertEquals("Norwegian Bokmål", LocaleManager.getLanguageName("nb"));
        assertEquals("Norwegian", LocaleManager.getLanguageName("no"));
        assertEquals("French", LocaleManager.getLanguageName("fr"));
        assertEquals("German", LocaleManager.getLanguageName("de"));
        assertEquals("Hindi", LocaleManager.getLanguageName("hi"));
    }

    public void testGetFlagImage() {
        LocaleManager manager = LocaleManager.get();
        Image us = manager.getFlag("US");
        Image gb = manager.getFlag("GB");
        Image ca = manager.getFlag("CA");
        Image es = manager.getFlag("ES");
        Image br = manager.getFlag("BR");
        Image pt = manager.getFlag("PT");
        assertSame(us, manager.getFlag("en", "US"));
        assertSame(gb, manager.getFlag("en", "GB"));
        assertSame(ca, manager.getFlag("en", "CA"));
        Locale.setDefault(Locale.US);
        assertSame(us, manager.getFlag("en", null));
        Locale.setDefault(Locale.UK);
        assertSame(gb, manager.getFlag("en", null));
        Locale.setDefault(Locale.CANADA);
        assertSame(ca, manager.getFlag("en", null));
        assertSame(manager.getFlag("NO"), manager.getFlag("nb", null));
        assertSame(manager.getFlag("FR"), manager.getFlag("fr", null));

        Locale.setDefault(new Locale("pt", "br"));
        assertSame(br, manager.getFlag("pt", null));
        assertSame(pt, manager.getFlag("pt", "PT"));
        Locale.setDefault(new Locale("pt", "pt"));
        assertSame(pt, manager.getFlag("pt", null));
        assertSame(br, manager.getFlag("pt", "BR"));

        // Special cases where we have custom flags
        assertNotSame(gb, manager.getFlag("cy", null)); // Wales
        assertNotSame(es, manager.getFlag("ca", null)); // Catalonia

        // Aliases - http://developer.android.com/reference/java/util/Locale.html
        assertSame(manager.getFlag("yi", null), manager.getFlag("ji", null));
        assertSame(manager.getFlag("in", null), manager.getFlag("id", null));
        assertSame(manager.getFlag("iw", null), manager.getFlag("he", null));
        assertSame(LocaleManager.getLanguageName("iw"), LocaleManager.getLanguageName("he"));
        assertSame(LocaleManager.getLanguageName("in"), LocaleManager.getLanguageName("id"));
        assertSame(LocaleManager.getLanguageName("yi"), LocaleManager.getLanguageName("ji"));

        assertSame(us, manager.getFlagForFolderName("values-en-rUS"));
        assertSame(gb, manager.getFlagForFolderName("values-en-rGB"));
        Locale.setDefault(Locale.CANADA);
        assertSame(ca, manager.getFlagForFolderName("values-en"));
    }

    public void testGetRegionNames() {
        assertEquals("United States", LocaleManager.getRegionName("US"));
        assertEquals("Norway", LocaleManager.getRegionName("NO"));
        assertEquals("France", LocaleManager.getRegionName("FR"));
        assertEquals("India", LocaleManager.getRegionName("IN"));
    }

    public void testAvailableIcons() {
        // Icons we have in WindowBuilder
        String[] icons = new String[] {
                "ad", "ae", "af", "ag", "ai", "al", "am", "ao", "ar", "as", "at", "au", "aw", "ax",
                "az", "ba", "bb", "bd", "be", "bf", "bg", "bh", "bi", "bj", "bm", "bn", "bo", "br",
                "bs", "bt", "bv", "bw", "by", "bz", "ca", "cc", "cd", "cf", "cg", "ch", "ci", "ck",
                "cl", "cm", "cn", "co", "cr", "cu", "cv", "cx", "cy", "cz", "de", "dj", "dk", "dm",
                "do", "dz", "ec", "ee", "eg", "eh", "er", "es", "et", "fi", "fj", "fk", "fm", "fo",
                "fr", "ga", "gb", "gd", "ge", "gf", "gh", "gi", "gl", "gm", "gn", "gp", "gq", "gr",
                "gs", "gt", "gu", "gw", "gy", "hk", "hm", "hn", "hr", "ht", "hu", "id", "ie", "il",
                "in", "io", "iq", "ir", "is", "it", "jm", "jo", "jp", "ke", "kg", "kh", "ki", "km",
                "kn", "kp", "kr", "kw", "ky", "kz", "la", "lb", "lc", "li", "lk", "lr", "ls", "lt",
                "lu", "lv", "ly", "ma", "mc", "md", "me", "mg", "mh", "mk", "ml", "mm", "mn", "mo",
                "mp", "mq", "mr", "ms", "mt", "mu", "mv", "mw", "mx", "my", "mz", "na", "nc", "ne",
                "nf", "ng", "ni", "nl", "no", "np", "nr", "nu", "nz", "om", "pa", "pe", "pf", "pg",
                "ph", "pk", "pl", "pm", "pn", "pr", "ps", "pt", "pw", "py", "qa", "re", "ro", "rs",
                "ru", "rw", "sa", "sb", "sc", "sd", "se", "sg", "sh", "si", "sj", "sk", "sl", "sm",
                "sn", "so", "sr", "st", "sv", "sy", "sz", "tc", "td", "tf", "tg", "th", "tj", "tk",
                "tl", "tm", "tn", "to", "tr", "tt", "tv", "tw", "tz", "ua", "ug", "um", "us", "uy",
                "uz", "va", "vc", "ve", "vg", "vi", "vn", "vu", "wf", "ws", "ye", "yt", "za", "zm",
                "zw",
        };
        Set<String> sIcons = new HashSet<String>(100);
        Map<String, String> regionNames = LocaleManager.getRegionNamesMap();
        Map<String, String> languageToCountry = LocaleManager.getLanguageToCountryMap();
        Map<String, String> languageNames = LocaleManager.getLanguageNamesMap();
        List<String> unused = new ArrayList<String>();
        for (String code : icons) {
            code = code.toUpperCase(Locale.US);
            sIcons.add(code);

            String country = regionNames.get(code);
            if (country == null) {
                System.out.println("No region name found for region code " + code);
            }

            if (!languageToCountry.values().contains(code)) {
                unused.add(code.toLowerCase() + ".png");
            }
        }
        if (!unused.isEmpty()) {
            System.out.println("The following icons are not referenced by any of the " +
                    "language to country bindings: " + unused);
        }

        // Make sure all our language bindings are languages we have maps for
        for (Map.Entry<String, String> entry : languageToCountry.entrySet()) {
            String language = entry.getKey();
            String region = entry.getValue();

            if (!sIcons.contains(region)) {
                System.out.println("No icon found for region " + region + "  "
                        + LocaleManager.getRegionName(region) + " (used for language "
                        + language + "(" + languageNames.get(language) + "))");
            }
        }
    }
}
