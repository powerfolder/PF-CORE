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

import com.jgoodies.binding.value.AbstractValueModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

 
/**
 * Converts a single Java Bean property into the generic ValueModel interface. 
 * The bean property must be a single value as described by the 
 * <a href="http://java.sun.com/products/javabeans/docs/spec.html">Java 
 * Bean Specification</a>. See below for a comparison with the more frequently 
 * used BeanAdapter and PresentationModel classes.<p>
 * 
 * The constructors accept either a property name or a triple of
 * (property name, getter name, setter name). If you just specify the
 * property name, the adapter uses the standard Java Bean introspection
 * to lookup the available properties and how to read and write the 
 * property value. In case of custom readers and writers you may
 * specify a custom BeanInfo class, or as a shortcut use the constructors
 * that accept the optional getter and setter name. If these are specified,
 * introspection will be bypassed and a PropertyDescriptor will be
 * created for the given property name, getter and setter name.<p>
 * 
 * Optionally the PropertyAdapter can observe changes in <em>bound 
 * properties</em> as described in section 7.4 of the Bean specification.
 * You can enable this feature by setting the constructor parameter 
 * <code>observeChanges</code> to <code>true</code>.
 * If the adapter observes changes, it will fire value change events,
 * i.e. PropertyChangeEvents for the property <code>&quot;value&quot;</code>.
 * Even if you ignore property changes, you can access the adapted
 * property value via <code>#getValue()</code>. 
 * It's just that you won't be notified about changes.<p>
 * 
 * The PropertyAdapter provides two access styles to the target bean 
 * that holds the adapted property: you can specify a bean directly, 
 * or you can use a <em>bean channel</em> to access the bean indirectly. 
 * In the latter case you specify a <code>ValueModel</code>
 * that holds the bean that in turn holds the adapted property.<p>
 * 
 * If the adapted bean is <code>null</code> the PropertyAdapter can 
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
 * PropertyAdapters that observe changes have a PropertyChangeListener 
 * registered with the target bean. Hence, a bean has a reference 
 * to any PropertyAdapter that observes it. To avoid memory leaks 
 * it is recommended to remove this listener if the bean lives much longer than 
 * the PropertyAdapter, enabling the garbage collector to remove the adapter.
 * To do so, you can call <code>setBean(null)</code> or set the
 * bean channel's value to null.
 * As an alternative you can use event listener lists in your beans
 * that implement references with <code>WeakReference</code>.<p>
 * 
 * Setting the bean to null has side-effects, for example the adapter fires 
 * a change event for the bound property <em>bean</em> and other properties.
 * And the adpter's value may change. 
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
 * If you adapt multiple properties of the same bean, you better use 
 * a {@link com.jgoodies.binding.beans.BeanAdapter}. The BeanAdapter
 * registers only a single PropertyChangeListener with the bean, 
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
 * PropertyAdapter adapter = new PropertyAdapter(address, "street");
 * adapter.setValue("Broadway");
 * System.out.println(address.getStreet());    // Prints "Broadway"
 * address.setStreet("Franz-Josef-Strasse");
 * System.out.println(adapter.getValue());     // Prints "Franz-Josef-Strasse"
 * 
 * 
 * //Direct access, observes changes
 * PropertyAdapter adapter = new PropertyAdapter(address, "street", true);
 * 
 * 
 * // Indirect access, ignores changes
 * ValueHolder addressHolder = new ValueHolder(address1);
 * PropertyAdapter adapter = new PropertyAdapter(addressHolder, "street");
 * adapter.setValue("Broadway");               // Sets the street in address1
 * System.out.println(address1.getValue());    // Prints "Broadway"
 * adapter.setBean(address2);
 * adapter.setValue("Robert-Koch-Strasse");    // Sets the street in address2
 * System.out.println(address2.getValue());    // Prints "Robert-Koch-Strasse"
 *
 *  
 * // Indirect access, observes changes
 * ValueHolder addressHolder = new ValueHolder();
 * PropertyAdapter adapter = new PropertyAdapter(addressHolder, "street", true);
 * addressHolder.setValue(address1);
 * address1.setStreet("Broadway");
 * System.out.println(adapter.getValue());     // Prints "Broadway"
 * </pre>
 * 
 * <strong>Adapter Chain Example:</strong>
 * <br>Builds an adapter chain from a domain model to the presentation layer.
 * <pre>
 * Country country = new Country();
 * country.setName("Germany");
 * country.setEuMember(true);
 * 
 * JTextField nameField = new JTextField();
 * nameField.setDocument(new DocumentAdapter(
 *      new PropertyAdapter(country, "name", true)));
 * 
 * JCheckBox euMemberBox = new JCheckBox("Is EU Member");
 * euMemberBox.setModel(new ToggleButtonAdapter(
 *      new PropertyAdapter(country, "euMember", true)));
 * 
 * // Using factory methods
 * JTextField nameField   = Factory.createTextField(country, "name");
 * JCheckBox  euMemberBox = Factory.createCheckBox (country, "euMember");
 * euMemberBox.setText("Is EU Member");
 * </pre><p>
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
 * @version $Revision: 1.6 $
 * 
 * @see     com.jgoodies.binding.beans.BeanAdapter
 * @see     ValueModel
 * @see     ValueModel#getValue()
 * @see     ValueModel#setValue(Object)
 * @see     PropertyChangeEvent
 * @see     PropertyChangeListener
 * @see     java.beans.Introspector
 * @see     java.beans.BeanInfo
 * @see     PropertyDescriptor
 */
