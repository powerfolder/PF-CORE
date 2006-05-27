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

package com.jgoodies.validation.util;

import com.jgoodies.validation.Severity;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.message.PropertyValidationMessage;


/**
 * A utility class that minimizes the effort to create instances
 * of {@link PropertyValidationMessage} in validation code.
 * You can use an instance of this class as a member field of your 
 * validator implementation and delegate the message creation to it.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.2 $
 * 
 * @see com.jgoodies.validation.message.PropertyValidationMessage
 */
public final class PropertyValidationSupport {
    
    /**
     * Refers to the {@link ValidationResult} that is used to add messages to
     * if no individual result is specified.
     * 
     * @see #add(String, String)
     * @see #addError(String, String)
     * @see #addWarning(String, String)
     * @see #clearResult()
     */
    private ValidationResult defaultResult;

    /**
     * Holds the severity that is used in the message creation and adder methods
     * that use no individual severity.
     * 
     * @see #create(String, String)
     * @see #add(String, String)
     * @see #add(ValidationResult, String, String)
     */
    private final Severity defaultSeverity;
    
    /**
     * Refers to the object to be validated.
     */
    private final Object target;
    
    /**
     * Describes the validation target's role in the outer context.
     */
    private final String role;
    
    
    // Instance Creation ******************************************************
    
    /**
     * Constructs a <code>PropertyValidationSupport</code> instance for the
     * given validation target and its validation role. The default severity
     * is set to <code>Severity.WARNING</code>.
     * 
     * @param target    the object to be validated
     * @param role      the validation target's role in the outer context
     * 
     * @throws NullPointerException if the target or role is <code>null</code>
     */
    public PropertyValidationSupport(Object target, String role) {
        this(Severity.WARNING, target, role);
    }
    
    
    /**
     * Constructs a <code>PropertyValidationSupport</code> instance for the
     * given validation target and its validation role.
     * 
     * @param defaultSeverity   the optional <code>Severity</code> used for 
     *     message creation when no severity is specified
     * @param target    the object to be validated
     * @param role      the validation target's role in the outer context
     * 
     * @throws NullPointerException if the target or role is <code>null</code>
     * @throws IllegalArgumentException if defaultSeverity is <code>Severity.OK</code>
     */
    public PropertyValidationSupport(Severity defaultSeverity, Object target, String role) {
        this(new ValidationResult(), defaultSeverity, target, role);
    }
    
    
    /**
     * Constructs a <code>PropertyValidationSupport</code> instance for the
     * given validation target and its validation role.
     * 
     * @param defaultResult     the optional <code>ValidationResult</code> 
     *     that is used to add <code>ValidationMessage</code>s to 
     * @param defaultSeverity   the optional <code>Severity</code> used for 
     *     message creation when no severity is specified
     * @param target    the object to be validated
     * @param role      the validation target's role in the outer context
     * 
     * @throws NullPointerException if the target or role is <code>null</code>
     * @throws IllegalArgumentException if defaultSeverity is <code>Severity.OK</code>
     */
    public PropertyValidationSupport(ValidationResult defaultResult, Severity defaultSeverity, Object target, String role) {
        if (defaultSeverity == Severity.OK)
            throw new IllegalArgumentException("Severity.OK must not be used in validation messages.");
        
        this.defaultResult = defaultResult;
        this.defaultSeverity = defaultSeverity;
        this.target = target;
        this.role   = role;
    }
    
    
    // Accessing the ValidationResult *****************************************
    
    /**
     * Sets an empty {@link ValidationResult} as default result.
     * Useful at the begin of a validation sequence.
     */
    public void clearResult() {
        defaultResult = new ValidationResult();
    }
    
    /**
     * Returns the default {@link ValidationResult}.
     * 
     * @return the default validation result
     */
    public ValidationResult getResult() {
        return defaultResult;
    }
    
    
    // Message Creation *******************************************************

