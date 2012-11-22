/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.lint;

import static com.android.SdkConstants.ATTR_IGNORE;
import static com.android.SdkConstants.DOT_XML;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AdtUtils;
import com.android.ide.eclipse.adt.internal.editors.AndroidXmlEditor;
import com.android.ide.eclipse.adt.internal.editors.IconFactory;
import com.android.ide.eclipse.adt.internal.editors.layout.gle2.DomUtilities;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Fix for adding {@code tools:ignore="id"} attributes in XML files.
 */
class AddSuppressAttribute implements ICompletionProposal {
    private final AndroidXmlEditor mEditor;
    private final String mId;
    private final IMarker mMarker;
    private final Element mElement;
    private final String mDescription;

    private AddSuppressAttribute(AndroidXmlEditor editor, String id, IMarker marker,
            Element element, String description) {
        mEditor = editor;
        mId = id;
        mMarker = marker;
        mElement = element;
        mDescription = description;
    }

    @Override
    public Point getSelection(IDocument document) {
        return null;
    }

    @Override
    public String getAdditionalProposalInfo() {
        return null;
    }

    @Override
    public String getDisplayString() {
        return mDescription;
    }

    @Override
    public IContextInformation getContextInformation() {
        return null;
    }

    @Override
    public Image getImage() {
        return IconFactory.getInstance().getIcon("newannotation"); //$NON-NLS-1$
    }

    @Override
    public void apply(IDocument document) {
        AdtUtils.setToolsAttribute(mEditor, mElement, "Suppress Lint Warning", ATTR_IGNORE, mId,
                true /*reveal*/, true /*append*/);

        try {
            // Remove the marker now that the suppress attribute has been added
            // (so the user doesn't have to re-run lint just to see it disappear)
            mMarker.delete();
        } catch (CoreException e) {
            AdtPlugin.log(e, "Could not add suppress annotation");
        }
    }

    /**
     * Returns a quickfix to suppress a specific lint issue id on the node corresponding to
     * the given marker.
     *
     * @param editor the associated editor containing the marker
     * @param marker the marker to create fixes for
     * @param id the issue id
     * @return a fix for this marker, or null if unable
     */
    @Nullable
    public static AddSuppressAttribute createFix(
            @NonNull AndroidXmlEditor editor,
            @NonNull IMarker marker,
            @NonNull String id) {
        // This only applies to XML files:
        String fileName = marker.getResource().getName();
        if (!fileName.endsWith(DOT_XML)) {
            return null;
        }

        int offset = marker.getAttribute(IMarker.CHAR_START, -1);
        Node node;
        if (offset == -1) {
            node = DomUtilities.getNode(editor.getStructuredDocument(), 0);
            if (node != null) {
                node = node.getOwnerDocument().getDocumentElement();
            }
        } else {
            node = DomUtilities.getNode(editor.getStructuredDocument(), offset);
        }
        if (node == null) {
            return null;
        }
        Document document = node.getOwnerDocument();
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getParentNode();
        }
        if (node == null) {
            node = document.getDocumentElement();
            if (node == null) {
                return null;
            }
        }

        String desc = String.format("Add ignore '%1$s\' to element", id);
        Element element = (Element) node;
        return new AddSuppressAttribute(editor, id, marker, element, desc);
    }

    /**
     * Returns a quickfix to suppress a given issue type on the <b>root element</b>
     * of the given editor.
     *
     * @param editor the associated editor containing the marker
     * @param marker the marker to create fixes for
     * @param id the issue id
     * @return a fix for this marker, or null if unable
     */
    @Nullable
    public static AddSuppressAttribute createFixForAll(
            @NonNull AndroidXmlEditor editor,
            @NonNull IMarker marker,
            @NonNull String id) {
        // This only applies to XML files:
        String fileName = marker.getResource().getName();
        if (!fileName.endsWith(DOT_XML)) {
            return null;
        }

        Node node = DomUtilities.getNode(editor.getStructuredDocument(), 0);
        if (node != null) {
            node = node.getOwnerDocument().getDocumentElement();
            String desc = String.format("Add ignore '%1$s\' to element", id);
            Element element = (Element) node;
            return new AddSuppressAttribute(editor, id, marker, element, desc);
        }

        return null;
    }
}
