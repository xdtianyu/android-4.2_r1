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
 * Bean object representing the data inside <code>&lt;uses-permission&gt;</code> node from AndroidManifest.xml
 */
public class Permission
{
    private String id;

    /** 
     * Constructor which assigns this {@link Permission} identifier.
     * 
     * @param id The value inside <code>&lt;uses-permission android:name=""&gt;</code>
     */
    public Permission(String id)
    {
        this.id = id;
    }

    /**
     * Gets the id as in the node <code>&lt;uses-permission android:name=""&gt;</code>.
     * 
     * @return Returns the id as in the node <code>&lt;uses-permission android:name=""&gt;</code>.
     */
    public String getId()
    {
        return id;
    }

    /**
     * 
     *  Set the id as in the node <code>&lt;uses-permission android:name=""&gt;</code>.
     *  
     * @param id The id as in the node <code>&lt;uses-permission android:name=""&gt;</code>.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Prints out the Id.
     * 
     * @return Returns the Id.
     * 
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return id;
    }

    /**
     * Creates a hash-code number based on this object´s Id.
     * 
     * @return Returns the hash-code based on this object´s Id.
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    /**
     * Returns true whether the compared object is a {@link Permission}
     * and the Ids are equal.
     * 
     * @param obj Object to be compared. it must be an instance of {@link Permission}.
     * 
     * @return Returns <code>true</code> if the {@link Permission} are equal,
     * <code>false</code> otherwise.
     * 
     * @see Object#equals(Object)
     */
    @Override
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
        Permission other = (Permission) obj;
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
