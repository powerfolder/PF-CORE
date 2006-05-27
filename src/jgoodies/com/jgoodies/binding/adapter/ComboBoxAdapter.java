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

package com.jgoodies.binding.adapter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueModel;

/**
 * A {@link ComboBoxModel} implementation that holds the choice list and a
 * selection. This adapter has two modes that differ primarily in how
 * the selection is kept synchronized with the combo's list.
 * 1) If you construct a ComboBoxAdapter with a {@link SelectionInList},
 * the selection will be guaranteed to be in the list, and the selection
 * will reflect changes in the list. 
 * 2) If you construct this adapter with a separate selection holder, 
 * the selection won't be affected by any change in the combo's list.<p>
 * 
 * In both cases, the combo's list of element will reflect changes in the list,
 * if it's a ListModel and will ignore content changes, if it's a List.<p>
 * 
 * <strong>Example:</strong><pre>
 * String[] countries = new String[] { &quot;USA&quot;, &quot;Germany&quot;, &quot;France&quot;, ... };
 * 
 * // Using an array and ValueModel
 * ValueModel countryModel = new PropertyAdapter(customer, &quot;country&quot;, true);
 * ComboBoxAdapter adapter = new ComboBoxAdapter(countries, contryModel);
 * JComboBox countryBox    = new JComboBox(adapter);
 * 
 * // Using a List and ValueModel
 * List countryList = Arrays.asList(countries);
 * ValueModel countryModel = new PropertyAdapter(customer, &quot;country&quot;, true);
 * ComboBoxAdapter adapter = new ComboBoxAdapter(countryList, contryModel);
 * JComboBox countryBox    = new JComboBox(adapter);
 * 
 * // Using a ListModel and ValueModel
 * ListModel countryListModel = new ArrayListModel(Arrays.asList(countries));
 * ValueModel countryModel = new PropertyAdapter(customer, &quot;country&quot;, true);
 * ComboBoxAdapter adapter = new ComboBoxAdapter(countryListModel, contryModel);
 * JComboBox countryBox    = new JComboBox(adapter);
 * 
 * // Using a SelectionInList - allows only selection of contained elements
 * ListModel countryListModel = new ArrayListModel(Arrays.asList(countries));
 * ValueModel countryModel = new PropertyAdapter(customer, &quot;country&quot;, true);
 * SelectionInList sil     = new SelectionInList(countryListModel, countryModel);
 * ComboBoxAdapter adapter = new ComboBoxAdapter(sil);
 * JComboBox countryBox    = new JComboBox(adapter);
 *  
 * // Using ValueModels for the list holder and the selection holder
 * class Country extends Model {
 *     ListModel getLocales();
 *     Locale getDefaultLocale();
 *     void setDefaultLocale(Locale locale);
 * }
 * 
 * BeanAdapter beanAdapter = new BeanAdapter(null, true);
 * ValueModel localesHolder      = beanAdapter.getValueModel(&quot;locales&quot;);
 * ValueModel defaultLocaleModel = beanAdapter.getValueModel(&quot;defaultLocale&quot;);
 * ComboBoxAdapter adapter = new ComboBoxAdapter(
 *         localesHolder, defaultLocaleModel);
 * JComboBox localeBox = new JComboBox(adapter);
 *  
 * beanAdapter.setBean(myCountry);
 * </pre>
 * 
 * @author Karsten Lentzsch
 * @author Jeanette Winzenburg
 * @version $Revision: 1.2 $
 * 
 * @see javax.swing.JComboBox
 */
public class ComboBoxAdapter extends AbstractListModel implements ComboBoxModel {

    /** 
     * Holds the list of choices. If this adapter has been constructed
     * without a separate selection holder, the SelectionInList also
     * holds this adapter's selection. 
     */
    private final SelectionInList selectionInList;

    /** 
     * In case this adapter has been constructed with a separate
     * selection holder, this ValueModel holds the combo's selection. 
     */
    private final ValueModel selectionHolder;


    // Instance creation ******************************************************

    /**
     * Constructs a ComboBoxAdapter for the specified List of items 
     * and the given selection holder. Structural changes in the list 
     * will be ignored.<p>
     * 
     * <strong>Example:</strong><pre>
     * String[] countries = new String[] { &quot;USA&quot;, &quot;Germany&quot;, &quot;France&quot;, ... };
     * List countryList = Arrays.asList(countries);
     * ValueModel countryModel = new PropertyAdapter(customer, &quot;country&quot;, true);
     * ComboBoxAdapter adapter = new ComboBoxAdapter(countryList, contryModel);
     * JComboBox countryBox    = new JComboBox(adapter);
     * </pre>
     * 
     * @param items            the list of items
     * @param selectionHolder  holds the selection of the combo
     * @throws NullPointerException if the list of items or the selection holder
     *         is <code>null</code>
     */
    public ComboBoxAdapter(List items, ValueModel selectionHolder) {
        this(new SelectionInList(items), selectionHolder, false);
    }


