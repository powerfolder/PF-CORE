/*********************************************************************************
                                 SysTrayMenuItem.java
                                 --------------------
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

import java.util.*;

/**
 * This class represents a labeled menu item. Interested listeners are called whenever
 * this item is clicked.
 */
public class SysTrayMenuItem
{
    protected String label;
    protected boolean enabled;
    protected Vector containers;
    protected String actionCommand;
    protected Vector listeners;

    /**
     * Creates a new <code>SysTrayMenuItem</code> object.
     * @param label The label of the item.
     */
    public SysTrayMenuItem( String label )
    {
        this( label, "" );
    }

    /**
     * Creates a new <code>SysTrayMenuItem</code> object.
     * @param label The label of the item.
     * @param actionCommand The action command emitted when this item is clicked.
     */
    public SysTrayMenuItem( String label, String actionCommand )
    {
        this.label = label;
        this.actionCommand = actionCommand;
        listeners = new Vector();
        containers = new Vector();
        enabled = true;
    }

    /**
     * Getter for the item label.
     * @return The label of this item.
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * Setter for the item label.
     * @param label The label of this item.
     */
    public void setLabel( String label )
    {
        if( !this.label.equals( label ) )
        {
            Object menu = null;
            for( int i = 0; i < containers.size(); i++ )
            {
                menu = containers.get( i );
                if( menu instanceof SysTrayMenu )
                {
                    SysTrayMenu mainMenu = ( SysTrayMenu ) menu;
                    SysTrayManager.setItemLabel(
                            mainMenu.id, mainMenu.items.indexOf( this ), label );
                }
                else
                {
                    SubMenu subMenu = ( SubMenu ) menu;
                    SysTrayManager.setItemLabel(
                            subMenu.id, subMenu.items.indexOf( this ), label );
                }
            }

            this.label = label;
        }
    }

    /**
     * Getter for the emitted action command.
     * @return The action command emitted when this item is clicked.
     */
    public String getActionCommand()
    {
        return actionCommand;
    }

    /**
     * Setter for the emitted action command.
     * @param actionCommand The action command emitted when this item is clicked.
     */
    public void setActionCommand( String actionCommand )
    {
        this.actionCommand = actionCommand;
    }

    /**
     * Adds the specified listener to receive events from this menu item.
     * @param listener The systray menu listener.
     */
    public void addSysTrayMenuListener( SysTrayMenuListener listener )
    {
        listeners.add( listener );
    }

    /**
     * Removes the specified listener so that it no longer receives events from this
     * menu item.
     * @param listener The systray menu listener.
     */
    public void removeSysTrayMenuListener( SysTrayMenuListener listener )
    {
        listeners.remove( listener );
    }

    /**
     * Enables/disables this item.
     * @param enabled Value to enable/disable this item.
     */
    public void setEnabled( boolean enabled )
    {
        if( this.enabled != enabled )
        {
            Object menu = null;
            for( int i = 0; i < containers.size(); i++ )
            {
                menu = containers.get( i );
                if( menu instanceof SysTrayMenu )
                {
                    SysTrayMenu mainMenu = ( SysTrayMenu ) menu;
                    SysTrayManager.enableItem(
                        mainMenu.id, mainMenu.items.indexOf( this ), enabled );
                }
                else
                {
                    SubMenu subMenu = ( SubMenu ) menu;
                    SysTrayManager.enableItem(
                        subMenu.id, subMenu.items.indexOf( this ), enabled );
                }
            }

            this.enabled = enabled;
        }
    }

    /**
     * Method to check whether this item is enabled.
     * @return The enabled state.
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    protected void fireMenuItemSelected()
    {
        SysTrayMenuListener listener = null;
        for( int i = 0; i < listeners.size(); i++ )
        {
            listener = ( SysTrayMenuListener ) listeners.elementAt( i );
            listener.menuItemSelected( new SysTrayMenuEvent( this, actionCommand ) );
        }
    }

    protected void addContainer( Object menu )
    {
        containers.add( menu );
    }

    protected void removeContainer( Object menu )
    {
        containers.remove( menu );
    }
}