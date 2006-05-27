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

package com.jgoodies.validation;


/**
 * Describes validation messages as used by the JGoodies Validation framework.
 * All validation messages provide a formatted text ({@link #formattedText()})
 * and are categorized into types of different severity (@link #severity()}). 
 * Validation messages are collected during the validation process and
 * are held by instances of {@link com.jgoodies.validation.ValidationResult}.<p>
 * 
 * This class has been designed to be decoupled from user interface components 
 * (views) that present and edit the validated data. The design goal is to be
 * able to use the same validation mechanism on the server side, in the domain 
 * layer, in a view-less model layer, and in the presentation layer.
 * And we want to ensure that multiple views can present the same model,
 * and so we typically don't store a view in the validation message.
 * On the other hand we want to detect which validation messages belongs
 * to a given user interface component, for example to let the component
 * paint a warning indication. 
 * This association between message and view is established by the message key
 * that can be shared between messages, validators, views, and other parties.
 * It can be requested using the {@link #key()} method. The association is
 * checked using <code>#equals</code>; implementors that use rich objects
 * as keys may consider overriding <code>#equals</code>.<p>
 * 
 * For example, a validator validates an address object and reports
 * that the zip code is invalid. You may choose the association key 
 * as <code>"address.zipCode"</code>. All views that present the zip code
 * can now check and verify whether a validation result contains messages
 * with this key and may paint a special warning background. 
 * If the validated data contains two different address objects, let's say
 * a shipping address and a physical address, the address validator may
 * add a prefix and create keys like <code>physical.address.zipCode</code>
 * and <code>shipping.address.zipCode</code>. A view can now differentiate
 * between the two zip codes.<p>
 * 
 * We've choosen to let the <code>ValidationMessage</code> check whether
 * an association key matches or not. This way, an implementation of this 
 * interface can choose to provide special checks. The default behavior
 * in class {@link com.jgoodies.validation.message.AbstractValidationMessage}
 * just checks whether a given association key equals a stored key.<p>
 * 
 * Implementors may hold additional objects, for example the validation target, 
 * a description of the target, or a description of the validated property.
 * Implementors are encouraged to implement <code>#equals</code> and 
 * <code>#hashCode</code> to prevent unnecessary change notifications
 * for the <em>result</em> property when a ValidationResultModel
 * gets a new ValidationResult. See for example the implementation of method
 * {@link com.jgoodies.validation.message.PropertyValidationMessage#equals(Object)}.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.3 $
 * 
 * @see     com.jgoodies.validation.ValidationResult
 * @see     com.jgoodies.validation.message.AbstractValidationMessage
 */
public interface ValidationMessage {
    
    /**
     * Returns this message's severity: error or warning.
     * <code>Severity.OK</code> is not allowed as the severity
     * of a single message, but OK is a valid ValidationResult severity.
     * 
     * @return this message's severity: error or warning
     */
    Severity severity();
    
    
    /** 
     * Returns a formatted text that describes the validation issue 
     * this message represents.
     * 
     * @return the message as a formatted text
     */
    String formattedText();
    
    
    /**
     * Returns this message's association key that can be used to model 
     * a loose coupling between validation messages and views that present 
     * the validated data. See the class comment for more information 
     * about this relation.
     * 
     * @return this message's association key
     */
    Object key();
    
    
}
