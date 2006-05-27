/*
 * Copyright (c) 2003-2006 JGoodies Karsten Lentzsch. All Rights Reserved.
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

package com.jgoodies.validation.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.text.JTextComponent;

import com.jgoodies.validation.Severity;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.util.ValidationUtils;

/**
 * Consists exclusively of static methods that provide convenience behavior for
 * operating on components that present validation data. Methods that access
 * component state utilize the {@link javax.swing.JComponent} client property
 * mechanism as a backing store.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.5 $
 * 
 * @see com.jgoodies.validation.ValidationMessage
 * @see com.jgoodies.validation.ValidationMessage#key()
 * @see com.jgoodies.validation.ValidationResult
 * @see com.jgoodies.validation.ValidationResult#subResult(Object)
 * @see com.jgoodies.validation.ValidationResult#keyMap()
 */
public final class ValidationComponentUtils {

    // Colors *****************************************************************

    private static final Color MANDATORY_FOREGROUND = new Color(70, 70, 210);

    private static final Color MANDATORY_BACKGROUND = new Color(235, 235, 255);

    private static final Color ERROR_BACKGROUND = new Color(255, 215, 215);

    private static final Color WARNING_BACKGROUND = new Color(255, 235, 205);

    
    // Client Property Keys **************************************************

    /**
     * The JComponent client property key for the mandatory property 
     * that indicates whether a component's content is mandatory or optional.
     * 
     * @see #isMandatory(JComponent)
     * @see #isMandatoryAndBlank(JComponent)
     * @see #setMandatory(JComponent, boolean)
     */
    private static final String MANDATORY_KEY = "validation.isMandatory";

    /**
     * The JComponent client property key used to associate 
     * a component with a set of ValidationMessages.
     * 
     * @see #getMessageKey(JComponent)
     * @see #setMessageKey(JComponent, Object)
     * @see com.jgoodies.validation.ValidationMessage#key()
     * @see ValidationResult#subResult(Object)
     * @see ValidationResult#keyMap()
     */
    private static final String MESSAGE_KEY = "validation.messageKey";

    /**
     * The JComponent client property key for the input hint text.
     * The text stored under this key is intended to be displayed if and only if
     * the component has the focus.
     * 
     * @see #getInputHint(JComponent)
     * @see #setInputHint(JComponent, Object)
     */
    private static final String INPUT_HINT_KEY = "validation.inputHint";

    /**
     * The JComponent client property key for the severity property.
     * Once a component's severity state has been set by the method
     * {@link #updateComponentTreeSeverity(Container, ValidationResult)}
     * it can be used to display validation feedback, such as background
     * changes, overlay information, etc. See for example 
     * {@link #updateComponentTreeSeverityBackground(Container, ValidationResult)}.
     * 
     * @see #getSeverity(JComponent)
     * @see #setSeverity(JComponent, Severity)
     * @see #updateComponentTreeSeverity(Container, ValidationResult)   
     * @see #updateComponentTreeSeverityBackground(Container, ValidationResult)
     */
    private static final String SEVERITY_KEY = "validation.severity";

    /**
     * The JComponent client property key used to store a component's 
     * original background color. The stored background can be restored later.
     * 
     * @see #getStoredBackground(JTextComponent)
     * @see #restoreBackground(JTextComponent)
     * @see #ensureCustomBackgroundStored(JTextComponent)
     * @see #setMandatoryBackground(JTextComponent)
     */
    private static final String STORED_BACKGROUND_KEY = "validation.storedBackground";

    /**
     * Holds a cached Border that is used to indicate mandatory text components.
     * It will be lazily created in method {@link #getMandatoryBorder()}.
     * 
     * @see #getMandatoryBorder()
     * @see #setMandatoryBorder(JTextComponent)
     */
    private static Border mandatoryBorder;
    
    
    // A Map that holds resuable prototype text components ********************
    
    /**
     * Maps text component classes to prototype instances of such a class.
     * Used to get the default background color of these component types.
     * 
     * @see #getDefaultBackground(JTextComponent)
     * @see #getPrototypeFor(Class)
     */
    private static final Map PROTOTYPE_COMPONENTS = new HashMap();
    

