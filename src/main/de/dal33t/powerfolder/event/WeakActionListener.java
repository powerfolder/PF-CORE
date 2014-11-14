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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * For weak property change listeners. Unregisteres itself once a
 * actionPerformed is fired and the listener has been GCed. Make sure that
 * <code>source</code> implements the method #removeActionListener
 *
 * @author sprajc
 */
public class WeakActionListener implements ActionListener {
    WeakReference<ActionListener> listenerRef;
    Object src;

    public WeakActionListener(ActionListener listener, Object src) {
        listenerRef = new WeakReference<ActionListener>(listener);
        this.src = src;
    }

    public void actionPerformed(ActionEvent evt) {
        ActionListener listener = listenerRef.get();
        if (listener == null) {
            removeListener();
        } else
            listener.actionPerformed(evt);
    }

    private void removeListener() {
        try {
            Method method = src.getClass().getMethod("removeActionListener",
                new Class[]{ActionListener.class});
            method.invoke(src, new Object[]{this});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}