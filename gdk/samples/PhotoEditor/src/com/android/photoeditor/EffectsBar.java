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

package com.android.photoeditor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.photoeditor.actions.AutoFixAction;
import com.android.photoeditor.actions.ColorTemperatureAction;
import com.android.photoeditor.actions.CropAction;
import com.android.photoeditor.actions.CrossProcessAction;
import com.android.photoeditor.actions.DocumentaryAction;
import com.android.photoeditor.actions.DoodleAction;
import com.android.photoeditor.actions.DuotoneAction;
import com.android.photoeditor.actions.FillLightAction;
import com.android.photoeditor.actions.FilterAction;
import com.android.photoeditor.actions.FisheyeAction;
import com.android.photoeditor.actions.FlipAction;
import com.android.photoeditor.actions.GrainAction;
import com.android.photoeditor.actions.GrayscaleAction;
import com.android.photoeditor.actions.HighlightAction;
import com.android.photoeditor.actions.LomoishAction;
import com.android.photoeditor.actions.NegativeAction;
import com.android.photoeditor.actions.PosterizeAction;
import com.android.photoeditor.actions.RedEyeAction;
import com.android.photoeditor.actions.RotateAction;
import com.android.photoeditor.actions.SaturationAction;
import com.android.photoeditor.actions.SepiaAction;
import com.android.photoeditor.actions.ShadowAction;
import com.android.photoeditor.actions.SharpenAction;
import com.android.photoeditor.actions.StraightenAction;
import com.android.photoeditor.actions.TintAction;
import com.android.photoeditor.actions.VignetteAction;
import com.android.photoeditor.actions.WarmifyAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Scroll view that contains all effects for editing photo by mapping each effect to trigger one
 * corresponding FilterAction.
 */
public class EffectsBar extends ScrollView {

    private final List<Effect> effects = new ArrayList<Effect>();
    private TextView effectName;

    public EffectsBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(FilterStack filterStack, PhotoView photoView, ViewGroup tools) {
        effects.add(new Effect(R.id.autofix_effect,
                new AutoFixAction(filterStack, tools)));

        effects.add(new Effect(R.id.crop_effect,
                new CropAction(filterStack, tools)));

        effects.add(new Effect(R.id.crossprocess_effect,
                new CrossProcessAction(filterStack, tools)));

        effects.add(new Effect(R.id.documentary_effect,
                new DocumentaryAction(filterStack, tools)));

        effects.add(new Effect(R.id.doodle_effect,
                new DoodleAction(filterStack, tools)));

        effects.add(new Effect(R.id.duotone_effect,
                new DuotoneAction(filterStack, tools)));

        effects.add(new Effect(R.id.filllight_effect,
                new FillLightAction(filterStack, tools)));

        effects.add(new Effect(R.id.fisheye_effect,
                new FisheyeAction(filterStack, tools)));

        effects.add(new Effect(R.id.flip_effect,
                new FlipAction(filterStack, tools)));

        effects.add(new Effect(R.id.grain_effect,
                new GrainAction(filterStack, tools)));

        effects.add(new Effect(R.id.grayscale_effect,
                new GrayscaleAction(filterStack, tools)));

        effects.add(new Effect(R.id.highlight_effect,
                new HighlightAction(filterStack, tools)));

        effects.add(new Effect(R.id.lomoish_effect,
                new LomoishAction(filterStack, tools)));

        effects.add(new Effect(R.id.negative_effect,
                new NegativeAction(filterStack, tools)));

        effects.add(new Effect(R.id.posterize_effect,
                new PosterizeAction(filterStack, tools)));

        effects.add(new Effect(R.id.redeye_effect,
                new RedEyeAction(filterStack, tools)));

        effects.add(new Effect(R.id.rotate_effect,
                new RotateAction(filterStack, tools)));

        effects.add(new Effect(R.id.saturation_effect,
                new SaturationAction(filterStack, tools)));

        effects.add(new Effect(R.id.sepia_effect,
                new SepiaAction(filterStack, tools)));

        effects.add(new Effect(R.id.shadow_effect,
                new ShadowAction(filterStack, tools)));

        effects.add(new Effect(R.id.sharpen_effect,
                new SharpenAction(filterStack, tools)));

        effects.add(new Effect(R.id.straighten_effect,
                new StraightenAction(filterStack, tools)));

        effects.add(new Effect(R.id.temperature_effect,
                new ColorTemperatureAction(filterStack, tools)));

        effects.add(new Effect(R.id.tint_effect,
                new TintAction(filterStack, tools)));

        effects.add(new Effect(R.id.vignette_effect,
                new VignetteAction(filterStack, tools)));

        effects.add(new Effect(R.id.warmify_effect,
                new WarmifyAction(filterStack, tools)));

        effectName = (TextView) tools.findViewById(R.id.action_effect_name);

        // Disable hardware acceleration on this view to make alpha animations work for idle fading.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        setEnabled(false);
    }

    public void effectsOff(Runnable runnableOnEffectsOff) {
        for (Effect effect : effects) {
            if (effect.on) {
                effect.turnOff(runnableOnEffectsOff);
                return;
            }
        }
        // Just execute the runnable right away if all effects are already off.
        if (runnableOnEffectsOff != null) {
            runnableOnEffectsOff.run();
        }
    }

    public boolean hasEffectOn() {
        for (Effect effect : effects) {
            if (effect.on) {
                return true;
            }
        }
        return false;
    }

    private class Effect implements FilterAction.FilterActionListener {

        private final FilterAction action;
        private final CharSequence name;
        private final IconIndicator button;
        private boolean on;
        private Runnable runnableOnODone;

        public Effect(int effectId, FilterAction action) {
            this.action = action;

            View view = findViewById(effectId);
            name = ((TextView) view.findViewById(R.id.effect_label)).getText();
            button = (IconIndicator) view.findViewById(R.id.effect_button);
            button.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (isEnabled()) {
                        if (on) {
                            turnOff(null);
                        } else {
                            // Have other effects done turning off first and then turn on itself.
                            effectsOff(new Runnable() {

                                @Override
                                public void run() {
                                    turnOn();
                                }
                            });
                        }
                    }
                }
            });
        }

        private void turnOn() {
            effectName.setText(name);
            button.setMode("on");
            on = true;
            action.begin(this);
        }

        private void turnOff(Runnable runnableOnODone) {
            this.runnableOnODone = runnableOnODone;
            action.end();
        }

        @Override
        public void onDone() {
            if (on) {
                effectName.setText("");
                button.setMode("off");
                on = false;

                if (runnableOnODone != null) {
                    runnableOnODone.run();
                    runnableOnODone = null;
                }
            }
        }
    }
}