    // Instance creation ******************************************************

    private ValidationComponentUtils() {
        // Override default constructor; prevents instantiation.
    }
    

    // Accessing Validation Properties ****************************************

    /**
     * Returns if the component has been marked as mandatory.
     * 
     * @param comp    the component to be checked
     * @return true if the component has been marked as mandatory
     * 
     * @see #isMandatoryAndBlank(JComponent)
     * @see #setMandatory(JComponent, boolean)
     * @see #setMandatoryBackground(JTextComponent)
     * @see #setMandatoryBorder(JTextComponent)
     */
    public static boolean isMandatory(JComponent comp) {
        return Boolean.TRUE.equals(comp.getClientProperty(MANDATORY_KEY));
    }

    /**
     * Returns if the component is a {@link JTextComponent} with blank content
     * and has been marked as mandatory.
     * 
     * @param comp  the component to be checked
     * @return true if the component's has a blank content and has been marked 
     *     as mandatory
     * 
     * @see #isMandatory(JComponent)
     * @see #setMandatory(JComponent, boolean)
     * @see #setMandatoryBackground(JTextComponent)
     * @see #setMandatoryBorder(JTextComponent)
     */
    public static boolean isMandatoryAndBlank(JComponent comp) {
        if (!(comp instanceof JTextComponent))
            return false;
        JTextComponent textComponent = (JTextComponent) comp;
        return isMandatory(textComponent)
                && ValidationUtils.isBlank(textComponent.getText());
    }

    /**
     * Marks the given component as mandatory or optional. 
     * The value will be stored as a client property value.
     * 
     * @param comp        the component to be marked
     * @param mandatory   true for mandatory, false for optional
     * 
     * @see #isMandatory(JComponent)
     * @see #isMandatoryAndBlank(JComponent)
     * @see #setMandatoryBackground(JTextComponent)
     * @see #setMandatoryBorder(JTextComponent)
     */
    public static void setMandatory(JComponent comp, boolean mandatory) {
        boolean oldMandatory = isMandatory(comp);
        if (oldMandatory != mandatory)
            comp.putClientProperty(MANDATORY_KEY, Boolean.valueOf(mandatory));
    }
    

    /**
     * Returns the component's {@link Severity} if it has been set before.
     * Useful for validation-aware containers that render the component's 
     * validation state. 
     * 
     * @param comp   the component to be read
     * @return the component's <code>Severity</code> as set before
     * 
     * @see #setSeverity(JComponent, Severity)
     * @see #updateComponentTreeSeverity(Container, ValidationResult)
     * @see #updateComponentTreeSeverityBackground(Container, ValidationResult)
     */
    public static Severity getSeverity(JComponent comp) {
        return (Severity) comp.getClientProperty(SEVERITY_KEY);
    }

    /**
     * Marks the given component with the specified severity.
     * The severity will be stored as a client property value.
     * Useful for validation-aware containers that render the component's
     * validation state once it has been set.
     * 
     * @param comp      the component that shall be marked
     * @param severity  the component's severity
     * 
     * @see #getSeverity(JComponent)
     * @see #updateComponentTreeSeverity(Container, ValidationResult)
     * @see #updateComponentTreeSeverityBackground(Container, ValidationResult)
     */
    public static void setSeverity(JComponent comp, Severity severity) {
        comp.putClientProperty(SEVERITY_KEY, severity);
    }
    
    
    /**
     * Returns the message key that has been set to associate the given 
     * component with a set of ValidationMessages. 
     * 
     * @param comp  the component to be requested
     * @return the component's validation association key
     * 
     * @see #setMessageKey(JComponent, Object)
     * @see com.jgoodies.validation.ValidationMessage
     * @see com.jgoodies.validation.ValidationMessage#key()
     * @see ValidationResult#subResult(Object)
     * @see ValidationResult#keyMap()
     */
    public static Object getMessageKey(JComponent comp) {
        return comp.getClientProperty(MESSAGE_KEY);
    }

