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

package com.jgoodies.binding.list;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ListModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.jgoodies.binding.beans.Model;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

/**
 * A ListModel implementation that delegates to a ListModel 
 * held by a ValueModel.<p>
 * 
 * <strong>Imporant Note:</strong> If you change the ListModel instance, 
 * either by calling <code>#setListModel(ListModel)</code> or by setting 
 * a new value to the underlying list holder, you must ensure that 
 * the list holder throws a PropertyChangeEvent whenever the instance changes. 
 * This event is used to remove a ListDataListener from the old ListModel 
 * instance and is later used to add it to the new ListModel instance.
 * It is easy to violate this constraint, just because Java's standard 
 * PropertyChangeSupport helper class that is used by many beans, checks 
 * a changed property value via <code>#equals</code>, not <code>==</code>. 
 * For example, if you change the SelectionInList's list model from an empty 
 * list <code>L1</code> to another empty list instance <code>L2</code>,
 * the PropertyChangeSupport won't generate a PropertyChangeEvent,
 * and so, the SelectionInList won't know about the change, which 
 * may lead to unexpected behavior.<p>
 * 
 * This class provides public convenience methods for firing ListDataEvents, 
 * see the methods <code>#fireContentsChanged</code>, 
 * <code>#fireIntervalAdded</code>, and <code>#fireIntervalRemoved</code>.
 * These are automatically invoked if the list holder holds a ListModel
 * that fires these events. If on the other hand the underlying List or
 * ListModel does not fire a required ListDataEvent, you can use these
 * methods to notify presentations about a change. It is recommended
 * to avoid sending duplicate ListDataEvents; hence check if the underlying
 * ListModel fires the necessary events or not. Typically an underlying
 * ListModel will fire the add and remove events; but often it'll lack 
 * an event if the (selected) contents has changed.<p>
 * 
 * The current class name "ListModelHolder" and accessor #getListModelHolder 
 * may confuse users: what's the difference between the class (a ListModel) 
 * and its list model holder (a ValueModel)?<p>
 * 
 * TODO: Find a better name and provide a transition to the new class name.
 * I consider using "IndirectListModel" as the new name. The Binding 1.2 may
 * offer the new class and keep the old as deprecated version. 
 * In the Binding 1.3 (or 2.0) the deprecated class can be removed. 
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.9 $
 * 
 * @see ListHolder
 * @see com.jgoodies.binding.extras.SelectionInListModel
 * 
 * @since 1.1
 */
public class ListModelHolder extends Model implements ListModel {

    // Constant Names for Bound Bean Properties *******************************
    
    /**
     * The name of the bound read-write <em>listModel</em> property.
     */
    public static final String PROPERTYNAME_LIST_MODEL = "listModel";

    /**
     * The name of the bound read-write <em>listModelHolder</em> property.
     */
    public static final String PROPERTYNAME_LIST_MODEL_HOLDER = "listModelHolder";

    
    // Instance Fields ********************************************************
    
    /**
     * Holds the ListModel we delegate to.
     */
    private ValueModel listModelHolder;

    /**
     * Holds a copy of the listModelHolder's value. Used as the old ListModel
     * when the listModelHolder's value changes. Required because a ValueModel
     * may use <code>null</code> as old value, but this ListModelHolder
     * must know about the old and the new ListModel.
     */
    protected ListModel listModel;
    
    /**
     * Handles changes of the ListModel.
     */
    private final PropertyChangeListener listModelChangeHandler;

    /**
     * Holds the change handler that fires changes is the ListModel changes.
     */
    protected final ListDataListener listDataChangeHandler;

    /**
     * Holds the ListDataListeners registered with this ListModel implementation.
     * 
     * @see #addListDataListener(ListDataListener)
     * @see #removeListDataListener(ListDataListener)
     */
    private final EventListenerList listenerList = new EventListenerList();


    // Instance Creation ******************************************************