public final class PropertyAdapter extends AbstractValueModel {
    
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

    
    // Fields *****************************************************************
    
    /**
     * Holds a <code>ValueModel</code> that holds the bean, that in turn
     * holds the adapted property.
     * 
     * @see #getBean()
     * @see #setBean(Object)
     */
    private final ValueModel beanChannel;  
    
    /** 
     * Holds the name of the adapted property.
     * 
     * @see #getPropertyName()            
     */
    private final String  propertyName;

    /** 
     * Holds the optional name of the property's getter.            
     */
    private final String  getterName;

    /** 
     * Holds the optional name of the property's setter.        
     */
    private final String  setterName;

    /**
     * Specifies whether we observe property changes and in turn
     * fire state changes.
     * 
     * @see #getObserveChanges()
     */
    private final boolean observeChanges;
    
    /**
     * Refers to the old bean. Used as old value if the bean changes.
     * Updated after a bean change in the BeanChangeHandler.
     */
    private Object storedOldBean;

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
     * in the adapted bean property. A new instance is created everytime
     * the target bean changes.
     */
    private PropertyChangeListener propertyChangeHandler;

    /**
     * Describes the property accessor; basically a getter and setter.
     */
    private PropertyDescriptor cachedPropertyDescriptor;
    
    /**
     * Holds the bean class associated with the cached property descriptor.
     */
    private Class cachedBeanClass;


    // Instance creation ****************************************************

    /**
     * Constructs a <code>PropertyAdapter</code> for the given
     * bean and property name; does not observe changes.
     * 
     * @param bean             the bean that owns the property
     * @param propertyName     the name of the adapted property
     * @throws NullPointerException if <code>propertyName</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>propertyName</code> is empty
     */
    public PropertyAdapter(Object bean, String propertyName) {
        this(bean, propertyName, false);
    }
    

    /**
     * Constructs a <code>PropertyAdapter</code> for the given
     * bean and property name; observes changes if specified.
     * 
     * @param bean             the bean that owns the property
     * @param propertyName     the name of the adapted property
     * @param observeChanges   <code>true</code> to observe changes of bound 
     *     or constrained properties, <code>false</code> to ignore changes
     * @throws NullPointerException if <code>propertyName</code> is <code>null</code>
     * @throws IllegalArgumentException  if <code>propertyName</code> is empty
     * @throws PropertyUnboundException  if <code>observeChanges</code> 
     *     is true but the property is unbound, i. e. the <code>bean</code> 
     *     does not provide a pair of methods to register a multicast 
     *     PropertyChangeListener
     */
    public PropertyAdapter(
        Object bean,
        String propertyName,
        boolean observeChanges) {
        this(bean, propertyName, null, null, observeChanges);
    }
    

