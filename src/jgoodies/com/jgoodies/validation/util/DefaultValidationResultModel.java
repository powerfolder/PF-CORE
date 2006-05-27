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

import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;

/**
 * A default implementation of the {@link ValidationResultModel} interface
 * that holds a ValidationResult.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.3 $
 */
public class DefaultValidationResultModel extends AbstractValidationResultModel {

    /**
     * Holds this model's validation result.
     */
    private ValidationResult validationResult;
    

    // Instance Creation ******************************************************

    /**
     * Constructs a DefaultValidationResultModel initialized
     * with an empty validation result.
     */
    public DefaultValidationResultModel() {
        validationResult = ValidationResult.EMPTY;
    }
    

    // Accessors **************************************************************

    /**
     * Returns this model's validation result.
     * 
     * @return the current validation result
     */
    public final ValidationResult getResult() {
        return validationResult;
    }
    
    
    /**
     * Sets a new validation result and notifies all registered listeners
     * about changes of the result itself and the properties for severity,
     * errors and messages. This method is typically invoked at the end of
     * the <code>#validate()</code> method.
     * 
     * @param newResult  the validation result to be set
     * 
     * @throws NullPointerException if the new result is <code>null</code>
     * 
     * @see #getResult()
     * @see ValidationResultModelContainer#setResult(ValidationResult)
     */
    public void setResult(ValidationResult newResult) {
        if (newResult == null)
            throw new NullPointerException("The new result must not be null.");
        
        ValidationResult oldResult = getResult();
        validationResult = newResult;
        firePropertyChanges(oldResult, newResult);
    }

}
