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

/**
 * A runtime exception that describes that a Java Bean property
 * could not be found.
 * 
 * @author  Karsten Lentzsch
 * @version $Revision: 1.3 $
 * 
 * @see     com.jgoodies.binding.beans.PropertyAdapter
 */
public final class PropertyNotFoundException extends PropertyException {

    /** 
     * Constructs a new exception instance with the specified detail message.
     * The cause is not initialized.
     *
     * @param propertyName   the name of the property that could not be found
     * @param bean           the Java Bean used to lookup the property
     */
    public PropertyNotFoundException(String propertyName, Object bean) {
        this(propertyName, bean, null);
    }


    /**
     * Constructs a new exception instance with the specified detail message 
     * and cause.  
     *
     * @param propertyName   the name of the property that could not be found
     * @param bean           the Java Bean used to lookup the property
     * @param cause          the cause (which is saved for later retrieval by the
     *    {@link #getCause()} method).  (A <tt>null</tt> value is
     *    permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public PropertyNotFoundException(String propertyName, Object bean, Throwable cause) {
        super("Property '" + propertyName + "' not found in bean " + bean, cause);
    }
    

    /**
     * Constructs a new exception instance with the specified detail message 
     * and cause.  
     *
     * @param propertyName   the name of the property that could not be found
     * @param beanClass      the Java Bean class used to lookup the property
     * @param cause          the cause (which is saved for later retrieval by the
     *    {@link #getCause()} method).  (A <tt>null</tt> value is
     *    permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public PropertyNotFoundException(String propertyName, Class beanClass, Throwable cause) {
        super("Property '" + propertyName + "' not found in bean class " + beanClass, cause);
    }
    
}
