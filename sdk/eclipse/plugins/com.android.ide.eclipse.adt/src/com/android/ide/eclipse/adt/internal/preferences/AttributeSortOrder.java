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
package com.android.ide.eclipse.adt.internal.preferences;

import static com.android.SdkConstants.XMLNS;

import com.android.ide.eclipse.adt.internal.editors.uimodel.UiAttributeNode;

import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.w3c.dom.Attr;

import java.util.Comparator;

/** Order to use when sorting attributes */
@SuppressWarnings("restriction") // IndexedRegion
public enum AttributeSortOrder {
    NO_SORTING("none"),     //$NON-NLS-1$
    ALPHABETICAL("alpha"),  //$NON-NLS-1$
    LOGICAL("logical");     //$NON-NLS-1$

    AttributeSortOrder(String key) {
        this.key = key;
    }

    public final String key;

    /**
     * @return a comparator for use by this attribute sort order
     */
    public Comparator<Attr> getAttributeComparator() {
        switch (this) {
            case ALPHABETICAL:
                return ALPHABETICAL_COMPARATOR;
            case NO_SORTING:
                return EXISTING_ORDER_COMPARATOR;
            case LOGICAL:
            default:
                return SORTED_ORDER_COMPARATOR;
        }
    }

    /** Comparator which can be used to sort attributes in the coding style priority order */
    private static final Comparator<Attr> SORTED_ORDER_COMPARATOR = new Comparator<Attr>() {
        @Override
        public int compare(Attr attr1, Attr attr2) {
            // Namespace declarations should always go first
            if (XMLNS.equals(attr1.getPrefix())) {
                if (XMLNS.equals(attr2.getPrefix())) {
                    return 0;
                }
                return -1;
            } else if (XMLNS.equals(attr2.getPrefix())) {
                return 1;
            }

            // Sort by preferred attribute order
            return UiAttributeNode.compareAttributes(
                    attr1.getPrefix(), attr1.getLocalName(),
                    attr2.getPrefix(), attr2.getLocalName());
        }
    };

    /**
     * Comparator which can be used to "sort" attributes into their existing source order
     * (which is not the same as the node map iteration order in the DOM model)
     */
    private static final Comparator<Attr> EXISTING_ORDER_COMPARATOR = new Comparator<Attr>() {
        @Override
        public int compare(Attr attr1, Attr attr2) {
            IndexedRegion region1 = (IndexedRegion) attr1;
            IndexedRegion region2 = (IndexedRegion) attr2;

            return region1.getStartOffset() - region2.getStartOffset();
        }
    };

    /**
     * Comparator which can be used to sort attributes into alphabetical order (but xmlns
     * is always first)
     */
    private static final Comparator<Attr> ALPHABETICAL_COMPARATOR = new Comparator<Attr>() {
        @Override
        public int compare(Attr attr1, Attr attr2) {
            // Namespace declarations should always go first
            if (XMLNS.equals(attr1.getPrefix())) {
                if (XMLNS.equals(attr2.getPrefix())) {
                    return 0;
                }
                return -1;
            } else if (XMLNS.equals(attr2.getPrefix())) {
                return 1;
            }

            // Sort by name rather than localname to ensure we sort by namespaces first,
            // then by names.
            return attr1.getName().compareTo(attr2.getName());
        }
    };
}