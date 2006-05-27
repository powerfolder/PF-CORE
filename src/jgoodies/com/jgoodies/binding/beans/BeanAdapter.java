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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.util.*;

import com.jgoodies.binding.value.AbstractValueModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

 
/**
 * Converts multiple Java Bean properties into ValueModels. 
 * The bean properties must be single valued properties as described by the 
 * <a href="http://java.sun.com/products/javabeans/docs/spec.html">Java 
 * Bean Specification</a>. See below for a comparison with the more frequently 
 * used PresentationModel class and the rarely used PropertyAdapter.<p>
 * 
 * ValueModels can be created for a property name using
 * {@link #getValueModel(String)} or for a triple of (property name, getter 
 * name, setter name) using {@link #getValueModel(String, String, String)}. 
 * If you just specify the property name, the adapter uses the standard 
 * Java Bean introspection to lookup the available properties and how to 
 * read and write the property value. In case of custom readers and writers 
 * you may specify a custom BeanInfo class, or as a shortcut use the method 
 * that accepts the optional getter and setter name. If these are specified,
 * introspection will be bypassed and a PropertyDescriptor will be
 * created for the given property name, getter and setter name.
 * <strong>Note: </strong> For each property name subsequent calls 
 * to these methods must use the same getter and setter names. Attempts 
 * to violate this constraint are rejected with an IllegalArgumentException.<p>
 * 
 * Property values for a given property name can be read using 
 * {@link #getValue(String)}. To set a value for a for a property name
 * invoke {@link #setValue(String, Object)}.<p>
 * 
 * Optionally the BeanAdapter can observe changes in <em>bound 
 * properties</em> as described in section 7.4 of the Bean specification.
 * The bean then must provide support for listening on properties as described 
 * in section 7.4 of this specification.
 * You can enable this feature by setting the constructor parameter 
 * <code>observeChanges</code> to <code>true</code>.
 * If the adapter observes changes, the ValueModels returned by
 * <code>#getValueModel</code> will fire value change events,
 * i.e. PropertyChangeEvents for the property <code>&quot;value&quot;</code>.
 * Even if you ignore property changes, you can access the adapted
 * property value via <code>#getValue()</code>. 
 * It's just that you won't be notified about changes.<p>
 * 
 * In addition you can observe the bean's bound properties 
 * by registering PropertyChangeListeners with the bean using 
 * <code>#addBeanPropertyChangeListener</code>. These listeners will be removed 
 * from the old bean before the bean changes and will be re-added after 
 * the new bean has been set. Therefore these listeners will be notified 
 * about changes only if the current bean changes a property. They won't be
 * notified if the bean changes - and in turn the property value. If you want
 * to observes property changes caused by bean changes too, register with the 
 * adapting ValueModel as returned by <code>#getValueModel(String)</code>.<p>
 * 
 * The BeanAdapter provides two access styles to the target bean 
 * that holds the adapted property: you can specify a bean directly, 
 * or you can use a <em>bean channel</em> to access the bean indirectly. 
 * In the latter case you specify a <code>ValueModel</code>
 * that holds the bean that in turn holds the adapted properties.<p>
 * 
 * If the adapted bean is <code>null</code> the BeanAdapter can 
 * neither read nor set a value. In this case <code>#getValue</code>
 * returns <code>null</code> and <code>#setValue</code> will silently 
 * ignore the new value.<p>
 * 
 * This adapter throws three PropertyChangeEvents if the bean changes:
 * <em>beforeBean</em>, <em>bean</em> and <em>afterBean</em>. This is useful
 * when sharing a bean channel and you must perform an operation before
 * or after other listeners handle a bean change. Since you cannot rely
 * on the order listeners will be notified, only the <em>beforeBean</em> 
 * and <em>afterBean</em> events are guaranteed to be fired before and
 * after the bean change is fired. 
 * Note that <code>#getBean()</code> returns the new bean before
 * any of these three PropertyChangeEvents is fired. Therefore listeners 
 * that handle these events must use the event's old and new value 
 * to determine the old and new bean. 
 * The order of events fired during a bean change is:<ol>
 * <li>this adapter's bean channel fires a <em>value</em> change,
 * <li>this adapter fires a <em>beforeBean</em> change,
 * <li>this adapter fires the <em>bean</em> change,
 * <li>this adapter fires an <em>afterBean</em> change.
 * </ol><p>
 * 
 * <strong>Note:</strong> 
 * BeanAdapters that observe changes have a PropertyChangeListener 
 * registered with the target bean. Hence, a bean has a reference 
 * to any BeanAdapter that observes it. To avoid memory leaks 
 * it is recommended to remove this listener if the bean lives much longer
 * than the BeanAdapter, enabling the garbage collector to remove the adapter.
 * To do so, you can call <code>setBean(null)</code> or set the
 * bean channel's value to null.
 * As an alternative you can use event listener lists in your beans
 * that implement references with <code>WeakReference</code>.<p>
 * 
 * Setting the bean to null has side-effects, for example the adapter 
 * fires a change event for the bound property <em>bean</em> and other properties.
 * And the value of ValueModel's vended by this adapter may change. 
 * However, typically this is fine and setting the bean to null 
 * is the first choice for removing the reference from the bean to the adapter. 
 * Another way to clear the reference from the target bean is
 * to call <code>#release</code>. It has no side-effects, but the adapter
 * must not be used anymore once #release has been called.<p> 
 * 
 * <strong>Constraints:</strong> If property changes shall be observed, 
 * the bean class must support bound properties, i. e. it must provide 
 * the following pair of methods for registration of multicast property 
 * change event listeners:
 * <pre>
 * public void addPropertyChangeListener(PropertyChangeListener x);
 * public void removePropertyChangeListener(PropertyChangeListener x);
 * </pre>
 * 
 * <strong>PropertyAdapter vs. BeanAdapter vs. PresentationModel</strong><br>
 * Basically the BeanAdapter does for multiple properties what the
 * {@link com.jgoodies.binding.beans.PropertyAdapter} does for a 
 * single bean property. 
 * If you adapt multiple properties of the same bean, you better use 
 * the BeanAdapter. It registers a single PropertyChangeListener with the bean, 
 * where multiple PropertyAdapters would register multiple listeners.
 * If you adapt bean properties for an editor, you will typically use the
 * {@link com.jgoodies.binding.PresentationModel}. The PresentationModel is
 * more powerful than the BeanAdapter. It adds support for buffered models, 
 * and provides an extensible mechanism for observing the change state
 * of the bean and related objects.<p>
 * 
 * <strong>Basic Examples:</strong>
 * <pre>
 * // Direct access, ignores changes
 * Address address = new Address()
 * BeanAdapter adapter = new BeanAdapter(address);
 * adapter.setValue("street", "Broadway");
 * System.out.println(address.getStreet());        // Prints "Broadway"
 * address.setStreet("Franz-Josef-Str.");
 * System.out.println(adapter.getValue("street")); // Prints "Franz-Josef-Str."
 * 
 * 
 * //Direct access, observes changes
 * BeanAdapter adapter = new BeanAdapter(address, true);
 * 
 * 
 * // Indirect access, ignores changes
 * ValueHolder addressHolder = new ValueHolder(address1);
 * BeanAdapter adapter = new BeanAdapter(addressHolder);
 * adapter.setValue("street", "Broadway");         // Sets the street in address1
 * System.out.println(address1.getStreet());       // Prints "Broadway"
 * adapter.setBean(address2);
 * adapter.setValue("street", "Robert-Koch-Str."); // Sets the street in address2
 * System.out.println(address2.getStreet());       // Prints "Robert-Koch-Str."
 *
 *  
 * // Indirect access, observes changes
 * ValueHolder addressHolder = new ValueHolder();
 * BeanAdapter adapter = new BeanAdapter(addressHolder, true);
 * addressHolder.setValue(address1);
 * address1.setStreet("Broadway");
 * System.out.println(adapter.getValue("street")); // Prints "Broadway"
 * 
 * 
 * // Access through ValueModels
 * Address address = new Address();
 * BeanAdapter adapter = new BeanAdapter(address);
 * ValueModel streetModel = adapter.getValueModel("street");
 * ValueModel cityModel   = adapter.getValueModel("city");
 * streetModel.setValue("Broadway");
 * System.out.println(address.getStreet());        // Prints "Broadway"
 * address.setCity("Hamburg");
 * System.out.println(cityModel.getValue());       // Prints "Hamburg"
 * </pre>
 * 
 * <strong>Adapter Chain Example:</strong>
 * <br>Builds an adapter chain from a domain model to the presentation layer.
 * <pre>
 * Country country = new Country();
 * country.setName("Germany");
 * country.setEuMember(true);
 * 
 * BeanAdapter countryAdapter = new BeanAdapter(country, true);
 * 
 * JTextField nameField = new JTextField();
 * nameField.setDocument(new DocumentAdapter(
 *     countryAdapter.getValueModel("name")));
 * 
 * JCheckBox euMemberBox = new JCheckBox("Is EU Member");
 * euMemberBox.setModel(new ToggleButtonAdapter(
 *      countryAdapter.getValueModel("euMember")));
 * 
 * // Using factory methods
 * JTextField nameField   = Factory.createTextField(country, "name");
 * JCheckBox  euMemberBox = Factory.createCheckBox (country, "euMember");
 * euMemberBox.setText("Is EU Member");
 * </pre><p>
 * 
 * TODO: Improve the class comment and focus on the main features.<p>
 * 
 * TODO: Consider adding a feature to ensure that update notifications
 * are performed in the event dispatch thread. In case the adapted bean 
 * is changed in a thread other than the event dispatch thread, such 
 * a feature would help complying with Swing's single thread rule. 
 * The feature could be implemented by an extended PropertyChangeSupport.<p>
 * 
 * TODO: I plan to improve the support for adapting beans that do not fire 
 * PropertyChangeEvents. This affects the classes PropertyAdapter, BeanAdapter, 
 * and PresentationModel. Basically the PropertyAdapter and the BeanAdapter's
 * internal SimplePropertyAdapter's shall be able to optionally self-fire 
 * a PropertyChangeEvent in case the bean does not. There are several 
 * downsides with self-firing events compared to bound bean properties.
 * See <a href="https://binding.dev.java.net/issues/show_bug.cgi?id=49">Issue
 * 49</a> for more information about the downsides.<p>
 * 
 * The observeChanges constructor parameter shall be replaced by a more
 * fine-grained choice to not observe (former observeChanges=false),
 * to observe bound properties (former observeChanges=true), and a new 
 * setting for self-firing PropertyChangeEvents if a value is set.
 * The latter case may be further splitted up to specify how the
 * self-fired PropertyChangeEvent is created:
 * <ol>
 * <li>oldValue=null, newValue=null
 * <li>oldValue=null, newValue=the value set
 * <li>oldValue=value read before the set, newValue=the value set
 * <li>oldValue=value read before the set, newValue=value read after the set
 * </ol>
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.10 $
 * 
 * @see     com.jgoodies.binding.beans.PropertyAdapter
 * @see     ValueModel
 * @see     ValueModel#getValue()
 * @see     ValueModel#setValue(Object)
 * @see     PropertyChangeEvent
 * @see     PropertyChangeListener
 * @see     java.beans.Introspector
 * @see     java.beans.BeanInfo
 * @see     PropertyDescriptor
 */