    /**
     * Constructs a ListModelHolder with an empty initial ArrayListModel.
     */
    public ListModelHolder() {     
        this(new ArrayListModel());
    }

    
    /**
     * Constructs a ListModelHolder on the given ListModel.
     * 
     * @param listModel   the initial ListModel
     */
    public ListModelHolder(ListModel listModel) {
        this(new ValueHolder(listModel, true));
    }
    
    
    /**
     * Constructs a ListModelHolder on the given ListModel.<p>
     * 
     * <strong>Constraints:</strong> 
     * 1) The listModelHolder must hold instances of ListModel and 
     * 2) must report a value change whenever the value's identity changes. 
     * Note that many bean properties don't fire a PropertyChangeEvent 
     * if the old and new value are equal - and so would break this constraint.
     * If you provide a ValueHolder, enable its identityCheck feature 
     * during construction. If you provide an adapted bean property from
     * a bean that extends the JGoodies <code>Model</code> class,
     * you can enable the identity check feature in the methods
     * <code>#firePropertyChange</code> by setting the trailing boolean 
     * parameter to <code>true</code>.
     * 
     * @param listModelHolder   provides the ListModel
     * 
     * @throws NullPointerException if the listModelHolder is <code>null</code>
     * @throws IllegalArgumentException if the listModelHolder is a ValueHolder
     *     that doesn't check the identity when changing its value
     * @throws ClassCastException   if the listModelHolder contents is
     *    not an instance of ListModel
     */
    public ListModelHolder(ValueModel listModelHolder) {
        this.listModelHolder = listModelHolder;
        if (listModelHolder == null)
            throw new NullPointerException("The ListModel holder must not be null.");

        checkListModelHolderIdentityCheck(listModelHolder);
        
        listModelChangeHandler = new ListModelChangeHandler();
        listDataChangeHandler  = createListDataChangeHandler();
        
        listModelHolder.addValueChangeListener(listModelChangeHandler);
        // If the ValueModel holds a ListModel observe list data changes too.
        listModel = (ListModel) listModelHolder.getValue();
        if (listModel != null) {
            listModel.addListDataListener(listDataChangeHandler);
        }
    }
    

    // Accessing the ListModel and ListModel Holder ***************************

    /**
     * Returns the contents of the list model holder.
     * 
     * @return the contents of the list model holder.
     * 
     * @see #setListModel(ListModel)
     */
    public final ListModel getListModel() {
        return (ListModel) getListModelHolder().getValue();
    }
    
    
    /**
     * Sets the given list model as value of the list holder.
     * 
     * @param newListModel   the list model to be set as new list content
     *     
     * @see #getListModel()
     */
    public final void setListModel(ListModel newListModel) {
        getListModelHolder().setValue(newListModel);
    }
    
    
    // Accessing the Holders for: List, Selection and Index *******************

    /**
     * Returns the ValueModel that holds the ListModel we delegate to.
     * 
     * @return the ValueModel that holds the ListModel we delegate to.
     */
    public final ValueModel getListModelHolder() {
        return listModelHolder;
    }
    
    /**
     * Sets a new ListModel holder. Does nothing if old and new holder are equal.
     * Removes the list change handler from the old holder and adds
     * it to the new one. The list data change handler is re-registered
     * by invoking <code>#updateListModel</code>.<p>
     * 
     * TODO: Check and verify whether the list data registration update
     * can be performed in one step <em>after</em> the listHolder has been
     * changed - instead of remove the list data change handler, then
     * changing the listHolder, and finally adding the list data change handler.
     * 
     * @param newListModelHolder   the ListModel holder to be set
     * 
     * @throws NullPointerException if the new ListModel holder is <code>null</code>
     * @throws IllegalArgumentException if the new ListModel holder
     *     is a ValueHolder that has the identityCheck feature disabled
     * @throws ClassCastException if the new ListModel holder's value is not a ListModel
     */
    public final void setListModelHolder(ValueModel newListModelHolder) {
        if (newListModelHolder == null) 
            throw new NullPointerException("The new ListModel holder must not be null.");
        checkListModelHolderIdentityCheck(newListModelHolder);
        
        ValueModel oldListModelHolder = getListModelHolder();
        if (equals(oldListModelHolder, newListModelHolder))
            return;
        
        ListModel oldListModel = listModel;
        ListModel newListModel = (ListModel) newListModelHolder.getValue();
        
        oldListModelHolder.removeValueChangeListener(listModelChangeHandler);
        listModelHolder = newListModelHolder;
        newListModelHolder.addValueChangeListener(listModelChangeHandler);

        updateListModel(oldListModel, newListModel);
        firePropertyChange(PROPERTYNAME_LIST_MODEL_HOLDER, 
                           oldListModelHolder, 
                           newListModelHolder);
    }
    

    // Convenience Code *******************************************************
    
    /**
     * Checks and answers if the ListModel is empty or <code>null</code>.
     * 
     * @return true if the ListModel is empty or <code>null</code>, false otherwise
     */
    public final boolean isEmpty() {
        return getSize() == 0;
    }
    

