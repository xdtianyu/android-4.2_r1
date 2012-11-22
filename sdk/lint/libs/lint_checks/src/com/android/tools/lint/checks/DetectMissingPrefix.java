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

import static com.android.SdkConstants.ANDROID_PKG_PREFIX;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_LAYOUT;
import static com.android.SdkConstants.ATTR_STYLE;
import static com.android.SdkConstants.VIEW_TAG;
import static com.android.SdkConstants.XMLNS_PREFIX;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Detects layout attributes on builtin Android widgets that do not specify
 * a prefix but probably should.
 */
public class DetectMissingPrefix extends LayoutDetector {

    /** Attributes missing the android: prefix */
    public static final Issue MISSING_NAMESPACE = Issue.create(
            "MissingPrefix", //$NON-NLS-1$
            "Detect XML attributes not using the Android namespace",
            "Most Android views have attributes in the Android namespace. When referencing " +
            "these attributes you *must* include the namespace prefix, or your attribute will " +
            "be interpreted by aapt as just a custom attribute.",

            Category.CORRECTNESS,
            8,
            Severity.WARNING,
            DetectMissingPrefix.class,
            Scope.RESOURCE_FILE_SCOPE);

    private static final Set<String> NO_PREFIX_ATTRS = new HashSet<String>();
    static {
        NO_PREFIX_ATTRS.add(ATTR_CLASS);
        NO_PREFIX_ATTRS.add(ATTR_STYLE);
        NO_PREFIX_ATTRS.add(ATTR_LAYOUT);
    }

    /** Constructs a new {@link DetectMissingPrefix} */
    public DetectMissingPrefix() {
    }

    @Override
    public @NonNull Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return ALL;
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        String uri = attribute.getNamespaceURI();
        if (uri == null || uri.length() == 0) {
            String name = attribute.getName();
            if (name == null) {
                return;
            }
            if (NO_PREFIX_ATTRS.contains(name)) {
                return;
            }

            Element element = attribute.getOwnerElement();
            if (isCustomView(element)) {
                return;
            }

            if (name.startsWith(XMLNS_PREFIX)) {
                return;
            }

            context.report(MISSING_NAMESPACE, attribute,
                    context.getLocation(attribute),
                    "Attribute is missing the Android namespace prefix",
                    null);
        }
    }

    private static boolean isCustomView(Element element) {
        // If this is a custom view, the usage of custom attributes can be legitimate
        String tag = element.getTagName();
        if (tag.equals(VIEW_TAG)) {
            // <view class="my.custom.view" ...>
            return true;
        }

        return tag.indexOf('.') != -1 && !tag.startsWith(ANDROID_PKG_PREFIX);
    }
}
