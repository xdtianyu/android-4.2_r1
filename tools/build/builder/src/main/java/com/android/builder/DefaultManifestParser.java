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

package com.android.builder;

import com.android.xml.AndroidXPathFactory;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class DefaultManifestParser implements ManifestParser {

    @Override
    public String getPackage(File manifestFile) {
        XPath xpath = AndroidXPathFactory.newXPath();

        try {
            return xpath.evaluate("/manifest/@package",
                    new InputSource(new FileInputStream(manifestFile)));
        } catch (XPathExpressionException e) {
            // won't happen.
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
