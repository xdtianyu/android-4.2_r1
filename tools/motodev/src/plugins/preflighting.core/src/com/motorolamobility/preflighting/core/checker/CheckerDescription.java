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
package com.motorolamobility.preflighting.core.checker;

/***
 * This class describes a checker and is intended to be used by UI classes 
 * that have to show information about checkers.
 * 
 */
public final class CheckerDescription
{
    private String id;

    private String name;

    private String description;

    /**
     * 
     * @return The checker id.
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the checker id.
     * @param id the checker id.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Returns the checker name.
     * @return the checker name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the checker name.
     * @param name the checker name.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Returns the checker description.
     * @return the checker description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the checker description.
     * @param decription the checker description.
     */
    public void setDescription(String decription)
    {
        this.description = decription;
    }

}
