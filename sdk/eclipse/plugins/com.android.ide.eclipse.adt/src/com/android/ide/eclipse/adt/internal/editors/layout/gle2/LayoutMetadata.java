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
package com.android.ide.eclipse.adt.internal.editors.layout.gle2;

import static com.android.SdkConstants.ANDROID_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_NUM_COLUMNS;
import static com.android.SdkConstants.EXPANDABLE_LIST_VIEW;
import static com.android.SdkConstants.GRID_VIEW;
import static com.android.SdkConstants.LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.TOOLS_URI;


import com.android.SdkConstants;
import static com.android.SdkConstants.ANDROID_URI;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.AdapterBinding;
import com.android.ide.common.rendering.api.DataBindingItem;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.eclipse.adt.AdtUtils;
import com.android.ide.eclipse.adt.internal.editors.AndroidXmlEditor;
import com.android.ide.eclipse.adt.internal.editors.layout.ProjectCallback;
import com.android.ide.eclipse.adt.internal.editors.layout.uimodel.UiViewElementNode;

import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Design-time metadata lookup for layouts, such as fragment and AdapterView bindings.
 */
@SuppressWarnings("restriction") // XML DOM model
public class LayoutMetadata {
    /** The default layout to use for list items in expandable list views */
    public static final String DEFAULT_EXPANDABLE_LIST_ITEM = "simple_expandable_list_item_2"; //$NON-NLS-1$
    /** The default layout to use for list items in plain list views */
    public static final String DEFAULT_LIST_ITEM = "simple_list_item_2"; //$NON-NLS-1$
    /** The default layout to use for list items in spinners */
    public static final String DEFAULT_SPINNER_ITEM = "simple_spinner_item"; //$NON-NLS-1$

    /** The string to start metadata comments with */
    private static final String COMMENT_PROLOGUE = " Preview: ";
    /** The string to end metadata comments with */
    private static final String COMMENT_EPILOGUE = " ";
    /** The property key, included in comments, which references a list item layout */
    public static final String KEY_LV_ITEM = "listitem";        //$NON-NLS-1$
    /** The property key, included in comments, which references a list header layout */
    public static final String KEY_LV_HEADER = "listheader";    //$NON-NLS-1$
    /** The property key, included in comments, which references a list footer layout */
    public static final String KEY_LV_FOOTER = "listfooter";    //$NON-NLS-1$
    /** The property key, included in comments, which references a fragment layout to show */
    public static final String KEY_FRAGMENT_LAYOUT = "layout";        //$NON-NLS-1$

    /** Utility class, do not create instances */
    private LayoutMetadata() {
    }

    /**
     * Returns the given property of the given DOM node, or null
     *
     * @param document the document to look up and read lock the model for
     * @param node the XML node to associate metadata with
     * @param name the name of the property to look up
     * @return the value stored with the given node and name, or null
     * @deprecated this method gets metadata using the old comment-based style; should
     *      only be used for migration at this point
     */
    @Deprecated
    @Nullable
    public static String getProperty(
            @Nullable IDocument document,
            @NonNull Node node,
            @NonNull String name) {
        IStructuredModel model = null;
        try {
            if (document != null) {
                IModelManager modelManager = StructuredModelManager.getModelManager();
                model = modelManager.getExistingModelForRead(document);
            }

            Node comment = findComment(node);
            if (comment != null) {
                String text = comment.getNodeValue();
                return getProperty(name, text);
            }

            return null;
        } finally {
            if (model != null) {
                model.releaseFromRead();
            }
        }
    }

    /**
     * Returns the given property specified in the <b>current</b> element being
     * processed by the given pull parser.
     *
     * @param parser the pull parser, which must be in the middle of processing
     *            the target element
     * @param name the property name to look up
     * @return the property value, or null if not defined
     */
    @Nullable
    public static String getProperty(@NonNull XmlPullParser parser, @NonNull String name) {
        String value = parser.getAttributeValue(TOOLS_URI, name);
        if (value != null && value.isEmpty()) {
            value = null;
        }

        return value;
    }

    /**
     * Returns the given property specified in the given XML comment
     *
     * @param name the name of the property to look up
     * @param text the comment text for an XML node
     * @return the value stored with the given node and name, or null
     */
    public static String getProperty(String name, String text) {
        assert text.startsWith(COMMENT_PROLOGUE);
        String valuesString = text.substring(COMMENT_PROLOGUE.length());
        String[] values = valuesString.split(","); //$NON-NLS-1$
        if (values.length == 1) {
            valuesString = values[0].trim();
            if (valuesString.indexOf('\n') != -1) {
                values = valuesString.split("\n"); //$NON-NLS-1$
            }
        }
        String target = name + '=';
        for (int j = 0; j < values.length; j++) {
            String value = values[j].trim();
            if (value.startsWith(target)) {
                return value.substring(target.length()).trim();
            }
        }
        return null;
    }

