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
package com.motorolamobility.preflighting.checkers.layout;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.w3c.dom.Node;

/**
 * Bean class representing all ids in a given layout found.
 * This class can contain a list of Ids and/or a Map<Node,String> where
 * the key is the XML Node declaring the id and the value is the id itself.  
 */
class LayoutFileId
{
    private File file;

    private HashSet<String> idsList;

    private Map<Node, String> nodeIdMap;

    public LayoutFileId(File file)
    {
        this.file = file;
        this.idsList = new HashSet<String>();
        this.nodeIdMap = new HashMap<Node, String>();
    }

    public LayoutFileId(File file, HashSet<String> idsList)
    {
        this.file = file;
        this.idsList = idsList;
        this.nodeIdMap = new HashMap<Node, String>();
    }

    public LayoutFileId(File file, Map<Node, String> nodeMap)
    {
        this.file = file;
        this.idsList = (HashSet<String>) nodeMap.values();
        this.nodeIdMap = nodeMap;
    }

    public File getLayoutFile()
    {
        return file;
    }

    public HashSet<String> getIdsList()
    {
        return idsList;
    }

    public Map<Node, String> getNodeIdMap()
    {
        return nodeIdMap;
    }

    public void addId(Node node, String id)
    {
        addId(id);
        nodeIdMap.put(node, id);
    }

    public void addId(String id)
    {
        idsList.add(id);
    }

}