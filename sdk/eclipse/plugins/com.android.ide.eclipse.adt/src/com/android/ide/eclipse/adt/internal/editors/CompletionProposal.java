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
package com.android.ide.eclipse.adt.internal.editors;

import com.android.ide.common.api.IAttributeInfo;
import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.IDescriptorProvider;
import com.android.ide.eclipse.adt.internal.editors.descriptors.TextAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Just like {@link org.eclipse.jface.text.contentassist.CompletionProposal},
 * but computes the documentation string lazily since they are typically only
 * displayed for a small subset (the currently focused item) of the available
 * proposals, and producing the strings requires some computation.
 * <p>
 * It also attempts to compute documentation for value strings like
 * ?android:attr/dividerHeight.
 * <p>
 * TODO: Enhance this to compute documentation for additional values, such as
 * the various enum values (which are available in the attrs.xml file, but not
 * in the AttributeInfo objects for each enum value). To do this, I should
 * basically keep around the maps computed by the attrs.xml parser.
 */
class CompletionProposal implements ICompletionProposal {
    private static final Pattern ATTRIBUTE_PATTERN =
            Pattern.compile("[@?]android:attr/(.*)"); //$NON-NLS-1$

    private final AndroidContentAssist mAssist;
    private final Object mChoice;
    private final int mCursorPosition;
    private final int mReplacementOffset;
    private final int mReplacementLength;
    private final String mReplacementString;
    private final Image mImage;
    private final String mDisplayString;
    private final IContextInformation mContextInformation;
    private String mAdditionalProposalInfo;

    CompletionProposal(AndroidContentAssist assist,
            Object choice, String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, Image image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo) {
        assert replacementString != null;
        assert replacementOffset >= 0;
        assert replacementLength >= 0;
        assert cursorPosition >= 0;

        mAssist = assist;
        mChoice = choice;
        mCursorPosition = cursorPosition;
        mReplacementOffset = replacementOffset;
        mReplacementLength = replacementLength;
        mReplacementString = replacementString;
        mImage = image;
        mDisplayString = displayString;
        mContextInformation = contextInformation;
        mAdditionalProposalInfo = additionalProposalInfo;
    }

    @Override
    public Point getSelection(IDocument document) {
        return new Point(mReplacementOffset + mCursorPosition, 0);
    }

    @Override
    public IContextInformation getContextInformation() {
        return mContextInformation;
    }

    @Override
    public Image getImage() {
        return mImage;
    }

    @Override
    public String getDisplayString() {
        if (mDisplayString != null) {
            return mDisplayString;
        }
        return mReplacementString;
    }

    @Override
    public String getAdditionalProposalInfo() {
        if (mAdditionalProposalInfo == null) {
            if (mChoice instanceof ElementDescriptor) {
                String tooltip = ((ElementDescriptor)mChoice).getTooltip();
                mAdditionalProposalInfo = DescriptorsUtils.formatTooltip(tooltip);
            } else if (mChoice instanceof TextAttributeDescriptor) {
                mAdditionalProposalInfo = ((TextAttributeDescriptor) mChoice).getTooltip();
            } else if (mChoice instanceof String) {
                // Try to produce it lazily for strings like @android
                String value = (String) mChoice;
                Matcher matcher = ATTRIBUTE_PATTERN.matcher(value);
                if (matcher.matches()) {
                    String attrName = matcher.group(1);
                    AndroidTargetData data = mAssist.getEditor().getTargetData();
                    if (data != null) {
                        IDescriptorProvider descriptorProvider =
                            data.getDescriptorProvider(mAssist.getRootDescriptorId());
                        if (descriptorProvider != null) {
                            ElementDescriptor[] rootElementDescriptors =
                                descriptorProvider.getRootElementDescriptors();
                            for (ElementDescriptor elementDesc : rootElementDescriptors) {
                                for (AttributeDescriptor desc : elementDesc.getAttributes()) {
                                    String name = desc.getXmlLocalName();
                                    if (attrName.equals(name)) {
                                        IAttributeInfo attributeInfo = desc.getAttributeInfo();
                                        if (attributeInfo != null) {
                                            return attributeInfo.getJavaDoc();
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }

        return mAdditionalProposalInfo;
    }

    @Override
    public void apply(IDocument document) {
        try {
            document.replace(mReplacementOffset, mReplacementLength, mReplacementString);
        } catch (BadLocationException x) {
            // ignore
        }
    }
}