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

import java.beans.*;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;


/**
 * Keeps two bound Java Bean properties in synch. If one of the bean properties
 * changes, this connector will update the other to the same value. 
 * If a bean property is read-only, the PropertyConnector will not listen
 * to the other bean's property and so won't update the read-only property.
 * The properties must be single value bean properties as described by the 
 * <a href="http://java.sun.com/products/javabeans/docs/spec.html">Java 
 * Bean Secification</a>.<p>
 * 
 * <strong>Constraints:</strong> the properties must be type compatible, 
 * i. e. values returned by one reader must be accepted by the other's writer, 
 * and vice versa.<p>
 * 
 * <strong>Examples:</strong><pre>
 * // Connects a ValueModel and a JFormattedTextField
 * JFormattedTextField textField = new JFormattedTextField();
 * textField.setEditable(editable);
 * PropertyConnector connector = 
 *     new PropertyConnector(valueModel, "value", textField, "value");
 * connector.updateProperty2();
 *
 * // Connects the boolean property "selectable" with a component enablement
 * JComboBox comboBox = new JComboBox();
 * ...
 * new PropertyConnector(mainModel, "selectable", comboBox, "enabled");
 * </pre><p>
 * 
 * TODO: Consider adding an option to keep the two properties synchronized,
 * even if one of them rejects or changes the value set. Background:
 * The PropertyConnector has been designed to update a bean property 
 * if another one changes. In most situations this will synchronize 
 * both bean properties. However, it does not yet guarantee that both 
 * properties are kept synchronized; this is the case, if one property 
 * rejects values set, or changes the value set, for example to trim
 * trailing spaces, or to turn a string to uppercase.
 * 
 * @author  Karsten Lentzsch
 * @version $Revision: 1.4 $
 * 
 * @see     PropertyChangeEvent
 * @see     PropertyChangeListener
 * @see     PropertyDescriptor
 */
public final class PropertyConnector {
    
    /**
     * Holds the first bean that in turn holds the first property.
     * 
     * @see #getBean1()
     */
    private final Object bean1;  
    
    /**
     * Holds the second bean that in turn holds the second property.
     * 
     * @see #getBean2()
     */
    private final Object bean2;  
    
    /**
     * Holds the class used to lookup methods for bean1.
     * In a future version this may differ from bean1.getClass().
     */
    private final Class bean1Class;
    
    /**
     * Holds the class used to lookup methods for bean2.
     * In a future version this may differ from bean2.getClass().
     */
    private final Class bean2Class;
    
    /** 
     * Holds the first property name.
     * 
     * @see #getProperty1Name()            
     */
    private final String  property1Name;

    /** 
     * Holds the second property name.
     * 
     * @see #getProperty2Name()            
     */
    private final String  property2Name;

    /**
     * The <code>PropertyChangeListener</code> used to handle
     * changes in the first bean property.
     */
    private final PropertyChangeListener property1ChangeHandler;

    /**
     * The <code>PropertyChangeListener</code> used to handle
     * changes in the second bean property.
     */
    private final PropertyChangeListener property2ChangeHandler;

    /**
     * Describes the accessor for property1; basically a getter and setter.
     */
    private final PropertyDescriptor property1Descriptor;
    
    /**
     * Describes the accessor for property1; basically a getter and setter.
     */
    private final PropertyDescriptor property2Descriptor;
    
    /**
     * Describes if the property1 is read-only or not.
     * Used to check if property2 shall be observed,
     * i.e. if a listener shall be registered with property2.
     */
    private final boolean property1Writable;
    
    /**
     * Describes if the property2 is read-only or not.
     * Used to check if property1 shall be observed,
     * i.e. if a listener shall be registered with property2.
     */
    private final boolean property2Writable;
    
    
    // Instance creation ****************************************************

