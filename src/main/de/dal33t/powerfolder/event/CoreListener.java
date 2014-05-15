/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
* $Id$
*/
package de.dal33t.powerfolder.event;

/**
 * Base class for all Listeners in PowerFolder that are handled with the
 * ListenerSupportFactory. This provides an general interface so that it is
 * posible for the class that listens to the event can decide if the event
 * should be fired in the event DispatchThread. Use with care if your
 * fireInEventDispatchThread() method return true, alway make it a quick
 * implemetation because userimput will be frozen during excution. Note that if
 * there is No "awtAvailable" the events are fired in the current thread
 * regardless of the return value of fireInEventDispatchThread.
 */
public interface CoreListener {
    /**
     * Overwrite this method to indicate if the ListenerSupportFactory should
     * fire this event in the Swing Event Dispatch Thread. Use with care if you
     * return true, alway make it a quick implemetation because user input and
     * the user inerface will be frozen during execution. Don't make the
     * implmentation of this method dynamic, when adding the listener to the
     * class that fires the events this method is evaluated.<BR>
     * Also when you return true when this method is added and false if the
     * method is removed from the class that fires the events will result in
     * undefined behaviour.
     *
     * @return true if the events should be fired in the Swing Event Dispatch
     *         Thread.
     */
    public boolean fireInEventDispatchThread();
}
