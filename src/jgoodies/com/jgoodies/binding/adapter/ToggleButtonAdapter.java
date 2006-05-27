/*
 * Copyright (c) 2002-2006 JGoodies Karsten Lentzsch. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *     
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *     
 *  o Neither the name of JGoodies Karsten Lentzsch nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

package com.jgoodies.binding.adapter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JToggleButton;

import com.jgoodies.binding.BindingUtils;
import com.jgoodies.binding.value.ValueModel;

/**
 * Converts ValueModels to the ToggleButtonModel interface. Useful to bind 
 * JToggleButton, JCheckBox and JCheckBoxMenuItem to a ValueModel.<p>
 * 
 * This adapter holds two values that represent the selected and the deselected
 * state. These are used to determine the selection state if the underlying
 * subject ValueModel changes its value. If the selection is set, the 
 * corresponding representant is written to the underlying ValueModel.<p>
 * 
 * <strong>Constraints:</strong> The subject ValueModel must allow 
 * read-access to its value. Also, it is strongly recommended (though not
 * required) that the underlying ValueModel provides only two values, for
 * example Boolean.TRUE and Boolean.FALSE. This is so because the toggle button
 * component may behave "strangely" when it is used with ValueModels that
 * provide more than two elements.<p>
 * 
 * <strong>Examples:</strong><pre>
 * // Recommended binding style using a factory
 * ValueModel model = presentationModel.getModel(MyBean.PROPERTYNAME_VISIBLE);
 * JCheckBox visibleBox = BasicComponentFactory.createCheckBox(model, "Visible");
 * 
 * // Binding using the Bindings class 
 * ValueModel model = presentationModel.getModel(MyBean.PROPERTYNAME_VISIBLE);
 * JCheckBox visibleBox = new JCheckBox("Visible");
 * Bindings.bind(visibleBox, model);
 * 
 * // Hand-made binding 
 * ValueModel model = presentationModel.getModel(MyBean.PROPERTYNAME_VISIBLE);
 * JCheckBox visibleBox = new JCheckBox("Visible");
 * visibleBox.setModel(new ToggleButtonAdapter(model));
 * </pre>
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.2 $
 * 
 * @see javax.swing.ButtonModel
 * @see javax.swing.JCheckBox
 * @see javax.swing.JCheckBoxMenuItem
 */

public final class ToggleButtonAdapter extends JToggleButton.ToggleButtonModel {

    /**
     * Refers to the underlying ValueModel that is used to read and write values.
     */
    private final ValueModel subject;

    /**
     * The value that represents the selected state.
     */
    private final Object selectedValue;

    /**
     * The value that represents the deselected state.
     */
    private final Object deselectedValue;
    

    // Instance Creation *****************************************************

    /**
     * Constructs a ToggleButtonAdapter on the given subject ValueModel.
     * The created adapter will be selected if and only if the
     * subject's initial value is <code>Boolean.TRUE</code>.
     * 
     * @param subject    the subject that holds the value
     * @throws NullPointerException if the subject is <code>null</code>.
     */
    public ToggleButtonAdapter(ValueModel subject) {
        this(subject, Boolean.TRUE, Boolean.FALSE);
    }

    
    /**
     * Constructs a ToggleButtonAdapter on the given subject ValueModel
     * using the specified values for the selected and deselected state.
     * The created adapter will be selected if and only if the
     * subject's initial value equals the given <code>selectedValue</code>.
     * 
     * @param subject           the subject that holds the value
     * @param selectedValue     the value that will be set if this is selected
     * @param deselectedValue   the value that will be set if this is deselected
     * 
     * @throws NullPointerException if the subject is <code>null</code>.
     * @throws IllegalArgumentException if the selected and deselected values
     *     are equal
     */
    public ToggleButtonAdapter(
            ValueModel subject, 
            Object selectedValue,
            Object deselectedValue) {
        if (subject == null)
            throw new NullPointerException("The subject must not be null.");
        if (BindingUtils.equals(selectedValue, deselectedValue))
            throw new IllegalArgumentException("The selected value must not equal the deselected value.");
        
        this.subject = subject;
        this.selectedValue = selectedValue;
        this.deselectedValue = deselectedValue;
        subject.addValueChangeListener(new SubjectValueChangeHandler());
        updateSelectedState();
    }
    

    // ToggleButtonModel Implementation ***********************************

    /**
     * First, the subject value is set to this adapter's selected value if 
     * the argument is <code>true</code>, to the deselected value otherwise.
     * Second, this adapter's state is set to the then current subject value. 
     * This ensures that the selected state is synchronized with the subject 
     * - even if the subject rejects the change.
     * 
     * @param b <code>true</code> sets the selected value as subject value, 
     *          <code>false</code> sets the deselected value as subject value
     */
    public void setSelected(boolean b) {
        subject.setValue(b ? selectedValue : deselectedValue);
        updateSelectedState();
    }
    

    /**
     * Updates this adapter's selected state to reflect 
     * whether the subject holds the selected value or not.  
     * Does not modify the subject value.
     */
    private void updateSelectedState() {
        boolean subjectHoldsChoiceValue = 
            BindingUtils.equals(selectedValue, subject.getValue());
        super.setSelected(subjectHoldsChoiceValue);
    }
    

    // Event Handling *********************************************************
    
    /**
     * Handles changes in the subject's value.
     */
    private final class SubjectValueChangeHandler implements PropertyChangeListener {

        /**
         * The subject value has changed. Updates this adapter's selected
         * state to reflect whether the subject holds the selected value or not. 
         * 
         * @param evt the property change event fired by the subject
         */
        public void propertyChange(PropertyChangeEvent evt) {
            updateSelectedState();
        }

    }

}
