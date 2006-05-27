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

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.colorchooser.DefaultColorSelectionModel;

import com.jgoodies.binding.value.ValueModel;

/**
 * Converts ValueModels to the ColorSelectionModel interface. Useful to bind 
 * JColorChooser and similar classes to a ValueModel.<p>
 * 
 * <strong>Constraints:</strong> The subject ValueModel must be of type Color
 * and must allow read-access to its value. Also, it is strongly recommended 
 * (though not required) that the underlying ValueModel provides only non-null
 * values. This is so because the ColorSelectionModel behavior is undefined
 * for <code>null</code> values and it may have unpredictable results.<p>
 * 
 * <strong>Examples:</strong><pre>
 * // Recommended binding style using a factory
 * ValueModel model = presentationModel.getModel(MyBean.PROPERTYNAME_COLOR);
 * JColorChooser colorChooser = BasicComponentFactory.createColorChooser(model);
 * 
 * // Binding using the Bindings class 
 * ValueModel model = presentationModel.getModel(MyBean.PROPERTYNAME_COLOR);
 * JColorChooser colorChooser = new JColorChooser();
 * Bindings.bind(colorChooser, model);
 * 
 * // Hand-made binding 
 * ValueModel model = presentationModel.getModel(MyBean.PROPERTYNAME_COLOR);
 * JColorChooser colorChooser = new JColorChooser(new ColorSelectionAdapter(model));
 * </pre>
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.4 $
 * 
 * @see javax.swing.colorchooser.ColorSelectionModel
 * @see javax.swing.JColorChooser
 * 
 * @since 1.0.3
 */
public final class ColorSelectionAdapter extends DefaultColorSelectionModel {

    /**
     * Refers to the underlying ValueModel that is used to read and write values.
     */
    private final ValueModel subject;
    
    /**
     * An optional color that is returned as selected color
     * if the underlying ValueModel returns <code>null</code>.
     */
    private final Color defaultColor;


    // Instance Creation *****************************************************

    /**
     * Constructs a ColorSelectionAdapter on the given subject ValueModel.
     * 
     * @param subject    the subject that holds the value
     * @throws NullPointerException if the subject is <code>null</code>.
     */
    public ColorSelectionAdapter(ValueModel subject) {
        this(subject, null);
    }
    

    /**
     * Constructs a ColorSelectionAdapter on the given subject ValueModel.
     * 
     * @param subject    the subject that holds the value
     * @param defaultColor an optional default color that is used as
     *     selected color if the subject returns <code>null</code>
     * @throws NullPointerException if the subject is <code>null</code>.
     */
    public ColorSelectionAdapter(ValueModel subject, Color defaultColor) {
        if (subject == null)
            throw new NullPointerException("The subject must not be null.");
        
        this.subject = subject;
        this.defaultColor = defaultColor;
        subject.addValueChangeListener(new SubjectValueChangeHandler());
    }
    

    // Overriding Superclass Behavior *****************************************

    /**
     * Returns the selected Color which should be non-<code>null</code>.
     * The return value is the subject value model's value, if non-null,
     * otherwise the default color. Note that the latter may be null too.
     *
     * @return the selected Color
     * 
     * @throws ClassCastException if the subject value is not a Color
     * 
     * @see #setSelectedColor(Color)
     */
    public Color getSelectedColor() {
        Color subjectColor = (Color) subject.getValue();
        return subjectColor != null ? subjectColor : defaultColor;
    }

    /**
     * Sets the selected color to <code>color</code>.
     * Note that setting the color to <code>null</code>
     * is undefined and may have unpredictable results.
     * This method fires a state changed event if it sets the
     * current color to a new non-<code>null</code> color.
     *
     * @param color the new Color
     * 
     * @see   #getSelectedColor()
     */
    public void setSelectedColor(Color color) {
        subject.setValue(color);
    }
    

    // Event Handling *********************************************************
    
    /**
     * Handles changes in the subject's value.
     */
    private final class SubjectValueChangeHandler implements PropertyChangeListener {

        /**
         * The subject value has changed. Notifies all registered listeners
         * about a state change.
         * 
         * @param evt the property change event fired by the subject
         */
        public void propertyChange(PropertyChangeEvent evt) {
            fireStateChanged();
        }

    }

}
