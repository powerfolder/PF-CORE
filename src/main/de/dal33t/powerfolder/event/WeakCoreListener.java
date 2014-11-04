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

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * For weak core listeners. Unregisteres itself once a event is fired and the
 * referred listener has been GCed.
 *
 * @author sprajc
 */
class WeakCoreListener implements CoreListener, InvocationHandler {
    WeakReference<CoreListener> listenerRef;
    ListenerSupportInvocationHandler<?> src;

    public WeakCoreListener(CoreListener listener,
        ListenerSupportInvocationHandler<?> src)
    {
        listenerRef = new WeakReference<CoreListener>(listener);
        this.src = src;
    }

    public boolean isValid() {
        return listenerRef.get() != null;
    }

    public boolean fireInEventDispatchThread() {
        CoreListener listener = listenerRef.get();
        if (listener == null) {
            removeListener();
            return false;
        } else {
            return listener.fireInEventDispatchThread();
        }
    }

    public CoreListener getRef() {
        return listenerRef.get();
    }

    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable
    {
        CoreListener listener = listenerRef.get();
        if (listener == null) {
            removeListener();
            return null;
        } else {
            return method.invoke(listener, args);
        }
    }

    private void removeListener() {
        // This may cause 100% CPU ?
        // src.removeListener(this);
    }

}