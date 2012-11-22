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
package com.motorolamobility.preflighting.core.applicationdata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/***
 * This class is intended to be used to represent strings.xml files under values folders in a Android Application
 */
public class StringsElement extends XMLElement
{
    private static final String RESOURCES_ELEMENT = "resources";

    private static final String STRING_ELEMENT = "string";

    private static final String STRING_ARRAY_ELEMENT = "string-array";

    private static final String STRING_NAME_ATTRIBUTE = "name";

    private Map<String, LocalizationValue> map;

    /**
     * Constructor which sets the minimum basic data.
     * 
     * @param name Name of the element.
     * @param parent String element's Parent.
     */
    public StringsElement(String name, Element parent)
    {
        super(name, parent, Element.Type.FILE_STRINGS);
        map = new HashMap<String, LocalizationValue>();
    }

    /**
     * Set the {@link Document} and load the Map.
     */
    @Override
    public void setDocument(Document document)
    {
        super.setDocument(document);
        loadMap();
    }

    /***
     * Loads the document nodes in a map 
     */
    private void loadMap()
    {
        Document doc = getDocument();
        if (doc != null)
        {
            NodeList list = doc.getElementsByTagName(RESOURCES_ELEMENT);
            if (list.getLength() > 0)
            {
                //Get first resource element
                Node rootNode = list.item(0);

                //Get strings entries
                NodeList nodes = rootNode.getChildNodes();
                Node node;

                if (nodes.getLength() > 0)
                {
                    int length = nodes.getLength();
                    int i;

                    String key;
                    LocalizationValue value;
                    List<String> valuesList;

                    //For each entry of strings.xml, here represented by the document, create an entry in the map    
                    for (i = 0; i < length; i++)
                    {
                        node = nodes.item(i);
                        if (node instanceof org.w3c.dom.Element)
                        {
                            //Single Value
                            if (node.getNodeName().equals(STRING_ELEMENT))
                            {
                                key =
                                        node.getAttributes().getNamedItem(STRING_NAME_ATTRIBUTE)
                                                .getNodeValue();
                                value =
                                        new LocalizationValue(ValueType.SINGLE,
                                                node.getTextContent(), null);

                                this.addEntry(key, value);
                            }
                            //String-array
                            else if (node.getNodeName().equals(STRING_ARRAY_ELEMENT))
                            {
                                key =
                                        node.getAttributes().getNamedItem(STRING_NAME_ATTRIBUTE)
                                                .getNodeValue();

                                valuesList = new ArrayList<String>();

                                //Read Array items
                                NodeList itemNodes = node.getChildNodes();
                                if (itemNodes.getLength() > 0)
                                {
                                    int j;
                                    for (j = 0; j < length; j++)
                                    {
                                        if (itemNodes.item(j) instanceof org.w3c.dom.Element)
                                        {
                                            if (itemNodes.item(j).getTextContent().length() > 0)
                                            {
                                                valuesList.add(itemNodes.item(j).getTextContent());
                                            }
                                        }
                                    }
                                }

                                value = new LocalizationValue(ValueType.ARRAY, null, valuesList);
                                this.addEntry(key, value);
                            }

                        }

                    }
                }
            }
        }
    }

    /***
     * Gets a list of keys from strings.xml represented by this element.
     * 
     * @return Returns a list of keys.
     */
    public List<String> getKeyList()
    {
        List<String> result = new ArrayList<String>();
        result.addAll(map.keySet());
        return result;
    }

    /**
     * Adds an entry to the map.
     * Intend to be used for creation of elements that does not represent a existing file. 
     * (e.g. Union of many strings.xml of the same language).
     * 
     * @param key The string that represents the key.
     * @param value A single string value or a List of strings in case the value is an array
     */
    @SuppressWarnings("unchecked")
    public void addEntry(String key, Object value)
    {
        if (value instanceof String)
        {
            map.put(key, new LocalizationValue(ValueType.SINGLE, (String) value, null));
        }
        else if (value instanceof List)
        {
            map.put(key, new LocalizationValue(ValueType.ARRAY, null, (List<String>) value));
        }
        else if (value instanceof LocalizationValue)
        {
            map.put(key, (LocalizationValue) value);
        }
    }

    /***
     * Check if the element contains a specific key.
     * 
     * @param key The key to be checked.
     * @return Returns <code>true</code> if yes, <code>false</code> otherwise.
     */
    public boolean containsKey(String key)
    {
        return map.containsKey(key);
    }

    /***
     * Check if the element contains values for a specific key.
     * 
     * @param key Key to be checked.
     * @return Returns <code>true</code> if yes, <code>false</code> otherwise.
     */
    public boolean containsValue(String key)
    {
        return (getValue(key) != null);
    }

    /**
     * Given a certain key, its value is returned.
     * 
     * @param key Key which is related to the value to be retrieved.
     * 
     * @return Returns a certain value based on a key.
     */
    public Object getValue(String key)
    {
        Object result = null;
        if (map != null)
        {
            LocalizationValue value = map.get(key);
            if (value != null)
            {
                if (value.getType() == ValueType.SINGLE)
                {
                    result = value.getValue();
                }
                else if (value.getType() == ValueType.ARRAY)
                {
                    result = value.getValues();
                }
            }
        }
        return result;
    }

    //Localization value

    /***
     * Enumeration that represents the possible types of a value.
     */
    public enum ValueType
    {
        /**
         * The value is a single element.
         */
        SINGLE,
        /**
         * The value is an array of values.
         */
        ARRAY,
        /**
         * The value is a plural representation of value.
         */
        PLURAL
    };

    /**
     * Class that represents a localization value. Can be a single value, an array or a plural    
     */
    class LocalizationValue
    {
        /**
         * Constructor which sets the minimum necessary data.
         * 
         * @param type The type of Location value.
         * @param value The value.
         * @param values List of values.
         */
        public LocalizationValue(ValueType type, String value, List<String> values)
        {
            this.type = type;
            this.value = value;
            this.values = values;
        }

        private final ValueType type;

        private final String value;

        private List<String> values = new ArrayList<String>();

        /**
         * Gets the type.
         * 
         * @return Returns the {@link ValueType}.
         */
        public ValueType getType()
        {
            return type;
        }

        /**
         * Gets the value.
         * 
         * @return Returns the value.
         */
        public String getValue()
        {
            return value;
        }

        /**
         * Gets the list of values.
         * 
         * @return Returns the list of values.
         */
        public List<String> getValues()
        {
            return values;
        }
    }

    /**
     * Clean the create Map.
     */
    @Override
    public void clean()
    {
        super.clean();
        if (map != null)
        {
            map.clear();
        }
        this.map = null;
    }

}
