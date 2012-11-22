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
package com.motorolamobility.preflighting.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.motorolamobility.preflighting.core.exception.ValidationLimitException;

/**
 * An implementation of ArrayList that will throw an {@link ValidationLimitException} if its size is bigger than maxSize after an item addition.
 * If UNLIMITED is passed as maxSize during list construction {@link LimitedList} will behave exactly like {@link ArrayList}.
 * For more information regarding the behavior of the list @see ArrayList.
 */
public class LimitedList<T> extends ArrayList<T>
{
    private static final long serialVersionUID = 7066195136542105428L;

    /**
     * Constant that determines if this List is unlimited, when used as MaxSize.
     */
    public static final int UNLIMITED = -1;

    /**
     * Flag indicating that this is an unlimited list.
     */
    private boolean unlimited = false;

    /**
     * The max size of this list.
     */
    private int maxSize = 0;

    /**
     * Constructs an empty list with an initial capacity of ten elements.
     * In order to construct an unlimited list use UNLIMITED as maxSize.
     * @param maxSize this list maxSize, must be greater than zero or UNLIMITED.
     * @exception IllegalArgumentException if the specified maxSize
     *            is not greater than zero.
     */
    public LimitedList(int maxSize)
    {
        this(10, maxSize);
    }

    /**
     * Constructs an empty list with the specified initial capacity.
     * In order to construct an unlimited list use UNLIMITED as maxSize.
     * @param initialCapacity the initial capacity of the list.
     * @param maxSize this list maxSize, must be greater than zero or UNLIMITED.
     * @exception IllegalArgumentException if the specified maxSize or initialCapacity
     *            is not greater than zero.
     */
    public LimitedList(int initialCapacity, int maxSize)
    {
        super(initialCapacity);
        if ((maxSize < 0) && (maxSize != UNLIMITED))
        {
            throw new IllegalArgumentException(
                    "Max size must be a positive integer or LimitedList.UNLIMITED");
        }
        this.maxSize = maxSize;
        unlimited = maxSize == UNLIMITED;
    }

    /*
     * (non-Javadoc)
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    @Override
    public boolean add(T element)
    {
        if (!exceedsMaxSize(1))
        {
            return super.add(element);
        }
        else
        {
            throw new ValidationLimitException();
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.ArrayList#add(int, java.lang.Object)
     */
    @Override
    public void add(int index, T element)
    {
        if (!exceedsMaxSize(1))
        {
            super.add(index, element);
        }
        else
        {
            throw new ValidationLimitException();
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.ArrayList#addAll(java.util.Collection)
     */
    @Override
    public boolean addAll(Collection<? extends T> c)
    {
        if (!exceedsMaxSize(c.size()))
        {
            return super.addAll(c);
        }
        else
        {
            List<T> allowedSubList = getAllowedList(c);
            super.addAll(allowedSubList);
            throw new ValidationLimitException();
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.ArrayList#addAll(int, java.util.Collection)
     */
    @Override
    public boolean addAll(int index, Collection<? extends T> c)
    {
        if (!exceedsMaxSize(c.size()))
        {
            return super.addAll(index, c);
        }
        else
        {
            List<T> allowedSubList = getAllowedList(c);
            super.addAll(index, allowedSubList);
            throw new ValidationLimitException();
        }
    }

    /**
     * Retrieves a subset of c containing all elements that can still be added into this list.
     */
    private List<T> getAllowedList(Collection<? extends T> c)
    {
        int allowed = maxSize - size();
        @SuppressWarnings("unchecked")
        T[] toBeAdded = (T[]) new Object[maxSize];
        System.arraycopy(c.toArray(), 0, toBeAdded, 0, allowed);
        List<T> allowedSubList = Arrays.asList(toBeAdded);
        return allowedSubList;
    }

    /**
     * Verifies if it's possible to add a given number of elements into
     * the list, without exceeding its maxSize.
     * @param newElementsCount number of elements to be added
     * @return true if newElementsCount would exceed maxSize if added
     */
    private boolean exceedsMaxSize(int newElementsCount)
    {
        boolean exceedsMaxSize = false;
        if (!unlimited)
        {
            if ((size() + newElementsCount) > maxSize)
            {
                exceedsMaxSize = true;
            }
        }
        return exceedsMaxSize;
    }

    /**
     * @return the maxSize
     */
    public int getMaxSize()
    {
        return maxSize;
    }

    /**
     * @param maxSize the maxSize to set
     */
    public void setMaxSize(int maxSize)
    {
        this.maxSize = maxSize;
    }

    /**
     * @return the unlimited
     */
    public boolean isUnlimited()
    {
        return unlimited;
    }
}