    /**
     * Constructs a ComboBoxAdapter for the given ListModel and selection
     * holder. Structural changes in the ListModel will be reflected by
     * this adapter, but won't affect the selection.<p>
     * 
     * <strong>Example:</strong><pre>
     * String[] countries = new String[] { &quot;USA&quot;, &quot;Germany&quot;, &quot;France&quot;, ... };
     * ListModel countryList = new ArrayListModel(Arrays.asList(countries));
     * ValueModel countryModel = new PropertyAdapter(customer, &quot;country&quot;, true);
     * ComboBoxAdapter adapter = new ComboBoxAdapter(countryList, contryModel);
     * JComboBox countryBox    = new JComboBox(adapter);
     * </pre>
     * 
     * @param listModel         the initial list model
     * @param selectionHolder   holds the selection of the combo
     * @throws NullPointerException if the list of items or the selection holder
     *         is <code>null</code>
     */
    public ComboBoxAdapter(ListModel listModel, ValueModel selectionHolder) {
        this(new SelectionInList(listModel), selectionHolder, false);
    }


    /**
     * Constructs a ComboBoxAdapter for the specified List of items and the
     * given selection holder. Structural changes in the list will be ignored.
     * <p>
     * 
     * <strong>Example:</strong><pre>
     * String[] countries = new String[] { &quot;USA&quot;, &quot;Germany&quot;, &quot;France&quot;, ... };
     * ValueModel countryModel = new PropertyAdapter(customer, &quot;country&quot;, true);
     * ComboBoxAdapter adapter = new ComboBoxAdapter(countries, contryModel);
     * JComboBox countryBox    = new JComboBox(adapter);
     * </pre>
     * 
     * @param items             the list of items
     * @param selectionHolder   holds the selection of the combo
     * @throws NullPointerException if the list of items or the selection holder
     *         is <code>null</code>
     */
    public ComboBoxAdapter(Object[] items, ValueModel selectionHolder) {
        this(new SelectionInList(items), selectionHolder, false);
    }


    /**
     * Constructs a ComboBoxAdapter for the specified list holder and the given
     * selection holder.<p>
     * 
     * <strong>Example:</strong><pre>
     * class Country extends Model {
     *     ListModel getLocales();
     *     Locale getDefaultLocale();
     *     void setDefaultLocale(Locale locale);
     * }
     * 
     * BeanAdapter beanAdapter = new BeanAdapter(null, true);
     * ValueModel localesHolder = beanAdapter.getValueModel(&quot;locales&quot;);
     * ValueModel defaultLocaleModel = beanAdapter.getValueModel(&quot;defaultLocale&quot;);
     * ComboBoxAdapter adapter = new ComboBoxAdapter(localesHolder, defaultLocaleModel);
     * JComboBox localeBox = new JComboBox(adapter);
     * 
     * beanAdapter.setBean(myCountry);
     * </pre>
     * 
     * @param listHolder       holds the list of items
     * @param selectionHolder  holds the selection of the combo
     * @throws NullPointerException if the list holder or the selection holder
     *         is <code>null</code>
     */
    public ComboBoxAdapter(ValueModel listHolder, ValueModel selectionHolder) {
        this(new SelectionInList(listHolder), selectionHolder, false);
    }


    /**
     * Constructs a ComboBoxAdapter for the given SelectionInList. Note that
     * selections which are not elements of the list will be rejected.<p>
     * 
     * <strong>Example:</strong><pre>
     * String[] countries = new String[] { &quot;USA&quot;, &quot;Germany&quot;, &quot;France&quot;, ... };
     * List countryList = Arrays.asList(countries);
     * ValueModel countryModel = new PropertyAdapter(customer, &quot;country&quot;, true);
     * SelectionInList sil     = new SelectionInList(countryList, countryModel);
     * ComboBoxAdapter adapter = new ComboBoxAdapter(sil);
     * JComboBox countryBox    = new JComboBox(adapter);
     * </pre>
     * 
     * @param selectionInList        provides the list and selection
     * @throws NullPointerException if the <code>selectionInList</code> is
     *         <code>null</code>
     */
    public ComboBoxAdapter(SelectionInList selectionInList) {
        this(selectionInList, null, true);
    }


