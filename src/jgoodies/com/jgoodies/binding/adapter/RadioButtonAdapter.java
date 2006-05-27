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

import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;

import com.jgoodies.binding.BindingUtils;
import com.jgoodies.binding.value.ValueModel;

/**
 * Converts ValueModels to the ToggleButtonModel interface. Useful to bind 
 * JRadioButtons and JRadioButtonMenuItems to a ValueModel.<p>
 * 
 * This adapter holds a <em>choice</em> object that is used to determine 
 * the selection state if the underlying subject ValueModel changes its value.
 * This model is selected if the subject's value equals the choice object.
 * And if the selection is set, the choice object is set to the subject.<p>
 * 
 * <strong>Note:</strong> You must not use a ButtonGroup with this adapter. 
 * The RadioButtonAdapter ensures that only one choice is selected by sharing
 * a single subject ValueModel - at least if all choice values differ. 
 * See also the example below.<p>
 * 
 * <strong>Example:</strong><pre>
 * // Recommended binding style using a factory
 * PresentationModel presentationModel = new PresentationModel(printerSettings);
 * ValueModel orientationModel = 
 *     presentationModel.getModel(PrinterSettings.PROPERTYNAME_ORIENTATION);
 * JRadioButton landscapeButton = BasicComponentFactory.createRadioButton(
 *     orientationModel, PrinterSettings.LANDSCAPE, "Landscape");
 * JRadioButton portraitButton  = BasicComponentFactory.createRadioButton(
 *     orientationModel, PrinterSettings.PORTRAIT, "Portrait");
 * 
 * // Binding using the Bindings class
 * ValueModel orientationModel = 
 *     presentationModel.getModel(PrinterSettings.PROPERTYNAME_ORIENTATION);
 * JRadioButton landscapeButton = new JRadioButton("Landscape");
 * Bindings.bind(landscapeButton, orientationModel, "landscape");
 * 
 * JRadioButton portraitButton = new JRadioButton("Portrait");
 * Bindings.bind(portraitButton, orientationModel, "portrait");
 * 
 * // Hand-made style
 * ValueModel orientationModel = 
 *     presentationModel.getModel(PrinterSettings.PROPERTYNAME_ORIENTATION);
 * JRadioButton landscapeButton = new JRadioButton("Landscape");
 * landscapeButton.setModel(new RadioButtonAdapter(model, "landscape");
 * 
 * JRadioButton portraitButton = new JRadioButton("Portrait");
 * portraitButton.setModel(new RadioButtonAdapter(model, "portrait");
 * </pre>
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.2 $
 * 
 * @see javax.swing.ButtonModel
 * @see javax.swing.JRadioButton
 * @see javax.swing.JRadioButtonMenuItem
 */

public final class RadioButtonAdapter extends JToggleButton.ToggleButtonModel {

    /**
     * Refers to the underlying ValueModel that stores the state.
     */
    private final ValueModel subject;
    
    /**
     * Holds the object that is compared with the subject's value
     * to determine whether this adapter is selected or not.
     */
    private final Object choice;
    

    // Instance Creation ****************************************************

    /**
     * Constructs a RadioButtonAdapter on the given subject ValueModel 
     * for the specified choice.
     * The created adapter will be selected if and only if the
     * subject's initial value equals the given <code>choice</code>.
     * 
     * @param subject the subject that holds the value
     * @param choice the choice that indicates that this adapter is selected
     * 
     * @throws NullPointerException if the subject is <code>null</code>
     */
    public RadioButtonAdapter(ValueModel subject, Object choice) {
        if (subject == null)
            throw new NullPointerException("The subject must not be null.");
        this.subject = subject;
        this.choice = choice;
        subject.addValueChangeListener(new SubjectValueChangeHandler());
        updateSelectedState();
    }
    

    // ToggleButtonModel Implementation ***********************************

    /**
     * First, the subject value is set to this adapter's choice value if 
     * the argument is <code>true</code>. Second, this adapter's state is set 
     * to the then current subject value. The latter ensures that the selection 
     * state is synchronized with the subject - even if the subject rejects 
     * the change.<p>
     * 
     * Does nothing if the boolean argument is <code>false</code>, 
     * or if this adapter is already selected.
     * 
     * @param b <code>true</code> sets the choice value as subject value,
     *     and is intended to select this adapter (although it may not happen); 
     *     <code>false</code> does nothing
     */
    public void setSelected(boolean b) {
        if (!b || isSelected())
            return;
        subject.setValue(choice);
        updateSelectedState();
    }
    
    
    // Safety Check ***********************************************************
    
    /**
     * Throws an UnsupportedOperationException if the group 
     * is not <code>null</code>. You need not and must not
     * use a ButtonGroup with a set of RadioButtonAdapters. 
     * RadioButtonAdapters form a group by sharing the same 
     * subject ValueModel.
     *
     * @param group the <code>ButtonGroup</code> that will be rejected
     * 
     * @throws UnsupportedOperationException if the group is not <code>null</code>.
     */
    public void setGroup(ButtonGroup group) {
        if (group != null)
            throw new UnsupportedOperationException(
                    "You need not and must not use a ButtonGroup "
                  + "with a set of RadioButtonAdapters. These form "
                  + "a group by sharing the same subject ValueModel.");
    }


    /**
     * Updates this adapter's selected state to reflect 
     * whether the subject holds the selected value or not.  
     * Does not modify the subject value.
     */
    private void updateSelectedState() {
        boolean subjectHoldsChoiceValue = 
            BindingUtils.equals(choice, subject.getValue());
        super.setSelected(subjectHoldsChoiceValue);
    }
    
    
    // Event Handling *********************************************************
    
    /**
     * Handles changes in the subject's value.
     */
    private final class SubjectValueChangeHandler implements PropertyChangeListener {

        /**
         * The subject value has changed. Updates this adapter's selected
         * state to reflect whether the subject holds the choice value or not. 
         * 
         * @param evt the property change event fired by the subject
         */
        public void propertyChange(PropertyChangeEvent evt) {
            updateSelectedState();
        }

    }

}
