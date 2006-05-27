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
 
import java.util.Collection;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
 
/**
 * Adds {@link javax.swing.ListModel} capabilities to its superclass 
 * <code>LinkedList</code>,  i. e. allows to observe changes in the content and 
 * structure. Useful for lists that are bound to list views, for example 
 * JList, JComboBox and JTable.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.3 $
 * 
 * @see ArrayListModel
 * @see java.util.ListIterator
 */
public final class LinkedListModel extends LinkedList implements ObservableList {
    
    private static final long serialVersionUID = 5753378113505707237L;
    
    
    // Instance Creation ******************************************************
    
    /**
     * Constructs an empty linked list.
     */
    public LinkedListModel() {
        // Just invoke the super constructor implicitly.
    }
    

    /**
     * Constructs a linked list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param  c the collection whose elements are to be placed into this list.
     * @throws NullPointerException if the specified collection is 
     *     <code>null</code>
     */
    public LinkedListModel(Collection c) {
        super(c);
    }

    
    // Overriding Superclass Behavior *****************************************
    
    /**
     * Inserts the specified element at the specified position in this
     * list. Shifts the element currently at that position (if any) and
     * any subsequent elements to the right (adds one to their indices).
     *
     * @param index index at which the specified element is to be inserted.
     * @param element element to be inserted.
     * @throws    IndexOutOfBoundsException if index is out of range
     *            <code>(index &lt; 0 || index &gt; size())</code>.
     */
    public void add(int index, Object element) {
        super.add(index, element);
        fireIntervalAdded(index, index);
    }


    /**
     * Appends the specified element to the end of this list.
     *
     * @param o element to be appended to this list.
     * @return <code>true</code> (as per the general contract of Collection.add).
     */
    public boolean add(Object o) {
        int newIndex = size();
        super.add(o);
        fireIntervalAdded(newIndex, newIndex);
        return true;
    }


    /**
     * Inserts all of the elements in the specified Collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified Collection's iterator.
     *
     * @param index    index at which to insert first element
     *                 from the specified collection.
     * @param c        elements to be inserted into this list.
     * @return <code>true</code> if this list changed as a result of the call.
     * 
     * @throws    IndexOutOfBoundsException if index out of range 
     *            <code>(index &lt; 0 || index &gt; size())</code>.
     * @throws    NullPointerException if the specified Collection is null.
     */
    public boolean addAll(int index, Collection c) {
        boolean changed = super.addAll(index, c);
        if (changed) {
            int lastIndex = index + c.size() - 1;
            fireIntervalAdded(index, lastIndex);
        }
        return changed;
    }

    
    /**
     * Inserts the given element at the beginning of this list.
     * 
     * @param o the element to be inserted at the beginning of this list.
     */
    public void addFirst(Object o) {
        super.addFirst(o);
        fireIntervalAdded(0, 0);
    }

    /**
     * Appends the given element to the end of this list.  (Identical in
     * function to the <code>add</code> method; included only for consistency.)
     * 
     * @param o the element to be inserted at the end of this list.
     */
    public void addLast(Object o) {
        int newIndex = size();
        super.addLast(o);
        fireIntervalAdded(newIndex, newIndex);
    }

    /**
     * Removes all of the elements from this list.  The list will
     * be empty after this call returns.
     */
    public void clear() {
        if (isEmpty())
            return;
        
        int oldLastIndex = size() - 1;
        super.clear();
        fireIntervalRemoved(0, oldLastIndex);
    }

    
    /**
     * Removes the element at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their
     * indices).
     *
     * @param index the index of the element to removed.
     * @return the element that was removed from the list.
     * @throws    IndexOutOfBoundsException if index out of range 
     *            <code>(index &lt; 0 || index &gt;= size())</code>.
     */
    public Object remove(int index) {
        Object removedElement = super.remove(index);
        fireIntervalRemoved(index, index);
        return removedElement;
    }

    
    /**
     * Removes a single instance of the specified element from this
     * collection, if it is present (optional operation).  More formally,
     * removes an element <code>e</code> such that <code>(o==null ? e==null :
     * o.equals(e))</code>, if the collection contains one or more such
     * elements.  Returns <code>true</code> if the collection contained the
     * specified element (or equivalently, if the collection changed as a
     * result of the call).<p>
     *
     * This implementation iterates over the collection looking for the
     * specified element.  If it finds the element, it removes the element
     * from the collection using the iterator's remove method.<p>
     *
     * Note that this implementation throws an
     * <code>UnsupportedOperationException</code> if the iterator returned by this
     * collection's iterator method does not implement the <code>remove</code>
     * method and this collection contains the specified object.
     *
     * @param o element to be removed from this collection, if present.
     * @return <code>true</code> if the collection contained the specified
     *         element.
     */
    public boolean remove(Object o) {
        int index = indexOf(o);
        if (index == -1) {
            return false;
        }
        remove(index);
        return true;
    }


