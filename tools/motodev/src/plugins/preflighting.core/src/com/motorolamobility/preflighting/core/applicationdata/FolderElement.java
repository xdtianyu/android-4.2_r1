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

import java.io.File;

/***
 * Represents a folder of an application.
 */
public class FolderElement extends Element
{
    /***
     * Class constructor
     * 
     * @param folder File representing the folder location.
     * @param parent Parent Element.
     * @param type The type of the folder.
     */
    public FolderElement(File folder, Element parent, Type type)
    {
        super(folder.getName(), parent, type);
        setFile(folder);
    }

    /***
     * Verifies if the folder represented by this class contains a specified file.
     */
    public boolean containsFile(String filename)
    {
        boolean result = false;
        for (Element element : this.getChildren())
        {
            if (element.getName().equals(filename))
            {
                result = true;
                break;
            }
        }
        return result;
    }

    @Override
    public void clean()
    {
        super.clean();
    }
}