    /**
     * Associates the given component with the specified message key.
     * That in turn will associate the component with all ValidationMessages
     * that share this key. The latter can be checked by comparing this key 
     * with the key provided by a ValidationMessage.
     *  
     * @param comp         the component that shall be associated with the key
     * @param messageKey   the key to be set
     * 
     * @see #getMessageKey(JComponent)
     * @see com.jgoodies.validation.ValidationMessage
     * @see com.jgoodies.validation.ValidationMessage#key()
     * @see ValidationResult#subResult(Object)
     * @see ValidationResult#keyMap()
     */
    public static void setMessageKey(JComponent comp, Object messageKey) {
        comp.putClientProperty(MESSAGE_KEY, messageKey);
    }
    

    /**
     * Returns the component's input hint that is stored in a client property. 
     * Useful to indicate the format of valid data to the user. 
     * The input hint object can be a plain <code>String</code> or a 
     * compound object, for example that is able to localize the input hint 
     * for the active {@link java.util.Locale}.<p>
     * 
     * To make use of this information an editor should register a
     * listener with the focus management. Whenever the focused component
     * changes, the mechanism can request the input hint for the focus owner
     * using this service and can display the result hint in the user interface.
     *  
     * @param comp    the component to be requested
     * @return the component's input hint
     * 
     * @see #setInputHint(JComponent, Object)
     */
    public static Object getInputHint(JComponent comp) {
        return comp.getClientProperty(INPUT_HINT_KEY);
    }

    /**
     * Sets the input hint for the given component. This hint can be later
     * retrieved to indicate to the user the format of valid data for the
     * focused component.
     * 
     * @param comp    the component to set a hint for
     * @param hint    the input hint to be associated with the component
     * 
     * @see #getInputHint(JComponent)
     */
    public static void setInputHint(JComponent comp, Object hint) {
        comp.putClientProperty(INPUT_HINT_KEY, hint);
    }

    
    /**
     * Checks and answers if the specified component is associated with an
     * error message in the given validation result. As a prerequisite, 
     * the component must have an <em>association key</em> set. That can
     * be done using {@link #setMessageKey(JComponent, Object)}.<p>
     * 
     * <strong>Note:</strong> This method may become slow if invoked for larger
     * validation results <em>and</em> multiple components. In this case,
     * it is recommended to use {@link ValidationResult#keyMap()} instead. 
     * The latter iterates once over the validation result and can be used later
     * to request the severity for multiple components in almost linear time.
     * 
     * @param comp     used to get the association key from
     * @param result   used to lookup the validation messages from 
     * @return true if the given component is associated with an error message
     * @throws NullPointerException  if the component or validation result
     *     is <code>null</code>
     * 
     * @see #hasWarning(JComponent, ValidationResult)
     * @see #getMessageKey(JComponent)
     */
    public static boolean hasError(JComponent comp, ValidationResult result) {
        return result.subResult(getMessageKey(comp)).hasErrors();
    }

    /**
     * Checks and answers if the specified component is associated with a
     * warning message in the given validation result. As a prerequisite, 
     * the component must have an <em>association key</em> set. That can
     * be done using {@link #setMessageKey(JComponent, Object)}.<p>
     * 
     * <strong>Note:</strong> This method may become slow if invoked for larger
     * validation results <em>and</em> multiple components. In this case,
     * it is recommended to use {@link ValidationResult#keyMap()} instead. 
     * The latter iterates once over the validation result and can be used later
     * to request the severity for multiple components in almost linear time.
     * 
     * @param comp     used to get the association key from
     * @param result   used to lookup the validation messages from 
     * @return true if the given component is associated with a warning message
     * @throws NullPointerException  if the component or validation result
     *     is <code>null</code>
     * 
     * @see #hasError(JComponent, ValidationResult)
     * @see #getMessageKey(JComponent)
     */
    public static boolean hasWarning(JComponent comp, ValidationResult result) {
        return result.subResult(getMessageKey(comp)).hasWarnings();
    }

    
    /**
     * Returns a default background color that can be used as the component 
     * background for components with mandatory content. Typically this
     * color will be used with instances of {@link JTextComponent}.<p>
     * 
     * <strong>Note:</strong> The component background colors are managed
     * by the look&amp;feel implementation. Many l&amp;fs will honor a
     * custom background color. However, some l&amp;fs may ignore custom
     * background colors. It is recommended to check the
     * appearance in all l&amp;fs available in an application.
     * 
     * @return a background color useful for components with mandatory content
     * 
     * @see #getMandatoryForeground()
     */
    public static Color getMandatoryBackground() {
        return MANDATORY_BACKGROUND;
    }

