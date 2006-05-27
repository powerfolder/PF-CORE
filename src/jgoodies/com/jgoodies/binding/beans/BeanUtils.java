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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Consists exclusively of static methods that provide
 * convenience behavior for working with Java Bean properties.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.6 $
 * 
 * @see     Introspector
 * @see     BeanInfo
 * @see     PropertyDescriptor
 */
public final class BeanUtils {
    
    
    private BeanUtils() {
        // Override default constructor; prevents instantiation.
    }
    
    
    /**
     * Checks and answers whether the given class supports bound properties, 
     * i.e. it provides a pair of multicast event listener registration methods 
     * for <code>PropertyChangeListener</code>s:
     * <pre>
     * public void addPropertyChangeListener(PropertyChangeListener x);
     * public void removePropertyChangeListener(PropertyChangeListener x);
     * </pre> 
     * 
     * @param clazz    the class to test
     * @return true if the class supports bound properties, false otherwise
     */
    public static boolean supportsBoundProperties(Class clazz) {
        return  (getPCLAdder(clazz)   != null) 
             && (getPCLRemover(clazz) != null);
    }
    
    
    /**
     * Looks up and returns a <code>PropertyDescriptor</code> for the
     * given Java Bean class and property name using the standard 
     * Java Bean introspection behavior.
     * 
     * @param beanClass     the type of the bean that holds the property
     * @param propertyName  the name of the Bean property
     * @return the <code>PropertyDescriptor</code> associated with the given
     *     bean and property name as returned by the Bean introspection
     *     
     * @throws IntrospectionException if an exception occurs during
     *     introspection.
     * @throws NullPointerException if the beanClass or propertyName is <code>null</code>
     * 
     * @since 1.1.1
     */
    public static PropertyDescriptor getPropertyDescriptor(
        Class beanClass,
        String propertyName)
        throws IntrospectionException {

        BeanInfo info = Introspector.getBeanInfo(beanClass);
        PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
        for (int i = 0; i < descriptors.length; i++) {
            if (propertyName.equals(descriptors[i].getName()))
                return descriptors[i];
        }
        throw new IntrospectionException(
            "Property '" + propertyName + "' not found in bean " + beanClass);
    }

    
    /**
     * Looks up and returns a <code>PropertyDescriptor</code> for the given
     * Java Bean class and property name. If a getter name or setter name 
     * is available, these are used to create a PropertyDescriptor. 
     * Otherwise, the standard Java Bean introspection is used to determine 
     * the property descriptor. 
     * 
     * @param beanClass     the class of the bean that holds the property
     * @param propertyName  the name of the property to be accessed
     * @param getterName    the optional name of the property's getter
     * @param setterName    the optional name of the property's setter
     * @return the <code>PropertyDescriptor</code> associated with the
     *     given bean and property name
     *     
     * @throws PropertyNotFoundException   if the property could not be found
     * 
     * @since 1.1.1
     */
    public static PropertyDescriptor getPropertyDescriptor(
       Class beanClass, String propertyName, String getterName, String setterName) {
        try {
            return
                (getterName != null || setterName != null)
                    ? new PropertyDescriptor(
                        propertyName,
                        beanClass,
                        getterName,
                        setterName)
                    : getPropertyDescriptor(
                        beanClass,
                        propertyName);
        } catch (IntrospectionException e) {
            throw new PropertyNotFoundException(propertyName, beanClass, e);
        }
    }   
    
    
    /**
     * Looks up and returns a <code>PropertyDescriptor</code> for the
     * given Java Bean and property name using the standard Java Bean
     * introspection behavior.
     * 
     * @param bean          the bean that holds the property
     * @param propertyName  the name of the Bean property
     * @return the <code>PropertyDescriptor</code> associated with the given
     *     bean and property name as returned by the Bean introspection
     *     
     * @throws IntrospectionException if an exception occurs during
     *     introspection.
     * @throws NullPointerException if the bean or propertyName is <code>null</code>
     * 
     * @deprecated Replaced by {@link #getPropertyDescriptor(Class, String)}.
     */
    public static PropertyDescriptor getPropertyDescriptor(
        Object bean,
        String propertyName)
        throws IntrospectionException {
        return getPropertyDescriptor(bean.getClass(), propertyName);
    }

    
    /**
     * Looks up and returns a <code>PropertyDescriptor</code> for the given 
     * bean and property name. If a getter name or setter name is available, 
     * these are used to create a PropertyDescriptor. Otherwise, the standard 
     * Java Bean introspection is used to determine the property descriptor. 
     * 
     * @param bean          the bean that holds the property
     * @param propertyName  the name of the property to be accessed
     * @param getterName    the optional name of the property's getter
     * @param setterName    the optional name of the property's setter
     * @return the <code>PropertyDescriptor</code> associated with the
     *     given bean and property name
     *     
     * @throws PropertyNotFoundException   if the property could not be found
     * 
     * @deprecated Replaced by {@link #getPropertyDescriptor(Class, String, String, String)}.
     */
    public static PropertyDescriptor getPropertyDescriptor(
       Object bean, String propertyName, String getterName, String setterName) {
        return getPropertyDescriptor(bean.getClass(), propertyName, getterName, setterName);
    }   
    
    
    /**
     * Holds the class parameter list that is used to lookup
     * the adder and remover methods for PropertyChangeListeners.
     */
    private static final Class[] PCL_PARAMS =
        new Class[] {PropertyChangeListener.class}; 
    
    
    /**
     * Holds the class parameter list that is used to lookup
     * the adder and remover methods for PropertyChangeListeners.
     */
    private static final Class[] NAMED_PCL_PARAMS =
        new Class[] {String.class, PropertyChangeListener.class}; 
    
    
    /**
     * Looks up and returns the method that adds a multicast 
     * PropertyChangeListener to instances of the given class.
     * 
     * @param clazz   the class that provides the adder method
     * @return the method that adds multicast PropertyChangeListeners
     */
    public static Method getPCLAdder(Class clazz) {
        try {
            return clazz.getMethod("addPropertyChangeListener", PCL_PARAMS);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }


    /**
     * Looks up and returns the method that removes a multicast 
     * PropertyChangeListener from instances of the given class.
     * 
     * @param clazz   the class that provides the remover method
     * @return the method that removes multicast PropertyChangeListeners
     */
    public static Method getPCLRemover(Class clazz) {
        try {
            return clazz.getMethod("removePropertyChangeListener", PCL_PARAMS);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
    
    
    /**
     * Looks up and returns the method that adds a PropertyChangeListener 
     * for a specified property name to instances of the given class.
     * 
     * @param clazz   the class that provides the adder method
     * @return the method that adds the PropertyChangeListeners
     */
    public static Method getNamedPCLAdder(Class clazz) {
        try {
            return clazz.getMethod("addPropertyChangeListener", NAMED_PCL_PARAMS);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }


    /**
     * Looks up and returns the method that removes a PropertyChangeListener 
     * for a specified property name from instances of the given class.
     * 
     * @param clazz   the class that provides the remover method
     * @return the method that removes the PropertyChangeListeners
     */
    public static Method getNamedPCLRemover(Class clazz) {
        try {
            return clazz.getMethod("removePropertyChangeListener", NAMED_PCL_PARAMS);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    
    /**
     * Adds a property change listener to the given bean. First checks 
     * whether the bean supports <em>bound properties</em>, i.e. it provides 
     * a pair of methods to register multicast property change event listeners; 
     * see section 7.4.1 of the Java Beans specification for details.
     * 
     * @param bean          the bean to add the property change listener to
     * @param beanClass     the Bean class used to lookup methods from 
     * @param listener      the listener to add
     * 
     * @throws NullPointerException 
     *     if the bean or listener is <code>null</code>
     * @throws IllegalArgumentException
     *     if the bean is not an instance of the bean class
     * @throws PropertyUnboundException  
     *     if the bean does not support bound properties
     * @throws PropertyNotBindableException  
     *     if the property change handler cannot be added successfully
     *     
     * @since 1.1.1
     */
    public static void addPropertyChangeListener(Object bean, Class beanClass, PropertyChangeListener listener) {
        if (listener == null)
            throw new NullPointerException("The listener must not be null.");
        if (beanClass == null) {
            beanClass = bean.getClass();
        } else if (!beanClass.isInstance(bean)) {
            throw new IllegalArgumentException("The bean " + bean + " must be an instance of " + beanClass);
        }

        if (bean instanceof Model) {
            ((Model) bean).addPropertyChangeListener(listener);
            return;
        }

        // Check whether the bean supports bound properties.
        if (!BeanUtils.supportsBoundProperties(beanClass))
            throw new PropertyUnboundException(
                "Bound properties unsupported by bean class=" + beanClass
                    + "\nThe Bean class must provide a pair of methods:"
                    + "\npublic void addPropertyChangeListener(PropertyChangeListener x);"
                    + "\npublic void removePropertyChangeListener(PropertyChangeListener x);");

        Method multicastPCLAdder = getPCLAdder(beanClass);
        try {
            multicastPCLAdder.invoke(bean, new Object[] {listener});
        } catch (InvocationTargetException e) {
            throw new PropertyNotBindableException(
                "Due to an InvocationTargetException we failed to add " 
              + "a multicast PropertyChangeListener to bean: " + bean, e.getCause());
        } catch (IllegalAccessException e) {
            throw new PropertyNotBindableException(
                "Due to an IllegalAccessException we failed to add " 
              + "a multicast PropertyChangeListener to bean: " + bean, e);
        }
    }


    /**
     * Adds a named property change listener to the given bean. The bean 
     * must provide the optional support for listening on named properties
     * as described in section 7.4.5 of the 
     * <a href="http://java.sun.com/products/javabeans/docs/spec.html">Java Bean
     * Secification</a>. The bean class must provide the method:
     * <pre>
     * public void addPropertyChangeListener(String name, PropertyChangeListener l);
     * </pre>
     * 
     * @param bean          the bean to add a property change handler
     * @param beanClass     the Bean class used to lookup methods from 
     * @param propertyName  the name of the property to be observed
     * @param listener      the listener to add
     * 
     * @throws NullPointerException 
     *     if the bean, propertyName or listener is <code>null</code>
     * @throws IllegalArgumentException
     *     if the bean is not an instance of the bean class
     * @throws PropertyNotBindableException  
     *     if the property change handler cannot be added successfully 
     */
    public static void addPropertyChangeListener(
             Object bean, 
             Class beanClass,
             String propertyName,
             PropertyChangeListener listener) {
        
        if (propertyName == null)
            throw new NullPointerException("The property name must not be null.");
        if (listener == null)
            throw new NullPointerException("The listener must not be null.");
        if (beanClass == null) {
            beanClass = bean.getClass();
        } else if (!beanClass.isInstance(bean)) {
            throw new IllegalArgumentException("The bean " + bean + " must be an instance of " + beanClass);
        }

        if (bean instanceof Model) {
            ((Model) bean).addPropertyChangeListener(propertyName, listener);
            return;
        }
        
        Method namedPCLAdder = getNamedPCLAdder(beanClass);
        if (namedPCLAdder == null) 
            throw new PropertyNotBindableException(
              "Could not find the bean method"  
            + "/npublic void addPropertyChangeListener(String, PropertyChangeListener);"
            + "/nin bean:" + bean);

        try {
            namedPCLAdder.invoke(bean, new Object[] {propertyName, listener});
        } catch (InvocationTargetException e) {
            throw new PropertyNotBindableException(
                "Due to an InvocationTargetException we failed to add " 
              + "a named PropertyChangeListener to bean: " + bean, e.getCause());
        } catch (IllegalAccessException e) {
            throw new PropertyNotBindableException(
                "Due to an IllegalAccessException we failed to add "
              + "a named PropertyChangeListener to bean: " + bean, e);
        }
    }


    /**
     * Adds a property change listener to the given bean. First checks 
     * whether the bean supports <em>bound properties</em>, i.e. it provides 
     * a pair of methods to register multicast property change event listeners; 
     * see section 7.4.1 of the Java Beans specification for details.
     * 
     * @param bean          the bean to add the property change listener to
     * @param listener      the listener to add
     * 
     * @throws NullPointerException 
     *     if the bean or listener is <code>null</code>
     * @throws PropertyUnboundException  
     *     if the bean does not support bound properties
     * @throws PropertyNotBindableException  
     *     if the property change handler cannot be added successfully 
     */
    public static void addPropertyChangeListener(Object bean, PropertyChangeListener listener) {
        addPropertyChangeListener(bean, bean.getClass(), listener);
    }


    /**
     * Adds a named property change listener to the given bean. The bean 
     * must provide the optional support for listening on named properties
     * as described in section 7.4.5 of the 
     * <a href="http://java.sun.com/products/javabeans/docs/spec.html">Java Bean
     * Secification</a>. The bean class must provide the method:
     * <pre>
     * public void addPropertyChangeListener(String name, PropertyChangeListener l);
     * </pre>
     * 
     * @param bean          the bean to add a property change handler
     * @param propertyName  the name of the property to be observed
     * @param listener      the listener to add
     * 
     * @throws NullPointerException 
     *     if the bean, propertyName or listener is <code>null</code>
     * @throws PropertyNotBindableException  
     *     if the property change handler cannot be added successfully 
     */
    public static void addPropertyChangeListener(
             Object bean, 
             String propertyName,
             PropertyChangeListener listener) {
        addPropertyChangeListener(bean, bean.getClass(), propertyName, listener);
    }


    /**
     * Removes a property change listener from the given bean. 
     * 
     * @param bean          the bean to remove the property change listener from
     * @param beanClass     the Java Bean class used to lookup methods from
     * @param listener      the listener to remove
     * 
     * @throws NullPointerException 
     *     if the bean or listener is <code>null</code>
     * @throws IllegalArgumentException
     *     if the bean is not an instance of the bean class
     * @throws PropertyUnboundException  
     *     if the bean does not support bound properties
     * @throws PropertyNotBindableException  
     *     if the property change handler cannot be removed successfully
     *     
     * @since 1.1.1
     */
    public static void removePropertyChangeListener(Object bean, Class beanClass, PropertyChangeListener listener) {
        if (listener == null)
            throw new NullPointerException("The listener must not be null.");
        if (beanClass == null) {
            beanClass = bean.getClass();
        } else if (!beanClass.isInstance(bean)) {
            throw new IllegalArgumentException("The bean " + bean + " must be an instance of " + beanClass);
        }

        if (bean instanceof Model) {
            ((Model) bean).removePropertyChangeListener(listener);
            return;
        }

        Method multicastPCLRemover = getPCLRemover(beanClass);
        if (multicastPCLRemover == null) 
            throw new PropertyUnboundException("Could not find the method:" 
                + "\npublic void removePropertyChangeListener(String, PropertyChangeListener x);" 
                + "\nfor bean:" + bean);
        try {
            multicastPCLRemover.invoke(bean, new Object[]{listener});
        } catch (InvocationTargetException e) {
            throw new PropertyNotBindableException(
                "Due to an InvocationTargetException we failed to remove " 
              + "a multicast PropertyChangeListener from bean: " + bean, e.getCause());
        } catch (IllegalAccessException e) {
            throw new PropertyNotBindableException(
                "Due to an IllegalAccessException we failed to remove " 
              + "a multicast PropertyChangeListener from bean: " + bean, e);
        }
    }


    /**
     * Removes a named property change listener from the given bean. The bean 
     * must provide the optional support for listening on named properties
     * as described in section 7.4.5 of the 
     * <a href="http://java.sun.com/products/javabeans/docs/spec.html">Java Bean
     * Secification</a>. The bean class must provide the method:
     * <pre>
     * public void removePropertyChangeHandler(String name, PropertyChangeListener l);
     * </pre>
     * 
     * @param bean          the bean to remove the property change listener from
     * @param beanClass     the Java Bean class used to lookup methods from
     * @param propertyName  the name of the observed property
     * @param listener      the listener to remove
     * 
     * @throws NullPointerException 
     *     if the bean, propertyName, or listener is <code>null</code>
     * @throws IllegalArgumentException
     *     if the bean is not an instance of the bean class
     * @throws PropertyNotBindableException  
     *     if the property change handler cannot be removed successfully
     *     
     * @since 1.1.1
     */
    public static void removePropertyChangeListener(
             Object bean, 
             Class beanClass,
             String propertyName,
             PropertyChangeListener listener) {
        
        if (propertyName == null)
            throw new NullPointerException("The property name must not be null.");
        if (listener == null)
            throw new NullPointerException("The listener must not be null.");
        if (beanClass == null) {
            beanClass = bean.getClass();
        } else if (!beanClass.isInstance(bean)) {
            throw new IllegalArgumentException("The bean " + bean + " must be an instance of " + beanClass);
        }

        if (bean instanceof Model) {
            ((Model) bean).removePropertyChangeListener(propertyName, listener);
            return;
        }

        Method namedPCLRemover = getNamedPCLRemover(beanClass);
        if (namedPCLRemover == null) 
            throw new PropertyNotBindableException(
              "Could not find the bean method"  
            + "/npublic void removePropertyChangeListener(String, PropertyChangeListener);"
            + "/nin bean:" + bean);
        
        try {
            namedPCLRemover.invoke(bean, new Object[]{propertyName, listener});
        } catch (InvocationTargetException e) {
            throw new PropertyNotBindableException(
                "Due to an InvocationTargetException we failed to remove "
              + "a named PropertyChangeListener from bean: " + bean, e.getCause());
        } catch (IllegalAccessException e) {
            throw new PropertyNotBindableException(
                "Due to an IllegalAccessException we failed to remove "
              + "a named PropertyChangeListener from bean: " + bean, e);
        }
    }
    
    
    /**
     * Removes a property change listener from the given bean. 
     * 
     * @param bean          the bean to remove the property change listener from
     * @param listener      the listener to remove
     * @throws NullPointerException if the bean or listener is <code>null</code>
     * @throws PropertyUnboundException  
     *     if the bean does not support bound properties
     * @throws PropertyNotBindableException  
     *     if the property change handler cannot be removed successfully 
     */
    public static void removePropertyChangeListener(Object bean, PropertyChangeListener listener) {
        removePropertyChangeListener(bean, bean.getClass(), listener);
    }


    /**
     * Removes a named property change listener from the given bean. The bean 
     * must provide the optional support for listening on named properties
     * as described in section 7.4.5 of the 
     * <a href="http://java.sun.com/products/javabeans/docs/spec.html">Java Bean
     * Secification</a>. The bean class must provide the method:
     * <pre>
     * public void removePropertyChangeHandler(String name, PropertyChangeListener l);
     * </pre>
     * 
     * @param bean          the bean to remove the property change listener from
     * @param propertyName  the name of the observed property
     * @param listener      the listener to remove
     * @throws NullPointerException 
     *     if the bean, propertyName, or listener is <code>null</code>
     * @throws PropertyNotBindableException  
     *     if the property change handler cannot be removed successfully 
     */
    public static void removePropertyChangeListener(
             Object bean, 
             String propertyName,
             PropertyChangeListener listener) {
        removePropertyChangeListener(bean, bean.getClass(), propertyName, listener);
    }
    
    
    // Getting and Setting Property Values ************************************
    
    /**
     * Returns the value of the specified property of the given non-null bean.
     * This operation is unsupported if the bean property is read-only.<p>
     * 
     * If the read access fails, a PropertyAccessException is thrown
     * that provides the Throwable that caused the failure.
     * 
     * @param bean                the bean to read the value from
     * @param propertyDescriptor  describes the property to be read
     * @return the bean's property value
     * 
     * @throws NullPointerException           if the bean is <code>null</code>
     * @throws UnsupportedOperationException  if the bean property is write-only
     * @throws PropertyAccessException        if the new value could not be read
     */
    public static Object getValue(Object bean, PropertyDescriptor propertyDescriptor) {
        if (bean == null)
            throw new NullPointerException("The bean must not be null.");
        
        Method getter = propertyDescriptor.getReadMethod();
        if (getter == null) {
            throw new UnsupportedOperationException(
                "The property '" + propertyDescriptor.getName() + "' is write-only.");
        }
        
        try {
            return getter.invoke(bean, null);
        } catch (InvocationTargetException e) {
            throw PropertyAccessException.createReadAccessException(
                bean, propertyDescriptor, e.getCause());
        } catch (IllegalAccessException e) {
            throw PropertyAccessException.createReadAccessException(
                bean, propertyDescriptor, e);
        }
    }    
    
    
    /**
     * Sets the given object as new value of the specified property of the given 
     * non-null bean. This is unsupported if the bean property is read-only.<p>
     * 
     * If the write access fails, a PropertyAccessException is thrown
     * that provides the Throwable that caused the failure.
     * If the bean property is constrained and a VetoableChangeListener
     * has vetoed against the value change, the PropertyAccessException
     * wraps the PropertyVetoException thrown by the setter.
     * 
     * @param bean                the bean that holds the adapted property
     * @param propertyDescriptor  describes the property to be set
     * @param newValue            the property value to be set
     * 
     * @throws NullPointerException           if the bean is <code>null</code>
     * @throws UnsupportedOperationException  if the bean property is read-only
     * @throws PropertyAccessException        if the new value could not be set
     * @throws PropertyVetoException          if the bean setter throws this exception
     */
    public static void setValue(Object bean, PropertyDescriptor propertyDescriptor, Object newValue) 
        throws PropertyVetoException {
        if (bean == null)
            throw new NullPointerException("The bean must not be null.");
        
        Method setter = propertyDescriptor.getWriteMethod();
        if (setter == null) {
            throw new UnsupportedOperationException(
                "The property '" + propertyDescriptor.getName() + "' is read-only.");
        }
        try {
            setter.invoke(bean, new Object[] {newValue});
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof PropertyVetoException) {
                throw (PropertyVetoException) cause;
            }
            throw PropertyAccessException.createWriteAccessException(
                    bean, newValue, propertyDescriptor, cause);
            
        } catch (IllegalAccessException e) {
            throw PropertyAccessException.createWriteAccessException(
                bean, newValue, propertyDescriptor, e);
        } catch (IllegalArgumentException e) {
            throw PropertyAccessException.createWriteAccessException(
                bean, newValue, propertyDescriptor, e);
        }
    }

 
}