    /**
     * Constructs a <code>PropertyAdapter</code> for the given bean, 
     * property name, getter and setter name; does not observe changes.
     * 
     * @param bean             the bean that owns the property
     * @param propertyName     the name of the adapted property
     * @param getterName       the optional name of the property reader
     * @param setterName       the optional name of the property writer
     * @throws NullPointerException if <code>propertyName</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>propertyName</code> is empty
     */
    public PropertyAdapter(Object bean, String propertyName, String getterName, String setterName) {
        this(bean, propertyName, getterName, setterName, false);
    }
    

    /**
     * Constructs a <code>PropertyAdapter</code> for the given bean,
     * property name, getter and setter name; observes changes if specified.
     * 
     * @param bean             the bean that owns the property
     * @param propertyName     the name of the adapted property
     * @param getterName       the optional name of the property reader
     * @param setterName       the optional name of the property writer
     * @param observeChanges   <code>true</code> to observe changes of bound 
     *     or constrained properties, <code>false</code> to ignore changes
     * @throws NullPointerException if <code>propertyName</code> is <code>null</code>
     * @throws IllegalArgumentException  if <code>propertyName</code> is empty
     * @throws PropertyUnboundException  if <code>observeChanges</code> 
     *     is true but the property is unbound, i. e. the <code>bean</code> 
     *     does not provide a pair of methods to register a multicast 
     *     PropertyChangeListener
     */
    public PropertyAdapter(
        Object bean,
        String propertyName,
        String getterName,
        String setterName,
        boolean observeChanges) {
        this(new ValueHolder(bean, true), propertyName, getterName, setterName, observeChanges);
    }
    

    /**
     * Constructs a <code>PropertyAdapter</code> for the given
     * bean channel and property name; does not observe changes.
     * 
     * @param beanChannel    the <code>ValueModel</code> that holds the bean
     * @param propertyName   the name of the adapted property
     * @throws NullPointerException if <code>beanChannel</code> or
     *     <code>propertyName</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>propertyName</code> is empty
     */
    public PropertyAdapter(ValueModel beanChannel, String propertyName) {
        this(beanChannel, propertyName, false);
    }


    /**
     * Constructs a <code>PropertyAdapter</code> for the given
     * bean channel and property name; observes changes if specified.
     * 
     * @param beanChannel     the <code>ValueModel</code> that holds the bean
     * @param propertyName    the name of the adapted property
     * @param observeChanges  <code>true</code> to observe changes of bound 
     *  or constrained properties, <code>false</code> to ignore changes
     * @throws NullPointerException if <code>beanChannel</code> or 
     *     <code>propertyName</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>propertyName</code> is empty
     * @throws PropertyUnboundException    if <code>observeChanges</code> 
     *     is true but the property is unbound, i. e. the <code>bean</code> 
     *     does not provide a pair of methods to register a multicast 
     *     PropertyChangeListener
     * @throws PropertyAccessException if the <code>beanChannel</code>'s value 
     *     does not provide a property descriptor for <code>propertyName</code>
     */
    public PropertyAdapter(
        ValueModel beanChannel,
        String propertyName,
        boolean observeChanges) {
        this(beanChannel, propertyName, null, null, observeChanges);
    }
    

    /**
     * Constructs a <code>PropertyAdapter</code> for the given bean channel,
     * property name, getter and setter name; does not observe changes.
     * 
     * @param beanChannel    the <code>ValueModel</code> that holds the bean
     * @param propertyName   the name of the adapted property
     * @param getterName       the optional name of the property reader
     * @param setterName       the optional name of the property writer
     * @throws NullPointerException if <code>beanChannel</code> or 
     *     <code>propertyName</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>propertyName</code> is empty
     */
    public PropertyAdapter(ValueModel beanChannel, String propertyName, String getterName, String setterName) {
        this(beanChannel, propertyName, getterName, setterName, false);
    }