    /**
     * Constructs a PropertyConnector that synchronizes the two bound 
     * bean properties as specified by the given pairs of bean and associated 
     * property name.
     * If <code>Bean1#property1Name</code> changes it updates 
     * <code>Bean2#property2Name</code> and vice versa. 
     * If a bean property is read-only, this connector will not listen to 
     * the other bean's property and so won't update the read-only property.<p>
     * 
     * In case you don't need the PropertyConnector instance, you better use
     * the static method {@link #connect(Object, String, Object, String)}.
     * This constructor may confuse developers if you just use 
     * the side effects performed in the constructor; this is because it is
     * quite unconventional to instantiate an object that you never use.
     * 
     * @param bean1             the bean that owns the first property
     * @param property1Name     the name of the first property
     * @param bean2             the bean that owns the second property
     * @param property2Name     the name of the second property
     * @throws NullPointerException 
     *     if a bean or property name is <code>null</code>
     * @throws IllegalArgumentException if the beans are identical and
     *    the property name are equal
     */
    public PropertyConnector(Object bean1, String property1Name,
                             Object bean2, String property2Name) {
        if (bean1 == null)
            throw new NullPointerException("Bean1 must not be null.");
        if (bean2 == null)
            throw new NullPointerException("Bean2 must not be null.");
        if (property1Name == null)
            throw new NullPointerException("PropertyName1 must not be null.");
        if (property2Name == null)
            throw new NullPointerException("PropertyName2 must not be null.");
        if ((bean1 == bean2) && (property1Name.equals(property2Name)))
            throw new IllegalArgumentException(
                    "Cannot connect a bean property to itself on the same bean.");
        
        this.bean1 = bean1;
        this.bean2 = bean2;
        this.bean1Class = bean1.getClass();
        this.bean2Class = bean2.getClass();
        this.property1Name = property1Name;
        this.property2Name = property2Name;
        
        property1Descriptor = getPropertyDescriptor(bean1Class, property1Name);
        property2Descriptor = getPropertyDescriptor(bean2Class, property2Name);
        
        property1Writable = property1Descriptor.getWriteMethod() != null;
        property2Writable = property2Descriptor.getWriteMethod() != null;
        // Reject to connect to read-only properties
        if (!property1Writable && !property2Writable)
            throw new IllegalArgumentException(
                    "Cannot connect two read-only properties.");
        
        property1ChangeHandler = new PropertyChangeHandler(
                bean1, property1Descriptor, bean2, property2Descriptor);
        property2ChangeHandler = new PropertyChangeHandler(
                bean2, property2Descriptor, bean1, property1Descriptor);
        
        // Observe property1 only if propert2 is writable, in other words
        // ignore changes in property1 if property2 is read-only.
        if (property2Writable) {
            addPropertyChangeHandler(bean1, bean1Class, property1ChangeHandler);
        }
        
        // Observe property2 only if property1 is writable, in other words
        // ignore changes in property2 if property1 is read-only.
        if (property1Writable) {
            addPropertyChangeHandler(bean2, bean2Class, property2ChangeHandler);
        }
    }
    
    
    /**
     * Synchronizes the two bound bean properties as specified 
     * by the given pairs of bean and associated property name.
     * If <code>Bean1#property1Name</code> changes it updates 
     * <code>Bean2#property2Name</code> and vice versa. 
     * If a bean property is read-only, this connector will not listen
     * to the other bean's property and so won't update the read-only property.
     * 
     * @param bean1             the bean that owns the first property
     * @param property1Name     the name of the first property
     * @param bean2             the bean that owns the second property
     * @param property2Name     the name of the second property
     * @throws NullPointerException 
     *     if a bean or property name is <code>null</code>
     * @throws IllegalArgumentException if the beans are identical and
     *    the property name are equal
     */
    public static void connect(Object bean1, String property1Name,
                               Object bean2, String property2Name) {
        new PropertyConnector(bean1, property1Name,
                              bean2, property2Name);
    }
    

    // Property Accessors *****************************************************
    
    /**
     * Returns the Java Bean that holds the first property.
     * 
     * @return the Bean that holds the first property
     */
    public Object getBean1() {
        return bean1;
    }
    
    /**
     * Returns the Java Bean that holds the first property.
     * 
     * @return the Bean that holds the first property
     */
    public Object getBean2() {
        return bean2;
    }
    
    /**
     * Returns the name of the first Java Bean property.
     * 
     * @return the name of the first property
     */
    public String getProperty1Name() {
        return property1Name;
    }
    
    /**
     * Returns the name of the second Java Bean property.
     * 
     * @return the name of the second property
     */
    public String getProperty2Name() {
        return property2Name;
    }
    

    // Sychronization *********************************************************
    
    /**
     * Reads the value of the second bean property and sets it as new
     * value of the first bean property.
     * 
     * @see #updateProperty2()
     */
    public void updateProperty1() {
        Object property2Value = BeanUtils.getValue(bean2, property2Descriptor);
        setValueSilently(bean1, property1Descriptor, property2Value);
    }

    /**
     * Reads the value of the first bean property and sets it as new
     * value of the second bean property.
     * 
     * @see #updateProperty1()
     */
    public void updateProperty2() {
        Object property1Value = BeanUtils.getValue(bean1, property1Descriptor);
        setValueSilently(bean2, property2Descriptor, property1Value);
    }


    // Release ****************************************************************

