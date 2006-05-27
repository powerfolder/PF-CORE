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
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

import com.jgoodies.binding.beans.PropertyConnector;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.BufferedValueModel;
import com.jgoodies.binding.value.ComponentValueModel;
import com.jgoodies.binding.value.ValueModel;

/**
 * Consists only of static methods that bind Swing components to ValueModels.
 * This is one of two helper classes that assist you in establishing a binding:
 * 1) this Bindings class binds components that have been created before;
 * it wraps ValueModels with the adapters from package 
 * <code>com.jgoodies.binding.adapter</code>.
 * 2) the BasicComponentFactory creates Swing components that are then
 * bound using this Bindings class.<p>
 * 
 * If you have an existing factory that vends Swing components, 
 * you can use this Bindings class to bind them to ValueModels. 
 * If you don't have such a factory, you can use the BasicComponentFactory 
 * or a custom subclass to create and bind Swing components.<p> 
 * 
 * The binding process for JCheckBox, JRadioButton, and other AbstractButtons
 * retains the former enablement state. Before the new (adapting) model
 * is set, the enablement is requested from the model, not the button.
 * This enablement is set after the new model has been set.<p>
 *  
 * TODO: Consider adding binding methods for JProgressBar, 
 * JSlider, JSpinner, and JTabbedPane.<p>
 * 
 * TODO: Consider adding connection methods for pairs of bean properties.
 * In addition to the PropertyConnector's <code>#connect</code> method,
 * this could add boolean operators such as: not, and, or, nor.
 * 
 * @author  Karsten Lentzsch
 * @version $Revision: 1.11 $
 * 
 * @see com.jgoodies.binding.value.ValueModel
 * @see BasicComponentFactory
 */
public final class Bindings {
    
    /**
     * A JComponent client property key used to store 
     * and retrieve an associated ComponentValueModel.
     * 
     * @see #addComponentPropertyHandler(JComponent, ValueModel)
     * @see #removeComponentPropertyHandler(JComponent)
     * @see ComponentValueModel
     */
    private static final String COMPONENT_VALUE_MODEL_KEY =
        "componentValueModel";

    
    /**
     * A JComponent client property key used to store 
     * and retrieve an associated ComponentPropertyHandler.
     * 
     * @see #addComponentPropertyHandler(JComponent, ValueModel)
     * @see #removeComponentPropertyHandler(JComponent)
     */
    private static final String COMPONENT_PROPERTY_HANDLER_KEY =
        "componentPropertyHandler";
    
    
    /**
     * Triggers a commit in the shared focus lost trigger
     * if focus is lost permanently. Shared among all components
     * that are configured to commit on focus lost.
     * 
     * @see #createCommitOnFocusLostModel(ValueModel, Component)
     */
    static final FocusLostHandler FOCUS_LOST_HANDLER = new FocusLostHandler();
    
    /**
     * Holds a weak trigger that is shared by BufferedValueModels
     * that commit on permanent focus change.
     * 
     * @see #createCommitOnFocusLostModel(ValueModel, Component)
     */
    static final WeakTrigger FOCUS_LOST_TRIGGER = new WeakTrigger();
    
    
    private Bindings() {
        // Suppresses default constructor, ensuring non-instantiability.
    }
    
    
    // Binding Methods ********************************************************
    
    /**
     * Binds a JCheckBox to the given ValueModel and retains the enablement 
     * state. The bound check box is selected if and only if the model's value 
     * equals <code>Boolean.TRUE</code>.<p>
     * 
     * The value model is converted to the required interface 
     * ToggleButtonModel using a ToggleButtonAdapter.
     * 
     * @param checkBox       the check box to be bound
     * @param valueModel     the model that provides a Boolean value
     * 
     * @throws NullPointerException if the checkBox or valueModel 
     *     is <code>null</code>
     */
    public static void bind(JCheckBox checkBox, ValueModel valueModel) {
        boolean enabled = checkBox.getModel().isEnabled();
        checkBox.setModel(new ToggleButtonAdapter(valueModel));
        checkBox.setEnabled(enabled);
        
        addComponentPropertyHandler(checkBox, valueModel);
    }


