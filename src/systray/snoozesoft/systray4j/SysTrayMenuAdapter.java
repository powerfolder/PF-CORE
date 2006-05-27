/*********************************************************************************
                               SysTrayMenuAdapter.java
                               -----------------------
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

/**
 * An abstract adapter class for receiving systray menu events. The methods in this
 * class are empty. This class exists as convenience for creating listener objects.
 */
public class SysTrayMenuAdapter implements SysTrayMenuListener
{
    /**
     * Called when a menu item was selected.
     * @param e The context of this event.
     */
    public void menuItemSelected( SysTrayMenuEvent e )
    {
    }

    /**
     * Called when the systray icon was left-clicked.
     * @param e The context of this event.
     */
    public void iconLeftClicked( SysTrayMenuEvent e )
    {
    }

    /**
     * Called when the systray icon was left-double-clicked.
     * @param e The context of this event.
     */
    public void iconLeftDoubleClicked( SysTrayMenuEvent e )
    {
    }
}
