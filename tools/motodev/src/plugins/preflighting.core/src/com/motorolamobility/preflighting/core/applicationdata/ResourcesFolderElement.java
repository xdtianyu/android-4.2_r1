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
package com.motorolamobility.preflighting.core.applicationdata;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/***
 * This class is intended to be used to represent the resources folder (\res) of an application
 */
public class ResourcesFolderElement extends FolderElement
{
    private static final String VALUES = "values";

    private static final String STRINGS_XML = "strings.xml";

    /**
     * Represents the Android's Project "drawable" folder.
     */
    public static final String DRAWABLE_FOLDER = "drawable";

    /**
     * Represents the Android's Project "drawable-ldpi" folder.
     */
    public static final String LDPI_DRAWABLE_FOLDER = "drawable-ldpi";

    /**
     * Represents the Android's Project "drawable-mdpi" folder.
     */
    public static final String MDPI_DRAWABLE_FOLDER = "drawable-mdpi";

    /**
     * Represents the Android's Project "drawable-hdpi" folder.
     */
    public static final String HDPI_DRAWABLE_FOLDER = "drawable-hdpi";

    /**
     * Represents the Android's Project "drawable-xhdpi" folder.
     */
    public static final String XHDPI_DRAWABLE_FOLDER = "drawable-xhdpi";

    private List<Locale> localeList = null;

    /***
     * Class constructor that always call superclass constructor using type {@link Element.Type#FOLDER_RES}.
     *      
     * @param name Folder File representing the folder.
     * @param parent Parent element.
     */
    public ResourcesFolderElement(File folder, Element parent)
    {
        super(folder, parent, Type.FOLDER_RES);
    }

    /* Drawable methods */
    /***
     * Gets all drawable folders found in a a application.
     * 
     * @return A list of drawable folders.
     */
    public List<FolderElement> getDrawableFolders()
    {
        List<FolderElement> list = new ArrayList<FolderElement>();
        for (Element element : this.getChildren())
        {
            if (element.getType() == Element.Type.FOLDER_DRAWABLE)
            {
                if (element instanceof FolderElement)
                {
                    list.add((FolderElement) element);
                }
            }
        }
        return list;
    }

    /**
     * Gets the standard drawable folder
     * 
     * @return Retruns the folder element that represents the standard drawable folder.
     */
    public FolderElement getDrawableFolder()
    {
        return getDrawableFolder(ResourcesFolderElement.DRAWABLE_FOLDER);
    }

    /***
     * Gets the low DPI drawable folder.
     * 
     * @return Retruns the folder element that represents the low DPI drawable folder. Can be null.
     */
    public FolderElement getLdpiDrawableFolder()
    {
        return getDrawableFolder(ResourcesFolderElement.LDPI_DRAWABLE_FOLDER);
    }

    /***
     * Gets the medium DPI drawable folder.
     * 
     * @return Returns the folder element that represents the medium DPI drawable folder. Can be null.
     */
    public FolderElement getMdpiDrawableFolder()
    {
        return getDrawableFolder(ResourcesFolderElement.MDPI_DRAWABLE_FOLDER);
    }

    /***
     * Gets the high DPI drawable folder.
     * 
     * @return Returns the folder element that represents the high DPI drawable folder. Can be null.
     */
    public FolderElement getHdpiDrawableFolder()
    {
        return getDrawableFolder(ResourcesFolderElement.HDPI_DRAWABLE_FOLDER);
    }

    /***
     * Gets the extra high DPI drawable folder.
     * 
     * @return Returns the folder element that represents the extra high DPI drawable folder. Can be null.
     */
    public FolderElement getXhdpiDrawableFolder()
    {
        return getDrawableFolder(ResourcesFolderElement.XHDPI_DRAWABLE_FOLDER);
    }

    /***
     * Gets an drawable folder.
     * 
     * @param folderName Foler name which represents a drawable one.
     * 
     * @return Returns the folder element.
     */
    private FolderElement getDrawableFolder(String folderName)
    {
        FolderElement result = null;
        for (FolderElement element : getDrawableFolders())
        {
            if (element.getName().equals(folderName))
            {
                result = element;
                break;
            }
        }
        return result;
    }

    /*Strings methods*/
    /**
     * Gets all values folders elements.
     * 
     * @return Returns the folder element list.
     */
    public List<FolderElement> getValuesFolders()
    {
        List<FolderElement> result = new ArrayList<FolderElement>();
        for (Element element : getChildren())
        {
            if (element.getType().equals(Element.Type.FOLDER_VALUES))
            {
                if (element instanceof FolderElement)
                {
                    result.add((FolderElement) element);
                }
            }
        }
        return result;
    }