    /**
     * Binds a JCheckBoxMenuItem to the given ValueModel and retains
     * the enablement state. The bound menu item is selected if and only if 
     * the model's value equals <code>Boolean.TRUE</code>.<p>
     * 
     * <strong>Note:</strong> For users of the JGoodies UIF (user interface
     * framework) the recommended way to create and bind check box menu items 
     * is the class <code>com.jgoodies.uif.ToggleAction</code>.<p>
     * 
     * The value model is converted to the required interface 
     * <code>ToggleButtonModel</code> using a <code>ToggleButtonAdapter</code>.
     * 
     * @param checkBoxMenuItem  the check box menu item to be bound
     * @param valueModel        the model that provides a Boolean value
     * 
     * @throws NullPointerException if the menu item or valueModel 
     *     is <code>null</code>
     */
    public static void bind(JCheckBoxMenuItem checkBoxMenuItem, ValueModel valueModel) {
        boolean enabled = checkBoxMenuItem.getModel().isEnabled();
        checkBoxMenuItem.setModel(new ToggleButtonAdapter(valueModel));
        checkBoxMenuItem.setEnabled(enabled);
        
        addComponentPropertyHandler(checkBoxMenuItem, valueModel);
    }
    
    
    /**
     * Binds a JColorChooser to the given Color-typed ValueModel.
     * The ValueModel must be of type Color and must
     * allow read-access to its value.<p>
     * 
     * Also, it is strongly recommended (though not required) that 
     * the underlying ValueModel provides only non-null values. 
     * This is so because the ColorSelectionModel behavior is undefined 
     * for <code>null</code> values and it may have unpredictable results.
     * To avoid these problems, you may bind the ColorChooser with
     * a default color using {@link #bind(JColorChooser, ValueModel, Color)}.
     *
     * <strong>Note:</strong> There's a bug in Java 1.4.2, Java 5 and Java 6
     * that affects this binding. The BasicColorChooserUI doesn't listen
     * to changes in the selection model, and so the preview panel won't
     * update if the selected color changes. As a workaround you can use
     * {@link BasicComponentFactory#createColorChooser(ValueModel)},
     * or you could use a Look&amp;Feel that fixes the bug mentioned above.
     * 
     * @param colorChooser  the color chooser to be bound
     * @param valueModel    the model that provides non-<code>null</code>
     *     Color values.
     * 
     * @throws NullPointerException if the color chooser or value model 
     *     is <code>null</code>
     *     
     * @see #bind(JColorChooser, ValueModel, Color)
     *     
     * @since 1.0.3
     */
    public static void bind(JColorChooser colorChooser, ValueModel valueModel) {
        colorChooser.setSelectionModel(new ColorSelectionAdapter(valueModel));
    }


    /**
     * Binds a JColorChooser to the given Color-typed ValueModel.
     * The ValueModel must be of type Color and must allow read-access 
     * to its value. The default color will be used if the valueModel 
     * returns <code>null</code>.<p>
     * 
     * <strong>Note:</strong> There's a bug in Java 1.4.2, Java 5 and Java 6
     * that affects this binding. The BasicColorChooserUI doesn't listen
     * to changes in the selection model, and so the preview panel won't
     * update if the selected color changes. As a workaround you can use
     * {@link BasicComponentFactory#createColorChooser(ValueModel)},
     * or you could use a Look&amp;Feel that fixes the bug mentioned above.
     * 
     * @param colorChooser  the color chooser to be bound
     * @param valueModel    the model that provides non-<code>null</code>
     *     Color values.
     * @param defaultColor  the color used if the valueModel returns null
     * 
     * @throws NullPointerException if the color chooser, value model,
     *     or default color is <code>null</code>
     *     
     * @since 1.1
     */
    public static void bind(JColorChooser colorChooser, ValueModel valueModel, Color defaultColor) {
        if (defaultColor == null)
            throw new NullPointerException("The default color must not be null.");
        
        colorChooser.setSelectionModel(new ColorSelectionAdapter(valueModel, defaultColor));
    }