public final class BeanAdapter extends Model {
    
    /**
     * The property name used in the PropertyChangeEvent that is fired
     * before the <em>bean</em> property fires its PropertyChangeEvent.
     * Useful to perform an operation before listeners that handle the
     * bean change are notified. See also the class comment.
     */
    public static final String PROPERTYNAME_BEFORE_BEAN = "beforeBean";

    /**
     * The name of the read-write bound property that holds the target bean.
     * 
     * @see #getBean()
     * @see #setBean(Object)
     */
    public static final String PROPERTYNAME_BEAN = "bean";

    /**
     * The property name used in the PropertyChangeEvent that is fired
     * after the <em>bean</em> property fires its PropertyChangeEvent.
     * Useful to perform an operation after listeners that handle the
     * bean change are notified. See also the class comment.
     */
    public static final String PROPERTYNAME_AFTER_BEAN = "afterBean";

    /**
     * The name of the read-only bound bean property that 
     * indicates whether one of the observed properties has changed.
     * 
     * @see #isChanged()
     */
    public static final String PROPERTYNAME_CHANGED = "changed";
    
    
    // Private Constant *******************************************************
    
    private static final String PROPERTY_NULL_MESSAGE = 
        "The property name must not be null.";

    
    // Fields *****************************************************************
    
    /**
     * Holds a ValueModel that holds the bean 
     * that in turn holds the adapted property.
     * 
     * @see #getBean()
     * @see #setBean(Object)
     */
    private final ValueModel beanChannel;  
    