    /**
     * Removes and returns the first element from this list.
     *
     * @return the first element from this list.
     * @throws    java.util.NoSuchElementException if this list is empty.
     */
    public Object removeFirst() {
        Object first = super.removeFirst();
        fireIntervalRemoved(0, 0);
        return first;
    }

    /**
     * Removes and returns the last element from this list.
     *
     * @return the last element from this list.
     * @throws    java.util.NoSuchElementException if this list is empty.
     */
    public Object removeLast() {
        int lastIndex = size() - 1;
        Object last = super.removeLast();
        fireIntervalRemoved(lastIndex, lastIndex);
        return last;
    }

    /**
     * Removes from this List all of the elements whose index is between
     * fromIndex, inclusive and toIndex, exclusive.  Shifts any succeeding
     * elements to the left (reduces their index).
     * This call shortens the list by <code>(toIndex - fromIndex)</code> elements.
     * (If <code>toIndex==fromIndex</code>, this operation has no effect.)
     *
     * @param fromIndex index of first element to be removed.
     * @param toIndex index after last element to be removed.
     */
    protected void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
        fireIntervalRemoved(fromIndex, toIndex - 1);
    }

    
    /**
     * Replaces the element at the specified position in this list with
     * the specified element.
     *
     * @param index index of element to replace.
     * @param element element to be stored at the specified position.
     * @return the element previously at the specified position.
     * @throws IndexOutOfBoundsException if index out of range
     *         <code>(index &lt; 0 || index &gt;= size())</code>.
     */
    public Object set(int index, Object element) {
        Object previousElement = super.set(index, element);
        fireContentsChanged(index, index);
        return previousElement;
    }


    /**
     * Returns a list-iterator of the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     * Obeys the general contract of <tt>List.listIterator(int)</tt>.<p>
     *
     * The list-iterator is <i>fail-fast</i>: if the list is structurally
     * modified at any time after the Iterator is created, in any way except
     * through the list-iterator's own <tt>remove</tt> or <tt>add</tt>
     * methods, the list-iterator will throw a
     * <tt>ConcurrentModificationException</tt>.  Thus, in the face of
     * concurrent modification, the iterator fails quickly and cleanly, rather
     * than risking arbitrary, non-deterministic behavior at an undetermined
     * time in the future.
     *
     * @param index index of first element to be returned from the
     *          list-iterator (by a call to <tt>next</tt>).
     * @return a ListIterator of the elements in this list (in proper
     *         sequence), starting at the specified position in the list.
     * @throws    IndexOutOfBoundsException if index is out of range
     *        (<tt>index &lt; 0 || index &gt; size()</tt>).
     * @see java.util.List#listIterator(int)
     */
    public ListIterator listIterator(int index) {
        return new ReportingListIterator(super.listIterator(index));
    }
    

    // ListModel Field ********************************************************
    
    /**
     * Holds the registered ListDataListeners. The list that holds these 
     * listeners is initialized lazily in <code>#getEventListenerList</code>.
     * 
     * @see #addListDataListener(ListDataListener)
     * @see #removeListDataListener(ListDataListener)
     */
    private EventListenerList listenerList;


    // ListModel Implementation ***********************************************
    
    /**
     * Adds a listener to the list that's notified each time a change
     * to the data model occurs.
     *
     * @param l the <code>ListDataListener</code> to be added
     */  
    public void addListDataListener(ListDataListener l) {
        getEventListenerList().add(ListDataListener.class, l);
    }


    /**
     * Removes a listener from the list that's notified each time a 
     * change to the data model occurs.
     *
     * @param l the <code>ListDataListener</code> to be removed
     */  
    public void removeListDataListener(ListDataListener l) {
        getEventListenerList().remove(ListDataListener.class, l);
    }
    

    /**
     * Returns the value at the specified index.  
     * 
     * @param index the requested index
     * @return the value at <code>index</code>
     */
    public Object getElementAt(int index) {
        return get(index);
    }
    
    
    /** 
     * Returns the length of the list or 0 if there's no list.
     * 
     * @return the length of the list or 0 if there's no list
     */
    public int getSize() {
        return size();
    }

    
    // Explicit Change Notification *******************************************
    
    /**
     * Notifies all registered <code>ListDataListeners</code> that the element
     * at the specified index has changed. Useful if there's a content change
     * without any structural change.<p>
     * 
     * This method must be called <em>after</em> the element of the list changes. 
     * 
     * @param index    the index of the element that has changed
     * 
     * @see EventListenerList
     */
    public void fireContentsChanged(int index) {
        fireContentsChanged(index, index);
    }
    
    
    // ListModel Helper Code **************************************************
    
    /**
     * Returns an array of all the list data listeners
     * registered on this <code>LinkedListModel</code>.
     *
     * @return all of this model's <code>ListDataListener</code>s,
     *         or an empty array if no list data listeners
     *         are currently registered
     * 
     * @see #addListDataListener(ListDataListener)
     * @see #removeListDataListener(ListDataListener)
     */
    public ListDataListener[] getListDataListeners() {
        return (ListDataListener[]) getEventListenerList().getListeners(
                ListDataListener.class);
    }


    /**
     * This method must be called <em>after</em> one or more elements 
     * of the list change.  The changed elements
     * are specified by the closed interval index0, index1 -- the end points
     * are included.  Note that index0 need not be less than or equal to index1.
     * 
     * @param index0 one end of the new interval
     * @param index1 the other end of the new interval
     * @see EventListenerList
     */
    private void fireContentsChanged(int index0, int index1) {
        Object[] listeners = getEventListenerList().getListenerList();
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
     * This method must be called <em>after</em> one or more elements 
     * are added to the model.  The new elements
     * are specified by a closed interval index0, index1 -- the end points
     * are included.  Note that index0 need not be less than or equal to index1.
     * 
     * @param index0 one end of the new interval
     * @param index1 the other end of the new interval
     * @see EventListenerList
     */
    private void fireIntervalAdded(int index0, int index1) {
        Object[] listeners = getEventListenerList().getListenerList();
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
     * This method must be called <em>after</em>  one or more elements 
     * are removed from the model. 
     * <code>index0</code> and <code>index1</code> are the end points
     * of the interval that's been removed.  Note that <code>index0</code>
     * need not be less than or equal to <code>index1</code>.
     * 
     * @param index0 one end of the removed interval,
     *               including <code>index0</code>
     * @param index1 the other end of the removed interval,
     *               including <code>index1</code>
     * @see EventListenerList
     */
    private void fireIntervalRemoved(int index0, int index1) {
        Object[] listeners = getEventListenerList().getListenerList();
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
    
    
    /**
     * Lazily initializes and returns the event listener list used
     * to notify registered listeners.
     * 
     * @return the event listener list used to notify listeners
     */
    private EventListenerList getEventListenerList() {
        if (listenerList == null) {
            listenerList = new EventListenerList();
        }
        return listenerList;
    }
    
    
    // Helper Class ***********************************************************
    
    /**
     * A ListIterator that fires ListDataEvents if elements are added or removed.
     */
    private class ReportingListIterator implements ListIterator {
        
        /**
         * Refers to the wrapped ListIterator that is used 
         * to forward all ListIterator methods to.
         */
        private final ListIterator delegate;
        
        /**
         * Holds the object that was returned last by the underlying 
         * ListIteratur. Used to determine the index of the element
         * removed.
         */
        private int lastReturnedIndex;
        
        ReportingListIterator(ListIterator delegate) {
            this.delegate = delegate; 
            lastReturnedIndex = -1;
        }
    
        /**
         * Returns <tt>true</tt> if this list iterator has more elements when
         * traversing the list in the forward direction. (In other words, returns
         * <tt>true</tt> if <tt>next</tt> would return an element rather than
         * throwing an exception.)
         *
         * @return <tt>true</tt> if the list iterator has more elements when
         *      traversing the list in the forward direction.
         */
        public boolean hasNext() {
            return delegate.hasNext();
        }
    
        /**
         * Returns the next element in the list.  This method may be called
         * repeatedly to iterate through the list, or intermixed with calls to
         * <tt>previous</tt> to go back and forth.  (Note that alternating calls
         * to <tt>next</tt> and <tt>previous</tt> will return the same element
         * repeatedly.)
         *
         * @return the next element in the list.
         * 
         * @throws NoSuchElementException if the iteration has no next element.
         */
        public Object next() {
            lastReturnedIndex = nextIndex();
            return delegate.next();
        }
    
        /**
         * Returns <tt>true</tt> if this list iterator has more elements when
         * traversing the list in the reverse direction.  (In other words, returns
         * <tt>true</tt> if <tt>previous</tt> would return an element rather than
         * throwing an exception.)
         *
         * @return <tt>true</tt> if the list iterator has more elements when
         *         traversing the list in the reverse direction.
         */
        public boolean hasPrevious() {
            return delegate.hasPrevious();
        }
    
        /**
         * Returns the previous element in the list.  This method may be called
         * repeatedly to iterate through the list backwards, or intermixed with
         * calls to <tt>next</tt> to go back and forth.  (Note that alternating
         * calls to <tt>next</tt> and <tt>previous</tt> will return the same
         * element repeatedly.)
         *
         * @return the previous element in the list.
         * 
         * @throws NoSuchElementException if the iteration has no previous
         *            element.
         */
        public Object previous() {
            lastReturnedIndex = previousIndex();
            return delegate.previous();
        }
    
        /**
         * Returns the index of the element that would be returned by a subsequent
         * call to <tt>next</tt>. (Returns list size if the list iterator is at the
         * end of the list.)
         *
         * @return the index of the element that would be returned by a subsequent
         *         call to <tt>next</tt>, or list size if list iterator is at end
         *         of list. 
         */
        public int nextIndex() {
            return delegate.nextIndex();
        }
    
        /**
         * Returns the index of the element that would be returned by a subsequent
         * call to <tt>previous</tt>. (Returns -1 if the list iterator is at the
         * beginning of the list.)
         *
         * @return the index of the element that would be returned by a subsequent
         *         call to <tt>previous</tt>, or -1 if list iterator is at
         *         beginning of list.
         */ 
        public int previousIndex() {
            return delegate.previousIndex();
        }
    
        /**
         * Removes from the list the last element that was returned by
         * <tt>next</tt> or <tt>previous</tt> (optional operation).  This call can
         * only be made once per call to <tt>next</tt> or <tt>previous</tt>.  It
         * can be made only if <tt>ListIterator.add</tt> has not been called after
         * the last call to <tt>next</tt> or <tt>previous</tt>.
         *
         * @throws UnsupportedOperationException if the <tt>remove</tt>
         *        operation is not supported by this list iterator.
         * @throws IllegalStateException neither <tt>next</tt> nor
         *        <tt>previous</tt> have been called, or <tt>remove</tt> or
         *        <tt>add</tt> have been called after the last call to *
         *        <tt>next</tt> or <tt>previous</tt>.
         */
        public void remove() {
            int oldSize = size();
            delegate.remove();
            int newSize = size();
            if (newSize < oldSize)
                LinkedListModel.this.fireIntervalRemoved(lastReturnedIndex, lastReturnedIndex);
        }
    
        /**
         * Replaces the last element returned by <tt>next</tt> or
         * <tt>previous</tt> with the specified element (optional operation).
         * This call can be made only if neither <tt>ListIterator.remove</tt> nor
         * <tt>ListIterator.add</tt> have been called after the last call to
         * <tt>next</tt> or <tt>previous</tt>.
         *
         * @param o the element with which to replace the last element returned by
         *          <tt>next</tt> or <tt>previous</tt>.
         * @throws UnsupportedOperationException if the <tt>set</tt> operation
         *        is not supported by this list iterator.
         * @throws ClassCastException if the class of the specified element
         *        prevents it from being added to this list.
         * @throws IllegalArgumentException if some aspect of the specified
         *        element prevents it from being added to this list.
         * @throws IllegalStateException if neither <tt>next</tt> nor
         *            <tt>previous</tt> have been called, or <tt>remove</tt> or
         *        <tt>add</tt> have been called after the last call to
         *        <tt>next</tt> or <tt>previous</tt>.
         */
        public void set(Object o) {
            delegate.set(o);
        }
    
        /**
         * Inserts the specified element into the list (optional operation).  The
         * element is inserted immediately before the next element that would be
         * returned by <tt>next</tt>, if any, and after the next element that
         * would be returned by <tt>previous</tt>, if any.  (If the list contains
         * no elements, the new element becomes the sole element on the list.)
         * The new element is inserted before the implicit cursor: a subsequent
         * call to <tt>next</tt> would be unaffected, and a subsequent call to
         * <tt>previous</tt> would return the new element.  (This call increases
         * by one the value that would be returned by a call to <tt>nextIndex</tt>
         * or <tt>previousIndex</tt>.)
         *
         * @param o the element to insert.
         * @throws UnsupportedOperationException if the <tt>add</tt> method is
         *        not supported by this list iterator.
         * 
         * @throws ClassCastException if the class of the specified element
         *        prevents it from being added to this list.
         * 
         * @throws IllegalArgumentException if some aspect of this element
         *            prevents it from being added to this list.
         */
        public void add(Object o) {
            delegate.add(o);
            int newIndex = previousIndex();
            LinkedListModel.this.fireIntervalAdded(newIndex, newIndex);
            lastReturnedIndex = -1;
        }
    
    }
    

}