    /**
     * Binds a non-editable JComboBox to the given SelectionInList using 
     * the SelectionInList's ListModel as list data provider and the 
     * SelectionInList's selection index holder for the combo box model's
     * selected item.<p>
     * 
     * There are a couple of other possibilities to bind a JComboBox.
     * See the constructors and the class comment of the 
     * {@link ComboBoxAdapter}.
     * 
     * @param comboBox         the combo box to be bound
     * @param selectionInList  provides the list and selection
     * 
     * @throws NullPointerException  if the combo box or the selectionInList 
     *     is <code>null</code>
     *     
     * @see ComboBoxAdapter
     * 
     * @since 1.0.1
     */
    public static void bind(JComboBox comboBox, SelectionInList selectionInList) {
        if (selectionInList == null) 
            throw new NullPointerException("The SelectionInList must not be null.");

        comboBox.setModel(new ComboBoxAdapter(selectionInList));
    }
    

    /**
     * Binds the given JFormattedTextField to the specified ValueModel.
     * Synchronized the ValueModel's value with the text field's value
     * by means of a PropertyConnector. 
     * 
     * @param textField   the JFormattedTextField that is to be bound
     * @param valueModel  the model that provides the value
     * 
     * @throws NullPointerException if the text field or valueModel 
     *     is <code>null</code>
     */
    public static void bind(JFormattedTextField textField, ValueModel valueModel) {
        if (valueModel == null)
            throw new NullPointerException("The value model must not be null.");
        
        PropertyConnector connector = 
            new PropertyConnector(valueModel, "value", textField, "value");
        connector.updateProperty2();
        
        addComponentPropertyHandler(textField, valueModel);
    }
    
    
    /**
     * Binds the given JLabel to the specified ValueModel.
     * 
     * @param label       a label that shall be bound to the given value model
     * @param valueModel  the model that provides the value
     * 
     * @throws NullPointerException if the label or valueModel is <code>null</code>
     */
    public static void bind(JLabel label, ValueModel valueModel) {
        PropertyConnector connector = 
            new PropertyConnector(valueModel, "value", label, "text");
        connector.updateProperty2();
        
        addComponentPropertyHandler(label, valueModel);
    }
    

    /**
     * Binds a JList to the given SelectionInList using the SelectionInList's 
     * ListModel as list data provider and the SelectionInList's selection 
     * index holder for the selection model. 
     * 
     * @param list             the list to be bound
     * @param selectionInList  provides the list and selection
     * 
     * @throws NullPointerException  if the list or the selectionInList 
     *     is <code>null</code>
     */
    public static void bind(JList list, SelectionInList selectionInList) {
        if (selectionInList == null) 
            throw new NullPointerException("The SelectionInList must not be null.");

        list.setModel(selectionInList);
        list.setSelectionModel(
                new SingleListSelectionAdapter(
                        selectionInList.getSelectionIndexHolder()));
    }
    

    /**
     * Binds a JRadioButton to the given ValueModel and retains the enablement
     * state. The bound radio button is selected if and only if the model's 
     * value equals the specified choice value.<p>
     * 
     * The model is converted to the required interface 
     * ToggleButtonModel using a RadioButtonAdapter.
     * 
     * @param radioButton  the radio button to be bound to the given model
     * @param model        the model that provides the current choice
     * @param choice       this button's value
     * 
     * @throws NullPointerException if the valueModel is <code>null</code>
     */
    public static void bind(JRadioButton radioButton, ValueModel model, Object choice) {
        boolean enabled = radioButton.getModel().isEnabled();
        radioButton.setModel(new RadioButtonAdapter(model, choice));
        radioButton.setEnabled(enabled);
        
        addComponentPropertyHandler(radioButton, model);
    }


    /**
     * Binds a JRadioButtonMenuItem to the given ValueModel and retains
     * the enablement state. The bound menu item is selected if and only if 
     * the model's value equals the specified choice.<p>
     * 
     * <strong>Note:</strong> For users of the JGoodies UIF (user interface
     * framework) the recommended way to create and bind radio button menu items 
     * is the class <code>com.jgoodies.uif.ToggleAction</code>.<p>
     * 
     * The model is converted to the required interface 
     * ToggleButtonModel using a RadioButtonAdapter.
     * 
     * @param radioButtonMenuItem  the radio item to be bound to the given model
     * @param model        the model that provides the current choice
     * @param choice       this button's value
     * 
     * @throws NullPointerException if the valueModel is <code>null</code>
     */
    public static void bind(
            JRadioButtonMenuItem radioButtonMenuItem, 
            ValueModel model, 
            Object choice) {
        boolean enabled = radioButtonMenuItem.getModel().isEnabled();
        radioButtonMenuItem.setModel(new RadioButtonAdapter(model, choice));
        radioButtonMenuItem.setEnabled(enabled);
        
        addComponentPropertyHandler(radioButtonMenuItem, model);
    }


