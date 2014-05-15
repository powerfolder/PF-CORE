/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: JButtonMini.java 5009 2008-08-11 01:25:22Z tot $
 */
package de.dal33t.powerfolder.event;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * For weak property change listeners. Unregisteres itself once a propertyChange
 * is fired and the listener has been gced. Make sure that <code>source</code>
 * implements the method #removePropertyChangeListener
 *
 * @author sprajc
 */
public class WeakPropertyChangeListener implements PropertyChangeListener {
    WeakReference<PropertyChangeListener> listenerRef;
    Object src;

    public WeakPropertyChangeListener(PropertyChangeListener listener,
        Object src)
    {
        listenerRef = new WeakReference<PropertyChangeListener>(listener);
        this.src = src;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        PropertyChangeListener listener = (PropertyChangeListener) listenerRef
            .get();
        if (listener == null) {
            removeListener();
        } else
            listener.propertyChange(evt);
    }

    private void removeListener() {
        try {
            Method method = src.getClass().getMethod(
                "removePropertyChangeListener",
                new Class[]{PropertyChangeListener.class});
            method.invoke(src, new Object[]{this});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}