    // ListModel Implementation ***********************************************

    /**
     * Returns the length of the list, <code>0</code> if the list model 
     * is <code>null</code>.
     * 
     * @return the size of the list, <code>0</code> if the list model is 
     *     <code>null</code>
     */
    public final int getSize() {
        return getSize(getListModel());
    }
    
    
    /**
     * Returns the value at the specified index.  
     * 
     * @param index  the requested index
     * @return the value at <code>index</code>
     *      
     * @throws NullPointerException if the list model holder's content is null
     */
    public final Object getElementAt(int index) {
        return getListModel().getElementAt(index);
    }
    

    /**
     * Adds a listener to the list that's notified each time a change
     * to the data model occurs.
     *
     * @param l the <code>ListDataListener</code> to be added
     */  
    public final void addListDataListener(ListDataListener l) {
        listenerList.add(ListDataListener.class, l);
    }


    /**
     * Removes a listener from the list that's notified each time a 
     * change to the data model occurs.
     *
     * @param l the <code>ListDataListener</code> to be removed
     */  
    public final void removeListDataListener(ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
    }
    

    /**
     * Returns an array of all the list data listeners
     * registered on this <code>SelectionInList</code>.
     *
     * @return all of this model's <code>ListDataListener</code>s,
     *         or an empty array if no list data listeners
     *         are currently registered
     * 
     * @see #addListDataListener(ListDataListener)
     * @see #removeListDataListener(ListDataListener)
     */
    public final ListDataListener[] getListDataListeners() {
        return (ListDataListener[]) listenerList.getListeners(
                ListDataListener.class);
    }


    // Notifying ListDataListeners ********************************************
    