    /**
     * Binds a text area to the given ValueModel. 
     * The model is updated on every character typed.<p>
     * 
     * TODO: Consider changing the semantics to commit on focus lost.
     * This would be consistent with the text component vending factory methods 
     * in the BasicComponentFactory that have no boolean parameter.
     * 
     * @param textArea           the text area to be bound to the value model
     * @param valueModel         the model that provides the text value
     * 
     * @throws NullPointerException if the text component or valueModel 
     *     is <code>null</code>
     */
    public static void bind(
            JTextArea textArea, 
            ValueModel valueModel) {
        bind(textArea, valueModel, false);
    }
    
    
    /**
     * Binds a text area to the given ValueModel. 
     * The model can be updated on focus lost or on every character typed. 
     * The DocumentAdapter used in this binding doesn't filter newlines.
     * 
     * @param textArea           the text area to be bound to the value model
     * @param valueModel         the model that provides the text value
     * @param commitOnFocusLost  true to commit text changes on focus lost,
     *     false to commit text changes on every character typed
     * 
     * @throws NullPointerException if the text component or valueModel 
     *     is <code>null</code>
     */
    public static void bind(
            JTextArea textArea, 
            ValueModel valueModel, 
            boolean commitOnFocusLost) {
        bind(textArea, valueModel, new PlainDocument(), commitOnFocusLost);
    }
    
    
    /**
     * Binds a text area to the given ValueModel. 
     * The model can be updated on focus lost or on every character typed.
     * The DocumentAdapter used in this binding stores the contents 
     * in the given Document and doesn't filter newlines. 
     * 
     * @param textArea           the text area to be bound to the value model
     * @param valueModel         the model that provides the text value
     * @param document           the Document used to store the contents
     * @param commitOnFocusLost  true to commit text changes on focus lost,
     *     false to commit text changes on every character typed
     * 
     * @throws NullPointerException if the component, valueModel, or document 
     *     is <code>null</code>
     */
    public static void bind(
            JTextArea textArea, 
            ValueModel valueModel, 
            Document document,
            boolean commitOnFocusLost) {
        if (valueModel == null)
            throw new NullPointerException("The value model must not be null.");
        
        ValueModel textModel = commitOnFocusLost
            ? createCommitOnFocusLostModel(valueModel, textArea)
            : valueModel;
        textArea.setDocument(new DocumentAdapter(
                    textModel, 
                    document, 
                    false /* filterNewlines */));
        
        addComponentPropertyHandler(textArea, valueModel);
    }
    