    /**
     * Creates and returns an error <code>PropertyValidationMessage</code>
     * for the given property and message text.
     * 
     * @param property    describes the validated property
     * @param text        the message text
     * @return a <code>PropertyValidationMessage</code> with error severity
     *     for the given property and text
     */
    public PropertyValidationMessage createError(String property, String text) {
        return create(Severity.ERROR, property, text);
    }
    
    
    /**
     * Creates and returns a warning <code>PropertyValidationMessage</code>
     * for the given property and message text.
     * 
     * @param property    describes the validated property
     * @param text        the message text
     * @return a <code>PropertyValidationMessage</code> with warning severity
     *     for the given property and text
     */
    public PropertyValidationMessage createWarning(String property, String text) {
        return create(Severity.WARNING, property, text);
    }
    
    
    /**
     * Creates and returns a <code>PropertyValidationMessage</code> 
     * for the given property and message text using the default severity.
     * 
     * @param property    describes the validated property
     * @param text        the message text
     * @return a <code>PropertyValidationMessage</code> with default severity
     *     for the given property and text
     */
    public PropertyValidationMessage create(String property, String text) {
        return create(defaultSeverity, property, text);
    }
    
    
    /**
     * Creates and returns an error <code>PropertyValidationMessage</code>
     * for the given property and message text using the specified severity.
     * 
     * @param severity    the <code>Severity</code> to be used
     * @param property    describes the validated property
     * @param text        the message text
     * @return a <code>PropertyValidationMessage</code> with the specified severity
     *     for the given property and text
     */
    public PropertyValidationMessage create(Severity severity, String property, String text) {
        return new PropertyValidationMessage(severity, text, target, role, property);
    }
    
    
    // Adding Messages to the Default ValidationResult ************************
    
    /**
     * Adds an error <code>PropertyValidationMessage</code> to this object's
     * default <code>ValidationResult</code>. 
     * Uses the given property and message text.
     * 
     * @param property    describes the validated property
     * @param text        the message text
     */
    public void addError(String property, String text) {
        addError(defaultResult, property, text);
    }
    
    
    /**
     * Adds a warning <code>PropertyValidationMessage</code> to this object's
     * default <code>ValidationResult</code>. 
     * Uses the given property and message text.
     * 
     * @param property    describes the validated property
     * @param text        the message text
     */
    public void addWarning(String property, String text) {
        addWarning(defaultResult, property, text);
    }
    
    
    /**
     * Adds a <code>PropertyValidationMessage</code> to this object's
     * default <code>ValidationResult</code>. 
     * Uses the default severity and the given property and message text.
     * 
     * @param property    describes the validated property
     * @param text        the message text
     */
    public void add(String property, String text) {
        add(defaultResult, property, text);
    }
    
    
    /**
     * Adds a <code>PropertyValidationMessage</code> to this object's
     * default <code>ValidationResult</code>. Uses the specified 
     * <code>Severity</code> and given property and message text.
     * 
     * @param severity    the <code>Severity</code> to be used
     * @param property    describes the validated property
     * @param text        the message text
     */
    public void add(Severity severity, String property, String text) {
        add(defaultResult, severity, property, text);
    }
    
    
    // Adding Messages to a Given ValidationResult ****************************
    
    /**
     * Adds an error <code>PropertyValidationMessage</code> to the specified
     * <code>ValidationResult</code>. 
     * Uses the given property and message text.
     * 
     * @param result      the result the message will be added to
     * @param property    describes the validated property
     * @param text        the message text
     */
    public void addError(ValidationResult result, String property, String text) {
        result.add(createError(property, text));
    }
    
    
    /**
     * Adds a warning <code>PropertyValidationMessage</code> to the specified
     * <code>ValidationResult</code>. 
     * Uses the given property and message text.
     * 
     * @param result      the result the message will be added to
     * @param property    describes the validated property
     * @param text        the message text
     */
    public void addWarning(ValidationResult result, String property, String text) {
        result.add(createWarning(property, text));
    }
    
    
    /**
     * Adds a <code>PropertyValidationMessage</code> to the specified
     * <code>ValidationResult</code>. Uses this object's default severity
     * and the given property and message text.
     * 
     * @param result      the result the message will be added to
     * @param property    describes the validated property
     * @param text        the message text
     */
    public void add(ValidationResult result, String property, String text) {
        result.add(create(property, text));
    }
    
    
    /**
     * Adds a <code>PropertyValidationMessage</code> to the specified
     * <code>ValidationResult</code>. Uses the specified severity
     * and the given property and message text.
     * 
     * @param result      the result the message will be added to
     * @param severity    the severity used for the created message
     * @param property    describes the validated property
     * @param text        the message text
     * 
     * @throws IllegalArgumentException if severity is <code>Severity.OK</code>
     */
    public void add(ValidationResult result, Severity severity, String property, String text) {
        if (severity == Severity.OK)
            throw new IllegalArgumentException("Severity.OK must not be used in validation messages.");
        
        result.add(create(severity, property, text));
    }
    
    
}