    /**
     * Sets the given property of the given DOM node to a given value, or if null clears
     * the property.
     *
     * @param document the document to look up and write lock the model for
     * @param node the XML node to associate metadata with
     * @param name the name of the property to set
     * @param value the value to store for the given node and name, or null to remove it
     * @deprecated this method sets metadata using the old comment-based style; should
     *      only be used for migration at this point
     */
    @Deprecated
    public static void setProperty(IDocument document, Node node, String name, String value) {
        // Reserved characters: [,-=]
        assert name.indexOf('-') == -1;
        assert value == null || value.indexOf('-') == -1;
        assert name.indexOf(',') == -1;
        assert value == null || value.indexOf(',') == -1;
        assert name.indexOf('=') == -1;
        assert value == null || value.indexOf('=') == -1;

        IStructuredModel model = null;
        try {
            IModelManager modelManager = StructuredModelManager.getModelManager();
            model = modelManager.getExistingModelForEdit(document);
            if (model instanceof IDOMModel) {
                IDOMModel domModel = (IDOMModel) model;
                Document domDocument = domModel.getDocument();
                assert node.getOwnerDocument() == domDocument;
            }

            Document doc = node.getOwnerDocument();
            Node commentNode = findComment(node);

            String commentText = null;
            if (commentNode != null) {
                String text = commentNode.getNodeValue();
                assert text.startsWith(COMMENT_PROLOGUE);
                String valuesString = text.substring(COMMENT_PROLOGUE.length());
                String[] values = valuesString.split(","); //$NON-NLS-1$
                if (values.length == 1) {
                    valuesString = values[0].trim();
                    if (valuesString.indexOf('\n') != -1) {
                        values = valuesString.split("\n"); //$NON-NLS-1$
                    }
                }
                String target = name + '=';
                List<String> preserve = new ArrayList<String>();
                for (int j = 0; j < values.length; j++) {
                    String v = values[j].trim();
                    if (v.length() == 0) {
                        continue;
                    }
                    if (!v.startsWith(target)) {
                        preserve.add(v.trim());
                    }
                }
                if (value != null) {
                    preserve.add(name + '=' + value.trim());
                }
                if (preserve.size() > 0) {
                    if (preserve.size() > 1) {
                        Collections.sort(preserve);
                        String firstLineIndent = AndroidXmlEditor.getIndent(document, commentNode);
                        String oneIndentLevel = "    "; //$NON-NLS-1$
                        StringBuilder sb = new StringBuilder();
                        sb.append(COMMENT_PROLOGUE);
                        sb.append('\n');
                        for (String s : preserve) {
                            sb.append(firstLineIndent);
                            sb.append(oneIndentLevel);
                            sb.append(s);
                            sb.append('\n');
                        }
                        sb.append(firstLineIndent);
                        sb.append(COMMENT_EPILOGUE);
                        commentText = sb.toString();
                    } else {
                        commentText = COMMENT_PROLOGUE + preserve.get(0) + COMMENT_EPILOGUE;
                    }
                }
            } else if (value != null) {
                commentText = COMMENT_PROLOGUE + name + '=' + value + COMMENT_EPILOGUE;
            }

            if (commentText == null) {
                if (commentNode != null) {
                    // Remove the comment, along with surrounding whitespace if applicable
                    Node previous = commentNode.getPreviousSibling();
                    if (previous != null && previous.getNodeType() == Node.TEXT_NODE) {
                        String text = previous.getNodeValue();
                        if (text.trim().length() == 0) {
                            node.removeChild(previous);
                        }
                    }
                    node.removeChild(commentNode);
                    Node first = node.getFirstChild();
                    if (first != null && first.getNextSibling() == null
                            && first.getNodeType() == Node.TEXT_NODE) {
                        String text = first.getNodeValue();
                        if (text.trim().length() == 0) {
                            node.removeChild(first);
                        }
                    }
                }
                return;
            }

            if (commentNode != null) {
                commentNode.setNodeValue(commentText);
            } else {
                commentNode = doc.createComment(commentText);
                String firstLineIndent = AndroidXmlEditor.getIndent(document, node);
                Node firstChild = node.getFirstChild();
                boolean indentAfter = firstChild == null
                        || firstChild.getNodeType() != Node.TEXT_NODE
                        || firstChild.getNodeValue().indexOf('\n') == -1;
                String oneIndentLevel = "    "; //$NON-NLS-1$
                node.insertBefore(doc.createTextNode('\n' + firstLineIndent + oneIndentLevel),
                        firstChild);
                node.insertBefore(commentNode, firstChild);
                if (indentAfter) {
                    node.insertBefore(doc.createTextNode('\n' + firstLineIndent), firstChild);
                }
            }
        } finally {
            if (model != null) {
                model.releaseFromEdit();
            }
        }
    }

