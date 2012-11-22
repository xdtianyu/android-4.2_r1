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
package com.motorolamobility.preflighting.core.validation;

import java.util.List;

import com.motorolamobility.preflighting.core.utils.LimitedList;

/**
 * It contains a {@link LimitedList} of {@link ValidationResultData}, that is, 
 * a collection of results raised by a checker or condition.
 */
public class ValidationResult
{

    private LimitedList<ValidationResultData> data = null;

    private String checkerId;

    /**
     * Constructor
     * @param limit maximum number of results that can be returned (if the list of results exceed, only the first items will be displayed)
     * @param checkerId The checkerId for the checker being validated. The value is null for global validations.
     */
    public ValidationResult(String checkerId, int limit)
    {
        this.setCheckerId(checkerId);
        data = new LimitedList<ValidationResultData>(limit);
    }

    /**
     * Returns the list of results raised by a checker or condition 
     * @return list of {@link ValidationResultData}
     */
    public List<ValidationResultData> getValidationResult()
    {
        return data;
    }

    /**
     * Add one ValidationResultData to the ValidationResult object.
     * @param dataRow a new single issue found by a checker/condition
     */
    public void addValidationResult(ValidationResultData dataRow)
    {
        data.add(dataRow);
    }

    /**
     * @param checkerId The checkerId to set.
     */
    private void setCheckerId(String checkerId)
    {
        this.checkerId = checkerId;
    }

    /**
     * Returns the unique identifier for the checker (as declared in the <code>com.motorolamobility.preflighting.core.checker</code> extension point) 
     * @return The checkerId or null if not informed.
     */
    public String getCheckerId()
    {
        return checkerId;
    }

    /**
     * Clear the validation result list.
     */
    public void clear()
    {
        data.clear();
    }

    /**
     * Add all items from list of {@link ValidationResultData} to the validation result.
     * 
     * @param list of items to add into the {@link LimitedList} of results raised by condition/checker.
     */
    public void addAll(List<ValidationResultData> list)
    {
        data.addAll(list);
    }
}
