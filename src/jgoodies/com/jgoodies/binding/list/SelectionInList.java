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
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.jgoodies.binding.value.AbstractValueModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

 
/**
 * Represents a selection in a list of objects. Provides bound bean properties
 * for the list, the selection, the selection index, and the selection empty 
 * state. The SelectionInList implements ValueModel with the selection as value.
 * Selection changes fire an event only if the old and new value are not equal.
 * If you need to compare the identity you can use and observe the selection 
 * index instead of the selection or value.<p>
 * 
 * The SelectionInList uses three ValueModels to hold the list, the selection
 * and selection index and provides bound bean properties for these models. 
 * You can access, observe and replace these ValueModels. This is useful 
 * to connect a SelectionInList with other ValueModels; for example you can
 * use the SelectionInList's selection holder as bean channel for a 
 * PresentationModel. Since the SelectionInList is a ValueModel, it is often
 * used as bean channel. See the Binding tutorial classes for examples on how
 * to connect a SelectionInList with a PresentationModel.<p>
 * 
 * This class also implements the {@link ListModel} interface that allows
 * API users to observe fine grained changes in the structure and contents
 * of the list. Hence instances of this class can be used directly as model of 
 * a JList. If you want to use a SelectionInList with a JComboBox or JTable, 
 * you can convert the SelectionInList to the associated component model 
 * interfaces using the adapter classes 
 * {@link com.jgoodies.binding.adapter.ComboBoxAdapter}
 * and {@link com.jgoodies.binding.adapter.AbstractTableAdapter} respectively.
 * These classes are part of the Binding library too.<p>
 * 
 * The SelectionInList supports two list types as content of its list holder: 
 * <code>List</code> and <code>ListModel</code>. The two modes differ in how 
 * precise this class can fire events about changes to the content and structure 
 * of the list. If you use a List, this class can only report 
 * that the list changes completely; this is done by emitting 
 * a PropertyChangeEvent for the <em>list</em> property.
 * Also, a <code>ListDataEvent</code> is fired that reports a complete change.
 * In contrast, if you use a ListModel it will report the same
 * PropertyChangeEvent. But fine grained changes in the list 
 * model will be fired by this class to notify observes about changes in 
 * the content, added and removed elements.<p>
 * 
 * If the list content doesn't change at all, or if it always changes 
 * completely, you can work well with both List content and ListModel content. 
 * But if the list structure or content changes, the ListModel reports more 
 * fine grained events to registered ListDataListeners, which in turn allows
 * list views to chooser better user interface gestures: for example, a table 
 * with scroll pane may retain the current selection and scroll offset.<p>
 * 
 * An example for using a ListModel in a SelectionInList is the asynchronous 
 * transport of list elements from a server to a client. Let's say you transport 
 * the list elements in portions of 10 elements to improve the application's 
 * responsiveness. The user can then select and work with the SelectionInList 
 * as soon as the ListModel gets populated. If at a later time more elements 
 * are added to the list model, the SelectionInList can retain the selection 
 * index (and selection) and will just report a ListDataEvent about
 * the interval added. JList, JTable and JComboBox will then just add 
 * the new elements at the end of the list presentation.<p>
 * 
 * If you want to combine List operations and the ListModel change reports, 
 * you may consider using an implementation that combines these two interfaces, 
 * for example {@link ArrayListModel} or {@link LinkedListModel}.<p>
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
 * This binding library provides some help for firing PropertyChangeEvents 
 * if the old ListModel and new ListModel are equal but not the same.
 * Class {@link com.jgoodies.binding.beans.ExtendedPropertyChangeSupport} 
 * allows to permanently or individually check the identity (using 
 * <code>==</code>) instead of checking the equity (using <code>#equals</code>).
 * Class {@link com.jgoodies.binding.beans.Model} uses this extended 
 * property change support. And class {@link ValueHolder} uses it too 
 * and can be configured to always test the identity.<p> 
 * 
 * Since version 1.0.2 this class provides public convenience methods
 * for firing ListDataEvents, see the methods <code>#fireContentsChanged</code>,
 * <code>#fireIntervalAdded</code>, and <code>#fireIntervalRemoved</code>.
 * These are automatically invoked if the list holder holds a ListModel
 * that fires these events. If on the other hand the underlying List or
 * ListModel does not fire a required ListDataEvent, you can use these
 * methods to notify presentations about a change. It is recommended
 * to avoid sending duplicate ListDataEvents; hence check if the underlying
 * ListModel fires the necessary events or not. Typically an underlying
 * ListModel will fire the add and remove events; but often it'll lack 
 * an event if the (seletcted) contents has changed. A convenient way to
 * indicate that change is <code>#fireSelectedContentsChanged</code>. See 
 * the tutorial's AlbumManagerModel for an example how to use this feature.<p> 
 * 
 * <strong>Constraints:</strong> The list holder holds instances of {@link List}
 * or {@link ListModel}, the selection holder values of type <code>Object</code> 
 * and the selection index holder of type <code>Integer</code>. The selection
 * index holder must hold non-null index values; however, when firing
 * an index value change event, both the old and new value may be null.
 * If the ListModel changes, the underyling ValueModel must fire
 * a PropertyChangeEvent.<p>
 * 
 * TODO: Consider renaming members names <code>*ListHolder</code> to
 * <code>*ListModelHolder</code>. If so, mark the old methods as deprecated.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.12 $
 * 
 * @see     ValueModel
 * @see     List
 * @see     ListModel
 * @see     com.jgoodies.binding.adapter.ComboBoxAdapter
 * @see     com.jgoodies.binding.adapter.AbstractTableAdapter
 * @see     com.jgoodies.binding.beans.ExtendedPropertyChangeSupport
 * @see     com.jgoodies.binding.beans.Model
 * @see     com.jgoodies.binding.value.ValueHolder
 */

