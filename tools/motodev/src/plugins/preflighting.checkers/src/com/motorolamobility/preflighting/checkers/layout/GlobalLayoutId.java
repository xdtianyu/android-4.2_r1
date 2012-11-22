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

import java.util.HashMap;
import java.util.Set;

import org.w3c.dom.Node;

/**
 * This class represents the IDs of a given layout,
 * compiles ids from all configurations of a given layout.
 * It saves the node where the first occurrence was found.
 */
class GlobalLayoutId
{
    private HashMap<String, Node> idsMap;

    public GlobalLayoutId()
    {
        this.idsMap = new HashMap<String, Node>();
    }

    public void addID(String strID, Node node)
    {
        if (!idsMap.containsKey(strID))
        {
            idsMap.put(strID, node);
        }
    }

    /*
     * Returns the node where this ID was first defined.
     */
    public Node getNode(String strID)
    {
        return idsMap.get(strID);
    }

    /*
     * Returns the list of IDs defined in this layout.
     */
    public Set<String> getIdsList()
    {
        return idsMap.keySet();
    }
}