    /**
     * Specifies whether we observe property changes and in turn
     * fire state changes.
     * 
     * @see #getObserveChanges()
     */
    private final boolean observeChanges;

    /**
     * Maps property names to the associated SimplePropertyAdapters.
     * 
     * @see #getValueModel(String)
     * @see #getValueModel(String, String, String)
     */
    private final Map propertyAdapters;

    /**
     * Refers to the IndirectPropertyChangeSupport that is used to redirect
     * PropertyChangelisteners to the current target bean.
     */
    private IndirectPropertyChangeSupport indirectChangeSupport;
    
    /**
     * Refers to the old bean. Used as old value if the bean changes.
     * Updated after a bean change in the BeanChangeHandler.
     */
    Object storedOldBean;

    /**
     * Indicates whether a property in the current target been has changed.
     * Will be reset to <code>false</code> everytime the target bean changes.
     * 
     * @see #isChanged()
     * @see #setBean(Object)
     */
    private boolean changed = false;
    
    /**
     * The <code>PropertyChangeListener</code> used to handle changes 
     * in the bean properties. A new instance is created everytime
     * the target bean changes.
     */
    private PropertyChangeListener propertyChangeHandler;
    

    // Instance creation ****************************************************

    /**
     * Constructs a BeanAdapter for the given bean; 
     * does not observe changes.<p>
     * 
     * Installs a default bean channel that checks the identity not equity 
     * to ensure that listeners are reregistered properly if the old and
     * new bean are equal but not the same.
     * 
     * @param bean  the bean that owns the properties to adapt
     */
    public BeanAdapter(Object bean) {
        this(bean, false);
    }
    

    /**
     * Constructs a BeanAdapter for the given bean; 
     * observes changes if specified.<p>
     * 
     * Installs a default bean channel that checks the identity not equity 
     * to ensure that listeners are reregistered properly if the old and
     * new bean are equal but not the same.
     * 
     * @param bean             the bean that owns the properties to adapt
     * @param observeChanges   <code>true</code> to observe changes of bound 
     *     or constrained properties, <code>false</code> to ignore changes
     * @throws PropertyUnboundException  if <code>observeChanges</code> 
     *     is true but the property is unbound, i. e. the <code>bean</code> 
     *     does not provide a pair of methods to register a multicast 
     *     PropertyChangeListener
     */
    public BeanAdapter(
        Object bean,
        boolean observeChanges) {
        this(new ValueHolder(bean, true), observeChanges);
    }
    

    /**
     * Constructs a BeanAdapter for the given bean channel; 
     * does not observe changes.<p>
     * 
     * It is strongly recommended that the bean channel checks the identity 
     * not equity. This ensures that listeners are reregistered properly if 
     * the old and new bean are equal but not the same.
     * 
     * @param beanChannel    the ValueModel that holds the bean
     */
    public BeanAdapter(ValueModel beanChannel) {
        this(beanChannel, false);
    }


    /**
     * Constructs a BeanAdapter for the given bean channel;
     * observes changes if specified.<p>
     * 
     * It is strongly recommended that the bean channel checks the identity 
     * not equity. This ensures that listeners are reregistered properly if 
     * the old and new bean are equal but not the same.
     * 
     * @param beanChannel     the ValueModel that holds the bean
     * @param observeChanges  <code>true</code> to observe changes of bound 
     *  or constrained properties, <code>false</code> to ignore changes
     *  
     * @throws IllegalArgumentException    if the beanChannel is a ValueHolder
     *     that has the identityCheck feature disabled
     * @throws PropertyUnboundException    if <code>observeChanges</code> 
     *     is true but the property is unbound, i. e. the <code>bean</code> 
     *     does not provide a pair of methods to register a multicast 
     *     PropertyChangeListener
     */
    public BeanAdapter(
        ValueModel beanChannel,
        boolean observeChanges) {
        this.beanChannel = beanChannel != null 
            ? beanChannel 
            : new ValueHolder(null, true);
        checkBeanChannelIdentityCheck(beanChannel);
        
        this.observeChanges = observeChanges;
        this.propertyAdapters = new HashMap();
        
        this.beanChannel.addValueChangeListener(new BeanChangeHandler());

        Object initialBean = getBean();
        if (initialBean != null) {
            if (observeChanges 
             && !BeanUtils.supportsBoundProperties(getBeanClass(initialBean)))
                throw new PropertyUnboundException(
                        "The bean must provide support for listening on property changes "
                      + "as described in section 7.4.5 of the Java Bean Specification.");
            addChangeHandlerTo(initialBean);
        }
        storedOldBean = initialBean;
    }
    

    // Accessors ************************************************************

    /**
     * Returns the Java Bean that holds the adapted properties.
     * 
     * @return the Bean that holds the adapted properties
     * 
     * @see #setBean(Object)
     */
    public Object getBean() {
        return beanChannel.getValue();
    }
    
    
    /**
     * Sets a new Java Bean as holder of the adapted properties.
     * Notifies any registered value listeners that are registered
     * with the adapting ValueModels created in <code>#getValueModel</code>. 
     * Also notifies listeners that have been registered with this adapter
     * to observe the bound property <em>bean</em>.<p>
     * 
     * Resets the changed state to <code>false</code>.<p>
     *
     * If this adapter observes bean changes, the bean change handler 
     * will be removed from the former bean and will be added to the new bean.
     * Hence, if the new bean is <code>null</code>, this adapter has no 
     * listener registered with a bean. 
     * And so, <code>setBean(null)</code> can be used as a clean release method
     * that allows to use this adapter later again.
     * 
     * @param newBean  the new holder of the adapted properties
     * 
     * @see #getBean()
     * @see #isChanged()
     * @see #resetChanged()
     * @see #release()
     */
    public void setBean(Object newBean) {
        beanChannel.setValue(newBean);    
        resetChanged();
    }
    
    
    /**
     * Answers whether this adapter observes changes in the
     * adapted Bean properties.
     * 
     * @return true if this adapter observes changes, false if not
     */
    public boolean getObserveChanges() {
        return observeChanges;
    }
    
    /*
     * Sets whether changes in the bean's bound property shall be observed.
     * As a requirement the bean must provide support for listenening 
     * on property changes.
     * 
     * @param newValue  true to observe changes, false to ignore them
    public void setObserveChanges(boolean newValue) {
        if (newValue == getObserveChanges())
            return;
        observeChanges = newValue;
        Object bean = getBean();
        removePropertyChangeHandler(bean);
        addPropertyChangeHandler(bean);            
    }
    */