    /**
     * Returns a default foreground color that can be used as the component 
     * foreground for components with mandatory content. Typically this
     * color will be used with instances of {@link JTextComponent}.<p>
     * 
     * <strong>Note:</strong> The component foreground and border colors are 
     * managed by the look&amp;feel implementation. Many l&amp;fs will honor a
     * custom foreground color and custom border configuration. However, some 
     * l&amp;fs may ignore these custom settings. It is recommended to check 
     * the appearance in all l&amp;fs available in an application.
     * 
     * @return a foreground color useful for components with mandatory content
     * 
     * @see #getMandatoryBackground()
     * @see #getMandatoryBorder()
     */
    public static Color getMandatoryForeground() {
        return MANDATORY_FOREGROUND;
    }

    
    /**
     * Sets the text component's background to a color that shall indicate
     * that the component's content is mandatory.<p>
     * 
     * <strong>Note:</strong> The component background colors are 
     * managed by the look&amp;feel implementation. Many l&amp;fs will honor a
     * custom foreground color and custom border configuration. However, some 
     * l&amp;fs may ignore these custom settings. It is recommended to check
     * the appearance in all l&amp;fs available in an application.
     * 
     * @param comp   the text component that shall get a new background
     * 
     * @see #setMandatoryBorder(JTextComponent)
     * @see #setErrorBackground(JTextComponent)
     * @see #setWarningBackground(JTextComponent)
     */
    public static void setMandatoryBackground(JTextComponent comp) {
        comp.setBackground(MANDATORY_BACKGROUND);
    }
    
    
    /**
     * Returns the error background color used to mark components
     * that have an associated validation error.
     * 
     * @return the error background color
     * 
     * @see #getWarningBackground()
     * @see #setErrorBackground(JTextComponent)
     * @see #updateComponentTreeSeverityBackground(Container, ValidationResult)
     * 
     * @since 1.0.2
     */
    public static Color getErrorBackground() {
        return ERROR_BACKGROUND;
    }

    
    /**
     * Sets the text component's background to a color that shall indicate
     * that the component's content has is invalid with error severity.<p>
     * 
     * <strong>Note:</strong> The component background colors are 
     * managed by the look&amp;feel implementation. Many l&amp;fs will honor a
     * custom foreground color and custom border configuration. However, some 
     * l&amp;fs may ignore these custom settings. It is recommended to check
     * the appearance in all l&amp;fs available in an application.
     * 
     * @param comp   the text component that shall get a new background
     * 
     * @see #setMandatoryBackground(JTextComponent)
     * @see #setWarningBackground(JTextComponent)
     */
    public static void setErrorBackground(JTextComponent comp) {
        comp.setBackground(ERROR_BACKGROUND);
    }


    /**
     * Returns the warning background color used to mark components
     * that have an associated validation warning.
     * 
     * @return the warning background color
     * 
     * @see #getErrorBackground()
     * @see #setWarningBackground(JTextComponent)
     * @see #updateComponentTreeSeverityBackground(Container, ValidationResult)
     * 
     * @since 1.0.2
     */
    public static Color getWarningBackground() {
        return WARNING_BACKGROUND;
    }

    
    /**
     * Sets the text component's background to a color that shall indicate
     * that the component's content is invalid with warning severity.<p>
     * 
     * <strong>Note:</strong> The component background colors are 
     * managed by the look&amp;feel implementation. Many l&amp;fs will honor a
     * custom foreground color and custom border configuration. However, some 
     * l&amp;fs may ignore these custom settings. It is recommended to check
     * the appearance in all l&amp;fs available in an application.
     * 
     * @param comp   the text component that shall get a new background
     * 
     * @see #setMandatoryBackground(JTextComponent)
     * @see #setErrorBackground(JTextComponent)
     */
    public static void setWarningBackground(JTextComponent comp) {
        comp.setBackground(WARNING_BACKGROUND);
    }
    
    
    // Managing Borders *******************************************************

