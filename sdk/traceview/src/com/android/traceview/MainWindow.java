/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.traceview;

import com.android.sdkstats.SdkStatsService;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Properties;

public class MainWindow extends ApplicationWindow {

    private final static String PING_NAME = "Traceview";

    private TraceReader mReader;
    private String mTraceName;

    // A global cache of string names.
    public static HashMap<String, String> sStringCache = new HashMap<String, String>();

    public MainWindow(String traceName, TraceReader reader) {
        super(null);
        mReader = reader;
        mTraceName = traceName;

        addMenuBar();
    }

    public void run() {
        setBlockOnOpen(true);
        open();
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Traceview: " + mTraceName);

        InputStream in = getClass().getClassLoader().getResourceAsStream(
                "icons/traceview-128.png");
        if (in != null) {
            shell.setImage(new Image(shell.getDisplay(), in));
        }

        shell.setBounds(100, 10, 1282, 900);
    }

    @Override
    protected Control createContents(Composite parent) {
        ColorController.assignMethodColors(parent.getDisplay(), mReader.getMethods());
        SelectionController selectionController = new SelectionController();

        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        parent.setLayout(gridLayout);

        Display display = parent.getDisplay();
        Color darkGray = display.getSystemColor(SWT.COLOR_DARK_GRAY);

        // Create a sash form to separate the timeline view (on top)
        // and the profile view (on bottom)
        SashForm sashForm1 = new SashForm(parent, SWT.VERTICAL);
        sashForm1.setBackground(darkGray);
        sashForm1.SASH_WIDTH = 3;
        GridData data = new GridData(GridData.FILL_BOTH);
        sashForm1.setLayoutData(data);

        // Create the timeline view
        new TimeLineView(sashForm1, mReader, selectionController);

        // Create the profile view
        new ProfileView(sashForm1, mReader, selectionController);
        return sashForm1;
    }

    @Override
    protected MenuManager createMenuManager() {
        MenuManager manager = super.createMenuManager();

        MenuManager viewMenu = new MenuManager("View");
        manager.add(viewMenu);

        Action showPropertiesAction = new Action("Show Properties...") {
            @Override
            public void run() {
                showProperties();
            }
        };
        viewMenu.add(showPropertiesAction);

        return manager;
    }

    private void showProperties() {
        PropertiesDialog dialog = new PropertiesDialog(getShell());
        dialog.setProperties(mReader.getProperties());
        dialog.open();
    }

    /**
     * Convert the old two-file format into the current concatenated one.
     *
     * @param base Base path of the two files, i.e. base.key and base.data
     * @return Path to a temporary file that will be deleted on exit.
     * @throws IOException
     */
    private static String makeTempTraceFile(String base) throws IOException {
        // Make a temporary file that will go away on exit and prepare to
        // write into it.
        File temp = File.createTempFile(base, ".trace");
        temp.deleteOnExit();

        FileOutputStream dstStream = null;
        FileInputStream keyStream = null;
        FileInputStream dataStream = null;

        try {
            dstStream = new FileOutputStream(temp);
            FileChannel dstChannel = dstStream.getChannel();

            // First copy the contents of the key file into our temp file.
            keyStream = new FileInputStream(base + ".key");
            FileChannel srcChannel = keyStream.getChannel();
            long size = dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();

            // Then concatenate the data file.
            dataStream = new FileInputStream(base + ".data");
            srcChannel = dataStream.getChannel();
            dstChannel.transferFrom(srcChannel, size, srcChannel.size());
        } finally {
            if (dstStream != null) {
                dstStream.close(); // also closes dstChannel
            }
            if (keyStream != null) {
                keyStream.close(); // also closes srcChannel
            }
            if (dataStream != null) {
                dataStream.close();
            }
        }

        // Return the path of the temp file.
        return temp.getPath();
    }

    /**
     * Returns the tools revision number.
     */
    private static String getRevision() {
        Properties p = new Properties();
        try{
            String toolsdir = System.getProperty("com.android.traceview.toolsdir"); //$NON-NLS-1$
            File sourceProp;
            if (toolsdir == null || toolsdir.length() == 0) {
                sourceProp = new File("source.properties"); //$NON-NLS-1$
            } else {
                sourceProp = new File(toolsdir, "source.properties"); //$NON-NLS-1$
            }

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(sourceProp);
                p.load(fis);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignore) {
                    }
                }
            }

            String revision = p.getProperty("Pkg.Revision"); //$NON-NLS-1$
            if (revision != null && revision.length() > 0) {
                return revision;
            }
        } catch (FileNotFoundException e) {
            // couldn't find the file? don't ping.
        } catch (IOException e) {
            // couldn't find the file? don't ping.
        }

        return null;
    }


    public static void main(String[] args) {
        TraceReader reader = null;
        boolean regression = false;

        // ping the usage server

        String revision = getRevision();
        if (revision != null) {
            new SdkStatsService().ping(PING_NAME, revision);
        }

        // Process command line arguments
        int argc = 0;
        int len = args.length;
        while (argc < len) {
            String arg = args[argc];
            if (arg.charAt(0) != '-') {
                break;
            }
            if (arg.equals("-r")) {
                regression = true;
            } else {
                break;
            }
            argc++;
        }
        if (argc != len - 1) {
            System.out.printf("Usage: java %s [-r] trace%n", MainWindow.class.getName());
            System.out.printf("  -r   regression only%n");
            return;
        }

        String traceName = args[len - 1];
        File file = new File(traceName);
        if (file.exists() && file.isDirectory()) {
            System.out.printf("Qemu trace files not supported yet.\n");
            System.exit(1);
            // reader = new QtraceReader(traceName);
        } else {
            // If the filename as given doesn't exist...
            if (!file.exists()) {
                // Try appending .trace.
                if (new File(traceName + ".trace").exists()) {
                    traceName = traceName + ".trace";
                // Next, see if it is the old two-file trace.
                } else if (new File(traceName + ".data").exists()
                    && new File(traceName + ".key").exists()) {
                    try {
                        traceName = makeTempTraceFile(traceName);
                    } catch (IOException e) {
                        System.err.printf("cannot convert old trace file '%s'\n", traceName);
                        System.exit(1);
                    }
                // Otherwise, give up.
                } else {
                    System.err.printf("trace file '%s' not found\n", traceName);
                    System.exit(1);
                }
            }

            try {
                reader = new DmTraceReader(traceName, regression);
            } catch (IOException e) {
                System.err.printf("Failed to read the trace file");
                e.printStackTrace();
                System.exit(1);
                return;
            }
        }

        reader.getTraceUnits().setTimeScale(TraceUnits.TimeScale.MilliSeconds);

        Display.setAppName("Traceview");
        new MainWindow(traceName, reader).run();
    }
}
