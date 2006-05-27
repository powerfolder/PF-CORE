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

package com.jgoodies.validation.message;

import com.jgoodies.validation.Severity;
import com.jgoodies.validation.ValidationMessage;
import com.jgoodies.validation.util.ValidationUtils;

/**
 * An implementation of {@link ValidationMessage} that just holds a text.
 * It is the minimal validation message, not intended to be subclassed.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.2 $
 */
public final class SimpleValidationMessage extends AbstractValidationMessage {


    // Instance Creation ******************************************************

    /**
     * Constructs a simple warning message for the given text.
     * 
     * @param text   a String that describes this warning 
     * 
     * @throws NullPointerException if the text is <code>null</code>.
     */
    public SimpleValidationMessage(String text) {
        this(text, Severity.WARNING);
    }

    
    /**
     * Constructs a simple validation message for the given text
     * and message severity.
     * 
     * @param text       describes this message
     * @param severity   the message severity, either error or warning
     * 
     * @throws IllegalArgumentException if severity is <code>Severity.OK</code>
     * @throws NullPointerException if the text is <code>null</code>.
     */
    public SimpleValidationMessage(String text, Severity severity) {
        this(text, severity, null);
    }


    /**
     * Constructs a simple validation message for the given text,
     * message severity, and message key.
     * 
     * @param text       describes this message
     * @param severity   the message severity, either error or warning
     * @param key        the message's key
     * 
     * @throws IllegalArgumentException if severity is <code>Severity.OK</code>
     * @throws NullPointerException if the text is <code>null</code>.
     */
    public SimpleValidationMessage(String text, Severity severity, Object key) {
        super(text, severity, key);
        if (text == null)
            throw new NullPointerException("The text must not be null");
    }

    
    // Comparison and Hashing *************************************************
    
    /**
     * Compares the specified object with this validation message for equality.  
     * Returns <code>true</code> if and only if the specified object is also 
     * a simple validation message, both messages have the same severity,
     * key, and formatted text. In other words, two simple validation messages 
     * are defined to be equal if and only if they behave one like the other.<p>
     *
     * This implementation first checks if the specified object is this
     * a simple validation message. If so, it returns <code>true</code>; 
     * if not, it checks if the specified object is a simple validation message. 
     * If not, it returns <code>false</code>; if so, it checks and returns
     * if the severities, keys and formatted texts of both messages are equal.
     *
     * @param o the object to be compared for equality with this validation message.
     * 
     * @return <code>true</code> if the specified object is equal 
     *     to this validation message.
     *     
     * @see Object#equals(java.lang.Object)    
     */
    public boolean equals(Object o) {
        if (o == this) 
            return true;
        if (!(o instanceof SimpleValidationMessage)) 
            return false;
        SimpleValidationMessage other = (SimpleValidationMessage) o;
        return severity().equals(other.severity())
            && ValidationUtils.equals(key(), other.key())
            && ValidationUtils.equals(formattedText(), other.formattedText());
    }    

    
    /**
     * Returns the hash code value for this validation message. 
     * This implementation computes and returns the hash based 
     * on the hash code values of this messages' severity, key, 
     * and text.
     *
     * @return the hash code value for this validation message.
     * 
     * @see Object#hashCode()
     */
    public int hashCode() {
        String formattedText = formattedText();
        int result = 17;
        result = 37 * result + severity().hashCode();
        result = 37 * result + (key() == null ? 0 : key().hashCode());
        result = 37 * result + (formattedText == null ? 0 : formattedText.hashCode());
        return result;
    }
    

}