    /**
     * Bind a text fields or password field to the given ValueModel. 
     * The model is updated on every character typed.<p>
     * 
     * <strong>Security Note: </strong> If you use this method to bind a 
     * JPasswordField, the field's password will be requested as Strings
     * from the field and will be held as String by the given ValueModel.
     * These password String could potentially be observed in a security fraud.
     * For stronger security it is recommended to request a character array
     * from the JPasswordField and clear the array after use by setting 
     * each character to zero. Method {@link JPasswordField#getPassword()}
     * return's the field's password as a character array.<p>
     * 
     * TODO: Consider changing the semantics to commit on focus lost.
     * This would be consistent with the text component vending factory methods 
     * in the BasicComponentFactory that have no boolean parameter.
     * 
     * @param textField          the text field to be bound to the value model
     * @param valueModel         the model that provides the text value
     * 
     * @throws NullPointerException if the text component or valueModel 
     *     is <code>null</code>
     *     
     * @see JPasswordField#getPassword()
     */
    public static void bind(
            JTextField textField, 
            ValueModel valueModel) {
        bind(textField, valueModel, false);
    }
    
    
    /**
     * Binds a text field or password field to the given ValueModel. 
     * The model can be updated on focus lost or on every character typed.
     * The DocumentAdapter used in this binding filters newlines.<p>
     * 
     * <strong>Security Note: </strong> If you use this method to bind a 
     * JPasswordField, the field's password will be requested as Strings
     * from the field and will be held as String by the given ValueModel.
     * These password String could potentially be observed in a security fraud.
     * For stronger security it is recommended to request a character array
     * from the JPasswordField and clear the array after use by setting 
     * each character to zero. Method {@link JPasswordField#getPassword()}
     * return's the field's password as a character array.
     * 
     * @param textField          the text field to be bound to the value model
     * @param valueModel         the model that provides the text value
     * @param commitOnFocusLost  true to commit text changes on focus lost,
     *     false to commit text changes on every character typed
     * 
     * @throws NullPointerException if the text component or valueModel 
     *     is <code>null</code>
     *     
     * @see JPasswordField#getPassword()
     */
    public static void bind(
            JTextField textField, 
            ValueModel valueModel, 
            boolean commitOnFocusLost) {
        bind(textField, valueModel, new PlainDocument(), commitOnFocusLost);
    }
    
    
    /**
     * Binds a text field or password field to the given ValueModel. 
     * The model can be updated on focus lost or on every character typed.
     * The DocumentAdapter used in this binding stores the contents 
     * in the given Document and filters newlines.<p>
     * 
     * <strong>Security Note: </strong> If you use this method to bind a 
     * JPasswordField, the field's password will be requested as Strings
     * from the field and will be held as String by the given ValueModel.
     * These password String could potentially be observed in a security fraud.
     * For stronger security it is recommended to request a character array
     * from the JPasswordField and clear the array after use by setting 
     * each character to zero. Method {@link JPasswordField#getPassword()}
     * return's the field's password as a character array.
     * 
     * @param textField          the text field to be bound to the value model
     * @param valueModel         the model that provides the text value
     * @param document           the Document used to store the contents
     * @param commitOnFocusLost  true to commit text changes on focus lost,
     *     false to commit text changes on every character typed
     * 
     * @throws NullPointerException if the component, valueModel, or document 
     *     is <code>null</code>
     *     
     * @see JPasswordField#getPassword()
     */
    public static void bind(
            JTextField textField, 
            ValueModel valueModel, 
            Document document,
            boolean commitOnFocusLost) {
        if (valueModel == null)
            throw new NullPointerException("The value model must not be null.");
        
        ValueModel textModel = commitOnFocusLost
            ? createCommitOnFocusLostModel(valueModel, textField)
            : valueModel;
        textField.setDocument(new DocumentAdapter(
                textModel, 
                document, 
                true /* filterNewlines */));
        
        addComponentPropertyHandler(textField, valueModel);
    }
    

    // Updating Component State on ComponentValueModel Changes ****************
    
    /**
     * If the given model is a ComponentValueModel, a component property handler
     * is registered with this model. This handler updates the component state 
     * if the ComponentValueModel indicates a change in one of its properties,
     * for example: <em>visible</em>, <em>enabled</em>, and <em>editable</em>.<p>
     * 
     * Also the ComponentValueModel and the component handler are stored
     * as client properties with the component. This way they can be removed
     * later using <code>#removeComponentPropertyHandler</code>.
     * 
     * @param component   the component where the handler is registered
     * @param valueModel  the model to observe
     * 
     * @see #removeComponentPropertyHandler(JComponent)
     * @see ComponentValueModel
     * 
     * @since 1.1
     */
    public static void addComponentPropertyHandler(JComponent component, ValueModel valueModel) {
        if (!(valueModel instanceof ComponentValueModel)) {
            return;
        }
        ComponentValueModel cvm = (ComponentValueModel) valueModel;
        PropertyChangeListener componentHandler = new ComponentPropertyHandler(component);
        cvm.addPropertyChangeListener(componentHandler);
        component.putClientProperty(COMPONENT_VALUE_MODEL_KEY, cvm);
        component.putClientProperty(COMPONENT_PROPERTY_HANDLER_KEY, componentHandler);
        
        component.setEnabled(cvm.isEnabled());
        component.setVisible(cvm.isVisible());
        if (component instanceof JTextComponent) {
            ((JTextComponent) component).setEditable(cvm.isEditable());
        }
    }
    
    
    /**
     * If the given component holds a ComponentValueModel and 
     * a ComponentPropertyHandler in its client properties,
     * the handler is removed as listener from the model,
     * and the model and handler are removed from the client properties.
     * 
     * @param component
     * 
     * @see #addComponentPropertyHandler(JComponent, ValueModel)
     * @see ComponentValueModel
     * 
     * @since 1.1
     */
    public static void removeComponentPropertyHandler(JComponent component) {
        ComponentValueModel componentValueModel = (ComponentValueModel) component.getClientProperty(
                COMPONENT_VALUE_MODEL_KEY);
        PropertyChangeListener componentHandler = (PropertyChangeListener) component.getClientProperty(
                COMPONENT_PROPERTY_HANDLER_KEY);
        if ((componentValueModel != null) && (componentHandler != null)) {
            componentValueModel.removePropertyChangeListener(componentHandler);
            component.putClientProperty(COMPONENT_VALUE_MODEL_KEY, null);
            component.putClientProperty(COMPONENT_PROPERTY_HANDLER_KEY, null);
        } else if ((componentValueModel == null) && (componentHandler == null)) {
            return;
        } else if (componentValueModel != null) {
            throw new IllegalStateException(
                    "The component has a ComponentValueModel stored, "
                  + "but lacks the ComponentPropertyHandler.");
        } else {
            throw new IllegalStateException(
                    "The component has a ComponentPropertyHandler stored, "
                  + "but lacks the ComponentValueModel.");
        }
    }
    
    
    // Helper Code ************************************************************
    
