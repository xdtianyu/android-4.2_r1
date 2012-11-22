/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.editors.uimodel;

import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_STYLE;
import static com.android.ide.eclipse.adt.internal.editors.color.ColorDescriptors.ATTR_COLOR;
import static com.google.common.base.Strings.nullToEmpty;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.eclipse.adt.internal.editors.AndroidXmlEditor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.w3c.dom.Node;

import java.util.Comparator;

/**
 * Represents an XML attribute that can be modified by the XML editor's user interface.
 * <p/>
 * The characteristics of an {@link UiAttributeNode} are declared by a
 * corresponding {@link AttributeDescriptor}.
 * <p/>
 * This is an abstract class. Derived classes must implement the creation of the UI
 * and manage its synchronization with the XML.
 */
public abstract class UiAttributeNode implements Comparable<UiAttributeNode> {

    private AttributeDescriptor mDescriptor;
    private UiElementNode mUiParent;
    private boolean mIsDirty;
    private boolean mHasError;

    /** Creates a new {@link UiAttributeNode} linked to a specific {@link AttributeDescriptor}
     * and the corresponding runtime {@link UiElementNode} parent. */
    public UiAttributeNode(AttributeDescriptor attributeDescriptor, UiElementNode uiParent) {
        mDescriptor = attributeDescriptor;
        mUiParent = uiParent;
    }

    /** Returns the {@link AttributeDescriptor} specific to this UI attribute node */
    public final AttributeDescriptor getDescriptor() {
        return mDescriptor;
    }

    /** Returns the {@link UiElementNode} that owns this {@link UiAttributeNode} */
    public final UiElementNode getUiParent() {
        return mUiParent;
    }

    /** Returns the current value of the node. */
    public abstract String getCurrentValue();

    /**
     * @return True if the attribute has been changed since it was last loaded
     *         from the XML model.
     */
    public final boolean isDirty() {
        return mIsDirty;
    }

    /**
     * Sets whether the attribute is dirty and also notifies the editor some part's dirty
     * flag as changed.
     * <p/>
     * Subclasses should set the to true as a result of user interaction with the widgets in
     * the section and then should set to false when the commit() method completed.
     *
     * @param isDirty the new value to set the dirty-flag to
     */
    public void setDirty(boolean isDirty) {
        boolean wasDirty = mIsDirty;
        mIsDirty = isDirty;
        // TODO: for unknown attributes, getParent() != null && getParent().getEditor() != null
        if (wasDirty != isDirty) {
            AndroidXmlEditor editor = getUiParent().getEditor();
            if (editor != null) {
                editor.editorDirtyStateChanged();
            }
        }
    }

    /**
     * Sets the error flag value.
     * @param errorFlag the error flag
     */
    public final void setHasError(boolean errorFlag) {
        mHasError = errorFlag;
    }

    /**
     * Returns whether this node has errors.
     */
    public final boolean hasError() {
        return mHasError;
    }

    /**
     * Called once by the parent user interface to creates the necessary
     * user interface to edit this attribute.
     * <p/>
     * This method can be called more than once in the life cycle of an UI node,
     * typically when the UI is part of a master-detail tree, as pages are swapped.
     *
     * @param parent The composite where to create the user interface.
     * @param managedForm The managed form owning this part.
     */
    public abstract void createUiControl(Composite parent, IManagedForm managedForm);

    /**
     * Used to get a list of all possible values for this UI attribute.
     * <p/>
     * This is used, among other things, by the XML Content Assists to complete values
     * for an attribute.
     * <p/>
     * Implementations that do not have any known values should return null.
     *
     * @param prefix An optional prefix string, which is whatever the user has already started
     *   typing. Can be null or an empty string. The implementation can use this to filter choices
     *   and only return strings that match this prefix. A lazy or default implementation can
     *   simply ignore this and return everything.
     * @return A list of possible completion values, and empty array or null.
     */
    public abstract String[] getPossibleValues(String prefix);

