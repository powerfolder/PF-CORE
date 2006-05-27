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
 
package com.jgoodies.validation.view;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractListModel;

import com.jgoodies.validation.ValidationResultModel;

/**
 * Converts a {@link ValidationResultModel} into a {@link javax.swing.ListModel}.
 * Useful to bind <code>JLists</code> and <code>JTables</code> to a 
 * validation result model.
 * 
 * @author  Karsten Lentzsch
 * @version $Revision: 1.4 $
 */
public final class ValidationResultListAdapter extends AbstractListModel {

    /**
     * Refers to this adapter's underlying validation result model
     * that provides the messages and error and warning state. 
     */
    private final ValidationResultModel model;
    
    /**
     * Used to fire the correct interval size in ListDataEvents
     * fired by this adapter after a validation result change.
     * Describes this adapter's last index before the change.
     */
    private int oldSize;

    
    // Instance Creation ******************************************************
    
    /**
     * Constructs a ValidationResultListAdapter that converts 
     * the given model into a ListModel. Observes the given model 
     * to update this adapter everytime the validation result changes.
     * 
     * @param model  the model that provides the validation result
     */
    public ValidationResultListAdapter(ValidationResultModel model) {
        this.model = model;
        this.oldSize = getSize();
        model.addPropertyChangeListener(
                ValidationResultModel.PROPERTYNAME_RESULT,
                new ValidationResultHandler());
    }
    
    
    // Implementing Abstract Behavior *****************************************

    /**
     * Returns the validation result message at the given index.
     * 
     * @param index   the index of the message to return
     * @return the validation result message at the given index
     * @see javax.swing.ListModel#getElementAt(int)
     */
    public Object getElementAt(int index) {
        return model.getResult().getMessages().get(index);
    }

    
    /**
     * Returns the number of validation result messages.
     * 
     * @return the number of validation result messages
     * @see javax.swing.ListModel#getSize()
     */
    public int getSize() {
        return model.getResult().getMessages().size();
    }
    

    // ValidationResultModel Listeners ****************************************
    
    /**
     * Handles changes in the underlying ValidationResult and fires ListDataEvents. 
     */
    private final class ValidationResultHandler implements PropertyChangeListener {
        
        /**
         * Notifies all registered ListDataListeners that the ValidationResult 
         * in this adapter's underlying ValidationResultModel has changed.  
         */
        public void propertyChange(PropertyChangeEvent evt) {
            int newSize = getSize();
            int oldLastIndex = oldSize - 1;
            int newLastIndex = newSize - 1;
            oldSize = newSize;
            fireListChanged(oldLastIndex, newLastIndex);
        }
        
        /**
         * Notifies all registered ListDataListeners that this ListModel
         * has changed from an old list to a new list content. 
         * If the old and new list have the same size, 
         * a content change event is fired. 
         * Otherwise, two events are fired: 
         * a remove event for the old list's interval, 
         * and a add event for the new list's interval.<p>
         * 
         * This method is invoked by #updateList during the transition
         * from an old List(Model) to a new List(Model).
         * 
         * @param oldLastIndex   the last index of the old list
         * @param newLastIndex   the last index of the new list
         */
        private void fireListChanged(int oldLastIndex, int newLastIndex) {
            if (oldLastIndex == newLastIndex) {
                if (newLastIndex == -1) {
                    return;
                }
                fireContentsChanged(ValidationResultListAdapter.this, 
                        0, newLastIndex);
            } else {
                if (oldLastIndex >= 0) {
                    fireIntervalRemoved(ValidationResultListAdapter.this,
                            0, oldLastIndex);
                }
                if (newLastIndex >= 0) {
                    fireIntervalAdded(ValidationResultListAdapter.this,
                            0, newLastIndex);
                }
            }
        }

    
    }
    
    
}
