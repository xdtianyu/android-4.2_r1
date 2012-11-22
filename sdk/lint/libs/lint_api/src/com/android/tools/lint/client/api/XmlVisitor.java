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

package com.android.tools.lint.client.api;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.XmlScanner;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.annotations.Beta;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

/**
 * Specialized visitor for running detectors on an XML document.
 * It operates in two phases:
 * <ol>
 *   <li> First, it computes a set of maps where it generates a map from each
 *        significant element name, and each significant attribute name, to a list
 *        of detectors to consult for that element or attribute name.
 *        The set of element names or attribute names (or both) that a detector
 *        is interested in is provided by the detectors themselves.
 *   <li> Second, it iterates over the document a single time. For each element and
 *        attribute it looks up the list of interested detectors, and runs them.
 * </ol>
 * It also notifies all the detectors before and after the document is processed
 * such that they can do pre- and post-processing.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
class XmlVisitor {
    private final Map<String, List<Detector.XmlScanner>> mElementToCheck =
            new HashMap<String, List<Detector.XmlScanner>>();
    private final Map<String, List<Detector.XmlScanner>> mAttributeToCheck =
            new HashMap<String, List<Detector.XmlScanner>>();
    private final List<Detector.XmlScanner> mDocumentDetectors =
            new ArrayList<Detector.XmlScanner>();
    private final List<Detector.XmlScanner> mAllElementDetectors =
            new ArrayList<Detector.XmlScanner>();
    private final List<Detector.XmlScanner> mAllAttributeDetectors =
            new ArrayList<Detector.XmlScanner>();
    private final List<? extends Detector> mAllDetectors;
    private final IDomParser mParser;

    // Really want this:
    //<T extends List<Detector> & Detector.XmlScanner> XmlVisitor(IDomParser parser,
    //    T xmlDetectors) {
    // but it makes client code tricky and ugly.
    XmlVisitor(@NonNull IDomParser parser, @NonNull List<? extends Detector> xmlDetectors) {
        mParser = parser;
        mAllDetectors = xmlDetectors;

        // TODO: Check appliesTo() for files, and find a quick way to enable/disable
        // rules when running through a full project!
        for (Detector detector : xmlDetectors) {
            Detector.XmlScanner xmlDetector = (XmlScanner) detector;
            Collection<String> attributes = xmlDetector.getApplicableAttributes();
            if (attributes == XmlScanner.ALL) {
                mAllAttributeDetectors.add(xmlDetector);
            }  else if (attributes != null) {
                for (String attribute : attributes) {
                    List<Detector.XmlScanner> list = mAttributeToCheck.get(attribute);
                    if (list == null) {
                        list = new ArrayList<Detector.XmlScanner>();
                        mAttributeToCheck.put(attribute, list);
                    }
                    list.add(xmlDetector);
                }
            }
            Collection<String> elements = xmlDetector.getApplicableElements();
            if (elements == XmlScanner.ALL) {
                mAllElementDetectors.add(xmlDetector);
            } else if (elements != null) {
                for (String element : elements) {
                    List<Detector.XmlScanner> list = mElementToCheck.get(element);
                    if (list == null) {
                        list = new ArrayList<Detector.XmlScanner>();
                        mElementToCheck.put(element, list);
                    }
                    list.add(xmlDetector);
                }
            }

            if ((attributes == null || (attributes.size() == 0
                    && attributes != XmlScanner.ALL))
                  && (elements == null || (elements.size() == 0
                  && elements != XmlScanner.ALL))) {
                mDocumentDetectors.add(xmlDetector);
            }
        }
    }

    void visitFile(@NonNull XmlContext context, @NonNull File file) {
        assert LintUtils.isXmlFile(file);
        context.parser = mParser;

        try {
            if (context.document == null) {
                context.document = mParser.parseXml(context);
                if (context.document == null) {
                    // No need to log this; the parser should be reporting
                    // a full warning (such as IssueRegistry#PARSER_ERROR)
                    // with details, location, etc.
                    return;
                }
                if (context.document.getDocumentElement() == null) {
                    // Ignore empty documents
                    return;
                }
            }

            for (Detector check : mAllDetectors) {
                check.beforeCheckFile(context);
            }

            for (Detector.XmlScanner check : mDocumentDetectors) {
                check.visitDocument(context, context.document);
            }

            if (mElementToCheck.size() > 0 || mAttributeToCheck.size() > 0
                    || mAllAttributeDetectors.size() > 0 || mAllElementDetectors.size() > 0) {
                visitElement(context, context.document.getDocumentElement());
            }

            for (Detector check : mAllDetectors) {
                check.afterCheckFile(context);
            }
        } finally {
            if (context.document != null) {
                mParser.dispose(context, context.document);
                context.document = null;
            }
        }
    }

    private void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        List<Detector.XmlScanner> elementChecks = mElementToCheck.get(element.getTagName());
        if (elementChecks != null) {
            assert elementChecks instanceof RandomAccess;
            for (int i = 0, n = elementChecks.size(); i < n; i++) {
                Detector.XmlScanner check = elementChecks.get(i);
                check.visitElement(context, element);
            }
        }
        if (mAllElementDetectors.size() > 0) {
            for (int i = 0, n = mAllElementDetectors.size(); i < n; i++) {
                Detector.XmlScanner check = mAllElementDetectors.get(i);
                check.visitElement(context, element);
            }
        }

        if (mAttributeToCheck.size() > 0 || mAllAttributeDetectors.size() > 0) {
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0, n = attributes.getLength(); i < n; i++) {
                Attr attribute = (Attr) attributes.item(i);
                String name = attribute.getLocalName();
                if (name == null) {
                    name = attribute.getName();
                }
                List<Detector.XmlScanner> list = mAttributeToCheck.get(name);
                if (list != null) {
                    for (int j = 0, max = list.size(); j < max; j++) {
                        Detector.XmlScanner check = list.get(j);
                        check.visitAttribute(context, attribute);
                    }
                }
                if (mAllAttributeDetectors.size() > 0) {
                    for (int j = 0, max = mAllAttributeDetectors.size(); j < max; j++) {
                        Detector.XmlScanner check = mAllAttributeDetectors.get(j);
                        check.visitAttribute(context, attribute);
                    }
                }
            }
        }

        // Visit children
        NodeList childNodes = element.getChildNodes();
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                visitElement(context, (Element) child);
            }
        }

        // Post hooks
        if (elementChecks != null) {
            for (int i = 0, n = elementChecks.size(); i < n; i++) {
                Detector.XmlScanner check = elementChecks.get(i);
                check.visitElementAfter(context, element);
            }
        }
        if (mAllElementDetectors.size() > 0) {
            for (int i = 0, n = mAllElementDetectors.size(); i < n; i++) {
                Detector.XmlScanner check = mAllElementDetectors.get(i);
                check.visitElementAfter(context, element);
            }
        }
    }
}