    /**
     * Called when the XML is being loaded or has changed to
     * update the value held by this user interface attribute node.
     * <p/>
     * The XML Node <em>may</em> be null, which denotes that the attribute is not
     * specified in the XML model. In general, this means the "default" value of the
     * attribute should be used.
     * <p/>
     * The caller doesn't really know if attributes have changed,
     * so it will call this to refresh the attribute anyway. It's up to the
     * UI implementation to minimize refreshes.
     *
     * @param node the node to read the value from
     */
    public abstract void updateValue(Node node);

    /**
     * Called by the user interface when the editor is saved or its state changed
     * and the modified attributes must be committed (i.e. written) to the XML model.
     * <p/>
     * Important behaviors:
     * <ul>
     * <li>The caller *must* have called IStructuredModel.aboutToChangeModel before.
     *     The implemented methods must assume it is safe to modify the XML model.
     * <li>On success, the implementation *must* call setDirty(false).
     * <li>On failure, the implementation can fail with an exception, which
     *     is trapped and logged by the caller, or do nothing, whichever is more
     *     appropriate.
     * </ul>
     */
    public abstract void commit();

    // ---- Implements Comparable ----
    @Override
    public int compareTo(UiAttributeNode o) {
        return compareAttributes(mDescriptor.getXmlLocalName(), o.mDescriptor.getXmlLocalName());
    }

    /**
     * Returns {@link Comparator} values for ordering attributes in the following
     * order:
     * <ul>
     *   <li> id
     *   <li> style
     *   <li> layout_width
     *   <li> layout_height
     *   <li> other layout params, sorted alphabetically
     *   <li> other attributes, sorted alphabetically
     * </ul>
     *
     * @param name1 the first attribute name to compare
     * @param name2 the second attribute name to compare
     * @return a negative number if name1 should be ordered before name2
     */
    public static int compareAttributes(String name1, String name2) {
        int priority1 = getAttributePriority(name1);
        int priority2 = getAttributePriority(name2);
        if (priority1 != priority2) {
            return priority1 - priority2;
        }

        // Sort remaining attributes alphabetically
        return name1.compareTo(name2);
    }

    /**
     * Returns {@link Comparator} values for ordering attributes in the following
     * order:
     * <ul>
     *   <li> id
     *   <li> style
     *   <li> layout_width
     *   <li> layout_height
     *   <li> other layout params, sorted alphabetically
     *   <li> other attributes, sorted alphabetically, first by namespace, then by name
     * </ul>
     * @param prefix1 the namespace prefix, if any, of {@code name1}
     * @param name1 the first attribute name to compare
     * @param prefix2  the namespace prefix, if any, of {@code name2}
     * @param name2 the second attribute name to compare
     * @return a negative number if name1 should be ordered before name2
     */
    public static int compareAttributes(
            @Nullable String prefix1, @NonNull String name1,
            @Nullable String prefix2, @NonNull String name2) {
        int priority1 = getAttributePriority(name1);
        int priority2 = getAttributePriority(name2);
        if (priority1 != priority2) {
            return priority1 - priority2;
        }

        int namespaceDelta = nullToEmpty(prefix1).compareTo(nullToEmpty(prefix2));
        if (namespaceDelta != 0) {
            return namespaceDelta;
        }

        // Sort remaining attributes alphabetically
        return name1.compareTo(name2);
    }


    /** Returns a sorting priority for the given attribute name */
    private static int getAttributePriority(String name) {
        if (ATTR_ID.equals(name)) {
            return 10;
        }

        if (ATTR_NAME.equals(name)) {
            return 15;
        }

        if (ATTR_STYLE.equals(name)) {
            return 20;
        }

        if (name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
            // Width and height are special cased because we (a) want width and height
            // before the other layout attributes, and (b) we want width to sort before height
            // even though it comes after it alphabetically.
            if (name.equals(ATTR_LAYOUT_WIDTH)) {
                return 30;
            }
            if (name.equals(ATTR_LAYOUT_HEIGHT)) {
                return 40;
            }

            return 50;
        }

        // "color" sorts to the end
        if (ATTR_COLOR.equals(name)) {
            return 100;
        }

        return 60;
    }
}
