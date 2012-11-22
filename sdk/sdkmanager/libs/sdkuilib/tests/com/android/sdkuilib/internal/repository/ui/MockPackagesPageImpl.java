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

package com.android.sdkuilib.internal.repository.ui;

import com.android.sdklib.internal.repository.DownloadCache;
import com.android.sdklib.internal.repository.DownloadCache.Strategy;
import com.android.sdklib.util.SparseIntArray;
import com.android.sdkuilib.internal.repository.MockDownloadCache;
import com.android.sdkuilib.internal.repository.UpdaterData;
import com.android.sdkuilib.internal.repository.core.PackageLoader;
import com.android.sdkuilib.internal.repository.core.PkgCategory;
import com.android.sdkuilib.internal.repository.core.PkgContentProvider;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Font;

import java.util.ArrayList;
import java.util.List;

public class MockPackagesPageImpl extends PackagesPageImpl {

    public MockPackagesPageImpl(UpdaterData updaterData) {
        super(updaterData);
    }

    /** UI is never disposed in the unit test. */
    @Override
    protected boolean isUiDisposed() {
        return false;
    }

    /** Sync exec always executes immediately in the unit test, no threading is used. */
    @Override
    protected void syncExec(Runnable runnable) {
        runnable.run();
    }

    private MockTreeViewer mTreeViewer;

    @Override
    void postCreate() {
        mTreeViewer = new MockTreeViewer();
        setITreeViewer(mTreeViewer);

        setIColumns(new MockTreeColumn(mTreeViewer),  // columnName
                    new MockTreeColumn(mTreeViewer),  // columnApi
                    new MockTreeColumn(mTreeViewer),  // columnRevision
                    new MockTreeColumn(mTreeViewer)); // columnStatus

        super.postCreate();
    }

    @Override
    protected void refreshViewerInput() {
        super.setViewerInput();
    }

    @Override
    protected boolean isSortByApi() {
        return true;
    }

    @Override
    protected Font getTreeFontItalic() {
        return null;
    }

    @Override
    protected void loadPackages(boolean useLocalCache, boolean overrideExisting) {
        super.loadPackagesImpl(useLocalCache, overrideExisting);
    }

    /**
     * In this mock version, we use the default {@link PackageLoader} which will
     * use the {@link DownloadCache} from the {@link UpdaterData}. This should be
     * the mock download cache, in which case we change the strategy at run-time
     * to set it to only-cache on the first manager update.
     */
    @Override
    protected PackageLoader getPackageLoader(boolean useLocalCache) {
        DownloadCache dc = mUpdaterData.getDownloadCache();
        assert dc instanceof MockDownloadCache;
        if (dc instanceof MockDownloadCache) {
            ((MockDownloadCache) dc).overrideStrategy(useLocalCache ? Strategy.ONLY_CACHE : null);
        }
        return mUpdaterData.getPackageLoader();
    }

    /**
     * Get a dump-out of the tree in a format suitable for unit testing.
     */
    public String getMockTreeDisplay() throws Exception {
        return mTreeViewer.getTreeDisplay();
    }

    private static class MockTreeViewer implements PackagesPageImpl.ICheckboxTreeViewer {
        private final SparseIntArray mWidths = new SparseIntArray();
        private final List<MockTreeColumn> mColumns = new ArrayList<MockTreeColumn>();
        private List<PkgCategory> mInput;
        private PkgContentProvider mPkgContentProvider;
        private String mLastRefresh;
        private static final String SPACE = "                                                 ";

        @Override
        public void setInput(List<PkgCategory> input) {
            mInput = input;
            refresh();
        }

        @Override
        public Object getInput() {
            return mInput;
        }

        @Override
        public void setContentProvider(PkgContentProvider pkgContentProvider) {
            mPkgContentProvider = pkgContentProvider;
        }

        @Override
        public void refresh() {
            // Recompute the display of the tree
            StringBuilder sb = new StringBuilder();
            boolean widthChanged = false;

            for (int render = 0; render < (widthChanged ? 2 : 1); render++) {
                widthChanged = false;
                sb.setLength(0);
                for (Object cat : mPkgContentProvider.getElements(mInput)) {
                    if (cat == null) {
                        continue;
                    }

                    if (sb.length() > 0) {
                        sb.append('\n');
                    }

                    widthChanged |= rowAsString(cat, sb, 3);

                    Object[] children = mPkgContentProvider.getElements(cat);
                    if (children == null) {
                        continue;
                    }
                    for (Object child : children) {
                        sb.append("\n L_");
                        widthChanged |= rowAsString(child, sb, 0);
                    }
                }
            }

            mLastRefresh = sb.toString();
        }

        boolean rowAsString(Object element, StringBuilder sb, int space) {
            boolean widthChanged = false;
            sb.append("[] ");
            for (int col = 0; col < mColumns.size(); col++) {
                if (col > 0) {
                    sb.append(" | ");
                }
                String t = mColumns.get(col).getLabelProvider().getText(element);
                if (t == null) {
                    t = "(null)";
                }
                int len = t.length();
                int w = mWidths.get(col);
                if (len > w) {
                    widthChanged = true;
                    mWidths.put(col, len);
                    w = len;
                }
                String pad = len >= w ? "" : SPACE.substring(SPACE.length() - w + len);
                if (col == 0 && space > 0) {
                    sb.append(SPACE.substring(SPACE.length() - space));
                }
                if (col >= 1 && col <= 2) {
                    sb.append(pad);
                }
                sb.append(t);
                if (col == 0 || col > 2) {
                    sb.append(pad);
                }
            }
            return widthChanged;
        }

        @Override
        public Object[] getCheckedElements() {
            return null;
        }

        public void addColumn(MockTreeColumn mockTreeColumn) {
            mColumns.add(mockTreeColumn);
        }

        public String getTreeDisplay() {
            return mLastRefresh;
        }
    }

    private static class MockTreeColumn implements PackagesPageImpl.ITreeViewerColumn {
        private ColumnLabelProvider mLabelProvider;

        public MockTreeColumn(MockTreeViewer treeViewer) {
            treeViewer.addColumn(this);
        }

        @Override
        public void setLabelProvider(ColumnLabelProvider labelProvider) {
            mLabelProvider = labelProvider;
        }

        public ColumnLabelProvider getLabelProvider() {
            return mLabelProvider;
        }
    }
}