    /**
     * Removes the PropertyChangeHandler from the observed bean, 
     * if the bean is not null and if property changes are not observed.<p>
     * 
     * To avoid memory leaks it is recommended to invoke this method 
     * if the connected beans live much longer than this connector.
     * Once <code>#release</code> has been invoked this instance
     * must not be used.<p>
     * 
     * As an alternative you may use event listener lists in the connected
     * beans that are implemented using <code>WeakReference</code>.
     * 
     * @see java.lang.ref.WeakReference
     */
    public void release() {
        removePropertyChangeHandler(bean1, bean1Class, property1ChangeHandler);
        removePropertyChangeHandler(bean2, bean2Class, property2ChangeHandler);
    }
    
    
    /**
     * Used to add this class' PropertyChangeHandler to the given bean 
     * if it is not <code>null</code>. First checks if the bean class 
     * supports <em>bound properties</em>, i.e. it provides a pair of methods 
     * to register multicast property change event listeners; 
     * see section 7.4.1 of the Java Beans specification for details.
     * 
     * @param bean      the bean to add a property change listener
     * @param listener  the property change listener to be added
     * @throws NullPointerException 
     *     if the listener is <code>null</code>
     * @throws PropertyUnboundException  
     *     if the bean does not support bound properties
     * @throws PropertyNotBindableException  
     *     if the property change handler cannot be added successfully 
     */
    private static void addPropertyChangeHandler(Object bean, Class beanClass, PropertyChangeListener listener) {
        if (bean != null) {
            BeanUtils.addPropertyChangeListener(bean, beanClass, listener);
        }
    }


    /**
     * Used to remove this class' PropertyChangeHandler from the given bean
     * if it is not <code>null</code>.
     * 
     * @param bean      the bean to remove the property change listener from
     * @param listener  the property change listener to be removed
     * @throws PropertyUnboundException  
     *     if the bean does not support bound properties
     * @throws PropertyNotBindableException  
     *     if the property change handler cannot be removed successfully 
     */
    private static void removePropertyChangeHandler(Object bean, Class beanClass, PropertyChangeListener listener) {
        if (bean != null) {
            BeanUtils.removePropertyChangeListener(bean, beanClass, listener);
        }
    }


    // Helper Methods to Get and Set a Property Value *************************
    
    private void setValueSilently(Object bean, PropertyDescriptor propertyDescriptor, Object newValue) {
        if (property2Writable) {
            removePropertyChangeHandler(bean1, bean1Class, property1ChangeHandler);
        }
        if (property1Writable) {
            removePropertyChangeHandler(bean2, bean2Class, property2ChangeHandler);
        }
        try {
            BeanUtils.setValue(bean, propertyDescriptor, newValue);
        } catch (PropertyVetoException e) {
            // Silently ignore this situation.
        }
        if (property2Writable) {
            addPropertyChangeHandler(bean1, bean1Class, property1ChangeHandler);
        }
        if (property1Writable) {
            addPropertyChangeHandler(bean2, bean2Class, property2ChangeHandler);
        }
    }


    /**
     * Looks up, lazily initializes and returns a <code>PropertyDescriptor</code> 
     * for the given Java Bean and property name.
     * 
     * @param beanClass     the Java Bean class used to lookup the property from
     * @param propertyName  the name of the property
     * @return the descriptor for the given bean and property name 
     * @throws PropertyNotFoundException   if the property could not be found
     */
    private static PropertyDescriptor getPropertyDescriptor(
        Class beanClass, 
        String propertyName) {
        try {
            return BeanUtils.getPropertyDescriptor(beanClass, propertyName);
        } catch (IntrospectionException e) {
            throw new PropertyNotFoundException(propertyName, beanClass, e);
        }
    }
    
    /**
     * Listens to changes of a bean property and updates the property.
     */
    private final class PropertyChangeHandler implements PropertyChangeListener {
        
        /**
         * Holds the bean that sends updates.
         */
        private final Object sourceBean;

        /**
         * Holds the property descriptor for the bean to read from.
         */
        private final PropertyDescriptor sourcePropertyDescriptor;
        
        /**
         * Holds the bean to update.
         */
        private final Object targetBean;
        
        /**
         * Holds the property descriptor for the bean to update.
         */
        private final PropertyDescriptor targetPropertyDescriptor;
        
        
        private PropertyChangeHandler(
                Object sourceBean,
                PropertyDescriptor sourcePropertyDescriptor,
                Object targetBean, 
                PropertyDescriptor targetPropertyDescriptor) {
            this.sourceBean = sourceBean;
            this.sourcePropertyDescriptor = sourcePropertyDescriptor;
            this.targetBean = targetBean;
            this.targetPropertyDescriptor = targetPropertyDescriptor;
        }
        
        /**
         * A property in the observed bean has changed. First checks,
         * if this listener should handle the event, because the event's
         * property name is the one to be observed or the event indicates
         * that any property may have changed. In case the event provides 
         * no new value, it is read from the source bean.
         * 
         * @param evt   the property change event to be handled
         */
        public void propertyChange(PropertyChangeEvent evt) {
            String sourcePropertyName = sourcePropertyDescriptor.getName();
            if    ((evt.getPropertyName() == null) 
                || (evt.getPropertyName().equals(sourcePropertyName))) {
                Object newValue = evt.getNewValue();
                if (newValue == null) {
                    newValue = BeanUtils.getValue(sourceBean, sourcePropertyDescriptor);
                }
                setValueSilently(targetBean, targetPropertyDescriptor, newValue);
            }
        }
        
    }
    
}