    /**
     * Notifies all registered ListDataListeners that the contents
     * of one or more list elements has changed.  
     * The changed elements are specified by the closed interval index0, index1 
     * -- the end points are included. Note that index0 need not be less than 
     * or equal to index1.
     * 
     * @param index0 one end of the new interval
     * @param index1 the other end of the new interval
     * 
     * @see ListModel
     * @see ListDataListener
     * @see ListDataEvent
     */
    public final void fireContentsChanged(int index0, int index1) {
        Object[] listeners = listenerList.getListenerList();
        ListDataEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ListDataListener.class) {
                if (e == null) {
                    e = new ListDataEvent(this,
                            ListDataEvent.CONTENTS_CHANGED, index0, index1);
                }
                ((ListDataListener) listeners[i + 1]).contentsChanged(e);
            }
        }
    }

    
    /**
     * Notifies all registered ListDataListeners that one or more elements 
     * have been added to this SelectionInList's List/ListModel. 
     * The new elements are specified by a closed interval index0, index1 
     * -- the end points are included. Note that index0 need not be less than 
     * or equal to index1.
     * 
     * @param index0 one end of the new interval
     * @param index1 the other end of the new interval
     * 
     * @see ListModel
     * @see ListDataListener
     * @see ListDataEvent
     */
    public final void fireIntervalAdded(int index0, int index1) {
        Object[] listeners = listenerList.getListenerList();
        ListDataEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ListDataListener.class) {
                if (e == null) {
                    e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index0, index1);
                }
                ((ListDataListener) listeners[i + 1]).intervalAdded(e);
            }
        }
    }


    /**
     * Notifies all registered ListDataListeners that one or more elements 
     * have been removed from this SelectionInList's List/ListModel.
     * <code>index0</code> and <code>index1</code> are the end points
     * of the interval that's been removed.  Note that <code>index0</code>
     * need not be less than or equal to <code>index1</code>.
     * 
     * @param index0 one end of the removed interval,
     *               including <code>index0</code>
     * @param index1 the other end of the removed interval,
     *               including <code>index1</code>
     *               
     * @see ListModel
     * @see ListDataListener
     * @see ListDataEvent
     */
    public final void fireIntervalRemoved(int index0, int index1) {
        Object[] listeners = listenerList.getListenerList();
        ListDataEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ListDataListener.class) {
                if (e == null) {
                    e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index0, index1);
                }
                ((ListDataListener) listeners[i + 1]).intervalRemoved(e);
            }
        }
    }
    
    
    // Default Behavior *******************************************************
    
    /**
     * Creates and returns the ListDataListener used to observe
     * changes in the underlying ListModel. It is re-registered
     * in <code>#updateListModel</code>.
     * 
     * @return the ListDataListener that handles changes 
     *     in the underlying ListModel
     */
    protected ListDataListener createListDataChangeHandler() {
        return new ListDataChangeHandler();
    }
    
    
    /**
     * Removes the list data change handler from the old list in case
     * it is not <code>null</code> and adds it to new one in case
     * it is a not <code>null</code>.
     * Also fires a property change for <em>listModel</em> and 
     * a ListModel update event.
     * 
     * @param oldListModel   the old ListModel
     * @param newListModel   the new ListModel
     */
    protected void updateListModel(ListModel oldListModel, ListModel newListModel) {
        if (oldListModel != null) {
            oldListModel.removeListDataListener(listDataChangeHandler);
        }
        if (newListModel != null) {
            newListModel.addListDataListener(listDataChangeHandler);
        }
        listModel = newListModel;
        firePropertyChange(PROPERTYNAME_LIST_MODEL, oldListModel, newListModel);
        fireListChanged(getSize(oldListModel) - 1, getSize(newListModel) - 1);
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
    protected final void fireListChanged(int oldLastIndex, int newLastIndex) {
        if (oldLastIndex == newLastIndex) {
            if (newLastIndex == -1) {
                return;
            }
            fireContentsChanged(0, newLastIndex);
        } else {
            if (oldLastIndex >= 0) {
                fireIntervalRemoved(0, oldLastIndex);
            }
            if (newLastIndex >= 0) {
                fireIntervalAdded(0, newLastIndex);
            }
        }
    }
    
    
    /**
     * Returns the lists size or 0 if the list is null.
     * 
     * @param aListOrNull  a List or null
     * @return the list's size or 0 if the list is null
     */
    protected final int getSize(ListModel aListOrNull) {
        return aListOrNull == null ? 0 : aListOrNull.getSize();
    }

    
    // Helper Code ************************************************************

    /**
     * Throws an IllegalArgumentException if the given ValueModel
     * is a ValueHolder that has the identityCheck feature disabled.
     */
    private void checkListModelHolderIdentityCheck(ValueModel aListModelHolder) 
        throws IllegalArgumentException {
        if (!(aListModelHolder instanceof ValueHolder))
            return;
        
        ValueHolder valueHolder = (ValueHolder) aListModelHolder;
        if (!valueHolder.isIdentityCheckEnabled()) 
            throw new IllegalArgumentException(
                 "The ListModel holder must have the identity check enabled."); 
    }
    
    
    // Event Handlers *********************************************************
        
    /**
     * Handles changes of the list model.
     */
    private final class ListModelChangeHandler implements PropertyChangeListener {

        /**
         * The list has been changed.
         * Notifies all registered listeners about the change.
         * 
         * @param evt   the property change event to be handled
         */
        public void propertyChange(PropertyChangeEvent evt) {
            ListModel oldListModel = listModel;
            ListModel newListModel = (ListModel) evt.getNewValue();
            updateListModel(oldListModel, newListModel);
        }
    }

    
    /**
     * Handles ListDataEvents in the underlying ListModel
     * and just forwards them to this ListModelHolder, 
     * which provides the necessary methods #fireXXX.
     */
    private final class ListDataChangeHandler implements ListDataListener {

        /** 
         * Sent after the indices in the index0, index1 
         * interval have been inserted in the data model.
         * The new interval includes both index0 and index1.
         *
         * @param evt  a <code>ListDataEvent</code> encapsulating the
         *    event information
         */
        public void intervalAdded(ListDataEvent evt) {
            fireIntervalAdded(evt.getIndex0(), evt.getIndex1());
        }

        
        /**
         * Sent after the indices in the index0, index1 interval
         * have been removed from the data model.  The interval 
         * includes both index0 and index1.
         *
         * @param evt  a <code>ListDataEvent</code> encapsulating the
         *    event information
         */
        public void intervalRemoved(ListDataEvent evt) {
            fireIntervalRemoved(evt.getIndex0(), evt.getIndex1());
        }


        /** 
         * Sent when the contents of the list has changed in a way 
         * that's too complex to characterize with the previous 
         * methods. For example, this is sent when an item has been
         * replaced. Index0 and index1 bracket the change.
         *
         * @param evt  a <code>ListDataEvent</code> encapsulating the
         *    event information
         */
        public void contentsChanged(ListDataEvent evt) {
            fireContentsChanged(evt.getIndex0(), evt.getIndex1());
        }
        
    }


}
