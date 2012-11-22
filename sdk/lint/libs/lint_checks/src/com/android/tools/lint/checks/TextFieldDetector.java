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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_HINT;
import static com.android.SdkConstants.ATTR_INPUT_METHOD;
import static com.android.SdkConstants.ATTR_INPUT_TYPE;
import static com.android.SdkConstants.EDIT_TEXT;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Collections;

/**
 * Checks for usability problems in text fields: omitting inputType, or omitting a hint.
 */
public class TextFieldDetector extends LayoutDetector {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "TextFields", //$NON-NLS-1$
            "Looks for text fields missing inputType or hint settings",
            "Providing an `inputType` attribute on a text field improves usability " +
            "because depending on the data to be input, optimized keyboards can be shown " +
            "to the user (such as just digits and parentheses for a phone number). Similarly," +
            "a hint attribute displays a hint to the user for what is expected in the " +
            "text field.\n" +
            "\n" +
            "If you really want to keep the text field generic, you can suppress this warning " +
            "by setting `inputType=\"text\"`.",

            Category.USABILITY,
            5,
            Severity.WARNING,
            TextFieldDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** Constructs a new {@link TextFieldDetector} */
    public TextFieldDetector() {
    }

    @Override
    public @NonNull Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(EDIT_TEXT);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (!element.hasAttributeNS(ANDROID_URI, ATTR_INPUT_TYPE) &&
                !element.hasAttributeNS(ANDROID_URI, ATTR_HINT)) {
            // Also make sure the EditText does not set an inputMethod in which case
            // an inputType might be provided from the input.
            if (element.hasAttributeNS(ANDROID_URI, ATTR_INPUT_METHOD)) {
                return;
            }

            context.report(ISSUE, element, context.getLocation(element),
                    "This text field does not specify an inputType or a hint", null);
        }
    }
}
