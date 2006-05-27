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

package com.jgoodies.binding.beans;

import java.beans.PropertyDescriptor;

/**
 * A runtime exception that describes read and write access problems when
 * getting/setting a Java Bean property.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.2 $
 * @see com.jgoodies.binding.beans.PropertyAdapter
 */
public final class PropertyAccessException extends PropertyException {

    
    /**
     * Constructs a new exception instance with the specified detail message
     * and cause.
     * 
     * @param message   the detail message (which is saved for later retrieval 
     *     by the {@link #getMessage()} method).
     * @param cause     the cause (which is saved for later retrieval by the
     *     {@link #getCause()} method). (A <code>null</code> value is permitted,
     *     and indicates that the cause is nonexistent or unknown.)
     */
    public PropertyAccessException(String message, Throwable cause) {
        super(message, cause);
    }
    

    /**
     * Creates and returns a new PropertyAccessException instance for a failed
     * read access for the specified bean, property descriptor and cause.
     * 
     * @param bean                  the target bean
     * @param propertyDescriptor    describes the bean's property
     * @param cause                 the Throwable that caused this exception
     * @return an exception that describes a read access problem
     */
    public static PropertyAccessException createReadAccessException(
        Object bean,
        PropertyDescriptor propertyDescriptor,
        Throwable cause) {

        String beanType = bean  == null ? null : bean.getClass().getName();
        String message =
            "Failed to read an adapted Java Bean property."
            + "\ncause="
            + cause
            + "\nbean="
            + bean
            + "\nbean type="
            + beanType
            + "\nproperty name="
            + propertyDescriptor.getName()
            + "\nproperty type="
            + propertyDescriptor.getPropertyType().getName()
            + "\nproperty reader="
            + propertyDescriptor.getReadMethod();

        return new PropertyAccessException(message, cause);
    }
    

    /**
     * Creates and returns a new PropertyAccessException instance for a failed
     * write access for the specified bean, value, property descriptor and
     * cause.
     * 
     * @param bean               the target bean
     * @param value              the value that could not be set
     * @param propertyDescriptor describes the bean's property
     * @param cause             the Throwable that caused this exception
     * @return an exception that describes a write access problem
     */
    public static PropertyAccessException createWriteAccessException(
        Object bean,
        Object value,
        PropertyDescriptor propertyDescriptor,
        Throwable cause) {

        String beanType  = bean  == null ? null : bean.getClass().getName();
        String valueType = value == null ? null : value.getClass().getName();
        String message =
            "Failed to set an adapted Java Bean property."
            + "\ncause="
            + cause
            + "\nbean="
            + bean
            + "\nbean type="
            + beanType
            + "\nvalue="
            + value
            + "\nvalue type="
            + valueType
            + "\nproperty name="
            + propertyDescriptor.getName()
            + "\nproperty type="
            + propertyDescriptor.getPropertyType().getName()
            + "\nproperty setter="
            + propertyDescriptor.getWriteMethod();

        return new PropertyAccessException(message, cause);
    }

}
