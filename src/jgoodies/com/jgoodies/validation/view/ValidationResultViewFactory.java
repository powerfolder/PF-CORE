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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import com.jgoodies.validation.Severity;
import com.jgoodies.validation.ValidationMessage;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;

/**
 * A factory class that vends views that present the state and contents
 * of {@link ValidationResult}s. The validation views are composed from
 * user interface components like text areas, lists, labels, etc.
 * Most factory methods require a {@link ValidationResultModel} that 
 * notifies the view about changes in an underlying ValidationResult.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.3 $
 * 
 * @see     ValidationResult
 * @see     ValidationMessage
 */
public final class ValidationResultViewFactory {

    private static final Color DEFAULT_REPORT_BACKGROUND = 
        new Color(255, 255, 210);

    private ValidationResultViewFactory() {
        // Override default constructor; prevents instantiation.
    }

    
    // Creating Validation Views **********************************************
    
    /**
     * Creates and returns an icon label that indicates the validation severity.
     * A handler updates the label's visibility and icon each time the severity
     * of the given validation result model changes.
     * 
     * @param model   the model that provides the observable validation result
     * @return a label with an icon that presents the validation severity
     */
    public static JLabel createReportIconLabel(ValidationResultModel model) {
        JLabel label = new JLabel();
        LabelIconChangeHandler.updateVisibilityAndIcon(label, model.getSeverity());
        model.addPropertyChangeListener(
                ValidationResultModel.PROPERTYNAME_SEVERITY, 
                new LabelIconChangeHandler(label));
        return label;
    }
    
    
    /**
     * Creates and returns a label with icon and text that indicates 
     * the validation state and displays the first message text.
     * 
     * @param model   the model that provides the observable validation result
     * @return a label with text and icon that shows the validation severity
     *     and the first validation message text
     */
    public static JLabel createReportIconAndTextLabel(ValidationResultModel model) {
        JLabel label = createReportIconLabel(model);
        LabelTextChangeHandler.updateText(label, model.getResult());
        model.addPropertyChangeListener(
                ValidationResultModel.PROPERTYNAME_RESULT, 
                new LabelTextChangeHandler(label));
        return label;
    }
    
    
    /**
     * Creates and returns a transparent panel that consists of a report label 
     * and a transparent report text area. Both components are bound to
     * the given ValidationResultModel: the text content presents
     * the validation result message text and the whole panel is visible
     * if and only if the model has messages.
     * 
     * @param model   the model that provides the observable validation result
     * @return a panel with icon and text area bound to a validation result
     */
    public static JComponent createReportIconAndTextPane(ValidationResultModel model) {
        JLabel label   = createReportIconLabel(model);
        JTextArea area = createReportTextArea(model);
        area.setOpaque(false);
        
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel reportPane = new JPanel(gbl);
        reportPane.setOpaque(false);
        reportPane.setVisible(model.hasMessages());
        
        gbc.gridwidth = 1;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 0, 0, 0);
        reportPane.add(label, gbc);
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 4, 0, 0);
        gbc.weightx   = 1.0;
        gbc.weighty   = 1.0;
        reportPane.add(area, gbc);

        reportPane.setFocusable(false);
        model.addPropertyChangeListener(
                ValidationResultModel.PROPERTYNAME_MESSAGES, 
                new MessageStateChangeHandler(reportPane));
        
        return reportPane;
    }
    

    /**
     * Creates and returns a list that presents validation messages. 
     * The list content is bound to the given {@link ValidationResultModel}
     * using a {@link ValidationResultListAdapter}.
     * 
     * @param model   the model that provides the observable validation result
     * @return a <code>JList</code> that shows validation messages
     */
    public static JComponent createReportList(ValidationResultModel model) {
        return createReportList(model, DEFAULT_REPORT_BACKGROUND);
    }
    

    /**
     * Creates and returns a list wrapped in a scollpane that presents 
     * validation messages. The list content is bound to the given 
     * {@link ValidationResultModel} using a {@link ValidationResultListAdapter}.
     * 
     * @param model   the model that provides the observable validation result
     * @param backgroundColor   the color used to paint the area's background
     * @return a <code>JList</code> that shows validation messages
     */
    public static JComponent createReportList(ValidationResultModel model,
                                              Color backgroundColor) {
        JList list = new JList();
        list.setFocusable(false);
        list.setBackground(backgroundColor);
        list.setCellRenderer(new BasicValidationMessageCellRenderer());
        list.setModel(new ValidationResultListAdapter(model));
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setVisible(model.hasMessages());
        
        model.addPropertyChangeListener(
                ValidationResultModel.PROPERTYNAME_MESSAGES, 
                new MessageStateChangeHandler(scrollPane));
        
        return scrollPane;
    }
    

    /**
     * Creates and returns a text area that is intended to show validation 
     * messages. The area is bound to the given {@link ValidationResultModel}: the text content reflects
     * the validation result message text and the component is visible
     * if and only if the model has messages.
     * 
     * @param model   the model that provides the observable validation result
     * @return a text area intended to show validation messages
     */
    public static JTextArea createReportTextArea(ValidationResultModel model) {
        return createReportTextArea(model, DEFAULT_REPORT_BACKGROUND);
    }
    
    
    /**
     * Creates and returns a text area that is intended to show validation 
     * messages. The area is bound to the given {@link ValidationResultModel}: the text content reflects
     * the validation result message text and the component is visible
     * if and only if the model has messages.
     * 
     * @param model   the model that provides the observable validation result
     * @param backgroundColor   the color used to paint the area's background
     * @return a text area intended to show validation messages
     */
    public static JTextArea createReportTextArea(ValidationResultModel model, Color backgroundColor) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(true);
        area.setBackground(backgroundColor);
        
        MessageTextChangeHandler.updateText(area, model.getResult());

        model.addPropertyChangeListener(
                ValidationResultModel.PROPERTYNAME_RESULT, 
                new MessageTextChangeHandler(area));
        return area;
    }
    
    
    /**
     * Creates and returns a text area wrapped by a scroll pane that is 
     * intended to show validation messages. The component is bound to
     * the given {@link ValidationResultModel}: the text content reflects
     * the validation result message text and the component is visible
     * if and only if the model has messages.
     * 
     * @param model   the model that provides the observable validation result
     * @return a scrollable text component intended to show validation messages
     */
    public static JComponent createReportTextPane(ValidationResultModel model) {
        return createReportTextPane(model, DEFAULT_REPORT_BACKGROUND);
    }
    

    /**
     * Creates and returns a text area wrapped by a scroll pane that is 
     * intended to show validation messages. The component is bound to
     * the given {@link ValidationResultModel}: the text content reflects
     * the validation result message text and the component is visible
     * if and only if the model has messages.
     * 
     * @param model   the model that provides the observable validation result
     * @param backgroundColor   the color used to paint the area's background
     * @return a scrollable text component intended to show validation messages
     */
    public static JComponent createReportTextPane(ValidationResultModel model, Color backgroundColor) {
        JTextArea area = createReportTextArea(model, backgroundColor);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setVisible(model.hasMessages());

        model.addPropertyChangeListener(
                ValidationResultModel.PROPERTYNAME_MESSAGES, 
                new MessageStateChangeHandler(scrollPane));
        
        return scrollPane;
    }
    

    // Accessing Useful Icons *************************************************
    
    /**
     * Returns a default error icon useful to indicate validation errors.
     * 
     * @return a default error icon
     */
    public static ImageIcon getErrorIcon() {
        return Icons.ERROR_ICON;
    }

    /**
     * Returns a default warnings icon useful to indicate validation warnings.
     * 
     * @return a default warning icon
     */
    public static ImageIcon getWarningIcon() {
        return Icons.WARNING_ICON;
    }

    /**
     * Returns a small default error icon useful to indicate validation errors
     * attached to UI components.
     * 
     * @return a small default error icon
     */
    public static ImageIcon getSmallErrorIcon() {
        return Icons.SMALL_ERROR_ICON;
    }

    /**
     * Returns a small default warning icon useful to indicate validation 
     * warnings attached to UI components.
     * 
     * @return a small default warning icon
     */
    public static ImageIcon getSmallWarningIcon() {
        return Icons.SMALL_WARNING_ICON;
    }

    /**
     * Returns a default information icon useful to indicate input hints.
     * 
     * @return a default information icon
     */
    public static ImageIcon getInfoIcon() {
        return Icons.INFO_ICON;
    }

    /**
     * Returns a small default information icon useful to indicate input hints.
     * 
     * @return a small default information icon
     */
    public static ImageIcon getSmallInfoIcon() {
        return Icons.SMALL_INFO_ICON;
    }

    /**
     * Returns a check icon useful to indicate good vs. no good.
     * 
     * @return a check icon
     */
    public static ImageIcon getCheckIcon() {
        return Icons.CHECK_ICON;
    }

    /**
     * Returns the warning icon for warnings, the error icon for errors
     * and <code>null</code> otherwise.
     * 
     * @param severity   the severity used to lookup the icon
     * @return the warning icon for warnings, error icon for errors,
     *     <code>null</code> otherwise
     * @see #getWarningIcon()
     * @see #getErrorIcon()
     * @see #getSmallIcon(Severity)
     */
    public static Icon getIcon(Severity severity) {
        if (severity == Severity.ERROR)
            return getErrorIcon();
        else if (severity == Severity.WARNING)
            return getWarningIcon();
        else 
            return null;
    }
    
    /**
     * Returns the small warning icon for warnings, the small error icon for 
     * errors and <code>null</code> otherwise.
     * 
     * @param severity   the severity used to lookup the icon
     * @return the small warning icon for warnings, the small error icon for 
     *     errors, <code>null</code> otherwise
     * @see #getSmallWarningIcon()
     * @see #getSmallErrorIcon()
     * @see #getIcon(Severity)
     */
    public static Icon getSmallIcon(Severity severity) {
        if (severity == Severity.ERROR)
            return getSmallErrorIcon();
        else if (severity == Severity.WARNING)
            return getSmallWarningIcon();
        else 
            return null;
    }
    
    
    // ValidationResultModel Listeners ****************************************
    
    /**
     * Sets a JTextComponent's text to the messages text of a ValidationResult.
     */
    private static final class MessageTextChangeHandler implements PropertyChangeListener {
        
        private final JTextComponent textComponent;
        
        /**
         * Constructs a MessageTextChangeHandler for the given text component.
         * 
         * @param textComponent  the target component to set texts in
         */
        private MessageTextChangeHandler(JTextComponent textComponent) {
            this.textComponent = textComponent;
        }
        
        /**
         * Sets the message text of the given ValidationResult as text
         * in the given component.
         * 
         * @param aTextComponent   the target where to set the message text
         * @param result           used to request the validation message text
         */
        private static void updateText(JTextComponent aTextComponent, ValidationResult result) {
            aTextComponent.setText(result.getMessagesText());
            aTextComponent.setCaretPosition(0);
        }

        /**
         * The ValidationResult of the observed ValidationResultModel 
         * has changed. Updates this handler's text component's text
         * to the message text of the new ValidationResult.
         */
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(ValidationResultModel.PROPERTYNAME_RESULT)) {
                updateText(textComponent, (ValidationResult) evt.getNewValue());
            }
        }
    
    }
    
    /**
     *  Sets the component visible iff the validation result has messages.
     */
    public static final class MessageStateChangeHandler implements PropertyChangeListener {
        
        private final Component component;
        
        /**
         * Constructs a MessageStateHanlder that updates the visibility
         * of the given Component.
         * 
         * @param component   the component to show and hide
         */
        public MessageStateChangeHandler(Component component) {
            this.component = component;
        }
        
        /**
         * The ValidationResult's 'messages' property has changed. 
         * Hides or shows the component if the ValidationResult is OK
         * or has messages resp. 
         */
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(ValidationResultModel.PROPERTYNAME_MESSAGES)) {
                boolean hasMessages = ((Boolean) evt.getNewValue()).booleanValue();
                component.setVisible(hasMessages);
            }
        }
    
    }
    
    /** 
     * Sets the component visible iff the validation result has errors.
     */
    private static final class LabelIconChangeHandler implements PropertyChangeListener {
        
        private final JLabel label;
        
        private LabelIconChangeHandler(JLabel label) {
            this.label = label;
        }
        
        private static void updateVisibilityAndIcon(JLabel aLabel, Severity severity) {
            aLabel.setVisible(severity != Severity.OK);
            if (severity == Severity.ERROR)
                aLabel.setIcon(getErrorIcon());
            else if (severity == Severity.WARNING)
                aLabel.setIcon(getWarningIcon());
        }

        /**
         * The ValidationResult's severity has changed. 
         * Updates the label's visibility and icon. 
         */
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(ValidationResultModel.PROPERTYNAME_SEVERITY)) {
                updateVisibilityAndIcon(label, (Severity) evt.getNewValue());
            }
        }
    
    }
    
    
    /**
     * Sets the component visible iff the validation result has errors.
     */
    private static final class LabelTextChangeHandler implements PropertyChangeListener {
        
        private final JLabel label;
        
        private LabelTextChangeHandler(JLabel label) {
            this.label = label;
        }
        
        private static void updateText(JLabel label, ValidationResult result) {
            label.setText(result.hasMessages()
                    ? ((ValidationMessage) result.getMessages().get(0)).formattedText()
                    : "");
        }

        /**
         * The ValidationResult's content has changed. 
         * Updates the label's text to become the formatted text
         * of the new ValidationResult.
         */
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(ValidationResultModel.PROPERTYNAME_RESULT)) {
                updateText(label, (ValidationResult) evt.getNewValue());
            }
        }
    
    }
    
    
    // Renderer ***************************************************************

    /**
     * A <code>ListCellRenderer</code> implementation which renders 
     * labels for instances of <code>ValidationMessage</code>.  
     */
    private static class BasicValidationMessageCellRenderer
        extends DefaultListCellRenderer {
            
        /**
         * In addition to the superclass behavior, this method 
         * assumes that the value is a ValidationMessage.
         * It sets the renderer's icon to the one associated with the
         * ValidationMessage's severity and sets the renderer's text
         * to the message's formatted text.
         */
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
            super.getListCellRendererComponent(
                list,
                value,
                index,
                false,  // Ignore the selection state
                false); // Ignore the cell focus state
            ValidationMessage message = (ValidationMessage) value;
            this.setIcon(ValidationResultViewFactory.getIcon(message.severity()));
            this.setText(message.formattedText());
            return this;
        }
    }
    

    // Icon Pixel Data and Definitions ****************************************
    
    /**
     * Provides icons useful for presenting validation feedback.
     * These icons are constructed from byte arrays.
     */
    private static class Icons {

        private static final byte[] ERROR_GIF_BYTES = {71, 73, 70, 56, 57, 97,
            16, 0, 16, 0, -77, 0, 0, -1, 127, 63, -8, 88, 56, -1, 95, 63, -8,
            56, 56, -33, 63, 63, -65, 63, 63, -104, 56, 56, 127, 63, 63, -1,
            -65, -65, -97, 127, 127, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 33, -7, 4, 1, 0, 0, 11, 0, 44, 0, 0, 0, 0, 16, 0,
            16, 0, 0, 4, 84, 112, -55, 73, -85, -67, 120, -91, -62, 75, -54,
            -59, 32, 14, 68, 97, 92, 33, -96, 8, 65, -96, -104, 85, 50, 0, 0,
            -94, 12, 10, 82, 126, 83, 26, -32, 57, 18, -84, 55, 96, 1, 69, -91,
            3, 37, -12, -77, -35, -124, 74, 98, -64, 54, -96, -106, 78, -109,
            4, 1, 55, 66, 32, 76, -68, -119, -127, 64, 46, -101, -94, 21, 67,
            -121, 99, 64, 91, 18, -19, -125, 33, -100, -87, -37, 41, 17, 0, 59};

        private static final byte[] SMALL_ERROR_GIF_BYTES = {71, 73, 70, 56,
            57, 97, 7, 0, 8, 0, -94, 0, 0, -1, 0, 0, -65, 63, 63, 127, 63, 63,
            -1, 127, 127, -65, 95, 95, -33, -65, -65, -1, -1, -1, -1, -1, -1,
            33, -7, 4, 1, 0, 0, 7, 0, 44, 0, 0, 0, 0, 7, 0, 8, 0, 0, 3, 25, 88,
            -79, -85, 54, 96, -104, 37, -55, 19, 1, 88, 2, -80, 54, 28, 86, 93,
            -63, 19, 77, -118, -96, 6, 69, 2, 0, 59, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0};

        private static final byte[] WARNING_GIF_BYTES = {71, 73, 70, 56, 57,
            97, 16, 0, 16, 0, -77, 0, 0, -1, -1, 95, -1, -33, 63, -33, -65, 63,
            -1, -33, 95, -65, -97, 63, -1, -65, 63, -97, 127, 63, -65, -65,
            -65, 95, 95, 95, 0, 0, 0, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 33, -7, 4, 1, 0, 0, 10, 0, 44, 0, 0, 0, 0, 16, 0,
            16, 0, 0, 4, 69, 80, -55, 73, -85, -67, 120, 10, -110, -23, -39,
            71, 39, 9, -64, 38, 126, 67, 64, -124, 25, -103, 32, 38, -122, -66,
            -59, -118, -111, -64, 27, -60, -43, 7, 12, -70, 26, -117, 66, 26,
            0, 2, -56, 29, -57, 35, 48, 6, 104, 1, 33, -123, 96, 28, 12, 10, 5,
            -127, -106, -89, 32, 24, -66, -32, -80, 65, 68, 22, 69, 0, 0, 59};

        private static final byte[] SMALL_WARNING_GIF_BYTES = {71, 73, 70, 56,
            57, 97, 7, 0, 8, 0, -77, 0, 0, -1, -1, 63, -1, -1, 127, -65, -65,
            127, -33, -33, -65, -1, -33, 63, -65, -97, 63, -97, 127, 63, -1,
            -65, 95, -1, -1, -1, 0, 0, 0, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 33, -7, 4, 1, 0, 0, 10, 0, 44, 0, 0, 0, 0, 7,
            0, 8, 0, 0, 4, 33, 80, -95, 97, 6, 82, -78, 16, 115, 103, 73, 21,
            -126, 20, 65, 2, 112, 3, 105, 86, 36, 112, 0, -101, 1, 0, 9, -79,
            9, 70, -95, 27, 66, 4, 0, 59};

        private static final byte[] INFO_GIF_BYTES = {71, 73, 70, 56, 57, 97,
            16, 0, 16, 0, -94, 0, 0, 0, 0, -1, 63, 63, -65, 95, 95, -65, 127,
            127, -65, -90, -54, -16, -1, -1, -1, -1, -1, -1, 0, 0, 0, 33, -7,
            4, 1, 0, 0, 6, 0, 44, 0, 0, 0, 0, 16, 0, 16, 0, 0, 3, 48, 104, -70,
            -36, -2, 80, -107, 57, -93, 28, -128, 20, 107, 10, 0, -63, 102, 21,
            68, -96, -115, 84, 21, 21, -62, 55, -120, -48, -28, -62, -15, -52,
            121, -64, 123, -37, 43, -2, -46, 13, -42, 39, -96, -117, -91, 84,
            -100, 71, 2, 0, 59};
        
        private static final byte[] SMALL_INFO_GIF_BYTES = {71, 73, 70, 56, 57,
            97, 9, 0, 9, 0, -77, 0, 0, 0, 0, -124, 57, 57, 90, 57, 90, -100,
            -100, -100, -100, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, 33, -7, 4, 1, 0, 0, 3, 0, 44, 0, 0, 0,
            0, 9, 0, 9, 0, 0, 4, 29, 112, 12, 65, -87, -100, 36, -25, -128, 51,
            -48, -127, -96, 125, -101, -24, -127, 38, 65, 18, -31, 8, 14, -127,
            -74, 93, 65, 93, 75, 17, 0, 59};

        private static final byte[] CHECK_ICON_BYTES = {71, 73, 70, 56, 57, 97, 16, 0,
            16, 0, -77, 0, 0, 0, 0, -1, 63, 127, -65, 127, -97, -65, -90, -54,
            -16, -97, -65, -65, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 33, -7, 4, 1,
            0, 0, 5, 0, 44, 0, 0, 0, 0, 16, 0, 16, 0, 0, 4, 43, -80, -56, 73,
            -85, -67, 56, 19, -112, 105, 0, 67, 39, 125, -124, 88, 124, -95,
            40, -128, -26, 0, 8, -40, 0, 23, 110, 16, 3, 44, -102, -83, -17,
            43, -30, 56, 19, 47, -11, -101, -103, -114, -56, 8, 0, 59};
        

        private static final ImageIcon ERROR_ICON = new ImageIcon(
                ERROR_GIF_BYTES);

        private static final ImageIcon SMALL_ERROR_ICON = new ImageIcon(
                SMALL_ERROR_GIF_BYTES);

        private static final ImageIcon WARNING_ICON = new ImageIcon(
                WARNING_GIF_BYTES);

        private static final ImageIcon SMALL_WARNING_ICON = new ImageIcon(
                SMALL_WARNING_GIF_BYTES);

        private static final ImageIcon INFO_ICON = new ImageIcon(
                INFO_GIF_BYTES);

        private static final ImageIcon SMALL_INFO_ICON = new ImageIcon(
                SMALL_INFO_GIF_BYTES);

        private static final ImageIcon CHECK_ICON = new ImageIcon(
                CHECK_ICON_BYTES);
        
       }

}