    /**
     * Sets the text component's border to use a new border that shall indicate
     * that the component's content is mandatory.<p>
     * 
     * <strong>Note:</strong> The component foreground and border colors are 
     * managed by the look&amp;feel implementation. Many l&amp;fs will honor a
     * custom foreground color and custom border configuration. However, some 
     * l&amp;fs may ignore these custom settings. It is recommended to check
     * the appearance in all l&amp;fs available in an application.
     * 
     * @param comp   the component that gets a new border
     * 
     * @see #setMandatoryBackground(JTextComponent)
     * @see #getMandatoryBorder()
     */
    public static void setMandatoryBorder(JTextComponent comp) {
        Container parent = comp.getParent();
        if (parent instanceof JViewport) {
            Container grandpa = parent.getParent();
            if (grandpa instanceof JScrollPane) {
                ((JScrollPane) grandpa).setBorder(getMandatoryBorder());
                return;
            }
        }
        comp.setBorder(getMandatoryBorder());
    }

    /**
     * Lazily creates and returns a {@link Border} instance that is used
     * to indicate that a component's content is mandatory.
     * 
     * @return a <code>Border</code> that is used to indicate that 
     *     a component's content is mandatory
     */
    public static Border getMandatoryBorder() {
        if (mandatoryBorder == null) {
            mandatoryBorder = new CompoundBorder(new LineBorder(
                    getMandatoryForeground()),
                    new BasicBorders.MarginBorder());
        }
        return mandatoryBorder;
    }
    
    
    // Predefined Component Tree Updates **************************************
    
    /**
     * Traverses a component tree and sets mandatory backgrounds 
     * to text components that have been marked as mandatory 
     * with {@link #setMandatory(JComponent, boolean)} before.
     * The iteration starts at the given container.
     * 
     * @param container   the component tree root
     * 
     * @see #setMandatory(JComponent, boolean)
     * @see #setMandatoryBackground(JTextComponent)
     */
    public static void updateComponentTreeMandatoryBackground(Container container) {
        visitComponentTree(container, null, new MandatoryBackgroundVisitor());
    }


    /**
     * Traverses a component tree and sets mandatory backgrounds 
     * to text components that have blank content and have been marked 
     * as mandatory with {@link #setMandatory(JComponent, boolean)} before.
     * The iteration starts at the given container.
     * 
     * @param container   the component tree root
     * 
     * @see #setMandatory(JComponent, boolean)
     * @see #setMandatoryBackground(JTextComponent)
     */
    public static void updateComponentTreeMandatoryAndBlankBackground(Container container) {
        visitComponentTree(container, null, new MandatoryAndBlankBackgroundVisitor());
    }


    /**
     * Traverses a component tree and sets mandatory borders 
     * to text components that have been marked as mandatory 
     * with {@link #setMandatory(JComponent, boolean)} before.
     * The iteration starts at the given container.
     * 
     * @param container   the component tree root
     * 
     * @see #setMandatory(JComponent, boolean)
     * @see #setMandatoryBorder(JTextComponent)
     */
    public static void updateComponentTreeMandatoryBorder(Container container) {
        visitComponentTree(container, null, new MandatoryBorderVisitor());
    }


    /**
     * Traverses a component tree and sets the text component backgrounds 
     * according to the severity of an associated validation result - if any.
     * The iteration starts at the given container.<p>
     * 
     * The message keys used to associate components with validation messages
     * should be set using {@link #setMessageKey(JComponent, Object)} before
     * you call this method.
     * 
     * @param container   the component tree root
     * @param result      the validation result used to lookup the severities
     * 
     * @see #setMandatory(JComponent, boolean)
     * @see #setMessageKey(JComponent, Object)
     * @see #setMandatoryBackground(JTextComponent)
     * @see #setErrorBackground(JTextComponent)
     * @see #setWarningBackground(JTextComponent)
     * 
     * @since 1.0.2
     */
    public static void updateComponentTreeSeverityBackground(
                                                               Container container, ValidationResult result) {
        visitComponentTree(container, result.keyMap(), new SeverityBackgroundVisitor());
    }


