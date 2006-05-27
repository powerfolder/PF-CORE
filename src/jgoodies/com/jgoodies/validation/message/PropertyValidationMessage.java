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
 * An implementation of {@link ValidationMessage} that holds a text message,
 * the validated object (target), a descriptions of the validated property,
 * and a description of the role this object takes in the validation context.
 * The target can be used to identify the source of a validation message. 
 * The role and property together build the <em>aspect</em> that is used 
 * as association key; in other words, it can be used to determine whether 
 * a view is associated with a given message or not.<p>
 * 
 * <strong>Example:</strong> We validate an invoice that has a shipping address 
 * and a physical address. We want to report that the zip code of the shipping 
 * address is missing. This can be described by:<pre>
 * String  validationRole    = "Shipping address";
 * Address validationTarget  = invoice.getShippingAddress();
 * String  validationText    = "is mandatory";
 * String  validationProperty= "zip code";
 * if (validationTarget.getZipCode() ...) {
 *     validationResult.addMessage(
 *         new PropertyValidationMessage(
 *             validationText,
 *             validationTarget,
 *             validationRole,
 *             validationProperty)
 *     );
 * }
 * </pre> 
 * 
 * @author  Karsten Lentzsch
 * @version $Revision: 1.2 $
 */
public final class PropertyValidationMessage extends AbstractValidationMessage {
    
    /**
     * Refers to the object that holds the validated property, 
     * for example an instance of an <code>Address</code> class.
     */
    private final Object target;
    
    /**
     * Describes the optional role of the validated object, for example
     * <em>shipping</em> or <em>physical</em> address.
     */
    private final String role;
    
    /**
     * Holds the name of the validated property, for example &quot;zip code&quot.
     */
    private final String property;


    // Instance Creation ******************************************************

    /**
     * Constructs a PropertyValidationMessage of type warning 
     * for the given text, subject, role description and property description.<p>
     * 
     * <strong>Examples:</strong><pre>
     * new PropertyValidationMessage(
     *    "is mandatory", aCustomer, "Customer", "last name");
     * new PropertyValidationMessage(
     *    "must be over 18", aCustomer, "Customer", "age");
     * 
     * new PropertyValidationMessage(
     *    "is mandatory", shippingAddress, "Shipping address", "zip code");
     * new PropertyValidationMessage(
     *    "is mandatory", shippingAddress, "Physical address", "zip code");
     * </pre>
     * 
     * @param text      describes the validation problem
     * @param target    the object that holds the validated property
     * @param role      describes the target's role in the context
     * @param property  describes the validated property
     * 
     * @throws NullPointerException if the text, target, role, or property 
     *     is <code>null</code>
     * @throws IllegalArgumentException if severity is <code>Severity.OK</code>
     */
    public PropertyValidationMessage(String text, Object target, String role, String property) {
        this(Severity.WARNING, text, target, role, property);
    }
    

    /**
     * Constructs a PropertyValidationMessage for the given text,
     * subject, role description and property description.
     * 
     * <strong>Examples:</strong><pre>
     * new PropertyValidationMessage(
     *    Severity.ERROR, "is mandatory", aCustomer, "Customer", "last name");
     * new PropertyValidationMessage(
     *    Severity.WARNING, "must be over 18", aCustomer, "Customer", "age");
     * 
     * new PropertyValidationMessage(
     *    Severity.ERROR, "is mandatory", shippingAddress, "Shipping address", "zip code");
     * new PropertyValidationMessage(
     *    Severity.ERROR, "is mandatory", physicalAddress, "Physical address", "zip code");
     * </pre>
     * 
     * @param severity  the message severity, either error or warning
     * @param text      describes the validation problem
     * @param target    the object that holds the validated property
     * @param role      describes the target's role in the context
     * @param property  describes the validated property
     * 
     * @throws NullPointerException if the text, target, role, or property 
     *     is <code>null</code>
     * @throws IllegalArgumentException if severity is <code>Severity.OK</code>
     */
    public PropertyValidationMessage(Severity severity, String text, Object target, String role, String property) {
        super(text, severity);
        if (target == null)
            throw new NullPointerException("The target must not be null.");
        if (role == null)
            throw new NullPointerException("The role must not be null.");
        if (property == null)
            throw new NullPointerException("The property must not be null.");
        
        this.target   = target;
        this.role     = role;
        this.property = property;
    }
    
    
    // Accessors **************************************************************
    