    /** Finds the comment node associated with the given node, or null if not found */
    private static Node findComment(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.COMMENT_NODE) {
                String text = child.getNodeValue();
                if (text.startsWith(COMMENT_PROLOGUE)) {
                    return child;
                }
            }
        }

        return null;
    }

    /**
     * Returns the given property of the given DOM node, or null
     *
     * @param node the XML node to associate metadata with
     * @param name the name of the property to look up
     * @return the value stored with the given node and name, or null
     */
    @Nullable
    public static String getProperty(
            @NonNull Node node,
            @NonNull String name) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            String value = element.getAttributeNS(TOOLS_URI, name);
            if (value != null && value.isEmpty()) {
                value = null;
            }

            return value;
        }

        return null;
    }

    /**
     * Sets the given property of the given DOM node to a given value, or if null clears
     * the property.
     *
     * @param editor the editor associated with the property
     * @param node the XML node to associate metadata with
     * @param name the name of the property to set
     * @param value the value to store for the given node and name, or null to remove it
     */
    public static void setProperty(
            @NonNull AndroidXmlEditor editor,
            @NonNull final Node node,
            @NonNull final String name,
            @Nullable final String value) {
        // Clear out the old metadata
        IDocument document = editor.getStructuredSourceViewer().getDocument();
        setProperty(document, node, name, null);

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            AdtUtils.setToolsAttribute(editor, element, "Bind View", name, value,
                    false /*reveal*/, false /*append*/);
        }
    }

    /** Strips out @layout/ or @android:layout/ from the given layout reference */
    private static String stripLayoutPrefix(String layout) {
        if (layout.startsWith(ANDROID_LAYOUT_RESOURCE_PREFIX)) {
            layout = layout.substring(ANDROID_LAYOUT_RESOURCE_PREFIX.length());
        } else if (layout.startsWith(LAYOUT_RESOURCE_PREFIX)) {
            layout = layout.substring(LAYOUT_RESOURCE_PREFIX.length());
        }

        return layout;
    }

    /**
     * Creates an {@link AdapterBinding} for the given view object, or null if the user
     * has not yet chosen a target layout to use for the given AdapterView.
     *
     * @param viewObject the view object to create an adapter binding for
     * @param uiNode the ui node corresponding to the view object
     * @return a binding, or null
     */
    public static AdapterBinding getNodeBinding(Object viewObject, UiViewElementNode uiNode) {
        Node xmlNode = uiNode.getXmlNode();

        String header = getProperty(xmlNode, KEY_LV_HEADER);
        String footer = getProperty(xmlNode, KEY_LV_FOOTER);
        String layout = getProperty(xmlNode, KEY_LV_ITEM);
        if (layout != null || header != null || footer != null) {
            int count = 12;
            // If we're dealing with a grid view, multiply the list item count
            // by the number of columns to ensure we have enough items
            if (xmlNode instanceof Element && xmlNode.getNodeName().endsWith(GRID_VIEW)) {
                Element element = (Element) xmlNode;
                String columns = element.getAttributeNS(ANDROID_URI, ATTR_NUM_COLUMNS);
                int multiplier = 2;
                if (columns != null && columns.length() > 0) {
                    int c = Integer.parseInt(columns);
                    if (c >= 1 && c <= 10) {
                        multiplier = c;
                    }
                }
                count *= multiplier;
            }
            AdapterBinding binding = new AdapterBinding(count);

            if (header != null) {
                boolean isFramework = header.startsWith(ANDROID_LAYOUT_RESOURCE_PREFIX);
                binding.addHeader(new ResourceReference(stripLayoutPrefix(header),
                        isFramework));
            }

            if (footer != null) {
                boolean isFramework = footer.startsWith(ANDROID_LAYOUT_RESOURCE_PREFIX);
                binding.addFooter(new ResourceReference(stripLayoutPrefix(footer),
                        isFramework));
            }

            if (layout != null) {
                boolean isFramework = layout.startsWith(ANDROID_LAYOUT_RESOURCE_PREFIX);
                if (isFramework) {
                    layout = layout.substring(ANDROID_LAYOUT_RESOURCE_PREFIX.length());
                } else if (layout.startsWith(LAYOUT_RESOURCE_PREFIX)) {
                    layout = layout.substring(LAYOUT_RESOURCE_PREFIX.length());
                }

                binding.addItem(new DataBindingItem(layout, isFramework, 1));
            } else if (viewObject != null) {
                String listFqcn = ProjectCallback.getListAdapterViewFqcn(viewObject.getClass());
                if (listFqcn != null) {
                    if (listFqcn.endsWith(EXPANDABLE_LIST_VIEW)) {
                        binding.addItem(
                                new DataBindingItem(DEFAULT_EXPANDABLE_LIST_ITEM,
                                true /* isFramework */, 1));
                    } else {
                        binding.addItem(
                                new DataBindingItem(DEFAULT_LIST_ITEM,
                                true /* isFramework */, 1));
                    }
                }
            } else {
                binding.addItem(
                        new DataBindingItem(DEFAULT_LIST_ITEM,
                        true /* isFramework */, 1));
            }
            return binding;
        }

        return null;
    }
}