    /**
     * Traverses a component tree and sets the severity for all text components. 
     * The iteration starts at the given container. If a validation result is
     * associated with a component, the result's severity is set. Otherwise
     * the severity is set to <code>null</code>. The severity is set using
     * {@link #setSeverity(JComponent, Severity)}.<p>
     * 
     * Before you use this method, associate text component with validation 
     * messages using {@link #setMessageKey(JComponent, Object)}.
     * 
     * @param container   the component tree root
     * @param result      the validation result that provides the associated messages
     * 
     * @see #setSeverity(JComponent, Severity)
     */
    public static void updateComponentTreeSeverity(Container container,
                                                   ValidationResult result) {
        visitComponentTree(container, result.keyMap(), new SeverityVisitor());
    }


    // Visiting Text Components in a Component Tree ***************************
    
    /**
     * Traverses the component tree starting at the given container and invokes
     * the given visitor's <code>#visit</code> method on each instance of 
     * {@link JTextComponent}. Useful to perform custom component tree updates
     * that are not already provided by the <code>#updateComponentTreeXXX</code>
     * methods.<p>
     * 
     * The arguments passed to the #visit method are the visited component and 
     * its associated validation subresult. This subresult is requested from 
     * the specified <code>keyMap</code> using the component's message key.<p>
     * 
     * Before you use this method, associate text component with validation 
     * messages using {@link #setMessageKey(JComponent, Object)}.
     * 
     * @param container   the component tree root
     * @param keyMap      maps messages keys to associated validation results
     * @param visitor     the visitor that is applied to all text components
     * 
     * @see #setMessageKey(JComponent, Object)
     * @see Visitor 
     */
    public static void visitComponentTree(Container container, Map keyMap, Visitor visitor) {
        int componentCount = container.getComponentCount();
        for (int i = 0; i < componentCount; i++) {
            Component child = container.getComponent(i);
            if (child instanceof JTextComponent) {
                JComponent component = (JComponent) child;
                visitor.visit(component, keyMap);
            } else if (child instanceof Container) {
                visitComponentTree((Container) child, keyMap, visitor);
            }
        }
    }
    

    // Helper Code ************************************************************
    
    
    /**
     * Returns the ValidationResult associated with the given component
     * using the specified validation result key map, 
     * or <code>null</code> if the component has no message key set, 
     * or <code>ValidationResult.EMPTY</code> if the key map contains
     * no result for the component.
     * 
     * @param comp     the component may be marked with a validation message key
     * @param keyMap   maps validation message keys to ValidationResults
     * @return the ValidationResult associated with the given component
     *     as provided by the specified validation key map
     *     or <code>null</code> if the component has no message key set,
     *     or <code>ValidationResult.EMPTY</code> if no result is associated 
     *     with the component
     */
    private static ValidationResult getAssociatedResult(JComponent comp, Map keyMap) {
        Object messageKey = getMessageKey(comp);
        if ((messageKey == null) || (keyMap == null)) {
            return null;
        }
        ValidationResult result = (ValidationResult) keyMap.get(messageKey);
        return result == null 
            ? ValidationResult.EMPTY 
            : result;
    }
    
    
    /**
     * Returns a default background color that is requested from an instance
     * of a prototype component of the same type as the given component.
     * If such a component cannot be created, a JTextField is used.
     * The prototype's enabled and editable state is then set to the state
     * of the given component. Finally the prototype's background is returned.
     * 
     * @param component   the component to get the default background for
     * @return the background color of a prototype text component that has
     *     the same state as the given component
     * 
     * @see #restoreBackground(JTextComponent)
     */
    private static Color getDefaultBackground(JTextComponent component) {
        JTextComponent prototype = getPrototypeFor(component.getClass());
        prototype.setEnabled(component.isEnabled());
        prototype.setEditable(component.isEditable());
        return prototype.getBackground();
    }
    
    
    /**
     * Lazily creates and returns a prototype text component instance
     * of the given text component class. First ensures that the Look&amp;Feel
     * change handler is registered that clears the prototype map if
     * the L&amp;f changes.
     * 
     * @param prototypeClass   the class of the prototype to be returned
     * @return the lazily created prototype component
     */
    private static JTextComponent getPrototypeFor(Class prototypeClass) {
        ensureLookAndFeelChangeHandlerRegistered();
        JTextComponent prototype = 
            (JTextComponent) PROTOTYPE_COMPONENTS.get(prototypeClass);
        if (prototype == null) { 
            try {
                prototype = (JTextComponent) prototypeClass.newInstance();
            } catch (Exception e) {
                prototype = new JTextField();
            }
            PROTOTYPE_COMPONENTS.put(prototypeClass, prototype);
        }
        return prototype;
    }

