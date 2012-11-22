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

package com.motorolamobility.preflighting.core.source.model;

/**
 * Represents items inside of a method (invocations or constants).
 */
public abstract class Instruction
{
    private int line;

    private String sourceFileFullPath;

    /**
     * @return Returns the line of the instruction if possible, 0 if not found.
     */
    public int getLine()
    {
        return line;
    }

    /**
     * Sets the line of the instruction.
     * @param line the line of the instruction.
     */
    public void setLine(int line)
    {
        this.line = line;
    }

    /**
     * @return the full path of the source file.
     */
    public String getSourceFileFullPath()
    {
        return sourceFileFullPath;
    }

    /**
     * @param sourceFileFullPath the full path of the source file to be set.
     */
    public void setSourceFileFullPath(String sourceFileFullPath)
    {
        this.sourceFileFullPath = sourceFileFullPath;
    }

    @Override
    public String toString()
    {
        return "Instruction [line=" + line + "]";
    }
}
