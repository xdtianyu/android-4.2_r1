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
package com.android.chimpchat.core;

import java.util.HashMap;
import java.util.Map;

/**
 * TouchPressType enum contains valid input for the "touch" Monkey command.
 * When passed as a string, the "identifier" value is used.
 */
public enum TouchPressType {
    DOWN("down"), UP("up"), DOWN_AND_UP("downAndUp");

    private static final Map<String,TouchPressType> identifierToEnum =
        new HashMap<String,TouchPressType>();
    static {
        for (TouchPressType type : values()) {
            identifierToEnum.put(type.identifier, type);
        }
    }

    private String identifier;

    TouchPressType(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public static TouchPressType fromIdentifier(String name) {
        return identifierToEnum.get(name);
    }
}
