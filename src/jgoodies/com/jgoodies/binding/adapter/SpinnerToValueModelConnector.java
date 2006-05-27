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

import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.binding.value.ValueModel;

/** 
 * Synchronizes a SpinnerModel with a ValueModel.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.5 $
 * 
 * @see SpinnerModel
 * @see ValueModel
 * 
 * @since 1.1
 */
public final class SpinnerToValueModelConnector {

    /**
     * Holds a SpinnerModel instance that is used to read and set the 
     * spinner value.
     */
    private final SpinnerModel spinnerModel;

    /**
     * Holds the underlying ValueModel that is used to read values, to update 
     * the spinner model and to write values if the spinner changes.
     */
    private final ValueModel valueModel;
    
    /**
     * An optional value that will be set to update the spinner model
     * if the value model's value is <code>null</code>.
     */
    private final Object defaultValue;
    
    /**
     * Implements PropertyChangeListener and ChangeListener and
     * is used to update both the SpinnerModel and the ValueModel.
     */
    private final UpdateHandler updateHandler;


    // Instance Creation ******************************************************

    /**
     * Constructs a SpinnerToValueModelConnector that establishes a
     * Synchronization between the SpinnerModel and ValueModel.
     * This constructor does not synchronize the SpinnerModel and ValueModel now.
     * You may update the spinner model or value model using 
     * <code>#updateSpinnerModel</code> or <code>#updateValueModel</code>.<p>
     * 
     * In case you don't need the connector instance, you better use
     * the static method {@link #connect(SpinnerModel, ValueModel, Object)}.
     * This constructor may confuse developers if you just use 
     * the side effects performed in the constructor; this is because it is
     * quite unconventional to instantiate an object that you never use.
     * 
     * @param spinnerModel  the SpinnerModel to be synchronized
     * @param valueModel    the ValueModel to be synchronized
     * @param defaultValue  the value that will be used to update 
     *     the spinnerModel, if the valueModel's value is <code>null</code>
     * 
     * @throws NullPointerException  
     *     if the spinnerModel, valueModel or defaultValue is <code>null</code>
     */
    public SpinnerToValueModelConnector(
            SpinnerModel spinnerModel,
            ValueModel valueModel,
            Object defaultValue) {
    
        if (spinnerModel == null)
            throw new NullPointerException(
                    "The spinner model must not be null.");
        if (valueModel == null)
            throw new NullPointerException(
                    "The value model must not be null.");
        if (defaultValue == null)
            throw new NullPointerException(
                    "The default value must not be null.");
        this.spinnerModel = spinnerModel;
        this.valueModel = valueModel;
        this.defaultValue = defaultValue;
        this.updateHandler = new UpdateHandler();
        spinnerModel.addChangeListener(updateHandler);
        valueModel.addValueChangeListener(updateHandler);
    }
    
    
    /**
     * Establishes a synchronization between the SpinnerModel and ValueModel.
     * This method does not synchronize the SpinnerModel and ValueModel now.
     * You may update the spinner model or value model using 
     * <code>#updateSpinnerModel</code> or <code>#updateValueModel</code>.
     * 
     * @param spinnerModel  the SpinnerModel to be synchronized
     * @param valueModel    the ValueModel to be synchronized
     * @param defaultValue  the value used if the valueModel's value is <code>null</code>
     * 
     * @throws NullPointerException  
     *     if the spinnerModel or valueModel is <code>null</code>
     */
    public static void connect(SpinnerModel spinnerModel, ValueModel valueModel, Object defaultValue) {
        new SpinnerToValueModelConnector(spinnerModel, valueModel, defaultValue);
    }


    // Synchronization ********************************************************

    /**
     * Sets the subject value as spinner value.
     */
    public void updateSpinnerModel() {
        Object value = valueModel.getValue();
        Object valueWithDefault = value != null ? value : defaultValue;
        setSpinnerModelValueSilently(valueWithDefault);
    }


    /**
     * Sets the spinner value as value model's value.
     */
    public void updateValueModel() {
        setValueModelValueSilently(spinnerModel.getValue());
    }


    /**
     * Sets the spinner model's value without notifying the subject of changes.
     * Invoked by the subject change listener.
     * 
     * @param newValue   the value to be set in the spinner model
     */
    private void setSpinnerModelValueSilently(Object newValue) {
        spinnerModel.removeChangeListener(updateHandler);
        spinnerModel.setValue(newValue);
        spinnerModel.addChangeListener(updateHandler);
    }


    /**
     * Reads the current value from the spinner model and sets it as new
     * value of the subject. Removes the value change listener before the 
     * subject value is set and adds it after the new value has been set.
     * 
     * @param newValue   the value to be set in the value model
     */
    private void setValueModelValueSilently(Object newValue) {
        valueModel.removeValueChangeListener(updateHandler);
        valueModel.setValue(newValue);
        valueModel.addValueChangeListener(updateHandler);
    }

    
    /**
     * Registered with both the SpinnerModel and the ValueModel.
     * Used to update the spinner if the value changes, and vice versa.
     */
    private class UpdateHandler implements PropertyChangeListener, ChangeListener {

        /**
         * The valueModel's value has changed; update the spinner model.
         * 
         * @param evt   the event to handle
         */
        public void propertyChange(PropertyChangeEvent evt) {
            updateSpinnerModel();
        }


        /**
         * The spinner value has changed; update the valueModel and 
         * notify all listeners about the state change.
         *
         * @param evt the change event
         */
        public void stateChanged(ChangeEvent evt) {
            updateValueModel();
        }

    }
    
}
