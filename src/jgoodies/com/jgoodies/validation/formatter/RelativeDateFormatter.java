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

package com.jgoodies.validation.formatter;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.ListResourceBundle;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jgoodies.validation.util.ValidationUtils;

/**
 * Adds relative dates and output shortcuts to its superclass 
 * EmptyDateFormatter.<p>
 * 
 * If output shortcuts are enabled, Yesterday, Today and Tomorrow 
 * are formatted using their localized human-language print strings.<p>
 * 
 * If relative input is allowed, the parser accepts signed integers 
 * that encode a date relative to today; this input would otherwise
 * be considered invalid.
 * 
 * @author  Karsten Lentzsch
 * @version $Revision: 1.3 $
 * 
 * @see com.jgoodies.validation.util.ValidationUtils
 * @see javax.swing.JFormattedTextField
 */
public class RelativeDateFormatter extends EmptyDateFormatter {
    
    // Keys for the Shortcut Localization *************************************
    
    /**
     * The resource bundle key used to localize 'Yesterday'.
     */
    public static final String KEY_YESTERDAY = "RelativeDate.yesterday";

    /**
     * The resource bundle key used to localize 'Today'.
     */
    public static final String KEY_TODAY     = "RelativeDate.today";

    /**
     * The resource bundle key used to localize 'Tomorrow'.
     */
    public static final String KEY_TOMORROW  = "RelativeDate.tomorrow";
    
    
    // Static Fields **********************************************************
    
    /**
     * Holds the resource bundle that will be used as default
     * for all RelativeDateFormatters. Individual instances
     * can override this default using <code>#setResourceBundle</code>.<p>
     * 
     * The default bundle is lazily initialized in 
     * <code>#getResourceBundle()</code>.
     * 
     * @see #getResourceBundle()
     * @see #setResourceBundle(ResourceBundle)
     */
    private static ResourceBundle defaultResourceBundle;


    // ************************************************************************
    
    /**
     * Describes whether the date values for yesterday, today and tomorrow 
     * shall be converted to their natural strings or be formatted.
     * If a ResourceBundle has been set, the strings will be localized. 
     * 
     * @see #valueToString(Object)
     */
    private final boolean useOutputShortcuts;
    
    /**
     * Describes whether integers are valid edit values. If so, these
     * are interpreted as day offset relative to today.
     * 
     * @see #stringToValue(String)
     */
    private final boolean allowRelativeInput;
    
    /**
     * Refers to an optional ResourceBundle that is used to localize 
     * the texts for the output shortcuts: Yesterday, Today, Tomorrow.
     */
    private ResourceBundle resourceBundle;
    

    // Instance Creation ****************************************************

    /**
     * Constructs a RelativeDateFormatter using the default DateFormat 
     * with output shortcuts and relative input enabled.
     */
    public RelativeDateFormatter() {
        this(true, true);
    }

    /**
     * Constructs a RelativeDateFormatter using the given DateFormat 
     * with output shortcuts and relative input enabled.
     * 
     * @param format   the DateFormat used to format and parse dates
     */
    public RelativeDateFormatter(DateFormat format) {
        this(format, true, true);
    }

    /**
     * Constructs a RelativeDateFormatter using the default DateFormat 
     * with output shortcuts and relative input configured as specified.
     * 
     * @param useOutputShortcuts  true indicates that dates are formatted
     *   with shortcuts for yesterday, today, and tomorrow, where false 
     *   always converts using absolute numbers for day, month and year.
     * @param allowRelativeInput  true indicates that the parser accepts
     *   signed integers that encode a date relative to today; if false
     *   such input is considered invalid
     */
    public RelativeDateFormatter(
        boolean useOutputShortcuts,
        boolean allowRelativeInput) {
        this.useOutputShortcuts = useOutputShortcuts;
        this.allowRelativeInput = allowRelativeInput;
    }

    /**
     * Constructs a RelativeDateFormatter using the given DateFormat 
     * with output shortcuts and relative input configured as specified.
     * 
     * @param format   the DateFormat used to format and parse dates
     * @param useOutputShortcuts  true indicates that dates are formatted
     *   with shortcuts for yesterday, today, and tomorrow, where false 
     *   always converts using absolute numbers for day, month and year.
     * @param allowRelativeInput  true indicates that the parser accepts
     *   signed integers that encode a date relative to today; if false
     *   such input is considered invalid
     */
    public RelativeDateFormatter(
        DateFormat format,
        boolean useOutputShortcuts,
        boolean allowRelativeInput) {
        super(format);
        this.useOutputShortcuts = useOutputShortcuts;
        this.allowRelativeInput = allowRelativeInput;
    }
    
    
    // Overriding Superclass Behavior ****************************************