    // Accessing Property Values **********************************************

    /**
     * Returns the value of specified bean property, <code>null</code> 
     * if the current bean is <code>null</code>.<p>
     * 
     * This operation is supported only for readable bean properties.
     * 
     * @param propertyName  the name of the property to be read
     * @return the value of the adapted bean property, null if the bean is null
     * 
     * @throws NullPointerException           if propertyname is null
     * @throws UnsupportedOperationException  if the property is write-only
     * @throws PropertyNotFoundException      if the property could not be found
     * @throws PropertyAccessException        if the value could not be read
     */
    public Object getValue(String propertyName) {
        if (propertyName == null)
            throw new NullPointerException(PROPERTY_NULL_MESSAGE);
        
        Object bean = getBean();
        if (bean == null) {
            return null;
        }
        return getValue0(bean, propertyName);
    }

    
    /**
     * Sets the given new value for the specified bean property. Does nothing 
     * if this adapter's bean is <code>null</code>. If the setter associated
     * with the propertyName throws a PropertyVetoException, it is silently
     * ignored.<p>
     * 
     * Notifies the associated value change listeners if the bean reports 
     * a property change. Note that a bean may suppress PropertyChangeEvents
     * if the old and new value are the same, or if the old and new value
     * are equal.<p>
     * 
     * This operation is supported only for writable bean properties.
     * 
     * @param propertyName   the name of the property to set
     * @param newValue       the value to set
     * 
     * @throws NullPointerException           if propertyname is null
     * @throws UnsupportedOperationException  if the property is read-only
     * @throws PropertyNotFoundException      if the property could not be found
     * @throws PropertyAccessException        if the new value could not be set
     */
    public void setValue(String propertyName, Object newValue) {
        if (propertyName == null)
            throw new NullPointerException(PROPERTY_NULL_MESSAGE);
        
        try {
            setVetoableValue(propertyName, newValue);
        } catch (PropertyVetoException e) {
            // Silently ignore this situation.
        }
    }
    

    /**
     * Sets a new value for the specified bean property. Does nothing if the
     * bean is <code>null</code>. If the setter associated with the propertyName 
     * throws a PropertyVetoException, this methods throws the same exception.<p>
     * 
     * Notifies the associated value change listeners if the bean reports 
     * a property change. Note that a bean may suppress PropertyChangeEvents
     * if the old and new value are the same, or if the old and new value
     * are equal.<p>
     * 
     * This operation is supported only for writable bean properties.
     * 
     * @param propertyName   the name of the property to set
     * @param newValue       the value to set
     * 
     * @throws NullPointerException           if propertyname is null
     * @throws UnsupportedOperationException  if the property is read-only
     * @throws PropertyNotFoundException      if the property could not be found
     * @throws PropertyAccessException        if the new value could not be set
     * @throws PropertyVetoException          if the bean setter
     *     throws a PropertyVetoException
     *     
     * @since 1.1
     */
    public void setVetoableValue(String propertyName, Object newValue) throws PropertyVetoException {
        if (propertyName == null)
            throw new NullPointerException(PROPERTY_NULL_MESSAGE);
        
        Object bean = getBean();
        if (bean == null) {
            return;
        }
        setValue0(bean, propertyName, newValue);
    }
    

    // Creating and Accessing Adapting ValueModels ****************************
    
    /**
     * Looks up and lazily creates a ValueModel that adapts 
     * the bound property with the specified name. Uses the 
     * Bean introspection to look up the getter and setter names.<p>
     * 
     * Subsequent calls to this method with the same property name 
     * return the same ValueModel.<p>
     * 
     * To prevent potential runtime errors this method eagerly looks up 
     * the associated PropertyDescriptor if the target bean is not null.<p>
     * 
     * For each property name all calls to this method and to 
     * <code>#getValueModel(String, String, String)</code> must use 
     * the same getter and setter names. Attempts to violate this constraint 
     * will be rejected with an IllegalArgumentException. Especially once
     * you've called this method you must not call 
     * <code>#getValueModel(String, String, String)</code> with a non-null
     * getter or setter name. And vice versa, once you've called the latter 
     * method with a non-null getter or setter name, you must not call 
     * this method.<p>
     * 
     * This method uses a return type of AbstractValueModel, not a ValueModel.
     * This makes the AbstractValueModel convenience type converters available,
     * which can significantly shrink the source code necessary to read and 
     * write values from/to these models.
     * 
     * @param propertyName   the name of the property to adapt
     * @return a ValueModel that adapts the property with the specified name
     * 
     * @throws NullPointerException       if propertyname is null
     * @throws PropertyNotFoundException  if the property could not be found
     * @throws IllegalArgumentException   
     *     if <code>#getValueModel(String, String, String)</code> has been 
     *     called before with the same property name and a non-null getter 
     *     or setter name
     */
    public SimplePropertyAdapter getValueModel(String propertyName) {
        return getValueModel(propertyName, null, null);
    }

    
    /**
     * Looks up and lazily creates a ValueModel that adapts the bound property 
     * with the specified name. Unlike <code>#getValueModel(String)</code>
     * this method bypasses the Bean Introspection and uses the given getter 
     * and setter names to setup the access to the adapted Bean property.<p>
     * 
     * Subsequent calls to this method with the same parameters 
     * will return the same ValueModel.<p>
     * 
     * To prevent potential runtime errors this method eagerly looks up 
     * the associated PropertyDescriptor if the target bean is not null.<p>
     * 
     * For each property name all calls to this method 
     * and to <code>#getValueModel(String)</code> must use the same 
     * getter and setter names. Attempts to violate this constraint 
     * will be rejected with an IllegalArgumentException. Especially 
     * once you've called this method with a non-null getter or setter name, 
     * you must not call <code>#getValueModel(String)</code>. And vice versa, 
     * once you've called the latter method you must not call this method 
     * with a non-null getter or setter name.<p>
     * 
     * This method uses a return type of AbstractValueModel, not a ValueModel.
     * This makes the AbstractValueModel convenience type converters available,
     * which can significantly shrink the source code necessary to read and 
     * write values from/to these models.
     * 
     * @param propertyName   the name of the property to adapt
     * @param getterName     the name of the method that reads the value
     * @param setterName     the name of the method that sets the value
     * @return a ValueModel that adapts the property with the specified name
     * 
     * @throws NullPointerException       if propertyname is null
     * @throws PropertyNotFoundException  if the property could not be found
     * @throws IllegalArgumentException   if this method has been called before
     *     with the same property name and different getter or setter names
     */
    public SimplePropertyAdapter getValueModel(String propertyName, String getterName, String setterName) {
        if (propertyName == null)
            throw new NullPointerException("The property name must not be null.");
        
        SimplePropertyAdapter adaptingModel = getPropertyAdapter(propertyName);
        if (adaptingModel == null) {
            adaptingModel = new SimplePropertyAdapter(propertyName, getterName, setterName);
            propertyAdapters.put(propertyName, adaptingModel);
        } else if    (!equals(getterName, adaptingModel.getterName)
                   || !equals(setterName, adaptingModel.setterName)) {
               throw new IllegalArgumentException(
                       "You must not invoke this method twice "
                     + "with different getter and/or setter names."); 
        }
        return adaptingModel;
    }
    
    
    /**
     * Looks up and returns the SimplePropertyAdapter that adapts
     * the bound property with the specified name.
     * 
     * @param propertyName   the name of the adapted property
     * @return a SimplePropertyAdapter that adapts the bound property
     *    with the specified name or null, if none has been created before
     */
    SimplePropertyAdapter getPropertyAdapter(String propertyName) {
        return (SimplePropertyAdapter) propertyAdapters.get(propertyName);
    }
    