    /**
     * Returns the background color that has been previously stored for
     * the given component, or <code>null</code> if none.
     * 
     * @param comp  the component to be requested 
     * @return the background color that has been previously stored for
     *     the given component, or <code>null</code> if none.
     * 
     * @see #ensureCustomBackgroundStored(JTextComponent)
     * @see #restoreBackground(JTextComponent)
     */
    private static Color getStoredBackground(JTextComponent comp) {
        return (Color) comp.getClientProperty(STORED_BACKGROUND_KEY);
    }

    /**
     * Ensures that a text component's custom background - if any - 
     * is stored as a client property. Used to store the background once only. 
     * 
     * @param comp  the component to be requested
     * 
     * @see #getStoredBackground(JTextComponent)
     * @see #restoreBackground(JTextComponent)
     */
    private static void ensureCustomBackgroundStored(JTextComponent comp) {
        if (getStoredBackground(comp) != null)
            return;
        Color background = comp.getBackground();
        if (   (background == null) 
            || (background instanceof UIResource)
            || (background == WARNING_BACKGROUND)
            || (background == ERROR_BACKGROUND))
            return;
        comp.putClientProperty(STORED_BACKGROUND_KEY, background);
    }

    /**
     * Looks up and restores the text component's previously stored (original)
     * background color. 
     * 
     * @param comp  the component that shall get its original background color
     * 
     * @see #getStoredBackground(JTextComponent)
     * @see #ensureCustomBackgroundStored(JTextComponent)
     */
    private static void restoreBackground(JTextComponent comp) {
        Color storedBackground = getStoredBackground(comp);
        comp.setBackground(storedBackground == null
                ? getDefaultBackground(comp)
                : storedBackground);
    }


    // Visitor Definition and Predefined Visitor Implementations **************
    
    /**
     * Describes visitors that visit a component tree.
     * Visitor implementations are used to mark components, 
     * to change component background, to associate components 
     * with additional information; things that are not already
     * provided by the <code>#updateComponentTreeXXX</code> methods
     * and this class' predefined Visitor implementations.
     */
    public static interface Visitor {
        
        /**
         * Visits the given component using the specified key map, that maps
         * message keys to associated validation subresults.
         * Typically an implementation will operate on the component state.
         * 
         * @param component the component to be visited
         * @param keyMap    maps messages keys to associated validation results
         */
        void visit(JComponent component, Map keyMap);
    }
    
    
    /**
     * A validation visitor that sets the background color of JTextComponents
     * to mark mandatory components.
     */
    private static final class MandatoryBackgroundVisitor implements Visitor {
        
        /**
         * Sets the mandatory background to text components that have been marked
         * as mandatory.
         * 
         * @param component   the component to be visited
         * @param keyMap      ignored
         */
        public void visit(JComponent component, Map keyMap) {
            if ((component instanceof JTextComponent) && isMandatory(component)) {
                setMandatoryBackground((JTextComponent) component);
            }
        }

    }
    
    
    /**
     * A validation visitor that sets the background color of JTextComponents
     * to indicate if mandatory components have a blank text or not.
     */
    private static final class MandatoryAndBlankBackgroundVisitor implements Visitor {
        
