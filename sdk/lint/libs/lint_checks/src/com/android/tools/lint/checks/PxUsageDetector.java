/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_TEXT_SIZE;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_STYLE;
import static com.android.SdkConstants.UNIT_DIP;
import static com.android.SdkConstants.UNIT_DP;
import static com.android.SdkConstants.UNIT_PX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.Collections;

/**
 * Check for px dimensions instead of dp dimensions.
 * Also look for non-"sp" text sizes.
 */
public class PxUsageDetector extends LayoutDetector {
    /** The main issue discovered by this detector */
    public static final Issue PX_ISSUE = Issue.create(
            "PxUsage", //$NON-NLS-1$
            "Looks for use of the \"px\" dimension",
            // This description is from the below screen support document
            "For performance reasons and to keep the code simpler, the Android system uses pixels " +
            "as the standard unit for expressing dimension or coordinate values. That means that " +
            "the dimensions of a view are always expressed in the code using pixels, but " +
            "always based on the current screen density. For instance, if `myView.getWidth()` " +
            "returns 10, the view is 10 pixels wide on the current screen, but on a device with " +
            "a higher density screen, the value returned might be 15. If you use pixel values " +
            "in your application code to work with bitmaps that are not pre-scaled for the " +
            "current screen density, you might need to scale the pixel values that you use in " +
            "your code to match the un-scaled bitmap source.",
            Category.CORRECTNESS,
            2,
            Severity.WARNING,
            PxUsageDetector.class,
            Scope.RESOURCE_FILE_SCOPE).setMoreInfo(
            "http://developer.android.com/guide/practices/screens_support.html#screen-independence"); //$NON-NLS-1$

    /** The main issue discovered by this detector */
    public static final Issue DP_ISSUE = Issue.create(
            "SpUsage", //$NON-NLS-1$
            "Looks for uses of \"dp\" instead of \"sp\" dimensions for text sizes",

            "When setting text sizes, you should normally use `sp`, or \"scale-independent " +
            "pixels\". This is like the `dp` unit, but it is also scaled " +
            "by the user's font size preference. It is recommend you use this unit when " +
            "specifying font sizes, so they will be adjusted for both the screen density " +
            "and the user's preference.\n" +
            "\n" +
            "There *are* cases where you might need to use `dp`; typically this happens when " +
            "the text is in a container with a specific dp-size. This will prevent the text " +
            "from spilling outside the container. Note however that this means that the user's " +
            "font size settings are not respected, so consider adjusting the layout itself " +
            "to be more flexible.",
            Category.CORRECTNESS,
            2,
            Severity.WARNING,
            PxUsageDetector.class,
            Scope.RESOURCE_FILE_SCOPE).setMoreInfo(
            "http://developer.android.com/training/multiscreen/screendensities.html"); //$NON-NLS-1$

    /** Constructs a new {@link PxUsageDetector} */
    public PxUsageDetector() {
    }

    @Override
    public @NonNull Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        // Look in both layouts (at attribute values) and in value files (at style definitions)
        return folderType == ResourceFolderType.LAYOUT || folderType == ResourceFolderType.VALUES;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return ALL;
    }

    @Override
    @Nullable
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(TAG_STYLE);
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        if (context.getResourceFolderType() != ResourceFolderType.LAYOUT) {
            return;
        }

        String value = attribute.getValue();
        if (value.endsWith(UNIT_PX) && value.matches("\\d+px")) { //$NON-NLS-1$
            if (value.charAt(0) == '0') {
                // 0px is fine. 0px is 0dp regardless of density...
                return;
            }
            if (context.isEnabled(PX_ISSUE)) {
                context.report(PX_ISSUE, attribute, context.getLocation(attribute),
                    "Avoid using \"px\" as units; use \"dp\" instead", null);
            }
        } else if (ATTR_TEXT_SIZE.equals(attribute.getLocalName())
                && (value.endsWith(UNIT_DP) || value.endsWith(UNIT_DIP))
                && (value.matches("\\d+di?p"))) {
            if (context.isEnabled(DP_ISSUE)) {
                context.report(DP_ISSUE, attribute, context.getLocation(attribute),
                    "Should use \"sp\" instead of \"dp\" for text sizes", null);
            }
        }
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (context.getResourceFolderType() != ResourceFolderType.VALUES) {
            return;
        }

        assert element.getTagName().equals(TAG_STYLE);
        NodeList itemNodes = element.getChildNodes();
        for (int j = 0, nodeCount = itemNodes.getLength(); j < nodeCount; j++) {
            Node item = itemNodes.item(j);
            if (item.getNodeType() == Node.ELEMENT_NODE &&
                    TAG_ITEM.equals(item.getNodeName())) {
                Element itemElement = (Element) item;
                NodeList childNodes = item.getChildNodes();
                for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                    Node child = childNodes.item(i);
                    if (child.getNodeType() != Node.TEXT_NODE) {
                        return;
                    }

                    checkStyleItem(context, itemElement, child);
                }
            }
        }
    }

    private void checkStyleItem(XmlContext context, Element item, Node textNode) {
        String text = textNode.getNodeValue();
        for (int j = text.length() - 1; j > 0; j--) {
            char c = text.charAt(j);
            if (!Character.isWhitespace(c)) {
                if (c == 'x' && text.charAt(j - 1) == 'p') { // ends with px
                    text = text.trim();
                    if (text.matches("\\d+px")) { //$NON-NLS-1$
                        if (context.isEnabled(PX_ISSUE)) {
                            context.report(PX_ISSUE, item, context.getLocation(textNode),
                                "Avoid using \"px\" as units; use \"dp\" instead", null);
                        }
                    }
                } else if (c == 'p' && (text.charAt(j - 1) == 'd'
                        || text.charAt(j - 1) == 'i')) { // ends with dp or di
                    text = text.trim();
                    String name = item.getAttribute(ATTR_NAME);
                    if ((name.equals(ATTR_TEXT_SIZE)
                            || name.equals("android:textSize"))  //$NON-NLS-1$
                            && text.matches("\\d+di?p")) {  //$NON-NLS-1$
                        if (context.isEnabled(DP_ISSUE)) {
                            context.report(DP_ISSUE, item, context.getLocation(textNode),
                                "Should use \"sp\" instead of \"dp\" for text sizes", null);
                        }
                    }
                }
                break;
            }
        }
    }
}
