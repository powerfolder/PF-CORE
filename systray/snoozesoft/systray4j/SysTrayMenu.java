/*********************************************************************************
                                   SysTrayMenu.java
                                   ----------------
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
import java.awt.*;

/**
 * <p>This class is the main interface of this package. Access to the system tray begins
 * here when an instance of this class is created.</p>
 * <p>Each instance consists of:
 * <ul>
 * <li>an icon, displayed in the system tray</li>
 * <li>a tooltip, shown when the cursor is over the icon</li>
 * <li>a native popup menu, displayed when the icon is right-clicked</li>
 * </ul>
 * </p>
 * <p>The items of the menu are placed in bottom-up order:<br>
 *   <table border="0", width="100%" cellpadding="16">
 *     <td align="center"><img src="order.png"></td>
 *   </table>
 * </p>
 */
public class SysTrayMenu
{
    public static String VERSION = "2.4";

    /**
     * The separator object.
     */
    public static Separator SEPARATOR = new Separator();

    protected Vector items;
    protected SysTrayMenuIcon icon;
    protected String toolTip;
    protected int id;

    private boolean iconVisible;

    /**
     * <p>Convenience constructor.</p>
     * <p>Calls the 'real' constructor with empty tooltip and items arguments.</p>
     * @param icon The systray icon of this menu.
     */
    public SysTrayMenu( SysTrayMenuIcon icon )
    {
        this( icon, "", new Vector() );
    }

    /**
     * <p>Convenience constructor.</p>
     * <p>Calls the 'real' constructor with empty tooltip argument.</p>
     * @param icon The systray icon of this menu.
     * @param items The menu items.
     */
    public SysTrayMenu( SysTrayMenuIcon icon, Vector items )
    {
        this( icon, "", items );
    }

    /**
     * <p>Convenience constructor.</p>
     * <p>Calls the 'real' constructor with empty items argument.</p>
     * @param icon The systray icon of this menu.
     * @param toolTip The tooltip displayed when the cursor is over the icon.
     */
    public SysTrayMenu( SysTrayMenuIcon icon, String toolTip )
    {
        this( icon, toolTip, new Vector() );
    }

    /**
     * <p>'Real' construcor.</p>
     * <p>Constructs a new menu consisting of the passed icon displayed in the system
     * tray, the tooltip, and a popup menu appearing when the user right-clicks the
     * systray icon. The menu items are taken from the passed vector, and inserted in
     * bottom-up order.</p>
     * @param icon The systray icon of this menu.
     * @param toolTip The tooltip displayed when the cursor is over the icon.
     * @param items The menu items.
     */
    public SysTrayMenu( SysTrayMenuIcon icon, String toolTip, Vector items )
    {
        this.icon = icon;
        this.toolTip = toolTip;

        Object item = null;
        for( int i = 0; i < items.size(); i++ )
        {
            item = items.get( i );
            if( item instanceof SysTrayMenuItem )
                ( ( SysTrayMenuItem ) item ).addContainer( this );
        }

        this.items = ( Vector ) items.clone();

        iconVisible = true;

        SysTrayManager.addMainMenu( this );
    }

    /**
     * Prints out a version string and exits.
     * @param args ignored.
     */
    public static void main( String[] args )
    {
        System.out.println( "SysTray for Java v" + VERSION );
    }

    /**
     * <p>Enables checking whether SysTray for Java is available on this platform.</p>
     * <p>On win32 this method returns false, if the native library could not be
     * loaded. On KDE an attempt is made to establish a connection to the SysTray for
     * Java Daemon. If connecting fails, this methods returns false.</p>
     * @return true if SysTray for Java is available.
     */
    public static boolean isAvailable()
    {
        return SysTrayManager.isAvailable();
    }

    /**
     * Sets the icon to be displayed in the system tray for this menu.
     * @param icon The systray icon of this menu.
     */
    public void setIcon( SysTrayMenuIcon icon )
    {
        this.icon = icon;
        SysTrayManager.setIcon( id, icon.iconFile.getAbsolutePath() );
    }

    /**
     * Getter for the visibility of the icon.
     * @return true if the icon in the system tray is visible.
     */
    public boolean isIconVisible()
    {
        return iconVisible;
    }

    /**
     * Shows the icon, if it is currently hidden.
     */
    public void showIcon()
    {
        //if( !iconVisible ) iconVisible may be wrong
        //{
            SysTrayManager.showIcon( id, true );
            iconVisible = true;
        //}
    }

    /**
     * Hides the icon, if it is currently visible.
     */
    public void hideIcon()
    {
        if( iconVisible )
        {
            SysTrayManager.showIcon( id, false );
            iconVisible = false;
        }
    }

    /**
     * Getter for the assigned icon.
     * @return The systray icon of this menu.
     */
    public SysTrayMenuIcon getIcon()
    {
        return icon;
    }

    /**
     * Getter for the assigned tooltip.
     * @return The tooltip displayed when the cursor is over the icon.
     */
    public String getToolTip()
    {
        return toolTip;
    }


    /**
     * Sets the tooltip of this menu.
     * @param toolTip The tooltip displayed when the cursor is over the icon.
     */
    public void setToolTip( String toolTip )
    {
        this.toolTip = toolTip;
        SysTrayManager.setToolTip( id, toolTip );
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
        items.add( index, SEPARATOR );
        SysTrayManager.addItem( id, index, SEPARATOR );
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
     * Returns the first item labeled as <code>label</code> or <code>null</code> if
     * no such item could be found.
     * @param label The label of the menu item to look for.
     * @return the target item or <code>null</code>.
     */
    public SysTrayMenuItem getItem( String label )
    {
        Object object = null;
        SysTrayMenuItem item = null;
        for( int i = 0; i < items.size(); i++ )
        {
            object = items.elementAt( i );
            if( object instanceof SysTrayMenuItem )
            {
                item = ( SysTrayMenuItem ) object;
                if( !item.label.equals( label ) ) item = null;
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

    /**
     * In some cases <code>System.exit()</code> is not the way you want
     * to exit your application. Than you should call this function to
     * make sure that the SysTray for Java thread terminates along with
     * your threads.
     */
    public static void dispose()
    {
        SysTrayManager.dispose();
    }

    // called from native code or the KDE3SysTray instance
    void iconLeftClicked( boolean doubleClicked )
    {
        Runnable fireThread = new FireThread( icon, doubleClicked );

        EventQueue.invokeLater( fireThread );
    }

    // called from native code or the KDE3SysTray instance
    void menuItemSelected( int index )
    {
        SysTrayMenuItem item = ( SysTrayMenuItem ) items.get( index );

        Runnable fireThread = new FireThread( item );

        EventQueue.invokeLater( fireThread );
    }

    private static class Separator {}

    protected static class FireThread implements Runnable
    {
        private SysTrayMenuIcon icon;
        private SysTrayMenuItem item;
        private boolean doubleClicked;

        public FireThread( SysTrayMenuIcon icon, boolean doubleClicked )
        {
            this.icon = icon;
            this.doubleClicked = doubleClicked;
            item = null;
        }

        public FireThread( SysTrayMenuItem item )
        {
            this.item = item;
            icon = null;
        }

        public void run()
        {
            if( icon != null )
            {
                if( doubleClicked ) icon.fireIconLeftDoubleClicked();
                else icon.fireIconLeftClicked();
            }
            else item.fireMenuItemSelected();
        }
    }
}