    // Accessing the Changed State ********************************************
    
    /**
     * Answers whether a bean property has changed since the changed state
     * has been reset. The changed state is implicitly reset everytime 
     * the target bean changes.
     * 
     * @return true if a property of the current target bean 
     *     has changed since the last reset
     */
    public boolean isChanged() {
        return changed;
    }


    /**
     * Resets this tracker's changed state to <code>false</code>.
     */
    public void resetChanged() {
        setChanged(false);
    }
    
    
    /**
     * Sets the changed state to the given value. Invoked by the global
     * PropertyChangeHandler that observes all bean changes. Also invoked
     * by <code>#resetChanged</code>.
     * 
     * @param newValue  the new changed state
     */
    private void setChanged(boolean newValue) {
        boolean oldValue = isChanged();
        changed = newValue;
        firePropertyChange(PROPERTYNAME_CHANGED, oldValue, newValue);
    }

    
    // Managing Bean Property Change Listeners *******************************

    /**
     * Adds a PropertyChangeListener to the list of bean listeners. The 
     * listener is registered for all bound properties of the target bean.<p>
     * 
     * The listener will be notified if and only if this BeanAdapter's current 
     * bean changes a property. It'll not be notified if the bean changes.<p>
     * 
     * If listener is <code>null</code>, no exception is thrown and no action 
     * is performed.
     *
     * @param listener      the PropertyChangeListener to be added
     *
     * @see #removeBeanPropertyChangeListener(PropertyChangeListener)
     * @see #removeBeanPropertyChangeListener(String, PropertyChangeListener)
     * @see #addBeanPropertyChangeListener(String, PropertyChangeListener)
     * @see #getBeanPropertyChangeListeners()
     */
    public synchronized void addBeanPropertyChangeListener(
                                            PropertyChangeListener listener) {
        if (listener == null) 
            return;
        
        if (indirectChangeSupport == null) { 
            indirectChangeSupport = new IndirectPropertyChangeSupport(beanChannel);
        }
        indirectChangeSupport.addPropertyChangeListener(listener);
    }
    
    
    /**
     * Removes a PropertyChangeListener from the list of bean listeners. 
     * This method should be used to remove PropertyChangeListeners that 
     * were registered for all bound properties of the target bean.<p>
     * 
     * If listener is <code>null</code>, no exception is thrown and no action is performed.
     *
     * @param listener      the PropertyChangeListener to be removed
     * 
     * @see #addBeanPropertyChangeListener(PropertyChangeListener)
     * @see #addBeanPropertyChangeListener(String, PropertyChangeListener)
     * @see #removeBeanPropertyChangeListener(String, PropertyChangeListener)
     * @see #getBeanPropertyChangeListeners()
     */
    public synchronized void removeBeanPropertyChangeListener(
                                        PropertyChangeListener listener) {
        if (listener == null || indirectChangeSupport == null) {
            return;
        }
        indirectChangeSupport.removePropertyChangeListener(listener);
    }
    
    
    /**
     * Adds a PropertyChangeListener to the list of bean listeners for a 
     * specific property. The specified property may be user-defined.<p>
     * 
     * The listener will be notified if and only if this BeanAdapter's 
     * current bean changes the specified property. It'll not be notified 
     * if the bean changes. If you want to observe property changes and 
     * bean changes, you may observe the ValueModel that adapts this property 
     * - as returned by <code>#getValueModel(String)</code>.<p>
     * 
     * Note that if the bean is inheriting a bound property, then no event
     * will be fired in response to a change in the inherited property.<p>
     * 
     * If listener is <code>null</code>, no exception is thrown and no action is performed.
     *
     * @param propertyName      one of the property names listed above
     * @param listener          the PropertyChangeListener to be added
     *
     * @see #removeBeanPropertyChangeListener(String, PropertyChangeListener)
     * @see #addBeanPropertyChangeListener(String, PropertyChangeListener)
     * @see #getBeanPropertyChangeListeners(String)
     */
    public synchronized void addBeanPropertyChangeListener(
                                        String propertyName,
                                        PropertyChangeListener listener) {
        if (listener == null) 
            return;
        
        if (indirectChangeSupport == null) { 
            indirectChangeSupport = new IndirectPropertyChangeSupport(beanChannel);
        }
        indirectChangeSupport.addPropertyChangeListener(propertyName, listener);
    }
    
    
    /**
     * Removes a PropertyChangeListener from the listener list for a specific
     * property. This method should be used to remove PropertyChangeListeners
     * that were registered for a specific bound property.<p>
     * 
     * If listener is <code>null</code>, no exception is thrown and no action is performed.
     *
     * @param propertyName      a valid property name
     * @param listener          the PropertyChangeListener to be removed
     *
     * @see #addBeanPropertyChangeListener(String, PropertyChangeListener)
     * @see #removeBeanPropertyChangeListener(PropertyChangeListener)
     * @see #getBeanPropertyChangeListeners(String)
     */
    public synchronized void removeBeanPropertyChangeListener(
                                        String propertyName,
                                        PropertyChangeListener listener) {
        if (listener == null || indirectChangeSupport == null) {
            return;
        }
        indirectChangeSupport.removePropertyChangeListener(propertyName, listener);
    }
    
    
    // Requesting Listener Sets ***********************************************

