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
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

 
/**
 * A helper class for observing changes in bound bean properties 
 * where the target bean changes.
 * 
 * Provides two access styles to the target bean that holds the observed 
 * property: you can specify a bean directly, 
 * or you can use a <em>bean channel</em> to access the bean indirectly. 
 * In the latter case you specify a <code>ValueModel</code>
 * that holds the bean that in turn holds the observed properties.<p>
 * 
 * If the target bean is <code>null</code> won't report any changes.<p>
 * 
 * It is recommended to remove all listener by invoking <code>#removeAll</code> 
 * if the observed bean lives much longer than this change support instance.
 * As an alternative you may use event listener lists that are based
 * on <code>WeakReference</code>s.<p>
 * 
 * <strong>Constraints:</strong> All target bean classes must support 
 * bound properties, i. e. must provide the following pair of methods 
 * for registration of multicast property change event listeners:
 * <pre>
 * public void addPropertyChangeListener(PropertyChangeListener x);
 * public void removePropertyChangeListener(PropertyChangeListener x);
 * </pre>
 * and the following methods for listening on named properties:
 * <pre>
 * public void addPropertyChangeListener(String, PropertyChangeListener x);
 * public void removePropertyChangeListener(String, PropertyChangeListener x);
 * </pre>
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.2 $
 *
 * @see     PropertyChangeEvent
 * @see     PropertyChangeListener
 * @see     PropertyChangeSupport
 * @see     com.jgoodies.binding.beans.BeanAdapter 
 */
public final class IndirectPropertyChangeSupport {
    
    /**
     * Holds a <code>ValueModel</code> that holds the bean, that in turn
     * holds the adapted property.
     * 
     * @see #getBean()
     * @see #setBean(Object)
     */
    private final ValueModel beanChannel;  
    
    /**
     * Holds the PropertyChangeListeners that are registered for all bound
     * properties of the target bean. If the target bean changes, 
     * these listeners are removed from the old bean and added to the new bean.
     * 
     * @see #addPropertyChangeListener(PropertyChangeListener)
     * @see #removePropertyChangeListener(PropertyChangeListener)
     * @see #getPropertyChangeListeners()
     */
    private final List listenerList;
    
    /**
     * Maps property names to the list of PropertyChangeListeners that are 
     * registered for the associated bound property of the target bean. 
     * If the target bean changes, these listeners are removed from 
     * the old bean and added to the new bean.
     * 
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     * @see #removePropertyChangeListener(String, PropertyChangeListener)
     * @see #getPropertyChangeListeners(String)
     */
    private final Map namedListeners;
    

    // Instance creation ****************************************************

    /**
     * Constructs an IndirectPropertyChangeSupport that has no bean set.
     */
    public IndirectPropertyChangeSupport() {
        this(new ValueHolder(null, true));
    }

    /**
     * Constructs an IndirectPropertyChangeSupport with the given initial bean.
     * 
     * @param bean   the initial bean
     */
    public IndirectPropertyChangeSupport(Object bean) {
        this(new ValueHolder(bean, true));
    }

    /**
     * Constructs an IndirectPropertyChangeSupport using the given bean channel.
     * 
     * @param beanChannel    the ValueModel that holds the bean
     */
    public IndirectPropertyChangeSupport(ValueModel beanChannel) {
        if (beanChannel == null)
            throw new NullPointerException("The bean channel must not be null.");
        
        this.beanChannel = beanChannel;
        listenerList = new LinkedList();
        namedListeners = new HashMap();

        beanChannel.addValueChangeListener(new BeanChangeHandler());
    }
    

    // Accessors ************************************************************

    /**
     * Returns the Java Bean that holds the observed properties.
     * 
     * @return the Bean that holds the observed properties
     * 
     * @see #setBean(Object)
     */
    public Object getBean() {
        return beanChannel.getValue();
    }
    
    
    /**
     * Sets a new Java Bean as holder of the observed properties.
     * Removes all registered listeners from the old bean and
     * adds them to the new bean.
     * 
     * @param newBean  the new holder of the observed properties
     * 
     * @see #getBean()
     */
    public void setBean(Object newBean) {
        beanChannel.setValue(newBean);    
    }
    
    
    // Managing Property Change Listeners *************************************