    /**
     * An internal constructor that accepts either a SelectionInList
     * or a separate selection holder.
     * 
     * @param selectionInList      holds the combo's list data; also holds
     *     the selection if no selectionHolder is given
     * @param selectionHolder      holds the combo's selection if non-null
     * @param ignoreNullSelectionHolder  if true the selection holder may be
     *     <code>null</code>, otherwise it must be non-<code>null</code>
     */
    protected ComboBoxAdapter(SelectionInList selectionInList,
                              ValueModel selectionHolder, 
                              boolean ignoreNullSelectionHolder) {
        if ((!ignoreNullSelectionHolder) && (selectionHolder == null)) { 
            throw new NullPointerException("The selection holder must not be null."); 
        } else if (selectionInList == null) {
            throw new NullPointerException("The SelectionInList must not be null.");
        }
        this.selectionInList = selectionInList;
        this.selectionHolder = selectionHolder;
        getSelectionHolder().addValueChangeListener(new SelectionChangeHandler());
        selectionInList.addListDataListener(new ListDataChangeHandler());
    }


    // ComboBoxModel API ****************************************************

    /**
     * Returns the selected item by requesting the current value from the
     * either the selection holder or the SelectionInList's selection.
     * 
     * @return The selected item or <code>null</code> if there is no selection
     */
    public Object getSelectedItem() {
        return getSelectionHolder().getValue();
    }


    /**
     * Sets the selected item. The implementation of this method should notify
     * all registered <code>ListDataListener</code>s that the contents has
     * changed.
     * 
     * @param object the list object to select or <code>null</code> to clear
     *        the selection
     */
    public void setSelectedItem(Object object) {
        getSelectionHolder().setValue(object);
    }


    /**
     * Returns the length of the item list.
     * 
     * @return the length of the list
     */
    public int getSize() {
        return selectionInList.getSize();
    }


    /**
     * Returns the value at the specified index.
     * 
     * @param index the requested index
     * @return the value at <code>index</code>
     */
    public Object getElementAt(int index) {
        return selectionInList.getElementAt(index);
    }
    
    
    // Helper Code ************************************************************
    
    /**
     * Looks up and returns the ValueModel that holds the combo's selection.
     * If this adapter has been constructed with a separate selection holder,
     * this holder is returned. Otherwise the selection is held by the
     * SelectionInList, and so the SelectionInList is returned.
     */
    private ValueModel getSelectionHolder() {
        return selectionHolder != null
            ? selectionHolder
            : selectionInList;
    }


    // Event Handling *********************************************************

    /**
     * Listens to selection changes and fires a contents change event.
     */
    private class SelectionChangeHandler implements PropertyChangeListener {

        /**
         * The selection has changed. Notifies all
         * registered listeners about the change.
         * 
         * @param evt the property change event to be handled
         */
        public void propertyChange(PropertyChangeEvent evt) {
            fireContentsChanged(ComboBoxAdapter.this, -1, -1);
        }
    }

    
    /**
     * Handles ListDataEvents in the list model.
     */
    private class ListDataChangeHandler implements ListDataListener {

        /**
         * Sent after the indices in the index0, index1 interval have been
         * inserted in the data model. The new interval includes both index0 and
         * index1.
         * 
         * @param evt a <code>ListDataEvent</code> encapsulating the event
         *        information
         */
        public void intervalAdded(ListDataEvent evt) {
            fireIntervalAdded(ComboBoxAdapter.this, 
                              evt.getIndex0(), 
                              evt.getIndex1());
        }


        /**
         * Sent after the indices in the index0, index1 interval have been
         * removed from the data model. The interval includes both index0 and
         * index1.
         * 
         * @param evt a <code>ListDataEvent</code> encapsulating the event
         *        information
         */
        public void intervalRemoved(ListDataEvent evt) {
            fireIntervalRemoved(ComboBoxAdapter.this, 
                                evt.getIndex0(), 
                                evt.getIndex1());
        }


        /**
         * Sent when the contents of the list has changed in a way that's too
         * complex to characterize with the previous methods. For example, this
         * is sent when an item has been replaced. Index0 and index1 bracket the
         * change.
         * 
         * @param evt a <code>ListDataEvent</code> encapsulating the event
         *        information
         */
        public void contentsChanged(ListDataEvent evt) {
            fireContentsChanged(ComboBoxAdapter.this, 
                                evt.getIndex0(), 
                                evt.getIndex1());
        }
    }
    
}