    /**
     * Returns an array of all the property change listeners
     * registered on this component.
     *
     * @return all of this component's <code>PropertyChangeListener</code>s
     *         or an empty array if no property change
     *         listeners are currently registered
     *
     * @see #addBeanPropertyChangeListener(PropertyChangeListener)
     * @see #removeBeanPropertyChangeListener(PropertyChangeListener)
     * @see #getBeanPropertyChangeListeners(String)
     * @see java.beans.PropertyChangeSupport#getPropertyChangeListeners()
     */
    public synchronized PropertyChangeListener[] getBeanPropertyChangeListeners() {
        if (indirectChangeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return indirectChangeSupport.getPropertyChangeListeners();
    }

    
    /**
     * Returns an array of all the listeners which have been associated 
     * with the named property.
     *
     * @param propertyName   the name of the property to lookup listeners
     * @return all of the <code>PropertyChangeListeners</code> associated with
     *         the named property or an empty array if no listeners have 
     *         been added
     *
     * @see #addBeanPropertyChangeListener(String, PropertyChangeListener)
     * @see #removeBeanPropertyChangeListener(String, PropertyChangeListener)
     * @see #getBeanPropertyChangeListeners()
     */
    public synchronized PropertyChangeListener[] getBeanPropertyChangeListeners(String propertyName) {
        if (indirectChangeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return indirectChangeSupport.getPropertyChangeListeners(propertyName);
    }
    

    // Releasing PropertyChangeListeners **************************************
    
    /**
     * Removes the PropertyChangeHandler from the observed bean, if the bean 
     * is not <code>null</code> and if bean property changes are observed.<p>
     * 
     * BeanAdapters that observe changes have a PropertyChangeListener 
     * registered with the target bean. Hence, a bean has a reference 
     * to any BeanAdapter that observes it. To avoid memory leaks 
     * it is recommended to remove this listener if the bean lives much longer
     * than the BeanAdapter, enabling the garbage collector to remove the adapter.
     * To do so, you can call <code>setBean(null)</code> or set the
     * bean channel's value to null.
     * As an alternative you can use event listener lists in your beans
     * that implement references with <code>WeakReference</code>.<p>
     * 
     * Setting the bean to null has side-effects, for example the adapter 
     * fires a change event for the bound property <em>bean</em> and other properties.
     * And the value of ValueModel's vended by this adapter may change. 
     * However, typically this is fine and setting the bean to null 
     * is the first choice for removing the reference from the bean to the adapter. 
     * Another way to clear the reference from the target bean is
     * to call <code>#release</code>. It has no side-effects, but the adapter
     * must not be used anymore once #release has been called.
     * 
     * @see #setBean(Object)
     * @see java.lang.ref.WeakReference
     */
    public void release() {
        removeChangeHandlerFrom(getBean());    
    }
    
    
    // Changing the Bean & Adding and Removing the PropertyChangeHandler ******

    private void setBean0(Object oldBean, Object newBean) {
        firePropertyChange(PROPERTYNAME_BEFORE_BEAN, oldBean, newBean, true);
        removeChangeHandlerFrom(oldBean);
        forwardAllAdaptedValuesChanged(oldBean, newBean);
        resetChanged();
        addChangeHandlerTo(newBean);
        firePropertyChange(PROPERTYNAME_BEAN, oldBean, newBean, true);
        firePropertyChange(PROPERTYNAME_AFTER_BEAN, oldBean, newBean, true);
    }
    
    
    /**
     * Iterates over all internal property adapters to notify them
     * about a bean change from oldBean to newBean. These adapters then notify 
     * their observers to inform them about a value change - if any.<p>
     * 
     * Iterates over a copy of the property adapters to avoid 
     * ConcurrentModifications that may be thrown if a listener creates
     * a new SimplePropertyAdapter by requesting an adapting model using
     * <code>#getValueModel</code>.
     * 
     * @param oldBean   the bean before the change
     * @param newBean   the bean after the change
     */
    private void forwardAllAdaptedValuesChanged(Object oldBean, Object newBean) {
        for (Iterator iter = new LinkedList(propertyAdapters.values()).iterator(); iter.hasNext();) {
            SimplePropertyAdapter adapter = (SimplePropertyAdapter) iter.next();
            adapter.setBean0(oldBean, newBean);
        }
    }
    
    
    /**
     * Iterates over all internal property adapters to notify them
     * about a value change in the bean. These adapters then notify 
     * their observers to inform them about a value change - if any.<p>
     * 
     * Iterates over a copy of the property adapters to avoid 
     * ConcurrentModifications that may be thrown if a listener creates
     * a new SimplePropertyAdapter by requesting an adapting model using
     * <code>#getValueModel</code>.
     */
    private void forwardAllAdaptedValuesChanged() {
        Object currentBean = getBean();
        for (Iterator iter = new LinkedList(propertyAdapters.values()).iterator(); iter.hasNext();) {
            SimplePropertyAdapter adapter = (SimplePropertyAdapter) iter.next();
            adapter.fireChange(currentBean);
        }
    }
    
    
    /**
     * Adds a property change listener to the given bean if we observe changes 
     * and the bean is not null. First checks whether the bean class 
     * supports <em>bound properties</em>, i.e. it provides a pair of methods 
     * to register multicast property change event listeners; 
     * see section 7.4.1 of the Java Beans specification for details.
     * 
     * @param bean  the bean to add a property change handler.
     * @throws PropertyUnboundException  
     *     if the bean does not support bound properties
     * @throws PropertyNotBindableException  
     *     if the property change handler cannot be added successfully 
     *     
     * @see #removeChangeHandlerFrom(Object)
     */
    private void addChangeHandlerTo(Object bean) {
        if (!observeChanges || bean == null)
            return;

        propertyChangeHandler = new PropertyChangeHandler();
        BeanUtils.addPropertyChangeListener(bean, getBeanClass(bean), propertyChangeHandler);
    }


    /**
     * Removes the formerly added property change handler from the given bean
     * if we observe changes and the bean is not null. 
     * 
     * @param bean  the bean to remove the property change handler from.
     * @throws PropertyUnboundException  
     *     if the bean does not support bound properties
     * @throws PropertyNotBindableException  
     *     if the property change handler cannot be removed successfully 
     *     
     * @see #addChangeHandlerTo(Object)
     */
    private void removeChangeHandlerFrom(Object bean) {
        if (!observeChanges || bean == null)
            return;
        
        BeanUtils.removePropertyChangeListener(bean, getBeanClass(bean), propertyChangeHandler);
        propertyChangeHandler = null;
    }


    // Helper Methods to Get and Set a Property Value *************************
    
    /**
     * Returns the value of the specified property of the given bean, 
     * <code>null</code> if the bean is <code>null</code>.
     * 
     * @param bean          the bean to read the value from
     * @param propertyName  the name of the property to be read
     * @return the bean's property value
     */
    private Object getValue0(Object bean, String propertyName) {
        return getValue0(bean, getPropertyDescriptor(bean, propertyName));
    }
    
    
    /**
     * Returns the Java Bean class used by this adapter.
     * The current implementation just returns the given bean's class.<p>
     *  
     * A future version may return a type other than the concrete
     * class of the given bean. This beanClass could be specified
     * in a new set of constructors. This is useful if the beans
     * are specified by public interfaces, and implemented by 
     * package private classes. In this case, the class of the given bean
     * object shall be checked against the specified type.
     * 
     * @param bean    the bean that may be used to lookup the class from
     * @return the Java Bean class used for this adapter.
     */
    private Class getBeanClass(Object bean) {
        return bean.getClass();
        // The future version shall add a check like
        // beanClass.isInstance(bean) if the beanClass
        // has been specified in the constructor.
    }
    

    /**
     * Returns the value of the specified property of the given bean, 
     * <code>null</code> if the bean is <code>null</code>.
     * 
     * @param bean                the bean to read the value from
     * @param propertyDescriptor  describes the property to be read
     * @return the bean's property value
     */
    private Object getValue0(Object bean, PropertyDescriptor propertyDescriptor) {
        return bean == null
            ? null
            : BeanUtils.getValue(bean, propertyDescriptor);
    }    
    
    
    /**
     * Sets the given object as new value of the specified property of the
     * given bean. Does nothing if the bean is null. This operation is 
     * not supported if the bean property is read-only.
     * 
     * @param bean         the bean that holds the adapted property
     * @param propertyName the name of the property to be set
     * @param newValue     the property value to be set
     * 
     * @throws NullPointerException        if the bean is null
     * @throws PropertyNotFoundException   if the property could not be found
     * @throws PropertyAccessException     if the write access failed
     * @throws PropertyVetoException       if the invoked bean setter
     *     throws a PropertyVetoException
     */
    private void setValue0(Object bean, String propertyName, Object newValue)
            throws PropertyVetoException {
        setValue0(bean, getPropertyDescriptor(bean, propertyName), newValue);
    }
    
    
    /**
     * Sets the given object as new value of the specified property of the
     * given bean. Does nothing if the bean is null. This write operation is 
     * supported only for writable bean properties.
     * 
     * @param bean                the bean that holds the adapted property
     * @param propertyDescriptor  describes the property to be set
     * @param newValue            the property value to be set
     * 
     * @throws NullPointerException        if the bean is null
     * @throws PropertyNotFoundException   if the property could not be found
     * @throws PropertyAccessException     if the write access failed
     * @throws PropertyVetoException       if the invoked bean setter
     *     throws a PropertyVetoException
     */
    private void setValue0(Object bean, PropertyDescriptor propertyDescriptor, Object newValue) 
            throws PropertyVetoException {
        BeanUtils.setValue(bean, propertyDescriptor, newValue);
    }


    /**
     * Looks up and returns a <code>PropertyDescriptor</code> 
     * for the given Java Bean and specified property name.
     * 
     * @param bean          the bean that holds the property
     * @param propertyName  the name of the property to be accessed
     * @return the <code>PropertyDescriptor</code> 
     * @throws PropertyNotFoundException   if the property could not be found
     */
    private PropertyDescriptor getPropertyDescriptor(Object bean, String propertyName) {
        return BeanUtils.getPropertyDescriptor(
                getBeanClass(bean),
                propertyName,
                null,
                null);
    }
    
    
    /**
     * Throws an IllegalArgumentException if the given ValueModel
     * is a ValueHolder that has the identityCheck feature disabled.
     */
    private void checkBeanChannelIdentityCheck(ValueModel valueModel) 
        throws IllegalArgumentException {
        if (!(valueModel instanceof ValueHolder))
            return;
        
        ValueHolder valueHolder = (ValueHolder) valueModel;
        if (!valueHolder.isIdentityCheckEnabled()) 
            throw new IllegalArgumentException(
                 "The bean channel must have the identity check enabled."); 
    }
    
    
    // Helper Classes *********************************************************
    
    /**
     * Listens to changes of the bean.
     */
    private class BeanChangeHandler implements PropertyChangeListener {

        /**
         * The bean has been changed. Uses the stored old bean instead of
         * the event's old value, because the latter can be null.
         * If the event's new value is null, the new bean is requested 
         * from the bean channel.
         * 
         * @param evt   the property change event to be handled
         */
        public void propertyChange(PropertyChangeEvent evt) {
            Object newBean = evt.getNewValue() != null
                    ? evt.getNewValue()
                    : getBean();
            setBean0(storedOldBean, newBean);
            storedOldBean = newBean;
        }
    }
    

    /**
     * Listens to changes of all bean properties. Fires property changes
     * if the associated property or an arbitrary set of properties has changed.
     */
    private class PropertyChangeHandler implements PropertyChangeListener {
        
        /**
         * A bean property has been changed. Sets the changed state to true.
         * Checks whether the observed or multiple properties have changed. 
         * If so, notifies all registered listeners about the change.
         * 
         * @param evt   the property change event to be handled
         */
        public void propertyChange(PropertyChangeEvent evt) {
            setChanged(true);
            String propertyName = evt.getPropertyName();
            if (propertyName == null) {
                forwardAllAdaptedValuesChanged();
            } else {
                SimplePropertyAdapter adapter = getPropertyAdapter(propertyName);
                if (adapter != null) {
                    adapter.fireValueChange(evt.getOldValue(), evt.getNewValue(), true);
                }
            }
        }
    }


    /**
     * Implements the access to the individual bean properties.
     * All SimplePropertyAdapters created by this BeanAdapter
     * share a single PropertyChangeListener that is used to
     * fire value changes in this SimplePropertyAdapter.<p>
     * 
     * This class is public to enable reflection access.
     */
    public final class SimplePropertyAdapter extends AbstractValueModel {
        
        /** 
         * Holds the name of the adapted property.
         */
        private final String propertyName;

        /** 
         * Holds the optional name of the property's getter. 
         * Used to create the PropertyDescriptor.
         * Also used to reject potential misuse of 
         * {@link BeanAdapter#getValueModel(String)} and
         * {@link BeanAdapter#getValueModel(String, String, String)}.
         * See the latter methods for details. 
         */
        final String getterName;

        /** 
         * Holds the optional name of the property's setter.        
         * Used to create the PropertyDescriptor.
         * Also used to reject potential misuse of 
         * {@link BeanAdapter#getValueModel(String)} and
         * {@link BeanAdapter#getValueModel(String, String, String)}.
         * See the latter methods for details. 
         */
        final String setterName;

        /**
         * Describes the property accessor; basically a getter and setter.
         */
        private PropertyDescriptor cachedPropertyDescriptor;
        
        /**
         * Holds the bean class associated with the cached property descriptor.
         */
        private Class cachedBeanClass;
        
        
        // Instance Creation --------------------------------------------------
        
        SimplePropertyAdapter(String propertyName, String getterName, String setterName) {
            this.propertyName = propertyName;
            this.getterName   = getterName;
            this.setterName   = setterName;
            
            // Eagerly check the existence of the property to adapt.
            Object bean = getBean();
            if (bean != null) {
                getPropertyDescriptor(bean);
            }
        }
        
        
        // Implementing ValueModel --------------------------------------------
        
        /**
         * Returns the value of the adapted bean property, or null
         * if the bean is null.
         * 
         * @return the value of the adapted bean property, 
         *    null if the bean is null
         */
        public Object getValue() {
            Object bean = getBean();
            return bean == null 
                ? null 
                : getValue0(bean, getPropertyDescriptor(bean));
        }

        
        /**
         * Sets the given object as new value of the adapted bean property.
         * Does nothing if the bean is <code>null</code>. If the bean setter
         * throws a PropertyVetoException, it is silently ignored. 
         * This write operation is supported only for writable bean properties.<p>
         * 
         * Notifies any registered value listener if the bean reports 
         * a property change. Note that a bean may suppress PropertyChangeEvents
         * if the old and new value are the same, or if the old and new value
         * are equal.
         * 
         * @param newValue   the value to set
         * 
         * @throws UnsupportedOperationException if the property is read-only
         * @throws PropertyNotFoundException     if the property could not be found
         * @throws PropertyAccessException       if the new value could not be set
         */
        public void setValue(Object newValue) {
            Object bean = getBean();
            if (bean == null)
                return;
            try {
                setValue0(bean, getPropertyDescriptor(bean), newValue);
            } catch (PropertyVetoException e) {
                // Silently ignore that someone vetoed against this change
            }
        }
        
        
        /**
         * Sets the given object as new value of the adapted bean property.
         * Does nothing if the bean is <code>null</code>. If the bean setter
         * throws a PropertyVetoExeption, this method throws the same exception.
         * This write operation is supported only for writable bean properties.<p>
         * 
         * Notifies any registered value listener if the bean reports 
         * a property change. Note that a bean may suppress PropertyChangeEvents
         * if the old and new value are the same, or if the old and new value
         * are equal.
         * 
         * @param newValue   the value to set
         * 
         * @throws UnsupportedOperationException if the property is read-only
         * @throws PropertyNotFoundException     if the property could not be found
         * @throws PropertyAccessException       if the new value could not be set
         * @throws PropertyVetoException         if the invoked bean setter 
         *     throws a PropertyVetoException 
         *     
         * @since 1.1
         */
        public void setVetoableValue(Object newValue) throws PropertyVetoException {
            Object bean = getBean();
            if (bean == null)
                return;
            setValue0(bean, getPropertyDescriptor(bean), newValue);
        }
        
        
        // Accessing the Cached Property Descriptor --------------------------
        
        /**
         * Looks up, lazily initializes and returns a <code>PropertyDescriptor</code> 
         * for the given Java Bean and name of the adapted property.<p>
         * 
         * The cached PropertyDescriptor is considered invalid if the
         * bean's class has changed. In this case we recompute the
         * PropertyDescriptor.<p>
         * 
         * If a getter name or setter name is available, these are used
         * to directly create a PropertyDescriptor. Otherwise, the standard
         * Java Bean introspection is used to determine the property descriptor. 
         * 
         * @param bean          the bean that holds the property
         * @return the <code>PropertyDescriptor</code> 
         * @throws PropertyNotFoundException   if the property could not be found
         */
        private PropertyDescriptor getPropertyDescriptor(Object bean) {
            Class beanClass = getBeanClass(bean);
            if    ((cachedPropertyDescriptor == null)
                || (beanClass != cachedBeanClass)) {
                
                cachedPropertyDescriptor = BeanUtils.getPropertyDescriptor(
                            beanClass,
                            propertyName,
                            getterName,
                            setterName);
                cachedBeanClass = beanClass;
            }
            return cachedPropertyDescriptor;
        }
        
        void fireChange(Object currentBean) {
            Object newValue;
            if (currentBean == null) {
                newValue = null;
            } else {
                PropertyDescriptor propertyDescriptor = getPropertyDescriptor(currentBean);
                boolean isWriteOnly = null == propertyDescriptor.getReadMethod();
                newValue = isWriteOnly ? null : getValue0(currentBean, propertyDescriptor);
            }
            fireValueChange(null, newValue);
        }
        
        void setBean0(Object oldBean, Object newBean) {
            Object oldValue;
            Object newValue;
            if (oldBean == null) {
                oldValue = null;
            } else {
                PropertyDescriptor propertyDescriptor = getPropertyDescriptor(oldBean);
                boolean isWriteOnly = null == propertyDescriptor.getReadMethod();
                oldValue = isWriteOnly ? null : getValue0(oldBean, propertyDescriptor);
            }
            if (newBean == null) {
                newValue = null;
            } else {
                PropertyDescriptor propertyDescriptor = getPropertyDescriptor(newBean);
                boolean isWriteOnly = null == propertyDescriptor.getReadMethod();
                newValue = isWriteOnly ? null : getValue0(newBean, propertyDescriptor);
            }
            if (oldValue != null || newValue != null) {
                fireValueChange(oldValue, newValue, true);
            }
        }
        
        
    }

    
}