        /**
         * Sets the mandatory background to text components that have been marked
         * as mandatory if the content is blank, otherwise the original 
         * background is restored.
         * 
         * @param component   the component to be visited
         * @param keyMap      ignored
         */
        public void visit(JComponent component, Map keyMap) {
            JTextComponent textChild = (JTextComponent) component;
            if (isMandatoryAndBlank(textChild))
                setMandatoryBackground(textChild);
            else
                restoreBackground(textChild);
        }
    }
    
    
    /**
     * A validation visitor that sets a mandatory border for JTextComponents
     * that have been marked as mandatory.
     */
    private static final class MandatoryBorderVisitor implements Visitor {
        
        /**
         * Sets the mandatory border to text components that have been marked
         * as mandatory.
         * 
         * @param component   the component to be visited
         * @param keyMap      ignored
         */
        public void visit(JComponent component, Map keyMap) {
            if ((component instanceof JTextComponent) && isMandatory(component)) {
                setMandatoryBorder((JTextComponent) component);
            }
        }
    }
    

    /**
     * A validation visitor that sets the background color of JTextComponents
     * according to the severity of an associated validation result - if any.
     */
    private static final class SeverityBackgroundVisitor implements Visitor {
        
        /**
         * Sets the component background according to the associated 
         * validation result: default, error, warning.
         * 
         * @param component  the component to be visited
         * @param keyMap     maps messages keys to associated validation results
         */
        public void visit(JComponent component, Map keyMap) {
            Object messageKey = getMessageKey(component);
            if (messageKey == null) {
                return;
            }
            JTextComponent textChild = (JTextComponent) component;
            ensureCustomBackgroundStored(textChild);
            ValidationResult result = getAssociatedResult(component, keyMap);
            if ((result == null) || result.isEmpty()) {
                restoreBackground(textChild);
            } else if (result.hasErrors()) {
                setErrorBackground(textChild);
            } else if (result.hasWarnings()) {
                setWarningBackground(textChild);
            }
        }
    }
    
    
    /**
     * A validation visitor that sets each component's severity
     * according to its associated validation result or to <code>null</code>
     * if no message key is set for the component.
     */
    private static final class SeverityVisitor implements Visitor {
        
        /**
         * Sets the component's severity according to its associated 
         * validation result, or <code>null</code> if the component 
         * has no message key set.
         * 
         * @param component  the component to be visited
         * @param keyMap     maps messages keys to associated validation results
         */
        public void visit(JComponent component, Map keyMap) {
            ValidationResult result = getAssociatedResult(component, keyMap);
            Severity severity = result == null
                ? null
                : result.getSeverity();
            setSeverity(component, severity);    
        }
    }
    
    
    // Handling L&f Changes ***************************************************
    
    /**
     * Describes whether the <code>LookAndFeelChangeHandler</code>
     * has been registered with the <code>UIManager</code> or not.
     * It is registered lazily when the first prototype component
     * is requested in <code>#getPrototypeFor(Class)</code>.
     */
    private static boolean lafChangeHandlerRegistered = false;
    
    private static synchronized void ensureLookAndFeelChangeHandlerRegistered() {
        if (!lafChangeHandlerRegistered) {
            UIManager.addPropertyChangeListener(new LookAndFeelChangeHandler());
            lafChangeHandlerRegistered = true;
        }
    }
    
    /**
     * Clears the cached prototype components when the L&amp; changes.
     */
    private static final class LookAndFeelChangeHandler implements PropertyChangeListener {
        
        /**
         * Clears the cached prototype components, if the UIManager has fired 
         * any property change event. Since we need to handle look&amp;feel 
         * changes only, we check the event's property name to be 
         * "lookAndFeel" or <code>null</code>. The check for null is necessary
         * to handle the special event where property name, old and new value
         * are all <code>null</code> to indicate that multiple properties
         * have changed.
         * 
         * @param evt  describes the property change
         */
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if ((propertyName == null) || propertyName.equals("lookAndFeel")) {
                PROTOTYPE_COMPONENTS.clear();
            }
        }
    }
    

}
