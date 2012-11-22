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
package com.motorolamobility.preflighting.core.internal.cond.utils;

import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;

/**
 * Utility class for conditions
 */
public class ConditionUtils
{
    /**
     * Computes the link with the checker and condition detailed description.
     * This method returns the expected format for AppValidator Web.
     * @param checkerId
     * @param conditionId
     * @return the URL for the given checker/condition description
     */
    public static String getDescriptionLink(String checkerId, String conditionId,
            ValidationManagerConfiguration valManagerConfig)
    {
        String link =
                valManagerConfig
                        .getProperty(ValidationManagerConfiguration.ConfigProperties.BASE_URL_PROPERTY
                                .getName());
        if (link != null)
        {
            link +=
                    valManagerConfig
                            .getProperty(ValidationManagerConfiguration.ConfigProperties.URL_QUERY_PROPERTY
                                    .getName());
            link = link.replaceFirst("#PARAM_1#", checkerId).replaceFirst("#PARAM_2#", conditionId);
        }

        return link;
    }
}