    /**
     * Creates and returns a ValueModel that commits its value 
     * if the given component looses the focus permanently. 
     * It wraps the underlying ValueModel with a BufferedValueModel 
     * and delays the value commit until this class' shared FOCUS_LOST_TRIGGER
     * commits. This happens, because this class' shared FOCUS_LOST_HANDLER
     * is registered with the specified component. 
     * 
     * @param valueModel  the model that provides the value
     * @param component   the component that looses the focus
     * @return a buffering ValueModel that commits on focus lost
     * 
     * @throws NullPointerException if the value model is <code>null</code>
     */
    private static ValueModel createCommitOnFocusLostModel(
            ValueModel valueModel, 
            Component component) {
        if (valueModel == null)
            throw new NullPointerException("The value model must not be null.");
        
        ValueModel model = new BufferedValueModel(valueModel, FOCUS_LOST_TRIGGER);
        component.addFocusListener(FOCUS_LOST_HANDLER);
        return model;
    }
    

    // Helper Classes *********************************************************
    
    /** 
     * Triggers a commit event on permanent focus lost.
     */
    private static class FocusLostHandler extends FocusAdapter {
        
        /**
         * Triggers a commit event if the focus lost is permanent.
         * 
         * @param evt   the focus lost event
         */
        public void focusLost(FocusEvent evt) {
            if (!evt.isTemporary())
                FOCUS_LOST_TRIGGER.triggerCommit();
        }
    }
    
    
    /**
     * Listens to property changes in a ComponentValueModel and 
     * updates the associated component state.
     * 
     * @see ComponentValueModel
     */
    private static final class ComponentPropertyHandler implements PropertyChangeListener {
        
        private final JComponent component;
        
