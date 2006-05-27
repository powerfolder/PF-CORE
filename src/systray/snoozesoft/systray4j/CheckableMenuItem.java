/*********************************************************************************
                                CheckableMenuItem.java
                                ----------------------
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
 * This class represents a labeled menu item with a check mark.
 */
public class CheckableMenuItem extends SysTrayMenuItem
{
    private boolean state;

    /**
     * Creates a new <code>CheckableMenuItem</code> object.
     * @param label The label of the item.
     */
    public CheckableMenuItem( String label )
    {
        this( label, "" );
    }

    /**
     * Creates a new <code>CheckableMenuItem</code> object.
     * @param label The label of the item.
     * @param actionCommand The action command emitted when this item is clicked.
     */
    public CheckableMenuItem( String label, String actionCommand )
    {
        super( label, actionCommand );

        state = false;
    }

    /**
     * Getter for the state of this item.
     * @return true if this item is currently checked.
     */
    public boolean getState()
    {
        return state;
    }

    /**
     * Checks/unchecks this item according to <code>state</code>.
     * @param state if true this item will be checked.
     */
    public void setState( boolean state )
    {
        if( this.state != state )
        {
            Object menu = null;
            for( int i = 0; i < containers.size(); i++ )
            {
                menu = containers.get( i );
                if( menu instanceof SysTrayMenu )
                {
                    SysTrayMenu mainMenu = ( SysTrayMenu ) menu;
                    SysTrayManager.checkItem(
                        mainMenu.id, mainMenu.items.indexOf( this ), state );
                }
                else
                {
                    SubMenu subMenu = ( SubMenu ) menu;
                    SysTrayManager.checkItem(
                        subMenu.id, subMenu.items.indexOf( this ), state );
                }
            }

            this.state = state;
        }
    }

    protected void fireMenuItemSelected()
    {
        state = !state;

        SysTrayMenuListener listener = null;
        for( int i = 0; i < listeners.size(); i++ )
        {
            listener = ( SysTrayMenuListener ) listeners.elementAt( i );
            listener.menuItemSelected( new SysTrayMenuEvent( this, actionCommand ) );
        }
    }
}
