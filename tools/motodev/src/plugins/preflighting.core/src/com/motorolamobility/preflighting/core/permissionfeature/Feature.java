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
package com.motorolamobility.preflighting.core.permissionfeature;

/**
 * This class is a Bean object that represents the data inside of the <code>&lt;uses-feature&gt;</code> node from AndroidManifest.xml.
 */
public class Feature
{
    private String id;

    private boolean required = true;

    /** 
     * @param id the value inside <code>&lt;uses-feature android:name=""&gt;</code>.
     */
    public Feature(String id)
    {
        this.id = id;
    }

    /**
     * @return The id corresponds to the value inside <code>&lt;uses-feature android:name=""&gt;</code>.
     */
    public String getId()
    {
        return id;
    }

    /** 
     * @param id set the value which corresponds to <code>&lt;uses-feature android:name=""&gt;</code>.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return Returns true if this feature is set as required inside <code>&lt;uses-feature android:required=""&gt;</code>.
     */
    public boolean isRequired()
    {
        return required;
    }

    /**
     * @param required set true if this feature is required, as declared in <code>&lt;uses-feature android:required=""&gt;</code>.
     */
    public void setRequired(boolean required)
    {
        this.required = required;
    }

    @Override
    public String toString()
    {
        return id;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    /**
     * Equals if id attribute is the same
     */
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        Feature other = (Feature) obj;
        if (id == null)
        {
            if (other.id != null)
            {
                return false;
            }
        }
        else if (!id.equals(other.id))
        {
            return false;
        }
        return true;
    }

}