        private ComponentPropertyHandler(JComponent component) {
            this.component = component;
        }
        
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            ComponentValueModel model = (ComponentValueModel) evt.getSource();
            if (ComponentValueModel.PROPERTYNAME_ENABLED.equals(propertyName)) {
                component.setEnabled(model.isEnabled());
            } else if (ComponentValueModel.PROPERTYNAME_VISIBLE.equals(propertyName)) {
                component.setVisible(model.isVisible());
            } else if (ComponentValueModel.PROPERTYNAME_EDITABLE.equals(propertyName)) {
                ((JTextComponent) component).setEditable(model.isEditable());
            } 
        }
    }
    
    
    // Helper Code for a Weak Trigger *****************************************
    
    /**
     * Unlike the Trigger class, this implementation uses WeakReferences
     * to store value change listeners.
     */
    private static final class WeakTrigger implements ValueModel {

        private final transient WeakPropertyChangeSupport changeSupport;
        
        private Boolean value;
        

        // Instance Creation ******************************************************

        /**
         * Constructs a WeakTrigger set to neutral.
         */
        WeakTrigger() {
            value = null;
            changeSupport = new WeakPropertyChangeSupport(this);
        }
        

        // ValueModel Implementation **********************************************
        
        /**
         * Returns a Boolean that indicates the current trigger state.
         * 
         * @return a Boolean that indicates the current trigger state 
         */
        public Object getValue() {
            return value;
        }

        /**
         * Sets a new Boolean value and rejects all non-Boolean values.
         * Fires no change event if the new value is equal to the 
         * previously set value.<p>
         * 
         * This method is not intended to be used by API users.
         * Instead you should trigger commit and flush events by invoking
         * <code>#triggerCommit</code> or <code>#triggerFlush</code>. 
         * 
         * @param newValue  the Boolean value to be set
         * @throws IllegalArgumentException   if the newValue is not a Boolean
         */
        public void setValue(Object newValue) {
            if ((newValue != null) && !(newValue instanceof Boolean))
                throw new IllegalArgumentException(
                    "Trigger values must be of type Boolean.");
            
            Object oldValue = value;
            value = (Boolean) newValue;
            fireValueChange(oldValue, newValue);
        }


        // Change Management ****************************************************

        /**
         * Registers the given PropertyChangeListener with this model. 
         * The listener will be notified if the value has changed.<p>
         * 
         * The PropertyChangeEvents delivered to the listener have the name 
         * set to "value". In other words, the listeners won't get notified 
         * when a PropertyChangeEvent is fired that has a null object as 
         * the name to indicate an arbitrary set of the event source's 
         * properties have changed.<p>
         * 
         * In the rare case, where you want to notify a PropertyChangeListener
         * even with PropertyChangeEvents that have no property name set,
         * you can register the listener with #addPropertyChangeListener,
         * not #addValueChangeListener.
         *
         * @param listener the listener to add
         * 
         * @see ValueModel
         */
        public void addValueChangeListener(PropertyChangeListener listener) {
            if (listener == null) {
                return;
            }
            changeSupport.addPropertyChangeListener("value", listener);
        }
        

        /**
         * Removes the given PropertyChangeListener from the model.
         *
         * @param listener the listener to remove
         */
        public void removeValueChangeListener(PropertyChangeListener listener) {
            if (listener == null) {
                return;
            }
            changeSupport.removePropertyChangeListener("value", listener);
        }
        
        
        /**
         * Notifies all listeners that have registered interest for
         * notification on this event type.  The event instance 
         * is lazily created using the parameters passed into 
         * the fire method.
         *
         * @param oldValue   the value before the change
         * @param newValue   the value after the change
         * 
         * @see java.beans.PropertyChangeSupport
         */
        private void fireValueChange(Object oldValue, Object newValue) {
            changeSupport.firePropertyChange("value", oldValue, newValue);
        }
        

        // Triggering *************************************************************
        
        /**
         * Triggers a commit event in BufferedValueModels that share this Trigger.
         * Sets the value to <code>Boolean.TRUE</code> and ensures that dependents 
         * are notified about a value change to this new value. Only if necessary 
         * the value is temporarily set to <code>null</code>. This way it minimizes
         * the number of PropertyChangeEvents fired by this Trigger.
         */
        void triggerCommit() {
            if (Boolean.TRUE.equals(getValue()))
                setValue(null);
            setValue(Boolean.TRUE);
        }
        
    }
    
    
    /**
     * Differs from its superclass {@link PropertyChangeSupport} in that it 
     * uses WeakReferences for registering listeners. It wraps registered 
     * PropertyChangeListeners with instances of WeakPropertyChangeListener
     * and cleans up a list of stale references when firing an event.<p>
     * 
     * TODO: Merge this WeakPropertyChangeSupport with the
     * ExtendedPropertyChangeSupport.
     */
    private static final class WeakPropertyChangeSupport extends PropertyChangeSupport {
        
        
        // Instance Creation ******************************************************

        /**
         * Constructs a  WeakPropertyChangeSupport object.
         *
         * @param sourceBean  The bean to be given as the source for any events.
         */
        WeakPropertyChangeSupport(
            Object sourceBean) {
            super(sourceBean);
        }
        
        
        // Managing Property Change Listeners **********************************

        /** {@inheritDoc} */
        public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
            if (listener == null)
                return;
            if (listener instanceof PropertyChangeListenerProxy) {
                PropertyChangeListenerProxy proxy =
                           (PropertyChangeListenerProxy) listener;
                // Call two argument add method.
                addPropertyChangeListener(proxy.getPropertyName(),
                            (PropertyChangeListener) proxy.getListener());
            } else {
                super.addPropertyChangeListener(new WeakPropertyChangeListener(listener));
            }
        }


        /** {@inheritDoc} */
        public synchronized void addPropertyChangeListener(String propertyName,
                PropertyChangeListener listener) {
            if (listener == null)
                return;
            super.addPropertyChangeListener(propertyName,
                    new WeakPropertyChangeListener(propertyName, listener));
        }

        /** {@inheritDoc} */
        public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
            if (listener == null)
                return;
            if (listener instanceof PropertyChangeListenerProxy) {
                PropertyChangeListenerProxy proxy =
                            (PropertyChangeListenerProxy) listener;
                // Call two argument remove method.
                removePropertyChangeListener(proxy.getPropertyName(),
                           (PropertyChangeListener) proxy.getListener());
                return;
            }
            PropertyChangeListener[] listeners = getPropertyChangeListeners();
            WeakPropertyChangeListener wpcl;
            for (int i = listeners.length - 1; i >= 0; i--) {
                if (listeners[i] instanceof PropertyChangeListenerProxy)
                    continue;
                wpcl = (WeakPropertyChangeListener) listeners[i];
                if (wpcl.get() == listener) {
                    // TODO: Should we call here the #clear() method of wpcl???
                    super.removePropertyChangeListener(wpcl);
                    break;
                }
            }
        }

        /** {@inheritDoc} */
        public synchronized void removePropertyChangeListener(String propertyName,
                PropertyChangeListener listener) {
            if (listener == null)
                return;
            PropertyChangeListener[] listeners = getPropertyChangeListeners(propertyName);
            WeakPropertyChangeListener wpcl;
            for (int i = listeners.length - 1; i >= 0; i--) {
                wpcl = (WeakPropertyChangeListener) listeners[i];
                if (wpcl.get() == listener) {
                    // TODO: Should we call here the #clear() method of wpcl???
                    super.removePropertyChangeListener(propertyName, wpcl);
                    break;
                }
            }
        }


        // Firing Events **********************************************************

        /**
         * Fires the specified PropertyChangeEvent to any registered listeners.
         * 
         * @param evt  The PropertyChangeEvent object.
         * 
         * @see PropertyChangeSupport#firePropertyChange(PropertyChangeEvent)
         */
        public void firePropertyChange(PropertyChangeEvent evt) {
            cleanUp();
            super.firePropertyChange(evt);
        }
        

        /**
         * Reports a bound property update to any registered listeners.
         *
         * @param propertyName  The programmatic name of the property
         *      that was changed.
         * @param oldValue  The old value of the property.
         * @param newValue  The new value of the property.
         * 
         * @see PropertyChangeSupport#firePropertyChange(String, Object, Object)
         */
        public void firePropertyChange(
            String propertyName,
            Object oldValue,
            Object newValue) {
            cleanUp();
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
        

        static final ReferenceQueue QUEUE = new ReferenceQueue();

        private static void cleanUp() {
            WeakPropertyChangeListener wpcl;
            while ((wpcl = (WeakPropertyChangeListener) QUEUE.poll()) != null) {
                wpcl.removeListener();
            }
        }

        void removeWeakPropertyChangeListener(WeakPropertyChangeListener l) {
            if (l.propertyName == null) {
                super.removePropertyChangeListener(l);
            } else {
                super.removePropertyChangeListener(l.propertyName, l);
            }
        }

        
        /**
         * Wraps a PropertyChangeListener to make it weak.
         */
        private final class WeakPropertyChangeListener extends WeakReference
                implements PropertyChangeListener {

            final String propertyName;

            private WeakPropertyChangeListener(PropertyChangeListener delegate) {
                this(null, delegate);
            }

            private WeakPropertyChangeListener(String propertyName,
                    PropertyChangeListener delegate) {
                super(delegate, QUEUE);
                this.propertyName = propertyName;
            }

            /** {@inheritDoc} */
            public void propertyChange(PropertyChangeEvent evt) {
                PropertyChangeListener delegate = (PropertyChangeListener) get();
                if (delegate != null) {
                    delegate.propertyChange(evt);
                }
            }

            void removeListener() {
                removeWeakPropertyChangeListener(this);
            }
        }
    }
    
    
}
