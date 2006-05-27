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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.jgoodies.validation.ValidationMessage;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;

/**
 * A validation result model that holds multiple validation results. 
 * These can be concatenated or collapsed, see {@link #setExpanded(boolean)}, 
 * {@link #getCollapsedValidationResult()}.<p> 
 * 
 * TODO: The collapsed validation result shall honor the severity as
 * reported by the expanded validation result.<p>
 * 
 * TODO: The expanded validation result shall be cached.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.3 $
 * 
 * @see     com.jgoodies.validation.util.DefaultValidationResultModel
 */

public class ValidationResultModelContainer extends DefaultValidationResultModel {

    // Names of Bound Bean Properties *****************************************
    
    /**
     * The name of the bound read-write property for the expansion state.
     * 
     * @see #isExpanded()
     * @see #setExpanded(boolean)
     */
    public static final String PROPERTYNAME_EXPANDED = "expanded";
    
    
    // Instance Fields ********************************************************
    
    /**
     * Refers to the {@link ValidationMessage} that is used in
     * the collapsed state.
     * 
     * @see #getCollapsedValidationResult()
     */
    private final ValidationMessage collapsedMessage;
    
    /**
     * Indicates whether the contained validation results shall be
     * reported as expanded or collapsed. In expanded state the individual
     * validation results are concatenated; in collapsed state a 
     * {@link ValidationResult} will be created that consists only
     * of the <em>collapsedMessage</em>.
     * 
     * @see #isExpanded()
     * @see #setExpanded(boolean)
     */
    private boolean expanded;
    
    /**
     * A list that holds the child {@link ValidationResultModel}s.
     */
    private final List childResultModels;
    
    /**
     * Listens to changes in the validation result of a child validation result
     * model and updates the container result.
     */
    private final PropertyChangeListener childResultUpdateHandler;
    
    
    // Instance Creation ******************************************************

    /**
     * Constructs a <code>ValidationResultModelContainer</code> using the
     * given validation message for the collapsed state. The initial state
     * is <em>expanded</em>.
     * 
     * @param collapsedMessage   the <code>ValidationMessage</code> used
     *     if the container is in collapsed state
     * @throws NullPointerException  if the collapsed message is <code>null</code>
     */
    public ValidationResultModelContainer(ValidationMessage collapsedMessage) {
        this.collapsedMessage = collapsedMessage;
        childResultModels = new LinkedList();
        expanded = true;
        childResultUpdateHandler = new ChildResultUpdateHandler();
    }
    

    // Accessors **************************************************************
    
    /**
     * Returns if this container is in expanded or collapsed state.
     * 
     * @return true if expanded
     * 
     * @see #setExpanded(boolean)
     */
    public boolean isExpanded() {
        return expanded;
    }
    
    /**
     * Sets the container state to expanded or collapsed.
     * 
     * @param expanded   true to expand, false to collapse
     * 
     * @see #isExpanded()
     */
    public void setExpanded(boolean expanded) {
        boolean oldExpanded = isExpanded();
        this.expanded = expanded;
        updateContainerResult();
        firePropertyChange(PROPERTYNAME_EXPANDED, oldExpanded, expanded);
    }
        
    
    /**
     * Returns the collapsed validation result, i. e. a {@link ValidationResult}
     * that just consists of a single {@link ValidationMessage}; in this case
     * the collapsed validation message.<p>
     * 
     * TODO: Consider changing the implementation to <pre>
     * ValidationResult wrapper = new ValidationResult();
        if (getExpandedValidationResult().hasMessages()) {
            wrapper.add(collapsedMessage);
        }
        return wrapper;
     * </pre>
     * 
     * @return the collapsed validation result
     * 
     * @see #getExpandedValidationResult()
     */
    public ValidationResult getCollapsedValidationResult() {
        ValidationResult wrapper = new ValidationResult();
        wrapper.add(collapsedMessage);
        return wrapper;
    }
    
    /**
     * Returns the concatenation of all validation results that are held
     * by this container. 
     * 
     * @return the concatenation of all validation resuls that are held
     *      by this container
     * 
     * @see #getCollapsedValidationResult()
     */
    public ValidationResult getExpandedValidationResult() {
        ValidationResult concatenation = new ValidationResult();
        for (Iterator it = childResultModels.iterator(); it.hasNext();) {
            ValidationResultModel resultModel = (ValidationResultModel) it.next();
            concatenation.addAllFrom(resultModel.getResult());
        }
        return concatenation;
    }
    
    
    // Managing Child ValidationResultModels **********************************
    
    /**
     * Adds the given {@link ValidationResultModel} to this container's list
     * of children and registers the container to listen for changes in the child.
     * 
     * @param resultModel   the <code>ValidationResultModel</code> to be added
     * 
     * @see #remove(ValidationResultModel)
     */
    public void add(ValidationResultModel resultModel) {
        childResultModels.add(resultModel);
        resultModel.addPropertyChangeListener(
                ValidationResultModel.PROPERTYNAME_RESULT,
                childResultUpdateHandler);
    }

    /**
     * Removes the gieven {@link ValidationResultModel} from this container's
     * list of children. Also deregisters this container to no longer listen
     * for changes in the child.
     * 
     * @param resultModel   the <code>ValidationResultModel</code> to be removed
     * 
     * @see #add(ValidationResultModel)
     */
    public void remove(ValidationResultModel resultModel) {
        childResultModels.remove(resultModel);
        resultModel.removePropertyChangeListener(
                ValidationResultModel.PROPERTYNAME_RESULT,
                childResultUpdateHandler);
    }
    
    
    // Accessors **************************************************************

    /**
     * Rejects to set a new validation result. The validation result of this
     * container will be computed. Therefore API users are prevented from 
     * executing this method; throws an {@link UnsupportedOperationException}.
     * 
     * @param newResult  the validation result to be set (ignored)
     * @throws UnsupportedOperationException  always
     * 
     * @see #getResult()
     */
    public final void setResult(ValidationResult newResult) {
        throw new UnsupportedOperationException(
                "Cannot set a validation result for ValidationResultModelContainer. The result will be computed from the contained results instead.");
    }
    
    
    // Child Result Updates ***************************************************
    
    /**
     * Updates the container's compound result according to the current
     * expansion state.
     * 
     * @see #getExpandedValidationResult()
     * @see #getCollapsedValidationResult()
     */
    private void updateContainerResult() {
        ValidationResult newResult = isExpanded()
                ? getExpandedValidationResult()
                : getCollapsedValidationResult();
        super.setResult(newResult);
    }
    
    
    /**
     * Listens to changes in the property update.
     */
    private final class ChildResultUpdateHandler implements PropertyChangeListener {
        
        /**
         * The <em>validation result</em> property of a child has changed.
         * updates the container result which in turn will notify registered
         * users that a new version is coming.
         * 
         * @param evt   describes the property change
         */
        public void propertyChange(PropertyChangeEvent evt) {
            updateContainerResult();
        }
    }
    
    

}
