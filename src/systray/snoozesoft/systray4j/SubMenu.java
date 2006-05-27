/*********************************************************************************
                                     SubMenu.java
                                     ------------
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

import java.util.Vector;
import java.awt.EventQueue;

/**
 * This class represents a submenu. It consists of a special labeled (parent)
 * item, and the actual submenu appearing whenever the cursor enters the client
 * area of the parent item.
 */
public class SubMenu extends SysTrayMenuItem
{
    protected Vector items;
    protected int id;

    /**
     * Constructs an empty submenu with <code>label</code> as the label of
     * its parent item.
     * @param label The label of the new submenu.
     */
    public SubMenu( String label )
    {
        this( label, new Vector() );
    }

    /**
     * Constructs a new submenu with <code>label</code> as the label of its
     * parent item, and the passed items as content.
     * @param label The label of the new submenu.
     * @param items The menu items.
     */
    public SubMenu( String label, Vector items )
    {
        super( label );

        Object item = null;
        for( int i = 0; i < items.size(); i++ )
        {
            item = items.get( i );
            if( item instanceof SysTrayMenuItem )
                ( ( SysTrayMenuItem ) item ).addContainer( this );
        }

        this.items = ( Vector ) items.clone();

        SysTrayManager.addSubMenu( this );
    }

    /**
     * Inserts a separator at the top of this menu.
     */
    public void addSeparator()
    {
        addSeparator( items.size() );
    }

    /**
     * Inserts a separator to this menu.
     * @param index The position of the new separator item.
     */
    public void addSeparator( int index )
    {
        items.add( index, SysTrayMenu.SEPARATOR );
        SysTrayManager.addItem( id, index, SysTrayMenu.SEPARATOR );
    }

    /**
     * Inserts an item at the top of this menu.
     * @param item The new menu item.
     */
    public void addItem( SysTrayMenuItem item )
    {
        addItem( item, items.size() );
    }

    /**
     * Inserts an item to this menu.
     * @param item The new menu item.
     * @param index The position of the new menu item.
     */
    public void addItem( SysTrayMenuItem item, int index )
    {
        items.add( index, item );
        SysTrayManager.addItem( id, index, item );
        item.addContainer( this );
    }

    /**
     * Rebuilds this menu according to the passed vector.
     * @param items The new menu items.
     */
    public void setItems( Vector items )
    {
        Object item = null;
        for( int i = 0; i < this.items.size(); i++ )
        {
            item = this.items.elementAt( i );
            if( item instanceof SysTrayMenuItem )
                ( ( SysTrayMenuItem ) item ).removeContainer( this );
        }

        this.items.clear();

        for( int i = 0; i < items.size(); i++ )
        {
            item = items.get( i );
            if( item instanceof SysTrayMenuItem )
                ( ( SysTrayMenuItem ) item ).addContainer( this );
        }

        this.items = ( Vector ) items.clone();

        SysTrayManager.replaceItems( id, items );
    }

    /**
     * Getter for the size of this menu.
     * @return The number of items in this menu, including separators.
     */
    public int getItemCount()
    {
        return items.size();
    }

    /**
     * Returns the first item labeled as <code>label</code> or <code>null</code> if
     * no such item could be found.
     * @param aLabel The label of the menu item to look for.
     * @return the target item or <code>null</code>.
     */
    public SysTrayMenuItem getItem( String aLabel )
    {
        Object object = null;
        SysTrayMenuItem item = null;
        for( int i = 0; i < items.size(); i++ )
        {
            object = items.elementAt( i );
            if( object instanceof SysTrayMenuItem )
            {
                item = ( SysTrayMenuItem ) object;
                if( !item.label.equals( aLabel ) ) item = null;
                else break;
            }
        }

        return item;
    }

    /**
     * Returns the item at position <code>index</code>.
     * @param index The position of the menu item to look for.
     * @return the target item or <code>null</code> if the item at the given position is
     * a separator.
     */
    public SysTrayMenuItem getItemAt( int index )
    {
        Object item = items.get( index );
        if( item instanceof SysTrayMenuItem ) return ( SysTrayMenuItem ) item;
        return null;
    }

    /**
     * Removes the item at position <code>index</code> from this menu.
     * @param index The position of the menu item to remove.
     */
    public void removeItemAt( int index )
    {
        SysTrayManager.removeItem( id, index );

        Object item = items.get( index );
        if( item instanceof SysTrayMenuItem )
            ( ( SysTrayMenuItem ) item ).removeContainer( this );

        items.remove( index );
    }

    /**
     * Removes the passed item from this menu.
     * @param item The menu item to remove.
     */
    public void removeItem( Object item )
    {
        SysTrayManager.removeItem( id, items.indexOf( item ) );

        if( item instanceof SysTrayMenuItem )
            ( ( SysTrayMenuItem ) item ).removeContainer( this );

        items.remove( item );
    }

    /**
     * Removes all menu items.
     */
    public void removeAll()
    {
        Object item = null;
        for( int i = 0; i < items.size(); i++ )
        {
            item = items.elementAt( i );
            if( item instanceof SysTrayMenuItem )
                ( ( SysTrayMenuItem ) item ).removeContainer( this );
        }

        items.clear();

        SysTrayManager.replaceItems( id, items );
    }

    // called from native code or the KDE3SysTray instance
    void menuItemSelected( int index )
    {
        SysTrayMenuItem item = ( SysTrayMenuItem ) items.get( index );

        Runnable fireThread = new SysTrayMenu.FireThread( item );

        EventQueue.invokeLater( fireThread );
    }
}