    /**
     * Constructs a <code>PropertyAdapter</code> for the given bean channel,
     * property name, getter and setter name; observes changes if specified.
     * 
     * @param beanChannel     the <code>ValueModel</code> that holds the bean
     * @param propertyName    the name of the adapted property
     * @param getterName       the optional name of the property reader
     * @param setterName       the optional name of the property writer
     * @param observeChanges  <code>true</code> to observe changes of bound 
     *  or constrained properties, <code>false</code> to ignore changes
     *  
     * @throws NullPointerException if <code>propertyName</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>propertyName</code> is empty
     * @throws IllegalArgumentException if the bean channel is a ValueHolder
     *     that has the identityCheck feature disabled
     * @throws PropertyUnboundException    if <code>observeChanges</code> 
     *     is true but the property is unbound, i. e. the <code>bean</code> 
     *     does not provide a pair of methods to register a multicast 
     *     PropertyChangeListener
     * @throws PropertyAccessException if the <code>beanChannel</code>'s value 
     *     does not provide a property descriptor for <code>propertyName</code>
     */
    public PropertyAdapter(
        ValueModel beanChannel,
        String propertyName,
        String getterName,
        String setterName,
        boolean observeChanges) {
        this.beanChannel    = beanChannel != null 
            ? beanChannel 
            : new ValueHolder(null, true);
        this.propertyName   = propertyName;
        this.getterName     = getterName;
        this.setterName     = setterName;
        this.observeChanges = observeChanges;

        if (propertyName == null)
            throw new NullPointerException("The property name must not be null.");
        if (propertyName.length() == 0)
            throw new IllegalArgumentException("The property name must not be empty.");
        checkBeanChannelIdentityCheck(beanChannel);
        
        this.beanChannel.addValueChangeListener(new BeanChangeHandler());

        Object initialBean = getBean();
        // Eagerly check the existence of the property to adapt.
        if (initialBean != null) {
            getPropertyDescriptor(initialBean);
            addChangeHandlerTo(initialBean);
        }
        storedOldBean = initialBean;
    }
    

    // Accessors ************************************************************

    /**
     * Returns the Java Bean that holds the adapted property.
     * 
     * @return the Bean that holds the adapted property
     * 
     * @see #setBean(Object)
     */
    public Object getBean() {
        return beanChannel.getValue();
    }
    
    
    /**
     * Sets a new Java Bean as holder of the adapted property.
     * Notifies any registered value listeners if the value has changed. 
     * Also notifies listeners that have been registered with this adapter
     * to observe the bound property <em>bean</em>.  
     * 
     * @param newBean  the new holder of the property
     * 
     * @see #getBean()
     */
    public void setBean(Object newBean) {
        beanChannel.setValue(newBean);            
    }
    
    
    /**
     * Returns the name of the adapted Java Bean property.
     * 
     * @return the name of the adapted property
     */
    public String getPropertyName() {
        return propertyName;
    }
    
    
    /**
     * Answers whether this adapter observes changes in the
     * adapted Bean property.
     * 
     * @return true if this adapter observes changes, false if not
     */
    public boolean getObserveChanges() {
        return observeChanges;
    }
    
