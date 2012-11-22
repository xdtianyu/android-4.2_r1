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

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.android.photoeditor.filters.Filter;

import java.util.Stack;
import java.util.Vector;

/**
 * A stack of filters to be applied onto a photo.
 */
public class FilterStack {

    /**
     * Listener of stack changes.
     */
    public interface StackListener {

        void onStackChanged(boolean canUndo, boolean canRedo);
    }

    private static class OutputMessageObj {
        PhotoOutputCallback callback;
        Photo result;
    }

    private static final int COPY_SOURCE = 1;
    private static final int COPY_RESULT = 2;
    private static final int SET_SOURCE = 3;
    private static final int CLEAR_SOURCE = 4;
    private static final int CLEAR_STACKS = 5;
    private static final int PUSH_FILTER = 6;
    private static final int UNDO = 7;
    private static final int REDO = 8;
    private static final int TOP_FILTER_CHANGE = 9;
    private static final int OUTPUT = 10;

    private final Stack<Filter> appliedStack = new Stack<Filter>();
    private final Stack<Filter> redoStack = new Stack<Filter>();
    private final Vector<Message> pendingMessages = new Vector<Message>();
    private final Handler mainHandler;
    private final Handler workerHandler;

    // Use two photo buffers as in and out in turns to apply filters in the stack.
    private final Photo[] buffers = new Photo[2];

    private Photo source;
    private StackListener stackListener;

    public FilterStack() {
        HandlerThread workerThread = new HandlerThread("FilterStackWorker");
        workerThread.start();

        mainHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case OUTPUT:
                        OutputMessageObj obj = (OutputMessageObj) msg.obj;
                        obj.callback.onReady(obj.result);
                        break;
                }
            }
        };
        workerHandler = new Handler(workerThread.getLooper()) {

            private void output(PhotoOutputCallback callback, Photo target) {
                // Copy target photo in rgb-565 format to update photo-view or save.
                OutputMessageObj obj = new OutputMessageObj();
                obj.callback = callback;
                obj.result = (target != null) ? target.copy(Bitmap.Config.RGB_565) : null;
                mainHandler.sendMessage(mainHandler.obtainMessage(OUTPUT, obj));
            }

            private void clearBuffers() {
                pendingMessages.clear();
                workerHandler.removeMessages(TOP_FILTER_CHANGE);
                mainHandler.removeMessages(OUTPUT);
                for (int i = 0; i < buffers.length; i++) {
                    if (buffers[i] != null) {
                        buffers[i].clear();
                        buffers[i] = null;
                    }
                }
            }

            private void reallocateBuffer(int target) {
                int other = target ^ 1;
                buffers[target] = Photo.create(Bitmap.createBitmap(
                        buffers[other].width(), buffers[other].height(), Bitmap.Config.ARGB_8888));
            }

            private void invalidate() {
                // In/out buffers need redrawn by reloading source photo and re-applying filters.
                clearBuffers();
                buffers[0] = (source != null) ? source.copy(Bitmap.Config.ARGB_8888) : null;
                if (buffers[0] != null) {
                    reallocateBuffer(1);

                    int out = 1;
                    for (Filter filter : appliedStack) {
                        runFilter(filter, out);
                        out = out ^ 1;
                    }
                }
            }

            private void runFilter(Filter filter, int out) {
                if ((buffers[0] != null) && (buffers[1] != null)) {
                    int in = out ^ 1;
                    filter.process(buffers[in], buffers[out]);
                    if (!buffers[in].matchDimension(buffers[out])) {
                        buffers[in].clear();
                        reallocateBuffer(in);
                    }
                }
            }

            private int getOutBufferIndex() {
                // buffers[0] and buffers[1] are swapped in turns as the in/out buffers for
                // processing stacked filters. For example, the first filter reads buffer[0] and
                // writes buffer[1]; the second filter then reads buffer[1] and writes buffer[0].
                return appliedStack.size() % 2;
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case COPY_SOURCE:
                        output((PhotoOutputCallback) msg.obj, source);
                        break;

                    case COPY_RESULT:
                        output((PhotoOutputCallback) msg.obj, buffers[getOutBufferIndex()]);
                        break;

                    case SET_SOURCE:
                        source = (Photo) msg.obj;
                        invalidate();
                        break;

                    case CLEAR_SOURCE:
                        if (source != null) {
                            source.clear();
                            source = null;
                        }
                        clearBuffers();
                        break;

                    case CLEAR_STACKS:
                        redoStack.clear();
                        appliedStack.clear();
                        break;

                    case PUSH_FILTER:
                        redoStack.clear();
                        appliedStack.push((Filter) msg.obj);
                        stackChanged();
                        break;

                    case UNDO:
                        if (!appliedStack.empty()) {
                            redoStack.push(appliedStack.pop());
                            stackChanged();
                            invalidate();
                        }
                        output((PhotoOutputCallback) msg.obj, buffers[getOutBufferIndex()]);
                        break;

                    case REDO:
                        if (!redoStack.empty()) {
                            Filter filter = redoStack.pop();
                            appliedStack.push(filter);
                            stackChanged();
                            runFilter(filter, getOutBufferIndex());
                        }
                        output((PhotoOutputCallback) msg.obj, buffers[getOutBufferIndex()]);
                        break;

                    case TOP_FILTER_CHANGE:
                        if (pendingMessages.remove(msg) && !appliedStack.empty()) {
                            int out = getOutBufferIndex();
                            runFilter(appliedStack.peek(), out);
                            output((PhotoOutputCallback) msg.obj, buffers[out]);
                        }
                        break;
                }
            }
        };
    }

    public void getSourceCopy(PhotoOutputCallback callback) {
        workerHandler.sendMessage(workerHandler.obtainMessage(COPY_SOURCE, callback));
    }

    public void getResultCopy(PhotoOutputCallback callback) {
        workerHandler.sendMessage(workerHandler.obtainMessage(COPY_RESULT, callback));
    }

    public void setPhotoSource(Photo source) {
        workerHandler.sendMessage(workerHandler.obtainMessage(SET_SOURCE, source));
    }

    public void clearPhotoSource() {
        workerHandler.sendMessage(workerHandler.obtainMessage(CLEAR_SOURCE));
    }

    public void clearStacks() {
        workerHandler.sendMessage(workerHandler.obtainMessage(CLEAR_STACKS));
    }

    public void pushFilter(Filter filter) {
        workerHandler.sendMessage(workerHandler.obtainMessage(PUSH_FILTER, filter));
    }

    public void undo(PhotoOutputCallback callback) {
        workerHandler.sendMessage(workerHandler.obtainMessage(UNDO, callback));
    }

    public void redo(PhotoOutputCallback callback) {
        workerHandler.sendMessage(workerHandler.obtainMessage(REDO, callback));
    }

    public void topFilterChanged(PhotoOutputCallback callback) {
        // Flush outdated top-filter messages before sending new ones.
        Message msg = workerHandler.obtainMessage(TOP_FILTER_CHANGE, callback);
        pendingMessages.clear();
        pendingMessages.add(msg);
        workerHandler.removeMessages(TOP_FILTER_CHANGE);
        workerHandler.sendMessage(msg);
    }

    public synchronized void setStackListener(StackListener listener) {
        stackListener = listener;
    }

    private synchronized void stackChanged() {
        if (stackListener != null) {
            stackListener.onStackChanged(!appliedStack.empty(), !redoStack.empty());
        }
    }
}
