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

import java.io.Serializable;

import com.jgoodies.validation.Severity;
import com.jgoodies.validation.ValidationMessage;

/**
 * An abstract class that minimizes the effort required to implement the 
 * {@link ValidationMessage} interface. Holds the severity, a text message,
 * and the association key.<p>
 * 
 * Subclasses should implement <code>#equals</code> and <code>#hashCode</code> 
 * to prevent unnecessary change notifications for the <em>result</em> property 
 * when a ValidationResultModel gets a new ValidationResult. See for example 
 * the implementation of method {@link PropertyValidationMessage#equals(Object)}.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.2 $
 */
public abstract class AbstractValidationMessage 
    implements ValidationMessage, Serializable {

    /**
     * Holds this message's severity, either error or warning.
     */
    private final Severity severity;
    
    /** 
     * Holds the text messages.
     */
    private final String text;
    
    /**
     * Holds the association key that can be used to model a loose coupling
     * between messages and views.
     * 
     * @see ValidationMessage
     * @see #key()
     * @see com.jgoodies.validation.ValidationResult#subResult(Object)
     * @see com.jgoodies.validation.ValidationResult#keyMap()
     */
    private Object key;
    

    // Instance Creation ******************************************************

    /**
     * Constructs an AbstractValidationMessage for the given text and Severity.
     * 
     * @param text       describes this message
     * @param severity   this message's severity, either error or warning
     * 
     * @throws IllegalArgumentException if severity is <code>Severity.OK</code>
     */
    protected AbstractValidationMessage(String text, Severity severity) {
        this(text, severity, null);
    }

    /**
     * Constructs an AbstractValidationMessage for the given text,
     * Severity, and association key.
     * 
     * @param text       describes this message
     * @param severity   this message's severity, either error or warning
     * @param key        used to determine whether this message belongs 
     *     to a given view
     * 
     * @throws IllegalArgumentException if severity is <code>Severity.OK</code>
     */
    protected AbstractValidationMessage(String text, Severity severity, Object key) {
        if (severity == Severity.OK) 
            throw new IllegalArgumentException("Cannot create a validation messages with Severity.OK.");
        
        this.text = text;
        this.severity = severity;
        setKey(key);
    }

    
    // Implementation of the ValidationMessage API ****************************

    /**
     * Returns this message's severity, either error or warning.
     * 
     * @return message's severity, either error or warning
     */
    public final Severity severity() {
        return severity;
    }
    
    
    /**
     * Returns a message description as formatted text. This default 
     * implementation just returns the message text. 
     * Subclasses may override to add information about the type and
     * other message related information.
     * 
     * @return a message description as formatted text
     */
    public String formattedText() {
        return text();
    }
    
    
    /**
     * Returns this validation message's text.
     * 
     * @return the message text
     */
    protected final String text() {
        return text;
    }
    
    
    /**
     * Returns this message's association key that can be used to model 
     * a loose coupling between validation messages and views that present 
     * the validated data.<p>
     * 
     * Subclasses may override this method to return keys that are built from 
     * other internal data. For example, the {@link PropertyValidationMessage}
     * returns the <em>aspect</em> as key.<p> 
     * 
     * See the class comment for more information about this relation.
     * 
     * @return this message's association key
     */
    public Object key() {
        return key;
    }
    
    
    /**
     * Sets the given object as new association key.
     * 
     * @param associationKey  the key to be set
     */
    protected final void setKey(Object associationKey) {
        key = associationKey;
    }
    
    
    /**
     * Returns a string representation of this validation message.
     * Prints the class name and the formatted text.
     * 
     * @return a string representaiton of this validation message
     * @see Object#toString()
     */
    public String toString() {
        return getClass().getName() + ": " + formattedText();
    }

    
}