public final class SelectionInList extends AbstractValueModel 
    implements ListModel {
    
    
    // Constant Names for Bound Properties ************************************
   
    /**
     * The name of the bound write-only <em>list</em> property.
     */
    public static final String PROPERTYNAME_LIST = "list";

    /**
     * The name of the bound read-write <em>listHolder</em> property.
     */
    public static final String PROPERTYNAME_LIST_HOLDER = "listHolder";

    /**
     * The name of the bound read-write <em>selection</em> property.
     */
    public static final String PROPERTYNAME_SELECTION = "selection";

    /**
     * The name of the bound read-only <em>selectionEmpty</em> property.
     */
    public static final String PROPERTYNAME_SELECTION_EMPTY = "selectionEmpty";

    /**
     * The name of the bound read-write <em>selection holder</em> property.
     */
    public static final String PROPERTYNAME_SELECTION_HOLDER = "selectionHolder";

    /**
     * The name of the bound read-write <em>selectionIndex</em> property.
     */
    public static final String PROPERTYNAME_SELECTION_INDEX = "selectionIndex";

    /**
     * The name of the bound read-write <em>selection index holder</em> property.
     */
    public static final String PROPERTYNAME_SELECTION_INDEX_HOLDER = "selectionIndexHolder";

    
    // ************************************************************************
    
    /**
     * A special index that indicates that we have no selection. 
     */
    private static final int NO_SELECTION_INDEX = -1;
    
    /**
     * An empty <code>ListModel</code> that is used if the list holder's
     * content is null.
     * 
     * @see #getListModel()
     */
    private static final ListModel EMPTY_LIST_MODEL =
        new EmptyListModel();
    
    
    // Instance Fields ********************************************************
    
    /**
     * Holds a <code>List</code> or <code>ListModel</code> that in turn 
     * holds the elements.
     */
    private ValueModel listHolder;
    
    /**
     * Holds a copy of the listHolder's value. Used as the old list
     * when the listHolder's value changes. Required because a ValueModel
     * may use <code>null</code> as old value, but the SelectionInList
     * must know about the old and the new list.
     */
    private Object list;
    
    /** 
     * Holds the selection, an instance of <code>Object</code>.          
     */
    private ValueModel selectionHolder;
    
    /** 
     * Holds the selection index, an <code>Integer</code>.           
     */
    private ValueModel selectionIndexHolder;

    /**
     * Handles changes of the list.
     */
    private final PropertyChangeListener listChangeHandler;

    /**
     * Handles structural and content changes of the list model.
     */
    private final ListDataListener listDataChangeHandler;

    /**
     * The <code>PropertyChangeListener</code> used to handle
     * changes of the selection.
     */
    private final PropertyChangeListener selectionChangeHandler;

    /**
     * The <code>PropertyChangeListener</code> used to handle
     * changes of the selection index.
     */
    private final PropertyChangeListener selectionIndexChangeHandler;
    
    /**
     * Refers to the list of list data listeners that is used 
     * to notify registered listeners if the ListModel changes.
     */
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * Duplicates the value of the selectionHolder.
     * Used to provide better old values in PropertyChangeEvents
     * fired after selectionIndex changes.
     */
    private Object oldSelection;
    
    /**
     * Duplicates the value of the selectionIndexHolder.
     * Used to provide better old values in PropertyChangeEvents
     * fired after selectionIndex changes and selection changes.
     */
    private int oldSelectionIndex;
    
    
    // Instance creation ****************************************************

    /**
     * Constructs a SelectionInList with an empty initial 
     * <code>ArrayListModel</code> using defaults for the selection holder 
     * and selection index holder.
     */
    public SelectionInList() {     
        this((ListModel) new ArrayListModel());
    }

    
    /**
     * Constructs a SelectionInList on the given item array 
     * using defaults for the selection holder and selection index holder.
     * The specified array will be converted to a List.<p>
     * 
     * Changes to the list "write through" to the array, and changes 
     * to the array contents will be reflected in the list.
     *  
     * @param listItems        the array of initial items
     * 
     * @throws NullPointerException if <code>listItems</code> is <code>null</code>
     */
    public SelectionInList(Object[] listItems) {     
        this(Arrays.asList(listItems));
    }


    /**
     * Constructs a SelectionInList on the given item array and
     * selection holder using a default selection index holder.
     * The specified array will be converted to a List.<p>
     * 
     * Changes to the list "write through" to the array, and changes 
     * to the array contents will be reflected in the list.
     *  
     * @param listItems        the array of initial items
     * @param selectionHolder  holds the selection
     * 
     * @throws NullPointerException if <code>listItems</code> or
     *     <code>selectionHolder</code> is <code>null</code>
     */
    public SelectionInList(Object[] listItems, ValueModel selectionHolder) {     
        this(Arrays.asList(listItems), selectionHolder);
    }

    
    /**
     * Constructs a SelectionInList on the given item array and
     * selection holder using a default selection index holder.
     * The specified array will be converted to a List.<p>
     * 
     * Changes to the list "write through" to the array, and changes 
     * to the array contents will be reflected in the list.
     *  
     * @param listItems        the array of initial items
     * @param selectionHolder  holds the selection
     * @param selectionIndexHolder  holds the selection index
     * 
     * @throws NullPointerException if <code>listItems</code>, 
     *     <code>selectionHolder</code>, or <code>selectionIndexHolder</code> 
     *     is <code>null</code>
     */
    public SelectionInList(
            Object[] listItems, 
            ValueModel selectionHolder,
            ValueModel selectionIndexHolder) {     
        this(Arrays.asList(listItems), selectionHolder, selectionIndexHolder);
    }

    
    /**
     * Constructs a SelectionInList on the given list 
     * using defaults for the selection holder and selection index holder.<p>
     *
     * <strong>Note:</strong> Favor <code>ListModel</code> over 
     * <code>List</code> when working with the SelectionInList.
     * Why? The SelectionInList can work with both types. What's the
     * difference? ListModel provides all list access features
     * required by the SelectionInList's. In addition it reports more 
     * fine grained change events, instances of <code>ListDataEvents</code>. 
     * In contrast developer often create Lists and operate on them
     * and the ListModel may be inconvenient for these operations.<p>
     * 
     * A convenient solution for this situation is to use the
     * <code>ArrayListModel</code> and <code>LinkedListModel</code> classes.
     * These implement both List and ListModel, offer the standard List
     * operations and report the fine grained ListDataEvents.
     *  
     * @param list        the initial list
     */
    public SelectionInList(List list) {     
        this(new ValueHolder(list, true));
    }


    /**
     * Constructs a SelectionInList on the given list and 
     * selection holder using a default selection index holder.<p>
     *
     * <strong>Note:</strong> Favor <code>ListModel</code> over 
     * <code>List</code> when working with the SelectionInList.
     * Why? The SelectionInList can work with both types. What's the
     * difference? ListModel provides all list access features
     * required by the SelectionInList's. In addition it reports more 
     * fine grained change events, instances of <code>ListDataEvents</code>. 
     * In contrast developer often create Lists and operate on them
     * and the ListModel may be inconvenient for these operations.<p>
     * 
     * A convenient solution for this situation is to use the
     * <code>ArrayListModel</code> and <code>LinkedListModel</code> classes.
     * These implement both List and ListModel, offer the standard List
     * operations and report the fine grained ListDataEvents.
     *  
     * @param list             the initial list
     * @param selectionHolder  holds the selection
     * 
     * @throws NullPointerException 
     *     if <code>selectionHolder</code> is <code>null</code>
     */
    public SelectionInList(List list, ValueModel selectionHolder) {     
        this(new ValueHolder(list, true), selectionHolder);
    }


    /**
     * Constructs a SelectionInList on the given list, 
     * selection holder, and selection index holder.<p>
     *
     * <strong>Note:</strong> Favor <code>ListModel</code> over 
     * <code>List</code> when working with the SelectionInList.
     * Why? The SelectionInList can work with both types. What's the
     * difference? ListModel provides all list access features
     * required by the SelectionInList's. In addition it reports more 
     * fine grained change events, instances of <code>ListDataEvents</code>. 
     * In contrast developer often create Lists and operate on them
     * and the ListModel may be inconvenient for these operations.<p>
     * 
     * A convenient solution for this situation is to use the
     * <code>ArrayListModel</code> and <code>LinkedListModel</code> classes.
     * These implement both List and ListModel, offer the standard List
     * operations and report the fine grained ListDataEvents.
     *  
     * @param list                  the initial list
     * @param selectionHolder       holds the selection
     * @param selectionIndexHolder  holds the selection index
     * 
     * @throws NullPointerException if <code>selectionHolder</code>, 
     *     or <code>selectionIndexHolder</code> is <code>null</code>
     */
    public SelectionInList(
            List list, 
            ValueModel selectionHolder,
            ValueModel selectionIndexHolder) {     
        this(new ValueHolder(list, true), 
             selectionHolder,
             selectionIndexHolder);
    }


    /**
     * Constructs a SelectionInList on the given list model
     * using defaults for the selection holder and selection index holder.
     * 
     * @param listModel        the initial list model
     */
    public SelectionInList(ListModel listModel) {     
        this(new ValueHolder(listModel, true));
    }

    
    /**
     * Constructs a SelectionInList on the given list model 
     * and selection holder using a default selection index holder.
     * 
     * @param listModel        the initial list model
     * @param selectionHolder  holds the selection
     * 
     * @throws NullPointerException 
     *     if <code>selectionHolder</code> is <code>null</code>
     */
    public SelectionInList(ListModel listModel, ValueModel selectionHolder) {     
        this(new ValueHolder(listModel, true), selectionHolder);
    }

    
    /**
     * Constructs a SelectionInList on the given list model, 
     * selection holder, and selection index holder.
     * 
     * @param listModel             the initial list model
     * @param selectionHolder       holds the selection
     * @param selectionIndexHolder  holds the selection index
     * 
     * @throws NullPointerException if <code>selectionHolder</code>, 
     *     or <code>selectionIndexHolder</code> is <code>null</code>
     */
    public SelectionInList(
            ListModel listModel, 
            ValueModel selectionHolder,
            ValueModel selectionIndexHolder) {     
        this(new ValueHolder(listModel, true), 
             selectionHolder, 
             selectionIndexHolder);
    }

    
    /**
     * Constructs a SelectionInList on the given list holder
     * using defaults for the selection holder and selection index holder.<p>
     * 
     * <strong>Constraints:</strong> 
     * 1) The listHolder must hold instances of List or ListModel and 
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
     * @param listHolder          holds the list or list model
     * 
     * @throws NullPointerException 
     *     if <code>listHolder</code> is <code>null</code>
     */
    public SelectionInList(ValueModel listHolder) {     
        this(listHolder, new ValueHolder(null, true));
    }


    /**
     * Constructs a SelectionInList on the given list holder,
     * selection holder and selection index holder.<p>
     * 
     * <strong>Constraints:</strong> 
     * 1) The listHolder must hold instances of List or ListModel and 
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
     * @param listHolder             holds the list or list model
     * @param selectionHolder        holds the selection
     * @throws NullPointerException  if <code>listHolder</code>
     *     or <code>selectionHolder</code> is <code>null</code>
     */
    public SelectionInList(ValueModel listHolder, ValueModel selectionHolder) {
        this(
            listHolder,
            selectionHolder,
            new ValueHolder(new Integer(NO_SELECTION_INDEX)));
    }
    

    /**
     * Constructs a SelectionInList on the given list holder,
     * selection holder and selection index holder.<p>
     * 
     * <strong>Constraints:</strong> 
     * 1) The listHolder must hold instances of List or ListModel and 
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
     * @param listHolder               holds the list or list model
     * @param selectionHolder          holds the selection
     * @param selectionIndexHolder     holds the selection index
     * 
     * @throws NullPointerException    if the <code>listModelHolder</code>,
     *     <code>selectionHolder</code>, or <code>selectionIndexHolder</code> 
     *     is <code>null</code>
     * @throws IllegalArgumentException if the listHolder is a ValueHolder
     *     that doesn't check the identity when changing its value
     * @throws ClassCastException if the listModelHolder contents 
     *     is neither a List nor a ListModel
     */
    public SelectionInList(
        ValueModel listHolder, 
        ValueModel selectionHolder,
        ValueModel selectionIndexHolder) {
        if (listHolder == null)
            throw new NullPointerException("The list holder must not be null.");
        if (selectionHolder == null)
            throw new NullPointerException("The selection holder must not be null.");
        if (selectionIndexHolder == null)
            throw new NullPointerException("The selection index holder must not be null.");
        checkListHolderIdentityCheck(listHolder);
        
        listChangeHandler           = new ListChangeHandler();
        listDataChangeHandler       = new ListDataChangeHandler();
        selectionChangeHandler      = new SelectionChangeHandler();
        selectionIndexChangeHandler = new SelectionIndexChangeHandler();

        this.listHolder = listHolder;
        this.selectionHolder = selectionHolder;
        this.selectionIndexHolder = selectionIndexHolder;
        initializeSelectionIndex();

        this.listHolder.addValueChangeListener(listChangeHandler);
        this.selectionHolder.addValueChangeListener(selectionChangeHandler);
        this.selectionIndexHolder.addValueChangeListener(selectionIndexChangeHandler);
        
        // If the ValueModel holds a ListModel observe list data changes too.
        list = listHolder.getValue();
        if (list != null) {
            if (list instanceof ListModel) {
                ((ListModel) list).addListDataListener(listDataChangeHandler);
            } else if (!(list instanceof List)) {
                throw new ClassCastException("The listHolder's value must be a List or ListModel.");
            }
        }
    }

    
    // ListModel Implementation ***********************************************

    /**
     * Returns the length of the list, <code>0</code> if the list model 
     * is <code>null</code>.
     * 
     * @return the size of the list, <code>0</code> if the list model is 
     *     <code>null</code>
     */
    public int getSize() {
        return getSize(getListHolder().getValue());
    }
    
    /**
     * Returns the length of the given list, <code>0</code> if the list model 
     * is <code>null</code>.
     * 
     * @return the size of the given list, <code>0</code> if the list model is 
     *     <code>null</code>
     */
    private int getSize(Object aList) {
        if (aList == null)
            return 0;
        else if (aList instanceof ListModel)
            return ((ListModel) aList).getSize();
        else 
            return ((List) aList).size(); 
    }
    
    /**
     * Returns the value at the specified index, <code>null</code>
     * if the list model is <code>null</code>.  
     * 
     * @param index  the requested index
     * @return the value at <code>index</code>, <code>null</code>
     *      if the list model is <code>null</code>
     *      
     * @throws NullPointerException if the list holder's content is null
     */
    public Object getElementAt(int index) {
        return getElementAt(getListHolder().getValue(), index);
    }
    
    private Object getElementAt(Object aList, int index) {
        if (aList == null)
            throw new NullPointerException("The list contents is null.");
        else if (aList instanceof ListModel)
            return ((ListModel) aList).getElementAt(index);
        else 
            return ((List) aList).get(index); 
    }
    
    private Object getSafeElementAt(int index) {
        return (index < 0 || index >= getSize())
            ? null
            : getElementAt(index);
    }

    
    /**
     * Adds a listener to the list that's notified each time a change
     * to the data model occurs.
     *
     * @param l the <code>ListDataListener</code> to be added
     */  
    public void addListDataListener(ListDataListener l) {
        listenerList.add(ListDataListener.class, l);
    }


    /**
     * Removes a listener from the list that's notified each time a 
     * change to the data model occurs.
     *
     * @param l the <code>ListDataListener</code> to be removed
     */  
    public void removeListDataListener(ListDataListener l) {
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
    public ListDataListener[] getListDataListeners() {
        return (ListDataListener[]) listenerList.getListeners(
                ListDataListener.class);
    }


    // ListModel Helper Code **************************************************
    
    /**
     * Notifies all registered ListDataListeners that the contents 
     * of the selected list item - if any - has changed.
     * Useful to update a presentation after editing the selection.
     * See the tutorial's AlbumManagerModel for an example how to use
     * this feature.<p>
     * 
     * If the list holder holds a ListModel, this SelectionInList listens
     * to ListDataEvents fired by that ListModel, and forwards these events
     * by invoking the associated <code>#fireXXX</code> method, which in turn
     * notifies all registered ListDataListeners. Therefore if you fire 
     * ListDataEvents in an underlying ListModel, you don't need this method 
     * and should not use it to avoid sending duplicate ListDataEvents.
     *
     * @see ListModel
     * @see ListDataListener
     * @see ListDataEvent
     * 
     * @since 1.0.2
     */
    public void fireSelectedContentsChanged() {
        if (hasSelection()) {
            int selectionIndex = getSelectionIndex();
            fireContentsChanged(selectionIndex, selectionIndex);
        }
    }
    
    
    /**
     * Notifies all registered ListDataListeners that the contents
     * of one or more list elements has changed.  
     * The changed elements are specified by the closed interval index0, index1 
     * -- the end points are included. Note that index0 need not be less than 
     * or equal to index1.<p>
     * 
     * If the list holder holds a ListModel, this SelectionInList listens
     * to ListDataEvents fired by that ListModel, and forwards these events
     * by invoking the associated <code>#fireXXX</code> method, which in turn
     * notifies all registered ListDataListeners. Therefore if you fire 
     * ListDataEvents in an underlying ListModel, you don't need this method 
     * and should not use it to avoid sending duplicate ListDataEvents.
     * 
     * @param index0 one end of the new interval
     * @param index1 the other end of the new interval
     * 
     * @see ListModel
     * @see ListDataListener
     * @see ListDataEvent
     * 
     * @since 1.0.2
     */
    public void fireContentsChanged(int index0, int index1) {
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
     * or equal to index1.<p>
     * 
     * If the list holder holds a ListModel, this SelectionInList listens
     * to ListDataEvents fired by that ListModel, and forwards these events
     * by invoking the associated <code>#fireXXX</code> method, which in turn
     * notifies all registered ListDataListeners. Therefore if you fire 
     * ListDataEvents in an underlying ListModel, you don't need this method 
     * and should not use it to avoid sending duplicate ListDataEvents.
     * 
     * @param index0 one end of the new interval
     * @param index1 the other end of the new interval
     * 
     * @see ListModel
     * @see ListDataListener
     * @see ListDataEvent
     * 
     * @since 1.0.2
     */
    public void fireIntervalAdded(int index0, int index1) {
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
     * need not be less than or equal to <code>index1</code>.<p>
     * 
     * If the list holder holds a ListModel, this SelectionInList listens
     * to ListDataEvents fired by that ListModel, and forwards these events
     * by invoking the associated <code>#fireXXX</code> method, which in turn
     * notifies all registered ListDataListeners. Therefore if you fire 
     * ListDataEvents in an underlying ListModel, you don't need this method 
     * and should not use it to avoid sending duplicate ListDataEvents.
     * 
     * @param index0 one end of the removed interval,
     *               including <code>index0</code>
     * @param index1 the other end of the removed interval,
     *               including <code>index1</code>
     *               
     * @see ListModel
     * @see ListDataListener
     * @see ListDataEvent
     * 
     * @since 1.0.2
     */
    public void fireIntervalRemoved(int index0, int index1) {
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
    
    
    // Accessing the List, Selection and Index ********************************

    /**
     * Checks and answers if the list is empty or <code>null</code>.
     * 
     * @return true if the list is empty or <code>null</code>, false otherwise
     */
    public boolean isEmpty() {
        return getSize() == 0;
    }
    

    /**
     * Returns the contents of the list holder as <code>ListModel</code>.
     * If the list content is <code>null</code> an empty <code>ListModel</code>
     * is returned; if it is a <code>ListModel</code> the content is returned;
     * otherwise it is a <code>List</code> that will be converted to a 
     * <code>ListModel</code> using a wrapper.<p>
     * 
     * <strong>Note:</strong> This method will be removed from the
     * SelectionInList in version 1.2. It'll be replaced by <code>#getList</code>.
     * And the SelectionInListModel provides a <code>#getListModel</code>
     * that just returns the ListModel holder's value.
     * 
     * @return the list content as <code>ListModel</code>
     * 
     * @see #setList(List)
     * @see #setListModel(ListModel)
     * @see ListHolder
     * @see ListModelHolder
     * @see com.jgoodies.binding.extras.SelectionInList2
     * @see com.jgoodies.binding.extras.SelectionInListModel
     */
    public ListModel getListModel() {
        Object aList = getListHolder().getValue();
        if (aList == null)
            return EMPTY_LIST_MODEL;
        else if (aList instanceof ListModel)
            return (ListModel) aList;
        else 
            return new ListModelAdapter((List) aList); 
    }
    
    
    /**
     * Sets the given list model as value of the list holder.
     * 
     * @param newListModel   the list model to be set as new list content
     * 
     * @see #getListModel()
     * @see #setList(List)
     */
    public void setListModel(ListModel newListModel) {
        getListHolder().setValue(newListModel);
    }
    
    
    /**
     * Sets the given list as value of the list holder.<p>
     * 
     * <strong>Note:</strong> Favor <code>ListModel</code> over 
     * <code>List</code> when working with the SelectionInList.
     * Why? The SelectionInList can work with both types. What's the
     * difference? ListModel provides all list access features
     * required by the SelectionInList's. In addition it reports more 
     * fine grained change events, instances of <code>ListDataEvents</code>. 
     * In contrast developer often create Lists and operate on them
     * and the ListModel may be inconvenient for these operations.<p>
     * 
     * A convenient solution for this situation is to use the
     * <code>ArrayListModel</code> and <code>LinkedListModel</code> classes.
     * These implement both List and ListModel, offer the standard List
     * operations and report the fine grained ListDataEvents.
     * 
     * @param newList   the list to be set as new list content
     * 
     * @see #setListModel(ListModel)
     */
    public void setList(List newList) {
        getListHolder().setValue(newList);
    }
    
    
    /**
     * Looks up and returns the current selection using 
     * the current selection index. Returns <code>null</code> if 
     * no object is selected or if the list has no elements.
     * 
     * @return the current selection, <code>null</code> if none is selected
     */
    public Object getSelection() {
        return getSafeElementAt(getSelectionIndex());
    }
    
    /**
     * Sets the first list element that equals the given new selection
     * as new selection. Does nothing if the list is empty.
     * 
     * @param newSelection   the object to be set as new selection
     */
    public void setSelection(Object newSelection) {
        if (!isEmpty())
            setSelectionIndex(indexOf(newSelection));
    }
    
    /**
     * Checks and answers if an element is selected.
     * 
     * @return true if an element is selected, false otherwise
     */
    public boolean hasSelection() {
        return getSelectionIndex() != NO_SELECTION_INDEX;
    }
    
    
    /**
     * Checks and answers whether the selection is empty or not.
     * Unlike #hasSelection, the underlying property #selectionEmpty
     * for this method is bound. I.e. you can observe this property
     * using a PropertyChangeListener to update UI state.
     *
     * @return true if nothing is selected, false if there's a selection
     * @see #clearSelection
     * @see #hasSelection
     */
    public boolean isSelectionEmpty() {
        return !hasSelection();
    }
    
    
    /**
     * Clears the selection of this SelectionInList - if any.
     */
    public void clearSelection() {
        setSelectionIndex(NO_SELECTION_INDEX);
    }

    
    /**
     * Returns the selection index.
     * 
     * @return the selection index
     * 
     * @throws NullPointerException if the selection index holder 
     *     has a null Object set
     */
    public int getSelectionIndex() {
        return ((Integer) getSelectionIndexHolder().getValue()).intValue();
    }
    
    /**
     * Sets a new selection index. Does nothing if it is the same as before.
     * 
     * @param newSelectionIndex   the selection index to be set
     * @throws IndexOutOfBoundsException if the new selection index
     *    is outside the bounds of the list
     */
    public void setSelectionIndex(int newSelectionIndex) {
        if (newSelectionIndex < NO_SELECTION_INDEX || newSelectionIndex > getSize())
            throw new IndexOutOfBoundsException(
                    "The selection index must be between -1 and " + getSize());
        
        oldSelectionIndex = getSelectionIndex();
        if (oldSelectionIndex == newSelectionIndex)
            return;
        
        getSelectionIndexHolder().setValue(new Integer(newSelectionIndex));
    }
    

    // Accessing the Holders for: List, Selection and Index *******************

    /**
     * Returns the list holder.
     * 
     * @return the list holder
     */
    public ValueModel getListHolder() {
        return listHolder;
    }
    
    /**
     * Sets a new list holder. Does nothing if old and new holder are equal.
     * Removes the list change handler from the old holder and adds
     * it to the new one. In case the list holder contents is a ListModel,
     * the list data change handler is updated too by invoking
     * <code>#updateListDataRegistration</code> in the same way as done in the 
     * list change handler.<p>
     * 
     * TODO: Check and verify whether the list data registration update
     * can be performed in one step <em>after</em> the listHolder has been
     * changed - instead of remove the list data change handler, then
     * changing the listHolder, and finally adding the list data change handler.
     * 
     * @param newListHolder   the list holder to be set
     * 
     * @throws NullPointerException if the new list holder is <code>null</code>
     * @throws IllegalArgumentException if the listHolder is a ValueHolder
     *     that doesn't check the identity when changing its value
     */
    public void setListHolder(ValueModel newListHolder) {
        if (newListHolder == null) 
            throw new NullPointerException("The new list holder must not be null.");
        checkListHolderIdentityCheck(newListHolder);
        
        ValueModel oldListHolder = getListHolder();
        if (equals(oldListHolder, newListHolder))
            return;
        
        Object oldList = list;
        Object newList = newListHolder.getValue();
        
        oldListHolder.removeValueChangeListener(listChangeHandler);
        listHolder = newListHolder;
        newListHolder.addValueChangeListener(listChangeHandler);

        updateList(oldList, newList);
        firePropertyChange(PROPERTYNAME_LIST_HOLDER, 
                           oldListHolder, 
                           newListHolder);
    }
    
    /**
     * Returns the selection holder.
     * 
     * @return the selection holder
     */
    public ValueModel getSelectionHolder() {
        return selectionHolder;
    }
    
    /**
     * Sets a new selection holder. 
     * Does nothing if the new is the same as before.
     * The selection remains unchanged and is still driven
     * by the selection index holder. It's just that future
     * index changes will update the new selection holder
     * and that future selection holder changes affect the
     * selection index.
     * 
     * @param newSelectionHolder   the selection holder to set
     * 
     * @throws NullPointerException if the new selection holder is null
     */
    public void setSelectionHolder(ValueModel newSelectionHolder) {
        if (newSelectionHolder == null) 
            throw new NullPointerException("The new selection holder must not be null.");
        
        ValueModel oldSelectionHolder = getSelectionHolder();
        if (equals(oldSelectionHolder, newSelectionHolder))
            return;
            
        oldSelectionHolder.removeValueChangeListener(selectionChangeHandler);
        selectionHolder = newSelectionHolder;
        oldSelection = newSelectionHolder.getValue();
        newSelectionHolder.addValueChangeListener(selectionChangeHandler);
        firePropertyChange(PROPERTYNAME_SELECTION_HOLDER, 
                           oldSelectionHolder,
                           newSelectionHolder);
    }
    
    /**
     * Returns the selection index holder.
     * 
     * @return the selection index holder
     */
    public ValueModel getSelectionIndexHolder() {
        return selectionIndexHolder;
    }
    
    /**
     * Sets a new selection index holder. 
     * Does nothing if the new is the same as before.
     * 
     * @param newSelectionIndexHolder   the selection index holder to set
     * 
     * @throws NullPointerException if the new selection index holder is null
     * @throws IllegalArgumentException if the value of the new selection index
     *     holder is null
     */
    public void setSelectionIndexHolder(ValueModel newSelectionIndexHolder) {
        if (newSelectionIndexHolder == null) 
            throw new NullPointerException("The new selection index holder must not be null.");
        
        if (newSelectionIndexHolder.getValue() == null)
            throw new IllegalArgumentException("The value of the new selection index holder must not be null.");
        
        ValueModel oldSelectionIndexHolder = getSelectionIndexHolder();
        if (equals(oldSelectionIndexHolder, newSelectionIndexHolder))
            return;
            
        oldSelectionIndexHolder.removeValueChangeListener(selectionIndexChangeHandler);
        selectionIndexHolder = newSelectionIndexHolder;
        newSelectionIndexHolder.addValueChangeListener(selectionIndexChangeHandler);
        oldSelectionIndex = getSelectionIndex();
        oldSelection = getSafeElementAt(oldSelectionIndex);
        firePropertyChange(PROPERTYNAME_SELECTION_INDEX_HOLDER, 
                           oldSelectionIndexHolder,
                           newSelectionIndexHolder);
    }
    
    
    // ValueModel Implementation ********************************************

    /**
     * Returns the current selection, <code>null</code> if the selection index 
     * does not represent a selection in the list.
     * 
     * @return the selected element - if any
     */
    public Object getValue() {
        return getSelection();
    }

    /**
     * Sets the first list element that equals the given value as selection.
     * 
     * @param newValue   the new value to set
     */
    public void setValue(Object newValue) {
        setSelection(newValue);
    }


    // Helper Code ***********************************************************
    
    /**
     * Returns the index in the list of the first occurrence of the specified
     * element, or -1 if the list does not contain this element.
     *
     * @param element  the element to search for
     * @return the index in the list of the first occurrence of the 
     *     given element, or -1 if the list does not contain this element.
     */    
    protected int indexOf(Object element) {
        return indexOf(getListHolder().getValue(), element);
    }
    
    
    /**
     * Returns the index in the list of the first occurrence of the specified
     * element, or -1 if the list does not contain this element.
     *
     * @param aList    the list used to look up the element
     * @param element  the element to search for
     * @return the index in the list of the first occurrence of the 
     *     given element, or -1 if the list does not contain this element.
     */    
    private int indexOf(Object aList, Object element) {
        if (element == null)
            return NO_SELECTION_INDEX;
        else if (getSize(aList) == 0)
            return NO_SELECTION_INDEX;
        
        if (aList instanceof List)
            return ((List) aList).indexOf(element);
        
        // Search the first occurrence of element in the list model.
        ListModel listModel = (ListModel) aList;
        int size = listModel.getSize();
        for (int index = 0; index < size; index++) {
            if (element.equals(listModel.getElementAt(index)))
                return index;
        }
        return NO_SELECTION_INDEX;
    }
    
    
    /**
     * Sets the index according to the selection and initializes
     * the copied selection and selection index.
     * This method is invoked by the constructors to synchronize
     * the selection and index. No listeners are installed yet.
     */
    private void initializeSelectionIndex() {
        Object selectionValue = selectionHolder.getValue();
        if (selectionValue != null) {
            setSelectionIndex(indexOf(selectionValue));
        }
        oldSelection      = selectionValue;
        oldSelectionIndex = getSelectionIndex();
    }
    
    
    /**
     * Removes the list data change handler from the old list in case
     * it is a <code>ListModel</code> and adds it to new one in case
     * it is a <code>ListModel</code>.
     * It then fires a property change for the list and a contents change event
     * for the list content. Finally it tries to restore the previous selection
     * - if any.<p>
     * 
     * Since version 1.1 the selection will be restored after
     * the list content change has been indicated. This is because some
     * listeners may clear the selection in a side-effect.
     * For example a JTable that is bound to this SelectionInList
     * via an AbstractTableAdapter and a SingleSelectionAdapter
     * will clear the selection if the new list has a size other
     * than the old list.<p>
     * 
     * The contents change event is fired using an ExtendedListDataEvent.
     * This provides additional information about the last index of the
     * old list that can be used to convert it to an appropriate
     * TableModelEvent in the AbstractTableAdapter.
     * 
     * @param oldList   the old list content
     * @param newList   the new list content
     * 
     * @see javax.swing.JTable#tableChanged(javax.swing.event.TableModelEvent)
     */
    private void updateList(Object oldList, Object newList) {
        if (oldList != null && (oldList instanceof ListModel)) {
            ((ListModel) oldList).removeListDataListener(listDataChangeHandler);
        }
        if (newList != null && (newList instanceof ListModel)) {
            ((ListModel) newList).addListDataListener(listDataChangeHandler);
        }
        boolean hadSelection = hasSelection();
        Object oldSelectionHolderValue = hadSelection
            ? getSelectionHolder().getValue()
            : null;
        list = newList;
        firePropertyChange(PROPERTYNAME_LIST, oldList, newList);
        fireListChanged(getSize(oldList) - 1, getSize(newList) - 1);
        if (hadSelection) {
            setSelectionIndex(indexOf(newList, oldSelectionHolderValue));
        }
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
     * Throws an IllegalArgumentException if the given ValueModel
     * is a ValueHolder that has the identityCheck feature disabled.
     */
    private void checkListHolderIdentityCheck(ValueModel aListHolder) 
        throws IllegalArgumentException {
        if (!(aListHolder instanceof ValueHolder))
            return;
        
        ValueHolder valueHolder = (ValueHolder) aListHolder;
        if (!valueHolder.isIdentityCheckEnabled()) 
            throw new IllegalArgumentException(
                 "The list holder must have the identity check enabled."); 
    }
    
    
    // Helper Classes *********************************************************
    
    /** 
     * An empty ListModel that has no elements, 
     * a size of 0, and that never fire an event.
     */
    private static final class EmptyListModel implements ListModel, Serializable {
        
        /**
         * Returns zero to indicate an empty list.
         */
        public int getSize() { return 0; }
        
        /**
         * Returns <code>null</code> because this model has no elements.
         */
        public Object getElementAt(int index) { return null; }
        
        /**
         * Does nothing, because the empty list will never fire an event.
         * 
         * @param l the <code>ListDataListener</code> to be ignored
         */  
        public void addListDataListener(ListDataListener l) {
            // Do nothing.
        }

        /**
         * Does nothing, because the empty list will never fire an event.
         * 
         * @param l the <code>ListDataListener</code> to be ignored
         */  
        public void removeListDataListener(ListDataListener l) {
            // Do nothing.
        }
        
    }
    
    
    /**
     * Converts a List to ListModel by wrapping the underlying list.
     */ 
    private static final class ListModelAdapter extends AbstractListModel {

        private final List aList;
        
        ListModelAdapter(List list) { this.aList = list; }

       /** 
        * Returns the length of the list.
        * @return the length of the list
        */
        public int getSize() { return aList.size(); }

        /**
         * Returns the value at the specified index.  
         * @param index the requested index
         * @return the value at <code>index</code>
         */
        public Object getElementAt(int index) { return aList.get(index); }
     
    }
    
    
    // Event Handlers *********************************************************
        
    /**
     * Handles changes of the list model.
     */
    private final class ListChangeHandler implements PropertyChangeListener {

        /**
         * The list has been changed.
         * Notifies all registered listeners about the change.
         * 
         * @param evt   the property change event to be handled
         */
        public void propertyChange(PropertyChangeEvent evt) {
            Object oldList = list;
            Object newList = evt.getNewValue();
            updateList(oldList, newList);
        }
    }

    
    /**
     * Handles ListDataEvents in the list model.
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
            int index0 = evt.getIndex0();
            int index1 = evt.getIndex1();
            int index  = getSelectionIndex();
            fireIntervalAdded(index0, index1);
            // If the added elements are after the index; do nothing.
            if (index >= index0) { 
                setSelectionIndex(index + (index1 - index0 + 1));
            }
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
            int index0 = evt.getIndex0();
            int index1 = evt.getIndex1();
            int index  = getSelectionIndex();
            fireIntervalRemoved(index0, index1);
            if (index < index0) { 
                // The removed elements are after the index; do nothing.
            } else if (index <= index1) {
                setSelectionIndex(NO_SELECTION_INDEX);
            } else {
                setSelectionIndex(index - (index1 - index0 + 1));
            }
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
            updateSelectionContentsChanged(evt.getIndex0(), evt.getIndex1());
        }
        
        private void updateSelectionContentsChanged(int first, int last) {
            if (first < 0) return;
            int selectionIndex = getSelectionIndex();
            if (first <= selectionIndex && (selectionIndex <= last)) {
            // need to synch directly on the holder because the
            // usual methods for setting selection/-index check for 
            // equality
               getSelectionHolder().setValue(getElementAt(selectionIndex));
            }
        }    
        
    }

    /**
     * Listens to changes of the selection.
     */
    private final class SelectionChangeHandler implements PropertyChangeListener {

        /**
         * The selection has been changed. Updates the selection index holder's
         * value and notifies registered listeners about the changes - if any -
         * in the selection index, selection empty, selection, and value.<p>
         * 
         * Adjusts the selection holder's value and the old selection index
         * before any event is fired. This ensures that the event old and
         * new values are consistent with the SelectionInList's state.
         * 
         * @param evt   the property change event to be handled
         */
        public void propertyChange(PropertyChangeEvent evt) {
            Object oldValue     = evt.getOldValue();
            Object newSelection = evt.getNewValue();
            int newSelectionIndex = indexOf(newSelection);
            if (newSelectionIndex != oldSelectionIndex) {
                selectionIndexHolder.removeValueChangeListener(selectionIndexChangeHandler);
                selectionIndexHolder.setValue(new Integer(newSelectionIndex));
                selectionIndexHolder.addValueChangeListener(selectionIndexChangeHandler);
            }
            int theOldSelectionIndex = oldSelectionIndex;
            oldSelectionIndex = newSelectionIndex;
            oldSelection      = newSelection;
            firePropertyChange(PROPERTYNAME_SELECTION_INDEX, 
                    theOldSelectionIndex, 
                    newSelectionIndex);
            firePropertyChange(PROPERTYNAME_SELECTION_EMPTY, 
                    theOldSelectionIndex == NO_SELECTION_INDEX,
                    newSelectionIndex    == NO_SELECTION_INDEX);
            /*
             * Implementation Note: The following two lines fire the 
             * PropertyChangeEvents for the 'selection' and 'value' properties. 
             * If the old and new value are equal, no event is fired.
             * 
             * TODO: Consider using ==, not equals to check for changes.
             * That would enable API users to use the selection holder with
             * beans that must be checked with ==, not equals.
             * However, the SelectionInList's List would still use equals
             * to find the index of an element.
             */
            firePropertyChange(PROPERTYNAME_SELECTION, oldValue, newSelection);
            fireValueChange(oldValue, newSelection);
        }
    }

    /**
     * Listens to changes of the selection index.
     */
    private final class SelectionIndexChangeHandler implements PropertyChangeListener {

        /**
         * The selection index has been changed. Updates the selection holder
         * value and notifies registered listeners about changes - if any -
         * in the selection index, selection empty, selection, and value.<p>
         * 
         * Handles null old values in the index PropertyChangeEvent.
         * Ignores null new values in this events, because the selection
         * index value must always be a non-null value.<p>
         * 
         * Adjusts the selection holder's value and the old selection index
         * before any event is fired. This ensures that the event old and
         * new values are consistent with the SelectionInList's state.
         * 
         * @param evt   the property change event to be handled
         */
        public void propertyChange(PropertyChangeEvent evt) {
            int newSelectionIndex = getSelectionIndex();
            Object theOldSelection = oldSelection;
            //Object oldSelection = getSafeElementAt(oldSelectionIndex);
            Object newSelection = getSafeElementAt(newSelectionIndex);
            /*
             * Implementation Note: The following conditional suppresses 
             * value change events if the old and new selection are equal.
             * 
             * TODO: Consider using ==, not equals to check for changes.
             * That would enable API users to use the selection holder with
             * beans that must be checked with ==, not equals.
             * However, the SelectionInList's List would still use equals
             * to find the index of an element.
             */
            if (!SelectionInList.this.equals(theOldSelection, newSelection)) {
                selectionHolder.removeValueChangeListener(selectionChangeHandler);
                selectionHolder.setValue(newSelection);
                selectionHolder.addValueChangeListener(selectionChangeHandler);
            }
            int theOldSelectionIndex = oldSelectionIndex;
            oldSelectionIndex = newSelectionIndex;
            oldSelection      = newSelection;
            firePropertyChange(PROPERTYNAME_SELECTION_INDEX, 
                    theOldSelectionIndex, 
                    newSelectionIndex);
            firePropertyChange(PROPERTYNAME_SELECTION_EMPTY, 
                    theOldSelectionIndex == NO_SELECTION_INDEX,
                    newSelectionIndex    == NO_SELECTION_INDEX);
            firePropertyChange(PROPERTYNAME_SELECTION, theOldSelection, newSelection);
            fireValueChange(theOldSelection, newSelection);
        }
    }


}
