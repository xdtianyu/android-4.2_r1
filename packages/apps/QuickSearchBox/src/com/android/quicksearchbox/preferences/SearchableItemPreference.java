/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.quicksearchbox.preferences;

import com.android.quicksearchbox.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.view.View;
import android.widget.ImageView;

/**
 * A CheckBoxPreference with an icon added.
 */
public class SearchableItemPreference extends CheckBoxPreference {

    private Drawable mIcon;

    SearchableItemPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.searchable_item_preference);
    }

    public void setIcon(Drawable icon) {
        mIcon = icon;
    }

     @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        icon.setImageDrawable(mIcon);
    }

}
