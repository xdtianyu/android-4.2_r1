/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ide.eclipse.adt.internal.editors.layout.refactoring;

import static com.android.SdkConstants.FD_RES;
import static com.android.SdkConstants.FD_RES_LAYOUT;
import static com.android.SdkConstants.FD_RES_VALUES;

import com.android.SdkConstants;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.common.CommonXmlEditor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.layout.LayoutEditorDelegate;
import com.android.ide.eclipse.adt.internal.editors.layout.descriptors.ViewElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.layout.uimodel.UiViewElementNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.adt.internal.preferences.AdtPrefs;
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectCreator;
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectWizardState;
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectWizardState.Mode;
import com.android.ide.eclipse.tests.SdkTestCase;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"restriction", "javadoc"})
public class AdtProjectTest extends SdkTestCase {
    private static final int TARGET_API_LEVEL = 12;
    public static final String TEST_PROJECT_PACKAGE = "com.android.eclipse.tests"; //$NON-NLS-1$

    /** Update golden files if different from the actual results */
    private static final boolean UPDATE_DIFFERENT_FILES = false;
    /** Create golden files if missing */
    private static final boolean UPDATE_MISSING_FILES = true;
    private static final String TEST_DATA_REL_PATH =
        "eclipse/plugins/com.android.ide.eclipse.tests/src/com/android/ide/eclipse/adt/"
        + "internal/editors/layout/refactoring/testdata";
    private static final String PROJECTNAME_PREFIX = "testproject-";
    private static final long TESTS_START_TIME = System.currentTimeMillis();
    private static File sTempDir = null;

    /**
     * We don't stash the project used by each test case as a field such that test cases
     * can share a single project instance (which is typically much faster).
     * However, see {@link #getProjectName()} for exceptions to this sharing scheme.
     */
    private static Map<String, IProject> sProjectMap = new HashMap<String, IProject>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Prevent preview icon computation during plugin test to make test faster
        if (AdtPlugin.getDefault() == null) {
            fail("This test must be run as an Eclipse plugin test, not a plain JUnit test!");
        }
        AdtPrefs.getPrefs().setPaletteModes("ICON_TEXT"); //$NON-NLS-1$

