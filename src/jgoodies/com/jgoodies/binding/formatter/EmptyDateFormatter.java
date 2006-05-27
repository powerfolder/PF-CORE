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

package com.jgoodies.binding.formatter;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import javax.swing.text.DateFormatter;

import com.jgoodies.binding.BindingUtils;

/**
 * In addition to its superclass DateFormatter, this class converts 
 * to/from the empty string. Therefore it holds an <em>empty value</em> 
 * that is the counterpart of the empty string. 
 * The Method <code>#valueToString</code> converts the empty value to the
 * empty string. And <code>#stringToValue</code> converts blank strings
 * to the empty value. In all other cases the conversion is delegated
 * to its superclass.<p>
 * 
 * Often the empty value is <code>null</code>. As an alternative you can map 
 * the empty string to a given date, for example epoch (January 1, 1970).  
 * 
 * <strong>Examples:</strong><pre>
 * new EmptyDateFormatter();
 * new EmptyDateFormatter(new Date(0));
 * </pre> 
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.2 $
 * 
 * @see java.text.Format 
 */
public class EmptyDateFormatter extends DateFormatter {
        
    /**
     * Holds the date that is converted to an empty string and
     * that is the result of converting blank strings to a value.
     * 
     * @see #stringToValue(String)
     * @see #valueToString(Object)
     */
    private final Date emptyValue;
    
    
    // Instance Creation ****************************************************
    
    /**
     * Constructs an EmptyDateFormatter that converts <code>null</code> 
     * to the empty string and vice versa.
     */
    public EmptyDateFormatter() {
        this((Date) null);
    }
    

    /**
     * Constructs an EmptyDateFormatter configured with the specified
     * Format; maps <code>null</code> to the empty string and vice versa.
     *
     * @param format Format used to dictate legal values
     */
    public EmptyDateFormatter(DateFormat format) {
        this(format, null);
    }

    
    /**
     * Constructs an EmptyDateFormatter that converts the given 
     * <code>emptyValue</code> to the empty string and vice versa. 
     * 
     * @param emptyValue   the representation of the empty string
     */
    public EmptyDateFormatter(Date emptyValue) {
        this.emptyValue = emptyValue == null 
            ? null 
            : new Date(emptyValue.getTime());
    }
    
    
    /**
     * Constructs an EmptyDateFormatter configured with the specified
     * Format; maps <code>null</code> to the given <code>emptyValue</code>
     * and vice versa.
     *
     * @param format Format used to dictate legal values
     * @param emptyValue   the representation of the empty string
     */
    public EmptyDateFormatter(DateFormat format, Date emptyValue) {
        super(format);
        this.emptyValue = emptyValue == null 
            ? null 
            : new Date(emptyValue.getTime());
    }

    
    // Overriding Superclass Behavior *****************************************
    
    /**
     * Returns the <code>Object</code> representation of the
     * <code>String</code> <code>text</code>.<p>
     *
     * Unlike its superclass, this class converts blank strings
     * to the empty value.
     * 
     * @param text <code>String</code> to convert
     * @return <code>Object</code> representation of text
     * @throws ParseException if there is an error in the conversion
     */
    public Object stringToValue(String text) throws ParseException {
        return BindingUtils.isBlank(text)
            ? emptyValue
            : super.stringToValue(text);
    }


    /**
     * Returns a String representation of the Object <code>value</code>.
     * This invokes <code>format</code> on the current <code>Format</code>.<p>
     * 
     * Unlike its superclass, this class converts the empty value
     * to the empty string.
     *
     * @param value   the value to convert
     * @return a String representation of value
     * @throws ParseException if there is an error in the conversion
     */
    public String valueToString(Object value) throws ParseException {
        return BindingUtils.equals(value, emptyValue)
            ? ""
            : super.valueToString(value);
    }


}
