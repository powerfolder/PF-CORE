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

import java.io.Serializable;


/**
 * A typesafe enumeration for the different severities used in instances of 
 * {@link ValidationMessage}. Useful to categorize validation results, for 
 * example to prevent an object from being saved if the primary key is missing.
 * It is an ordinal-based comparable and serializable enumeration implementation
 * that is not extensible.<p>
 * 
 * The severity is used in almost all views that present validation messages 
 * and is used in operations on instances of 
 * {@link com.jgoodies.validation.ValidationResult}. 
 * 
 * @author  Karsten Lentzsch
 * @version $Revision: 1.2 $
 * 
 * @see com.jgoodies.validation.ValidationMessage
 * @see com.jgoodies.validation.ValidationResult
 */
public final class Severity implements Comparable, Serializable {
    
    /**
     * Indicates a problem that cannot be resumed or worked around. 
     * For example it prevents an edited value to be saved.
     */
    public static final Severity ERROR   = new Severity("Error");
    
    /**
     * Indicates a problem that can be resumed or handled in 
     * a reasonable way. For example a field value may break 
     * a constraint but the object can be temporarily saved.
     */
    public static final Severity WARNING = new Severity("Warning");

    /**
     * Returned by empty validation results to indicate that
     * no problem has been detected, or in other words, everything is fine.
     */
    public static final Severity OK = new Severity("OK");

    /**
     * Holds a transient name useful for debugging purposes.
     */
    private final transient String name;

    
    private Severity(String name) { 
        this.name = name; 
    }

    
    /**
     * Returns the name this object's string representation. 
     * 
     * @return this object's name
     */
    public String toString() { 
        return name; 
    }
    
  
    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.<p>
     * 
     * This implementation is consistent with <code>#equals</code>, i. e.
     * <code>(x.compareTo(y)==0) == (x.equals(y))</code>.<p>
     * 
     * The implementation is based on the internal ordinal.
     * 
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *      is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *      from being compared to this Object.
     */
    public int compareTo(Object o) {
        return ordinal - ((Severity) o).ordinal;
    }
    
    
    /**
     * Returns the higher of the given severities by comparing their ordinals.
     * Since the higher severities have a lower ordinal, this method returns
     * the severity with the lower ordinal.
     *  
     * @param severity1  the first severity to check
     * @param severity2  the second severity to check
     * @return the higher of the given severities
     */
    public static Severity max(Severity severity1, Severity severity2) {
        return VALUES[Math.min(severity1.ordinal, severity2.ordinal)];
    }
    
    
    // Serialization *********************************************************
    
    private static int nextOrdinal = 0;
    
    private final int ordinal = nextOrdinal++;
    
    private static final Severity[] VALUES = {ERROR, WARNING, OK};
    
    private Object readResolve() {
        return VALUES[ordinal];  // Canonicalize
    }


}
