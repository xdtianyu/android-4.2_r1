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

import com.android.SdkConstants;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;

import org.eclipse.core.runtime.IPath;

/**
 * Style to use when printing the XML. Different types of Android XML files use slightly
 * different preferred formats. For example, in layout files there is typically always a
 * newline between successive elements, whereas in a manifest file there is typically only
 * newlines between different types of elements. As another example, in resource files,
 * the format is typically much more compact: the text content of {@code <item>} tags is
 * included on the same line whereas for other layout styles the children are typically
 * placed on a line of their own.
 */
public enum XmlFormatStyle {
    /** Layout formatting style: blank lines between elements, attributes on separate lines */
    LAYOUT,

    /** Similar to layout formatting style, but no blank lines inside opening elements */
    FILE,

    /** Resource style: one line per complete element including text child content */
    RESOURCE,

    /**
     * Similar to layout style, but no newlines between related elements such as
     * successive {@code <uses-permission>} declarations, and no newlines inside
     * the second level elements (so an {@code <activity>} declaration appears as a
     * single block with no whitespace within it)
     */
    MANIFEST;

    /**
     * Returns the {@link XmlFormatStyle} to use for a resource of the given type
     *
     * @param resourceType the type of resource to be formatted
     * @return the suitable format style to use
     */
    public static XmlFormatStyle get(ResourceType resourceType) {
        switch (resourceType) {
            case ARRAY:
            case ATTR:
            case BOOL:
            case DECLARE_STYLEABLE:
            case DIMEN:
            case FRACTION:
            case ID:
            case INTEGER:
            case STRING:
            case PLURALS:
            case STYLE:
            case STYLEABLE:
            case COLOR:
                return RESOURCE;

            case LAYOUT:
                return LAYOUT;

            case DRAWABLE:
            case MENU:
            case ANIM:
            case ANIMATOR:
            case INTERPOLATOR:
            default:
                return FILE;
        }
    }

    /**
     * Returns the {@link XmlFormatStyle} to use for resource files in the given resource
     * folder
     *
     * @param folderType the type of folder containing the resource file
     * @return the suitable format style to use
     */
    public static XmlFormatStyle getForFolderType(ResourceFolderType folderType) {
        switch (folderType) {
            case LAYOUT:
                return LAYOUT;
            case COLOR:
            case VALUES:
                return RESOURCE;
            case ANIM:
            case ANIMATOR:
            case DRAWABLE:
            case INTERPOLATOR:
            case MENU:
            default:
                return FILE;
        }
    }

    /**
     * Returns the {@link XmlFormatStyle} to use for resource files of the given path.
     *
     * @param path the path to the resource file
     * @return the suitable format style to use
     */
    public static XmlFormatStyle getForFile(IPath path) {
        if (SdkConstants.FN_ANDROID_MANIFEST_XML.equals(path.lastSegment())) {
            return MANIFEST;
        }

        if (path.segmentCount() > 2) {
            String parentName = path.segment(path.segmentCount() - 2);
            ResourceFolderType folderType = ResourceFolderType.getFolderType(parentName);
            return getForFolderType(folderType);
        }

        return FILE;
    }
}