    /**
     * Gets all available Locales.
     * 
     * @return Returns all available locales.
     */
    public List<Locale> getAvailableLocales()
    {
        if (localeList == null)
        {
            localeList = new ArrayList<Locale>();
            Locale locale;
            for (FolderElement folder : getValuesFolders())
            {
                locale = getLocaleFromFolderName(folder.getName());
                if ((locale != null) && !localeList.contains(locale))
                {
                    localeList.add(locale);
                }
            }
        }
        return localeList;
    }

    /**
     * Merge a list of string elements in a single one.
     * 
     * @return Returns the merged String Elements.
     */
    private StringsElement mergeStringsElements(List<StringsElement> list)
    {
        StringsElement stringsUnion = null;
        if (list.size() > 0)
        {
            stringsUnion = new StringsElement(STRINGS_XML, null);

            for (StringsElement element : list)
            {
                for (String key : element.getKeyList())
                {
                    Object value = element.getValue(key);

                    boolean valid = value != null;
                    if (valid && (value instanceof String))
                    {
                        valid = ((String) value).length() > 0;
                    }
                    else if (valid && (value instanceof List))
                    {
                        valid = ((List) value).size() > 0;
                    }

                    if (!stringsUnion.containsKey(key) || (stringsUnion.containsKey(key) && !valid))
                    {
                        stringsUnion.addEntry(key, element.getValue(key));
                    }
                }
            }
        }
        return stringsUnion;
    }

    /**
     * Gets the Strings Element for a a specific Locale.
     * 
     * @param locale Specific {@link Locale}.
     * 
     * @return Returns the specific Strings element.
     */
    public StringsElement getValuesElement(Locale locale)
    {
        List<StringsElement> stringsElementsToMerge = new ArrayList<StringsElement>();

        Locale localefromFolder;
        for (FolderElement folder : getValuesFolders())
        {
            localefromFolder = getLocaleFromFolderName(folder.getName());

            //Searching for default values
            if (locale == null)
            {
                //Found default values
                if (localefromFolder == null)
                {
                    for (Element stringsElement : folder.getChildren())
                    {
                        if ((stringsElement.getType() == Element.Type.FILE_STRINGS))
                        {
                            stringsElementsToMerge.add((StringsElement) stringsElement);
                        }
                    }
                }
            }
            //Searching for specific language
            else
            {
                //Ignoring default values
                if (localefromFolder != null)
                {
                    if (localefromFolder.getLanguage().equals(locale.getLanguage())
                            && localefromFolder.getCountry().equals(locale.getCountry()))
                    {
                        for (Element stringsElement : folder.getChildren())
                        {
                            if ((stringsElement.getType() == Element.Type.FILE_STRINGS))
                            {
                                stringsElementsToMerge.add((StringsElement) stringsElement);
                            }
                        }
                    }
                }
            }
        }
        return mergeStringsElements(stringsElementsToMerge);
    }

    /**
     * Gets the default values element.
     * 
     * @return Returns the default Strings set.
     */
    public StringsElement getDefaultValuesElement()
    {
        return getValuesElement(null);
    }

    /***
     * Extract a locale based on a folder name. 
     * 
     * @param folderName Folder name which the {@link Locale} will be based on.
     * 
     * @return Returns the extracted {@link Locale}. Can be null.
     */
    private static Locale getLocaleFromFolderName(String folderName)
    {
        Locale result = null;
        folderName = folderName.replace(VALUES, "");
        String[] segments = folderName.split("-");

        String language = null;
        String country = null;

        List<String> isoLanguages = new ArrayList<String>();
        for (String lang : Locale.getISOLanguages())
        {
            isoLanguages.add(lang);
        }

        List<String> isoContries = new ArrayList<String>();
        for (String lang : Locale.getISOCountries())
        {
            isoContries.add(lang);
        }

        for (int i = 1; i < segments.length; i++)
        {
            if ((segments[i].length() == 2) && (language == null)
                    && (isoLanguages.contains(segments[i])))
            {
                language = segments[i];
            }
            else if (segments[i].matches("r[A-Z]{2}") && (country == null)
                    && (isoContries.contains(segments[i].substring(1))))
            {
                // Ignore 'r' character
                country = segments[i].substring(1);
            }
        }

        if (language != null)
        {
            if (country != null)
            {
                result = new Locale(language, country);
            }
            else
            {
                result = new Locale(language);
            }
        }

        return result;
    }

    /**
     * Clean the {@link Locale} list.
     */
    @Override
    public void clean()
    {
        super.clean();
        this.localeList = null;
    }
}