    /**
     * Returns the validated object that holds the validated property, 
     * for example an address object. This object can be further described 
     * by a role, for example <em>shipping</em> or <em>physical</em> address.
     * 
     * @return the validation target that holds the validated property
     */
    public Object target() {
        return target;
    }
    
    
    /**
     * Returns a description of the role of the validated object.
     * The role may differ from the type when multiple instances of the same
     * type are validated in a larger container.<p>
     * 
     * Example: An invoice object holds a single <code>Order</code> instance, 
     * and two instances of class <code>Address</code>, one for the shipping 
     * address and another for the physical address. You then may consider
     * using the following roles: <em>Customer, Shipping address</em>, and
     * <em>Physical address</em>. 
     * 
     * @return a description of the role of the validated object
     */
    public String role() {
        return role;
    }


    /**
     * Returns a description of the validated object property, for example
     * &quot;zip code&quot;.
     * 
     * @return a description of the validated property
     */
    public String property() {
        return property;
    }

    
    /**
     * Returns a description of the validated aspect, that is the target's
     * role plus the validated property.<p>
     * 
     * Examples: <pre>
     * "Customer.last name"
     * "Customer.age"
     * "Address.zip code"
     * "Shipping address.zip code"
     * "Physical address.zip code"
     * </pre> 
     * 
     * @return a String that describes the validated aspect
     */
    public String aspect() {
        return role() + "." + property();
    }
    

    /**
     * Returns a message description as formatted text. This implementation 
     * concatenates the validated aspect, i.e. role + property and
     * the message text.
     * 
     * @return a message description as formatted text
     */
    public String formattedText() {
        return aspect() + " " + text();
    }
    

    /**
     * Returns this message's aspect as association key. This key can be used
     * to associate messages with views.<p>
     * 
     * @return this messages's aspect as association key
     * 
     * @see #aspect()
     */
    public Object key() {
        return aspect();
    }
    
    
    // Comparison and Hashing *************************************************
    
    /**
     * Compares the specified object with this validation message for equality.  
     * Returns <code>true</code> if and only if the specified object is also 
     * a property validation message, both messages have the same severity,
     * text, target, role, and property. In other words, two property validation 
     * messages are defined to be equal if and only if they behave one like 
     * the other.<p>
     *
     * This implementation first checks if the specified object is this
     * a property validation message. If so, it returns <code>true</code>; 
     * if not, it checks if the specified object is a property validation message. 
     * If not, it returns <code>false</code>; if so, it checks and returns
     * if the severities, texts, targets, roles, and properties of both messages 
     * are equal.
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
        if (!(o instanceof PropertyValidationMessage)) 
            return false;
        PropertyValidationMessage other = (PropertyValidationMessage) o;
        return severity().equals(other.severity())
            && ValidationUtils.equals(text(), other.text())
            && ValidationUtils.equals(target(), other.target())
            && ValidationUtils.equals(role(), other.role())
            && ValidationUtils.equals(property(), other.property());
    }    

    
    /**
     * Returns the hash code value for this validation message. 
     * This implementation computes and returns the hash based 
     * on the hash code values of this messages' severity, text,
     * target, role, and property.<p>
     * 
     * If this class could be extended, we should check if the formatted text
     * is <code>null</code>.
     *
     * @return the hash code value for this validation message.
     * 
     * @see Object#hashCode()
     */
    public int hashCode() {
        int result = 17;
        result = 37 * result + severity().hashCode();
        result = 37 * result + (text()     == null ? 0 : text().hashCode());
        result = 37 * result + (target()   == null ? 0 : target().hashCode());
        result = 37 * result + (role()     == null ? 0 : role().hashCode());
        result = 37 * result + (property() == null ? 0 : property().hashCode());
        return result;
    }
    

}