        getProject();
    }

    /** Set to true if the subclass test case should use a per-instance project rather
     * than a shared project. This is needed by projects which modify the project in such
     * a way that it affects what other tests see (for example, the quickfix resource creation
     * tests will add in new resources, which the code completion tests will then list as
     * possible matches if the code completion test is run after the quickfix test.)
     * @return true to create a per-instance project instead of the default shared project
     */
    protected boolean testCaseNeedsUniqueProject() {
        return false;
    }

    protected boolean testNeedsUniqueProject() {
        return false;
    }

    @Override
    protected boolean validateSdk(IAndroidTarget target) {
        // Not quite working yet. When enabled will make tests run faster.
        //if (target.getVersion().getApiLevel() < TARGET_API_LEVEL) {
        //    return false;
        //}

        return true;
    }

    /** Returns a name to use for the project used in this test. Subclasses do not need to
     * override this if they can share a project with others - which is the case if they do
     * not modify the project in a way that does not affect other tests. For example
     * the resource quickfix test will create new resources which affect what shows up
     * in the code completion results, so the quickfix tests will override this method
     * to produce a unique project for its own tests.
     */
    private String getProjectName() {
        if (testNeedsUniqueProject()) {
            return PROJECTNAME_PREFIX + getClass().getSimpleName() + "-" + getName();
        } else if (testCaseNeedsUniqueProject()) {
            return PROJECTNAME_PREFIX + getClass().getSimpleName();
        } else {
            return PROJECTNAME_PREFIX + TESTS_START_TIME;
        }
    }

    protected IProject getProject() {
        String projectName = getProjectName();
        IProject project = sProjectMap.get(projectName);
        if (project == null) {
            project = createProject(projectName);
            assertNotNull(project);
            sProjectMap.put(projectName, project);
        }

        return project;
    }

    protected IFile getTestDataFile(IProject project, String name) throws Exception {
        return getTestDataFile(project, name, name);
    }

    protected IFile getLayoutFile(IProject project, String name) throws Exception {
        return getTestDataFile(project, name, FD_RES + "/" + FD_RES_LAYOUT + "/" + name);
    }

    protected IFile getValueFile(IProject project, String name) throws Exception {
        return getTestDataFile(project, name, FD_RES + "/" + FD_RES_VALUES + "/" + name);
    }

    protected IFile getTestDataFile(IProject project, String sourceName,
            String destPath) throws Exception {
        return getTestDataFile(project, sourceName, destPath, false);
    }

    protected IFile getTestDataFile(IProject project, String sourceName,
            String destPath, boolean overwrite) throws Exception {
        String[] split = destPath.split("/"); //$NON-NLS-1$
        IContainer parent;
        String name;
        if (split.length == 1) {
            parent = project;
            name = destPath;
        } else {
            IFolder folder = project.getFolder(split[0]);
            NullProgressMonitor monitor = new NullProgressMonitor();
            if (!folder.exists()) {
                folder.create(true /* force */, true /* local */, monitor);
            }
            for (int i = 1, n = split.length; i < n -1; i++) {
                IFolder subFolder = folder.getFolder(split[i]);
                if (!subFolder.exists()) {
                    subFolder.create(true /* force */, true /* local */, monitor);
                }
                folder = subFolder;
            }
            name = split[split.length - 1];
            parent = folder;
        }
        IFile file = parent.getFile(new Path(name));
        if (overwrite && file.exists()) {
            String currentContents = AdtPlugin.readFile(file);
            String newContents = readTestFile(sourceName, true);
            if (currentContents == null || !currentContents.equals(newContents)) {
                file.delete(true, new NullProgressMonitor());
            } else {
                return file;
            }
        }
        if (!file.exists()) {
            String xml = readTestFile(sourceName, true);
            InputStream bstream = new ByteArrayInputStream(xml.getBytes("UTF-8")); //$NON-NLS-1$
            NullProgressMonitor monitor = new NullProgressMonitor();
            file.create(bstream, false /* force */, monitor);
        }

        return file;
    }

    protected IProject createProject(String name) {
        IAndroidTarget target = null;

        IAndroidTarget[] targets = getSdk().getTargets();
        for (IAndroidTarget t : targets) {
            if (t.getVersion().getApiLevel() >= TARGET_API_LEVEL) {
                target = t;
                break;
            }
        }
        assertNotNull(target);


        IRunnableContext context = new IRunnableContext() {
            @Override
            public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
                    throws InvocationTargetException, InterruptedException {
                runnable.run(new NullProgressMonitor());
            }
        };
        NewProjectWizardState state = new NewProjectWizardState(Mode.ANY);
        state.projectName = name;
        state.target = target;
        state.packageName = TEST_PROJECT_PACKAGE;
        state.activityName = name;
        state.applicationName = name;
        state.createActivity = false;
        state.useDefaultLocation = true;

        NewProjectCreator creator = new NewProjectCreator(state, context);
        creator.createAndroidProjects();
        return validateProjectExists(name);
    }

    public void createTestProject() {
        IAndroidTarget target = null;

        IAndroidTarget[] targets = getSdk().getTargets();
        for (IAndroidTarget t : targets) {
            if (t.getVersion().getApiLevel() >= TARGET_API_LEVEL) {
                target = t;
                break;
            }
        }
        assertNotNull(target);
    }

    private static IProject validateProjectExists(String name) {
        IProject iproject = getProject(name);
        assertTrue(String.format("%s project not created", name), iproject.exists());
        assertTrue(String.format("%s project not opened", name), iproject.isOpen());
        return iproject;
    }

    private static IProject getProject(String name) {
        IProject iproject = ResourcesPlugin.getWorkspace().getRoot()
                .getProject(name);
        return iproject;
    }

    protected int getCaretOffset(IFile file, String caretLocation) {
        assertTrue(caretLocation, caretLocation.contains("^"));

        String fileContent = AdtPlugin.readFile(file);
        return getCaretOffset(fileContent, caretLocation);
    }

    protected int getCaretOffset(String fileContent, String caretLocation) {
        assertTrue(caretLocation, caretLocation.contains("^")); //$NON-NLS-1$

        int caretDelta = caretLocation.indexOf("^"); //$NON-NLS-1$
        assertTrue(caretLocation, caretDelta != -1);

        // String around caret/range without the range and caret marker characters
        String caretContext;
        if (caretLocation.contains("[^")) { //$NON-NLS-1$
            caretDelta--;
            assertTrue(caretLocation, caretLocation.startsWith("[^", caretDelta)); //$NON-NLS-1$
            int caretRangeEnd = caretLocation.indexOf(']', caretDelta + 2);
            assertTrue(caretLocation, caretRangeEnd != -1);
            caretContext = caretLocation.substring(0, caretDelta)
                    + caretLocation.substring(caretDelta + 2, caretRangeEnd)
                    + caretLocation.substring(caretRangeEnd + 1);
        } else {
            caretContext = caretLocation.substring(0, caretDelta)
                    + caretLocation.substring(caretDelta + 1); // +1: skip "^"
        }

        int caretContextIndex = fileContent.indexOf(caretContext);
        assertTrue("Caret content " + caretContext + " not found in file",
                caretContextIndex != -1);
        return caretContextIndex + caretDelta;
    }

    /**
     * If the given caret location string contains a selection range, select that range in
     * the given viewer
     *
     * @param viewer the viewer to contain the selection
     * @param caretLocation the location string
     */
    protected int updateCaret(ISourceViewer viewer, String caretLocation) {
        assertTrue(caretLocation, caretLocation.contains("^")); //$NON-NLS-1$

        int caretDelta = caretLocation.indexOf("^"); //$NON-NLS-1$
        assertTrue(caretLocation, caretDelta != -1);
        String text = viewer.getTextWidget().getText();

        int length = 0;

        // String around caret/range without the range and caret marker characters
        String caretContext;

        if (caretLocation.contains("[^")) { //$NON-NLS-1$
            caretDelta--;
            assertTrue(caretLocation, caretLocation.startsWith("[^", caretDelta)); //$NON-NLS-1$

            int caretRangeEnd = caretLocation.indexOf(']', caretDelta + 2);
            assertTrue(caretLocation, caretRangeEnd != -1);
            length = caretRangeEnd - caretDelta - 2;
            assertTrue(length > 0);
            caretContext = caretLocation.substring(0, caretDelta)
                    + caretLocation.substring(caretDelta + 2, caretRangeEnd)
                    + caretLocation.substring(caretRangeEnd + 1);
        } else {
            caretContext = caretLocation.substring(0, caretDelta)
                    + caretLocation.substring(caretDelta + 1); // +1: skip "^"
        }

        int caretContextIndex = text.indexOf(caretContext);

        assertTrue("Caret content " + caretContext + " not found in file",
                caretContextIndex != -1);

        int offset = caretContextIndex + caretDelta;
        viewer.setSelectedRange(offset, length);

        return offset;
    }

    protected String addSelection(String newFileContents, Point selectedRange) {
        int selectionBegin = selectedRange.x;
        int selectionEnd = selectionBegin + selectedRange.y;
        return addSelection(newFileContents, selectionBegin, selectionEnd);
    }

    protected String addSelection(String newFileContents, int selectionBegin, int selectionEnd) {
        // Insert selection markers -- [ ] for the selection range, ^ for the caret
        String newFileWithCaret;
        if (selectionBegin < selectionEnd) {
            newFileWithCaret = newFileContents.substring(0, selectionBegin) + "[^"
                    + newFileContents.substring(selectionBegin, selectionEnd) + "]"
                    + newFileContents.substring(selectionEnd);
        } else {
            // Selected range
            newFileWithCaret = newFileContents.substring(0, selectionBegin) + "^"
                    + newFileContents.substring(selectionBegin);
        }

        return newFileWithCaret;
    }

    protected String getCaretContext(String file, int offset) {
        int windowSize = 20;
        int begin = Math.max(0, offset - windowSize / 2);
        int end = Math.min(file.length(), offset + windowSize / 2);

        return "..." + file.substring(begin, offset) + "^" + file.substring(offset, end) + "...";
    }

    /**
     * Very primitive line differ, intended for files where there are very minor changes
     * (such as code completion apply-tests)
     */
    protected String getDiff(String before, String after) {
        // Do line by line analysis
        String[] beforeLines = before.split("\n");
        String[] afterLines = after.split("\n");

        int firstDelta = 0;
        for (; firstDelta < Math.min(beforeLines.length, afterLines.length); firstDelta++) {
            if (!beforeLines[firstDelta].equals(afterLines[firstDelta])) {
                break;
            }
        }

        if (firstDelta == beforeLines.length && firstDelta == afterLines.length) {
            return "";
        }

        // Counts from the end of both arrays
        int lastDelta = 0;
        for (; lastDelta < Math.min(beforeLines.length, afterLines.length); lastDelta++) {
            if (!beforeLines[beforeLines.length - 1 - lastDelta].equals(
                    afterLines[afterLines.length - 1 - lastDelta])) {
                break;
            }
        }

        boolean showBeforeWindow = firstDelta >= beforeLines.length - lastDelta;
        boolean showAfterWindow = firstDelta >= afterLines.length - lastDelta;

        StringBuilder sb = new StringBuilder();
        if (showAfterWindow && firstDelta > 0) {
            sb.append("  ");
            sb.append(afterLines[firstDelta - 1]);
            sb.append('\n');
        }
        for (int i = firstDelta; i < beforeLines.length - lastDelta; i++) {
            sb.append("<");
            if (beforeLines[i].length() > 0) {
                sb.append(" ");
            }
            sb.append(beforeLines[i]);
            sb.append('\n');
        }
        if (showAfterWindow && lastDelta < afterLines.length - 1) {
            sb.append("  ");
            sb.append(afterLines[afterLines.length - (lastDelta -1)]);
            sb.append('\n');
        }

        sb.append("---\n");

        if (showBeforeWindow && firstDelta > 0) {
            sb.append("  ");
            sb.append(beforeLines[firstDelta - 1]);
            sb.append('\n');
        }
        for (int i = firstDelta; i < afterLines.length - lastDelta; i++) {
            sb.append(">");
            if (afterLines[i].length() > 0) {
                sb.append(" ");
            }
            sb.append(afterLines[i]);
            sb.append('\n');
        }
        if (showBeforeWindow && lastDelta < beforeLines.length - 1) {
            sb.append("  ");
            sb.append(beforeLines[beforeLines.length - (lastDelta -1)]);
            sb.append('\n');
        }

        return sb.toString();
    }

    protected String removeSessionData(String data) {
        if (getProject() != null) {
            data = data.replace(getProject().getName(), "PROJECTNAME");
        }

        return data;
    }

    public static ViewElementDescriptor createDesc(String name, String fqn, boolean hasChildren) {
        if (hasChildren) {
            return new ViewElementDescriptor(name, name, fqn, "", "", new AttributeDescriptor[0],
                    new AttributeDescriptor[0], new ElementDescriptor[1], false);
        } else {
            return new ViewElementDescriptor(name, fqn);
        }
    }

    public static UiViewElementNode createNode(UiViewElementNode parent, String fqn,
            boolean hasChildren) {
        String name = fqn.substring(fqn.lastIndexOf('.') + 1);
        ViewElementDescriptor descriptor = createDesc(name, fqn, hasChildren);
        if (parent == null) {
            // All node hierarchies should be wrapped inside a document node at the root
            parent = new UiViewElementNode(createDesc("doc", "doc", true));
        }
        return (UiViewElementNode) parent.appendNewUiChild(descriptor);
    }

    public static UiViewElementNode createNode(String fqn, boolean hasChildren) {
        return createNode(null, fqn, hasChildren);
    }

    protected String readTestFile(String relativePath, boolean expectExists) {
        String path = "testdata" + File.separator + relativePath; //$NON-NLS-1$
        InputStream stream =
            AdtProjectTest.class.getResourceAsStream(path);
        if (!expectExists && stream == null) {
            return null;
        }

        assertNotNull(relativePath + " does not exist", stream);

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String xml = AdtPlugin.readFile(reader);
        assertNotNull(xml);
        assertTrue(xml.length() > 0);

        // Remove any references to the project name such that we are isolated from
        // that in golden file.
        // Appears in strings.xml etc.
        xml = removeSessionData(xml);

        return xml;
    }

    protected void assertEqualsGolden(String basename, String actual) {
        assertEqualsGolden(basename, actual, basename.substring(basename.lastIndexOf('.') + 1));
    }

    protected void assertEqualsGolden(String basename, String actual, String newExtension) {
        String testName = getName();
        if (testName.startsWith("test")) {
            testName = testName.substring(4);
            if (Character.isUpperCase(testName.charAt(0))) {
                testName = Character.toLowerCase(testName.charAt(0)) + testName.substring(1);
            }
        }
        String expectedName;
        String extension = basename.substring(basename.lastIndexOf('.') + 1);
        if (newExtension == null) {
            newExtension = extension;
        }
        expectedName = basename.substring(0, basename.indexOf('.'))
                + "-expected-" + testName + '.' + newExtension;
        String expected = readTestFile(expectedName, false);
        if (expected == null) {
            File expectedPath = new File(
                    UPDATE_MISSING_FILES ? getTargetDir() : getTempDir(), expectedName);
            AdtPlugin.writeFile(expectedPath, actual);
            System.out.println("Expected - written to " + expectedPath + ":\n");
            System.out.println(actual);
            fail("Did not find golden file (" + expectedName + "): Wrote contents as "
                    + expectedPath);
        } else {
            if (!expected.replaceAll("\r\n", "\n").equals(actual.replaceAll("\r\n", "\n"))) {
                File expectedPath = new File(getTempDir(), expectedName);
                File actualPath = new File(getTempDir(),
                        expectedName.replace("expected", "actual"));
                AdtPlugin.writeFile(expectedPath, expected);
                AdtPlugin.writeFile(actualPath, actual);
                // Also update data dir with the current value
                if (UPDATE_DIFFERENT_FILES) {
                    AdtPlugin.writeFile( new File(getTargetDir(), expectedName), actual);
                }
                System.out.println("The files differ: diff " + expectedPath + " "
                        + actualPath);
                assertEquals("The files differ - see " + expectedPath + " versus " + actualPath,
                        expected, actual);
            }
        }
    }

    /** Get the location to write missing golden files to */
    protected File getTargetDir() {
        // Set $ADT_SDK_SOURCE_PATH to point to your git "sdk" directory; if done, then
        // if you run a unit test which refers to a golden file which does not exist, it
        // will be created directly into the test data directory and you can rerun the
        // test
        // and it should pass (after you verify that the golden file contains the correct
        // result of course).
        String sdk = System.getenv("ADT_SDK_SOURCE_PATH");
        if (sdk != null) {
            File sdkPath = new File(sdk);
            if (sdkPath.exists()) {
                File testData = new File(sdkPath, TEST_DATA_REL_PATH.replace('/',
                        File.separatorChar));
                if (testData.exists()) {
                    return testData;
                }
            }
        }
        return getTempDir();
    }

    protected File getTempDir() {
        if (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_DARWIN) {
            return new File("/tmp"); //$NON-NLS-1$
        }

        if (sTempDir == null) {
            // On Windows, we don't want to pollute the temp folder (which is generally
            // already incredibly busy). So let's create a temp folder for the results.

            File base = new File(System.getProperty("java.io.tmpdir"));     //$NON-NLS-1$

            Calendar c = Calendar.getInstance();
            String name = String.format("adtTests_%1$tF_%1$tT", c).replace(':', '-'); //$NON-NLS-1$
            File tmpDir = new File(base, name);
            if (!tmpDir.exists() && tmpDir.mkdir()) {
                sTempDir = tmpDir;
            } else {
                sTempDir = base;
            }
        }

        return sTempDir;
    }

    /** Special editor context set on the model to be rendered */
    protected static class TestLayoutEditorDelegate extends LayoutEditorDelegate {

        public TestLayoutEditorDelegate(
                IFile file,
                IStructuredDocument structuredDocument,
                UiDocumentNode uiRootNode) {
            super(new TestAndroidXmlCommonEditor(file, structuredDocument, uiRootNode));
        }

        static class TestAndroidXmlCommonEditor extends CommonXmlEditor {

            private final IFile mFile;
            private final IStructuredDocument mStructuredDocument;
            private UiDocumentNode mUiRootNode;

            TestAndroidXmlCommonEditor(
                    IFile file,
                    IStructuredDocument structuredDocument,
                    UiDocumentNode uiRootNode) {
                mFile = file;
                mStructuredDocument = structuredDocument;
                mUiRootNode = uiRootNode;
            }

            @Override
            public IFile getInputFile() {
                return mFile;
            }

            @Override
            public IProject getProject() {
                return mFile.getProject();
            }

            @Override
            public IStructuredDocument getStructuredDocument() {
                return mStructuredDocument;
            }

            @Override
            public UiDocumentNode getUiRootNode() {
                return mUiRootNode;
            }

            @Override
            public void editorDirtyStateChanged() {
            }

            @Override
            public IStructuredModel getModelForRead() {
                IModelManager mm = StructuredModelManager.getModelManager();
                if (mm != null) {
                    try {
                        return mm.getModelForRead(mFile);
                    } catch (Exception e) {
                        fail(e.toString());
                    }
                }

                return null;
            }
        }
    }

    public void testDummy() {
        // This class contains shared test functionality for testcase subclasses,
        // but without an actual test in the class JUnit complains (even if we make
        // it abstract)
    }
}
