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

package com.android.anttargetprint;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class BuildXmlHandler extends DefaultHandler {

    private Map<String, String> mTargets = new HashMap<String, String>();
    private int mLevel = 0;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {

        mLevel++;

        if (mLevel == 2 && "target".equals(qName)) {
            String name = attributes.getValue("name");
            String depends = attributes.getValue("depends");

            if (name != null) {
                if (depends == null) {
                    depends = "";
                }

                mTargets.put(name, depends);
            }
        }

        super.startElement(uri, localName, qName, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        mLevel--;
        super.endElement(uri, localName, qName);
    }

    Map<String, List<String>> processTargets() {
        HashMap<String, List<String>> result = new HashMap<String, List<String>>();

        for (Entry<String, String> entry : mTargets.entrySet()) {
            process(entry.getKey(), entry.getValue(), result);
        }

        return result;
    }

    private List<String> process(String targetName, String targetDepends,
            Map<String, List<String>> resultMap) {

        // first check if this was already processed.
        List<String> resultList = resultMap.get(targetName);
        if (resultList != null) {
            return resultList;
        }

        resultList = new ArrayList<String>();

        if (targetDepends.length() > 0) {
            String[] dependencies = targetDepends.split(",");

            for (String dependency : dependencies) {
                String dependencyTrim = dependency.trim();
                // get all the dependencies for this targets.
                List<String> dependencyList = resultMap.get(dependencyTrim);
                if (dependencyList == null) {
                    dependencyList = process(dependencyTrim, mTargets.get(dependencyTrim),
                            resultMap);
                }

                // add those to the new result list
                resultList.addAll(dependencyList);

                // and add this dependency as well
                resultList.add(dependencyTrim);
            }
        }

        resultMap.put(targetName, resultList);

        return resultList;
    }

}
