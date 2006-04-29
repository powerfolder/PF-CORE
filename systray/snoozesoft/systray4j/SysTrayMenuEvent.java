/*********************************************************************************
                                SysTrayMenuEvent.java
                                ---------------------
    author               : Tamas Bara
    copyright            : (C) 2002-2004 by SnoozeSoft
    email                : snoozesoft@compuserve.de
 *********************************************************************************/

/*********************************************************************************
 *                                                                               *
 *   This library is free software; you can redistribute it and/or               *
 *   modify it under the terms of the GNU Lesser General Public                  *
 *   License as published by the Free Software Foundation; either                *
 *   version 2.1 of the License, or (at your option) any later version.          *
 *                                                                               *
 *   This library is distributed in the hope that it will be useful,             *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of              *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU           *
 *   Lesser General Public License for more details.                             *
 *                                                                               *
 *   You should have received a copy of the GNU Lesser General Public            *
 *   License along with this library; if not, write to the Free Software         *
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA   *
 *                                                                               *
 *********************************************************************************/

package snoozesoft.systray4j;

import java.util.EventObject;

/**
 * Instances of this class are passed to systray menu listeners who want to be informed
 * whenever a systray icon or a menu item is clicked.<br>The sender of this event can be
 * extracted either directly by accessing the source member, or looking at the action
 * command of this event.
 */
public class SysTrayMenuEvent extends EventObject
{
    private String actionCommand;

    /**
     * Creates a new <code>SysTrayMenuEvent</code> object.
     * @param source The object this event originates from.
     * @param actionCommand The action command of the event.
     */
    public SysTrayMenuEvent( Object source, String actionCommand )
    {
        super( source );
        this.actionCommand = actionCommand;
    }

    /**
     * Getter for the action command of this event object.
     * @return This objects action command.
     */
    public String getActionCommand()
    {
        return actionCommand;
    }
}