    /**
     * Returns the Object representation of the String <code>text</code>.<p>
     *
     * In addition to the delegate's behavior, this methods accepts
     * signed integers interpreted as days relative to today.
     * 
     * @param text  the String to convert
     * @return the Object representation of text
     * @throws ParseException if there is an error in the conversion
     */
    public Object stringToValue(String text) throws ParseException {
        if (!allowRelativeInput)
            return super.stringToValue(text);
        
        if (text.startsWith("+")) {
            text = text.substring(1);
        }
        try {
            int offsetDays = Integer.parseInt(text);
            return ValidationUtils.getRelativeDate(offsetDays);
        } catch (NumberFormatException e) {
            return super.stringToValue(text);
        }
    }


    /**
     * Returns a String representation of the Object <code>value</code>.
     * This invokes <code>format</code> on the current DateFormat.<p>
     * 
     * In addition to the superclass behavior, this method formats the dates 
     * for yesterday, today and tomorrow to the natural language strings.
     *
     * @param value   The value to convert
     * @return a String representation for the value
     * @throws ParseException if there is an error in the conversion
     */
    public String valueToString(Object value) throws ParseException {
        if (value == null || !useOutputShortcuts || !(value instanceof Date))
            return super.valueToString(value);
        
        Date date = (Date) value;
        if (ValidationUtils.isYesterday(date)) {
            return getString(KEY_YESTERDAY);
        } else if (ValidationUtils.isToday(date)) {
            return getString(KEY_TODAY);
        } else if (ValidationUtils.isTomorrow(date)) {
            return getString(KEY_TOMORROW);
        } else
            return super.valueToString(value);
    }

    
    // Internationalization **************************************************
    
    /**
     * Returns the ResourceBundle that is used as default
     * unless overriden by an individual bundle.
     * 
     * @return the ResourceBundle that is used as default
     */
    public static ResourceBundle getDefaultResourceBundle() {
        if (defaultResourceBundle == null) {
            defaultResourceBundle = new DefaultResources();
        }
        return defaultResourceBundle;
    }
    
    
    /**
     * Sets the ResourceBundle that is used as default for all
     * RelativeDateFormatters that have no individual bundle set.
     * 
     * @param newDefaultBundle the ResourceBundle to be used as default
     */
    public static void setDefaultResourceBundle(ResourceBundle newDefaultBundle) {
        defaultResourceBundle = newDefaultBundle;
    }

    
    /**
     * Returns the ResourceBundle used to lookup localized texts
     * for Yesterday, Today, and Tomorrow. In case no bundle is set,
     * these 3 dates will be formatted using the English texts.
     * 
     * @return the ResourceBundle used to lookup localized texts
     * 
     * @see #setResourceBundle(ResourceBundle)
     * @see #getDefaultResourceBundle()
     * @see #setDefaultResourceBundle(ResourceBundle)
     */
    public final ResourceBundle getResourceBundle() {
        return resourceBundle;
    }
    
    
    /**
     * Sets a ResourceBundle that will be used to lookup localized texts
     * for Yesterday, Today, and Tomorrow. In case no bundle is set,
     * the default bundle will be used.
     * 
     * @param newBundle   the ResourceBundle to set
     * 
     * @see #getResourceBundle()
     * @see #getDefaultResourceBundle()
     * @see #setDefaultResourceBundle(ResourceBundle)
     */
    public final void setResourceBundle(ResourceBundle newBundle) {
        resourceBundle = newBundle;
    }
    
    
    /**
     * Retrieves and returns a String for the given key 
     * from this formatter's resource bundle. If no individual
     * resource bundle has been set, the default bundle is used
     * to lookup the localized resources.
     * 
     * @param key  the key used to lookup the localized string
     * @return the localized text or default text
     * 
     * @see #getResourceBundle()
     * @see #setResourceBundle(ResourceBundle)
     * @see #getDefaultResourceBundle()
     * @see #setDefaultResourceBundle(ResourceBundle)
     */
    private String getString(String key) {
        ResourceBundle bundle = getResourceBundle();
        if (bundle == null)
            bundle = getDefaultResourceBundle();
            
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            Logger.getLogger(getClass().getName()).
                log(Level.WARNING, "RelativeDateFormatter", e);
            return "";
        }
    }
    
    
    // Default ResourceBundle *************************************************
    
    /**
     * A default ResourceBundle that provides the default resources 
     * used to localize the strings for Yesterday, Today, and Tomorrow. 
     */
    private static final class DefaultResources extends ListResourceBundle {
        
        private static final Object[][] CONTENTS = { 
            {KEY_YESTERDAY, "Yesterday"},
            {KEY_TODAY,     "Today"    },
            {KEY_TOMORROW,  "Tomorrow" },
        };
        
        public Object[][] getContents() {
            return CONTENTS;
        }
        
    }
    

}