    /*
     * Sets whether changes in the adapted Bean property shall be observed.
     * As a requirement the property must be bound.
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

    // ValueModel Implementation ********************************************

    /**
     * Returns the value of the bean's adapted property, <code>null</code> 
     * if the current bean is <code>null</code>.<p>
     * 
     * If the adapted bean property is write-only, this adapter is write-only 
     * too, and this operation is not supported and throws an exception.
     * 
     * @return the value of the adapted bean property, null if the bean is null
     * @throws UnsupportedOperationException  if the property is write-only
     * @throws PropertyNotFoundException      if the property could not be found
     * @throws PropertyAccessException        if the value could not be read
     */
    public Object getValue() {
        Object bean = getBean();
        if (bean == null) {
            return null;
        }
        return getValue0(bean);
    }

    
    /**
     * Sets the given object as new value of the adapted bean property.
     * Does nothing if the bean is <code>null</code>. If the bean setter
     * throws a PropertyVetoException, it is silently ignored. 
     * This write operation is supported only for writable bean properties.<p>
     * 
     * Notifies any registered value listeners if the bean reports 
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
            setValue0(bean, newValue);
        } catch (PropertyVetoException e) {
            // Silently ignore this situation.
        }
    }
    

    /**
     * Sets the given object as new value of the adapted bean property.
     * Does nothing if the bean is <code>null</code>. If the bean setter
     * throws a PropertyVetoExeption, this method throws the same exception.
     * This write operation is supported only for writable bean properties.<p>
     * 
     * Notifies any registered value listeners if the bean reports 
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
        setValue0(getBean(), newValue);
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

    
    // Releasing PropertyChangeListeners **************************************
    
    /**
     * Removes the PropertyChangeHandler from the observed bean, if the bean 
     * is not <code>null</code> and if property changes are observed.<p>
     * 
     * PropertyAdapters that observe changes have a PropertyChangeListener 
     * registered with the target bean. Hence, a bean has a reference 
     * to any PropertyAdapter that observes it. To avoid memory leaks 
     * it is recommended to remove this listener if the bean lives much longer than 
     * the PropertyAdapter, enabling the garbage collector to remove the adapter.
     * To do so, you can call <code>setBean(null)</code> or set the
     * bean channel's value to null.
     * As an alternative you can use event listener lists in your beans
     * that implement references with <code>WeakReference</code>.<p>
     * 
     * Setting the bean to null has side-effects, for example 
     * this adapter fires a change event for the bound property <em>bean</em> 
     * and other properties. And this adpter's value may change. 
     * However, typically this is fine and setting the bean to null is 
     * the first choice for removing the reference from the bean to the adapter. 
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
        forwardAdaptedValueChanged(oldBean, newBean);
        resetChanged();
        addChangeHandlerTo(newBean);
        firePropertyChange(PROPERTYNAME_BEAN, oldBean, newBean, true);
        firePropertyChange(PROPERTYNAME_AFTER_BEAN, oldBean, newBean, true);
    }
    
    private void forwardAdaptedValueChanged(Object oldBean, Object newBean) {
        Object oldValue = (oldBean == null) || isWriteOnlyProperty(oldBean)
            ? null
            : getValue0(oldBean);
        Object newValue = (newBean == null) || isWriteOnlyProperty(newBean)
            ? null
            : getValue0(newBean);
        if (oldValue != null || newValue != null) {
            fireValueChange(oldValue, newValue, true);
        }
    }

    private void forwardAdaptedValueChanged(Object newBean) {
        Object newValue = (newBean == null) || isWriteOnlyProperty(newBean)
            ? null
            : getValue0(newBean);
        fireValueChange(null, newValue);
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
     * Returns the current value of the bean's property, <code>null</code> 
     * if the current bean is <code>null</code>.
     * 
     * @param bean   the bean to read the value from
     * @return the bean's property value
     */
    private Object getValue0(Object bean) {
        return bean == null
            ? null
            : BeanUtils.getValue(bean, getPropertyDescriptor(bean));
    }

    /**
     * Sets the given object as new value of the adapted bean property.
     * Does nothing if the bean is <code>null</code>. This operation 
     * is unsupported if the bean property is read-only.<p>
     * 
     * The operation is delegated to the <code>BeanUtils</code> class.
     * 
     * @param bean      the bean that holds the adapted property
     * @param newValue  the property value to be set
     * 
     * @throws NullPointerException   if the bean is null
     * @throws PropertyVetoException  if the invoked bean setter 
     *     throws a PropertyVetoException 
     */
    private void setValue0(Object bean, Object newValue) throws PropertyVetoException {
        BeanUtils.setValue(bean, getPropertyDescriptor(bean), newValue);
    }


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
                        getPropertyName(),
                        getterName,
                        setterName);
            cachedBeanClass = beanClass;
        }
        return cachedPropertyDescriptor;
    }
    
    /**
     * Answers whether the adapted property has a setter but no getter.
     * In this case the PropertyAdapter doesn't check for the old value
     * when you set a new bean or a new value.
     * 
     * @param bean   the bean to test for the write only state
     * @return true if the property has a setter but no getter, false otherwise
     */
    private boolean isWriteOnlyProperty(Object bean) {
        return null == getPropertyDescriptor(bean).getReadMethod();
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
         * Checks whether the observed
         * property or multiple properties have changed. 
         * If so, notifies all registered listeners about the change.
         * 
         * @param evt   the property change event to be handled
         */
        public void propertyChange(PropertyChangeEvent evt) {
            setChanged(true);
            if (evt.getPropertyName() == null) {
                forwardAdaptedValueChanged(getBean());
            } else if (evt.getPropertyName().equals(getPropertyName())) {
                fireValueChange(evt.getOldValue(), evt.getNewValue(), true);
            }
        }
    }

    
}
