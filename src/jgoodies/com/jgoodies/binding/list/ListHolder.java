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
import java.util.ArrayList;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.jgoodies.binding.beans.Model;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

/**
 * A ListModel implementation that looks up its elements
 * from a List held by a ValueModel.
 * This class provides public convenience methods for firing ListDataEvents, 
 * see the methods <code>#fireContentsChanged</code>, 
 * <code>#fireIntervalAdded</code>, and <code>#fireIntervalRemoved</code>.<p>
 * 
 * The current class name "ListHolder" and accessor #getListHolder 
 * may confuse users: what's the difference between the class (a ListModel) 
 * and its list holder (a ValueModel)?<p>
 * 
 * TODO: Find a better name and provide a transition to the new class name.
 * I consider using "IndirectList" as the new name. The Binding 1.2 may
 * offer the new class and keep the old as deprecated version. 
 * In the Binding 1.3 (or 2.0) the deprecated class can be removed. 
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.8 $
 * 
 * @see ListModelHolder
 * @see com.jgoodies.binding.extras.SelectionInList2
 * 
 * @since 1.1
 */
public class ListHolder extends Model implements ListModel {

    // Constant Names for Bound Bean Properties *******************************
    
    /**
     * The name of the bound read-write <em>list</em> property.
     */
    public static final String PROPERTYNAME_LIST = "list";

    /**
     * The name of the bound read-write <em>listHolder</em> property.
     */
    public static final String PROPERTYNAME_LIST_HOLDER = "listHolder";

    
    // Instance Fields ********************************************************
    
    /**
     * Holds the List we delegate to.
     */
    private ValueModel listHolder;

    /**
     * Holds a copy of the listHolder's value. Used as the old List
     * when the listHolder's value changes. Required because a ValueModel
     * may use <code>null</code> as old value, but this ListModelHolder
     * must know about the old and the new List.
     */
    protected List list;
    
    /**
     * Handles changes of the List.
     */
    private final PropertyChangeListener listChangeHandler;

    /**
     * Holds the ListDataListeners registered with this ListModel implementation.
     * 
     * @see #addListDataListener(ListDataListener)
     * @see #removeListDataListener(ListDataListener)
     */
    private final EventListenerList listenerList = new EventListenerList();


    // Instance Creation ******************************************************

    /**
     * Constructs a ListHolder with an empty initial ArrayList.
     */
    public ListHolder() {     
        this(new ArrayList());
    }

    
    /**
     * Constructs a ListHolder on the given List.
     * 
     * @param list   the initial List
     */
    public ListHolder(List list) {
        this(new ValueHolder(list, true));
    }
    
    
    /**
     * Constructs a ListHolder on the given List.
     * 
     * @param listHolder   provides the List
     * 
     * @throws NullPointerException if the listHolder is <code>null</code>
     * @throws ClassCastException   if the listHolder contents is
     *    not an instance of List
     */
    public ListHolder(ValueModel listHolder) {
        this.listHolder = listHolder;
        if (listHolder == null)
            throw new NullPointerException("The List holder must not be null.");
        
        listChangeHandler = new ListChangeHandler();
        
        listHolder.addValueChangeListener(listChangeHandler);
        list = (List) listHolder.getValue();
    }
    

    // Accessing the ListModel and ListModel Holder ***************************

    /**
     * Returns the contents of the list holder.
     * 
     * @return the contents of the list holder.
     * 
     * @see #setList(List)
     */
    public final List getList() {
        return (List) getListHolder().getValue();
    }
    
    
    /**
     * Sets the given List as value of the list holder.
     * 
     * @param newList   the List to be set as new list content
     * 
     * @see #getList()
     */
    public final void setList(List newList) {
        getListHolder().setValue(newList);
    }
    
    
    // Accessing the Holders for: List, Selection and Index *******************

    /**
     * Returns the ValueModel that holds the List we delegate to.
     * 
     * @return the ValueModel that holds the List we delegate to.
     */
    public final ValueModel getListHolder() {
        return listHolder;
    }
    
    /**
     * Sets a new List holder. Does nothing if old and new holder are equal.
     * Removes the list change handler from the old holder and adds
     * it to the new one.
     * 
     * @param newListHolder   the list holder to be set
     * 
     * @throws NullPointerException if the new list holder is <code>null</code>
     * @throws ClassCastException if the new list holder's value is not a List
     */
    public final void setListHolder(ValueModel newListHolder) {
        if (newListHolder == null) 
            throw new NullPointerException("The new list holder must not be null.");
        
        ValueModel oldListHolder = getListHolder();
        if (equals(oldListHolder, newListHolder))
            return;
        
        List oldListModel = list;
        List newListModel = (List) newListHolder.getValue();
        
        oldListHolder.removeValueChangeListener(listChangeHandler);
        listHolder = newListHolder;
        newListHolder.addValueChangeListener(listChangeHandler);

        updateList(oldListModel, newListModel);
        firePropertyChange(PROPERTYNAME_LIST_HOLDER, 
                           oldListHolder, 
                           newListHolder);
    }
    
    
    // ListModel Convenience Code *********************************************
    
    /**
     * Checks and answers if the list is empty or <code>null</code>.
     * 
     * @return true if the list is empty or <code>null</code>, false otherwise
     */
    public final boolean isEmpty() {
        return getSize() == 0;
    }
    

    // ListModel Implementation ***********************************************

    /**
     * Returns the length of the list, <code>0</code> if the list  
     * is <code>null</code>.
     * 
     * @return the size of the list, <code>0</code> if the list is 
     *     <code>null</code>
     */
    public final int getSize() {
        return getSize(getList());
    }
    
    
    /**
     * Returns the value at the specified index.  
     * 
     * @param index  the requested index
     * @return the value at <code>index</code>.
     *      
     * @throws NullPointerException if the list holder's content is null
     */
    public final Object getElementAt(int index) {
        return getList().get(index);
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
    
    
    // Helper Code ***********************************************************
    
    /**
     * Fires a property change for <em>list</em> 
     * and a ListModel contents change event.
     * 
     * @param oldList   the old List
     * @param newList   the new List
     */
    protected void updateList(List oldList, List newList) {
        list = newList;
        firePropertyChange(PROPERTYNAME_LIST, oldList, newList);
        fireListChanged(getSize(oldList) - 1, getSize(newList) - 1);
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
    protected final int getSize(List aListOrNull) {
        return aListOrNull == null ? 0 : aListOrNull.size();
    }

    
    // Event Handlers *********************************************************
        
    /**
     * Handles changes of the list.
     */
    private final class ListChangeHandler implements PropertyChangeListener {

        /**
         * The list has been changed.
         * Notifies all registered listeners about the change.
         * 
         * @param evt   the property change event to be handled
         */
        public void propertyChange(PropertyChangeEvent evt) {
            List oldList = list;
            List newList = (List) evt.getNewValue();
            updateList(oldList, newList);
        }
    }

    
}
