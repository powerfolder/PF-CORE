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

package com.jgoodies.binding.value;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.adapter.Bindings;



/**
 * A ValueModel that provides relevant GUI state in presentation models. 
 * It provides bound properties for the frequently used JComponent state 
 * <em>enabled</em>/<em>visible</em> and JTextComponent state <em>editable</em>. 
 * ComponentValueModels can be used to set these properties at the
 * presentation model layer; any ComponentValueModel property change 
 * will be reflected by components bound to that ComponentValueModel.<p>
 * 
 * The ComponentValueModel is similar to the Swing Action class.
 * If you disable an Action, all buttons and menu items bound to that Action
 * will be disabled. If you disable a ComponentValueModel, all components
 * bound to that ComponentValueModel will be disabled. If you set the
 * ComponentValueModel to invisible, the component bound to it will become
 * invisible. If you set a ComponentValueModel to non-editable, 
 * the JTextComponents bound to it will become non-editable.<p>
 * 
 * Since version 1.1, PresentationModels can vend ComponentValueModels
 * using <code>#getComponentModel(String)</code> and
 * <code>#getBufferedComponentModel(String)</code>. Multiple calls 
 * to these factory methods return the same ComponentValueModel.<p>
 * 
 * The BasicComponentFactory and the Bindings class check if the ValueModel 
 * provided to create/bind a Swing component is a ComponentValueModel.
 * If so, the ComponentValueModel properties will be synchronized 
 * with the associated Swing component properties.<p>
 * 
 * It is recommended to use ComponentValueModels only for those models
 * that are bound to view components that require GUI state changes.<p>
 * 
 * <strong>Example Code:</strong><pre>
 * final class AlbumView {
 *  
 *  ...
 *     
 *     private void initComponents() {
 *         // No state modifications required for the name field.
 *         nameField = BasicComponentFactory.createTextField(
 *             presentationModel.getModel(Album.PROPERTYNAME_NAME));
 *         ...
 *         // Enablement shall change for the composer field
 *         composerField = BasicComponentFactory.createTextField(
 *             presentationModel.getComponentModel(Album.PROPERTYNAME_COMPOSER));
 *         ...
 *     }
 *     
 *  ...
 *  
 * }
 * 
 * 
 * public final class AlbumPresentationModel extends PresentationModel {
 * 
 *  ...
 *  
 *     private void updateComposerEnablement(boolean enabled) {
 *         getComponentModel(Album.PROPERTYNAME_COMPOSER).setEnabled(enabled);
 *     }
 *     
 *  ...
 *   
 * }
 * </pre><p>
 * 
 * As of the Binding version 1.1.0 the ComponentValueModel feature 
 * is implemented for text components, radio buttons, and check boxes.
 * JLists, JTables, JComboBoxes, and JColorChoosers bound using the
 * Bindings class will ignore ComponentValueModel state. See also
 * <a href="https://binding.dev.java.net/issues/show_bug.cgi?id=86">Issue 
 * 86</a>.<p>
 * 
 * TODO: Add an automatic binding for lists, tables, combos.
 *  
 * @author Karsten Lentzsch
 * @version $Revision: 1.7 $
 * 
 * @see PresentationModel#getComponentModel(String)
 * @see PresentationModel#getBufferedComponentModel(String)
 * @see BasicComponentFactory
 * @see Bindings
 * 
 * @since 1.1
 */
public final class ComponentValueModel extends AbstractValueModel {
    
    // Names of the Bean Properties *******************************************

    /**
     * The name of the property used to synchronize 
     * this model with the <em>enabled</em> property of JComponents.
     */
    public static final String PROPERTYNAME_ENABLED  = "enabled";

    /**
     * The name of the property used to synchronize 
     * this model with the <em>visible</em> property of JComponents.
     */
    public static final String PROPERTYNAME_VISIBLE  = "visible";
    
    /**
     * The name of the property used to synchronize 
     * this model with the <em>editable</em> property of JTextComponents.
     */
    public static final String PROPERTYNAME_EDITABLE = "editable";

    
    // Instance Fields ********************************************************
    
    /**
     * Holds the wrapped subject ValueModel that is used 
     * to read and write value.
     */
    private final ValueModel subject;
    
    private boolean enabled;
    private boolean visible;
    private boolean editable;
    
    
    // Instance Creation ******************************************************
    
    /**
     * Constructs a ComponentValueModel for the given ValueModel.
     * 
     * @param subject   the underlying (or wrapped) ValueModel 
     */
    public ComponentValueModel(ValueModel subject) {
        this.subject = subject;
        this.enabled = true;
        this.editable = true;
        this.visible = true;
        subject.addValueChangeListener(new SubjectValueChangeHandler());
    }
    
    
    // ValueModel Implementation **********************************************
    
    /**
     * Returns this model's current subject value.
     * 
     * @return this model's current subject value.
     */
    public Object getValue() {
        return subject.getValue();
    }
    
    
    /**
     * Sets the given value as new subject value.
     * 
     * @param newValue   the value to set
     */
    public void setValue(Object newValue) {
        subject.setValue(newValue);
    }

    
    // Firing Component Property Change Events ********************************
    
    /**
     * Returns if this model represents an enabled or disabled component state.
     * 
     * @return true for enabled, false for disabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    
    /**
     * Enables or disabled this model, which in turn 
     * will enable or disable all Swing components bound to this model. 
     * 
     * @param b true to enable, false to disable.
     */
    public void setEnabled(boolean b) {
        boolean oldEnabled = isEnabled();
        enabled = b;
        firePropertyChange(PROPERTYNAME_ENABLED, oldEnabled, b);
    }
    

    /**
     * Returns if this model represents the visible or invisible component state.
     *  
     * @return true for visible, false for invisible
     */
    public boolean isVisible() {
        return visible;
    }
    
    
    /**
     * Sets this model state to visible or invisible, which in turn
     * will make all Swing components bound to this model visible or invisible.
     * 
     * @param b    true for visible, false for invisible
     */
    public void setVisible(boolean b) {
        boolean oldVisible = isVisible();
        visible = b;
        firePropertyChange(PROPERTYNAME_VISIBLE, oldVisible, b);
    }
    
    
    /**
     * Returns if this model represents the editable or non-editable
     * text component state. 
     * 
     * @return true for editable, false for non-editable
     */
    public boolean isEditable() {
        return editable;
    }
    
    
    /**
     * Sets this model state to editable or non-editable, which in turn will 
     * make all text components bound to this model editable or non-editable.
     * 
     * @param b    true for editable, false for non-editable
     */
    public void setEditable(boolean b) {
        boolean oldEditable = isEditable();
        editable = b;
        firePropertyChange(PROPERTYNAME_EDITABLE, oldEditable, b);
    }
    
    
    // Event Handling *********************************************************
    
    /** 
     * Forwards value changes in the subject to listeners of this model. 
     */
    private final class SubjectValueChangeHandler implements PropertyChangeListener {
        
        public void propertyChange(PropertyChangeEvent evt) {
            fireValueChange(evt.getOldValue(), evt.getNewValue(), true);
        }
    }
    
}
