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
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.android.photoeditor.animation.FadeAnimation;
import com.android.photoeditor.animation.Rotate3DAnimation;
import com.android.photoeditor.animation.Rotate3DAnimation.Rotate;

/**
 * Toolbar that contains all tools and handles all operations for editing photo.
 */
public class Toolbar extends RelativeLayout {

    private static final int UNDO_REDO_ANIMATION_DURATION = 100;
    private static final int QUICKVIEW_ANIMATION_DURATION = 150;

    private final FilterStack filterStack = new FilterStack();
    private ToolbarLayoutHandler layoutHandler;
    private ToolbarIdleHandler idleHandler;
    private EffectsBar effectsBar;
    private ActionBar actionBar;
    private PhotoView photoView;
    private SpinnerProgressDialog progressDialog;
    private Uri sourceUri;

    public Toolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        idleHandler.killIdle();
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        layoutHandler.extraLayout(l, t, r, b);
    }

    public void initialize() {
        photoView = (PhotoView) findViewById(R.id.photo_view);
        initializeEffectsBar();
        initializeActionBar();
        layoutHandler = new ToolbarLayoutHandler(this);
        idleHandler = new ToolbarIdleHandler(this);
        idleHandler.killIdle();
    }

    private void initializeEffectsBar() {
        effectsBar = (EffectsBar) findViewById(R.id.effects_bar);
        effectsBar.initialize(filterStack, photoView, this);
    }

    private void initializeActionBar() {
        final PhotoOutputCallback callback = new PhotoOutputCallback() {

            @Override
            public void onReady(Photo photo) {
                photoView.setTransitionAnimations(
                        FadeAnimation.getFadeOutInAnimations(UNDO_REDO_ANIMATION_DURATION));
                photoView.update(photo);
                progressDialog.dismiss();
            }
        };

        actionBar = (ActionBar) findViewById(R.id.action_bar);
        actionBar.initialize(new ActionBar.ActionBarListener() {

            @Override
            public void onUndo() {
                effectsBar.effectsOff(new Runnable() {

                    @Override
                    public void run() {
                        progressDialog = SpinnerProgressDialog.show(Toolbar.this);
                        filterStack.undo(callback);
                    }
                });
            }

            @Override
            public void onRedo() {
                effectsBar.effectsOff(new Runnable() {

                    @Override
                    public void run() {
                        progressDialog = SpinnerProgressDialog.show(Toolbar.this);
                        filterStack.redo(callback);
                    }
                });
            }

            @Override
            public void onQuickview(final boolean on) {
                final PhotoOutputCallback callback = new PhotoOutputCallback() {

                    @Override
                    public void onReady(Photo photo) {
                        photoView.setTransitionAnimations(Rotate3DAnimation.getFlipAnimations(
                                on ? Rotate.RIGHT : Rotate.LEFT, QUICKVIEW_ANIMATION_DURATION));
                        photoView.update(photo);
                    }
                };

                if (on) {
                    effectsBar.effectsOff(new Runnable() {

                        @Override
                        public void run() {
                            effectsBar.setVisibility(INVISIBLE);
                            filterStack.getSourceCopy(callback);
                        }
                    });
                } else {
                    effectsBar.setVisibility(VISIBLE);
                    filterStack.getResultCopy(callback);
                }
            }

            @Override
            public void onSave() {
                effectsBar.effectsOff(new Runnable() {

                    @Override
                    public void run() {
                        savePhoto(null);
                    }
                });
            }
        });
    }

    public void openPhoto(Uri uri) {
        sourceUri = uri;
        filterStack.setStackListener(actionBar);

        // clearPhotoSource() should be called before loading a new source photo to avoid OOM.
        progressDialog = SpinnerProgressDialog.show(this);
        filterStack.clearPhotoSource();
        new LoadScreennailTask(getContext(), new LoadScreennailTask.Callback() {

            @Override
            public void onComplete(Bitmap bitmap) {
                filterStack.setPhotoSource(Photo.create(bitmap));
                filterStack.getResultCopy(new PhotoOutputCallback() {

                    @Override
                    public void onReady(Photo photo) {
                        photoView.update(photo);
                        progressDialog.dismiss();
                        effectsBar.setEnabled(photo != null);
                    }
                });
            }
        }).execute(sourceUri);
    }

    /**
     * Saves photo and executes runnable (if provided) after saving done.
     */
    public void savePhoto(final Runnable runnable) {
        progressDialog = SpinnerProgressDialog.show(this);
        filterStack.getResultCopy(new PhotoOutputCallback() {

            @Override
            public void onReady(Photo photo) {
                new SaveCopyTask(getContext(), sourceUri, new SaveCopyTask.Callback() {

                    @Override
                    public void onComplete(Uri uri) {
                        // TODO: Handle saving failure.
                        progressDialog.dismiss();
                        actionBar.disableSave();
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                }).execute(photo);
            }
        });
    }
}