    /**
     * Adds a PropertyChangeListener to the list of bean listeners. 
     * The listener is registered for all bound properties of the target bean.
     * <p>
     *  
     * If listener is <code>null</code>, no exception is thrown and no action is performed.
     *
     * @param listener      the PropertyChangeListener to be added
     *
     * @see #removePropertyChangeListener(PropertyChangeListener)
     * @see #removePropertyChangeListener(String, PropertyChangeListener)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     * @see #getPropertyChangeListeners()
     */
    public synchronized void addPropertyChangeListener(
                                            PropertyChangeListener listener) {
        if (listener == null)
            return;
        
        listenerList.add(listener);
        Object bean = getBean();
        if (bean != null) {
            BeanUtils.addPropertyChangeListener(bean, listener);
        }
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
     * @see #addPropertyChangeListener(PropertyChangeListener)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     * @see #removePropertyChangeListener(String, PropertyChangeListener)
     * @see #getPropertyChangeListeners()
     */
    public synchronized void removePropertyChangeListener(
                                        PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        listenerList.remove(listener);
        Object bean = getBean();
        if (bean != null) {
            BeanUtils.removePropertyChangeListener(bean, listener);
        }
    }
    
    
    /**
     * Adds a PropertyChangeListener to the list of bean listeners for a 
     * specific property. The specified property may be user-defined.<p>
     * 
     * Note that if the bean is inheriting a bound property, then no event
     * will be fired in response to a change in the inherited property.<p>
     * 
     * If listener is <code>null</code>, no exception is thrown and no action is performed.
     *
     * @param propertyName      one of the property names listed above
     * @param listener          the PropertyChangeListener to be added
     *
     * @see #removePropertyChangeListener(String, PropertyChangeListener)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     * @see #getPropertyChangeListeners(String)
     */
    public synchronized void addPropertyChangeListener(
                                        String propertyName,
                                        PropertyChangeListener listener) {
        if (listener == null) 
            return;
        
        List namedListenerList = (List) namedListeners.get(propertyName);
        if (namedListenerList == null) {
            namedListenerList = new LinkedList();
            namedListeners.put(propertyName, namedListenerList);
        }
        namedListenerList.add(listener);
        Object bean = getBean();
        if (bean != null) {
            BeanUtils.addPropertyChangeListener(bean, propertyName, listener);
        }
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
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     * @see #removePropertyChangeListener(PropertyChangeListener)
     * @see #getPropertyChangeListeners(String)
     */
    public synchronized void removePropertyChangeListener(
                                        String propertyName,
                                        PropertyChangeListener listener) {
        if (listener == null)
            return;
        
        List namedListenerList = (List) namedListeners.get(propertyName);
        if (namedListenerList == null) 
            return;
        namedListenerList.remove(listener);

        Object bean = getBean();
        if (bean != null) {
            BeanUtils.removePropertyChangeListener(bean, propertyName, listener);
        }
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
     * @see #addPropertyChangeListener(PropertyChangeListener)
     * @see #removePropertyChangeListener(PropertyChangeListener)
     * @see #getPropertyChangeListeners(String)
     * @see java.beans.PropertyChangeSupport#getPropertyChangeListeners()
     */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        if (listenerList.isEmpty()) {
            return new PropertyChangeListener[0];
        }
        return (PropertyChangeListener[]) 
                    listenerList.toArray(new PropertyChangeListener[listenerList.size()]);
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
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     * @see #removePropertyChangeListener(String, PropertyChangeListener)
     * @see #getPropertyChangeListeners()
     */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        List namedListenerList = (List) namedListeners.get(propertyName);
        if (namedListenerList == null || namedListenerList.isEmpty()) {
            return new PropertyChangeListener[0];
        }
        return (PropertyChangeListener[]) 
            namedListenerList.toArray(new PropertyChangeListener[namedListenerList.size()]);
    }
    

    // Releasing PropertyChangeListeners **************************************
    
    /**
     * Removes all registered PropertyChangeListeners from 
     * the current target bean - if any.
     */
    public void removeAll() {
        removeAllListenersFrom(getBean());
    }


    // Changing the Bean & Adding and Removing the PropertyChangeHandlers *****

    private void setBean0(Object oldBean, Object newBean) {
        removeAllListenersFrom(oldBean);
        addAllListenersTo(newBean);
    }
    
    
    /**
     * Adds all registered PropertyChangeListeners from the given bean.
     * If the bean is null no exception is thrown and no action is taken. 
     * 
     * @param bean  the bean to add a the property change listeners to
     */
    private void addAllListenersTo(Object bean) {
        if (bean == null)
            return;

        for (Iterator iter = listenerList.iterator(); iter.hasNext();) {
            PropertyChangeListener listener = (PropertyChangeListener) iter.next();
            BeanUtils.addPropertyChangeListener(bean, listener);
        }
        for (Iterator iter = namedListeners.entrySet().iterator(); iter.hasNext();) {
            Entry entry = (Entry) iter.next();
            String propertyName = (String) entry.getKey();
            List namedListenerList = (List) entry.getValue();
            for (Iterator i = namedListenerList.iterator(); i.hasNext();) {
                PropertyChangeListener listener = (PropertyChangeListener) i.next();
                BeanUtils.addPropertyChangeListener(bean, propertyName, listener);
            }
        }
    }


    /**
     * Removes all registered PropertyChangeListeners from the given bean.
     * If the bean is null no exception is thrown and no action is taken. 
     * 
     * @param bean  the bean to remove the property change handler from.
     * @throws PropertyUnboundException  
     *     if the bean does not support bound properties
     * @throws PropertyNotBindableException  
     *     if the property change handler cannot be removed successfully 
     */
    private void removeAllListenersFrom(Object bean) {
        if (bean == null)
            return;
        
        for (Iterator iter = listenerList.iterator(); iter.hasNext();) {
            PropertyChangeListener listener = (PropertyChangeListener) iter.next();
            BeanUtils.removePropertyChangeListener(bean, listener);
        }
        for (Iterator iter = namedListeners.entrySet().iterator(); iter.hasNext();) {
            Entry entry = (Entry) iter.next();
            String propertyName = (String) entry.getKey();
            List namedListenerList = (List) entry.getValue();
            for (Iterator i = namedListenerList.iterator(); i.hasNext();) {
                PropertyChangeListener listener = (PropertyChangeListener) i.next();
                BeanUtils.removePropertyChangeListener(bean, propertyName, listener);
            }
        }
    }


    // Helper Classes *********************************************************
    
    /**
     * Listens to changes of the bean.
     */
    private class BeanChangeHandler implements PropertyChangeListener {

        /**
         * The bean channel's value has been changed. Set the new bean,
         * remove all listeners from the old bean and add them to the new bean.
         * 
         * @param evt   the property change event to be handled
         */
        public void propertyChange(PropertyChangeEvent evt) {
            setBean0(evt.getOldValue(), evt.getNewValue());
        }
    }
    
   
}
