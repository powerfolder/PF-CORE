/*********************************************************************************
                                 SysTrayManager.java
                                 -------------------
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

class SysTrayManager
{
    static final boolean isWindows = System.getProperty( "os.name" ).startsWith( "Windows" );
    static final boolean isLinux = System.getProperty( "os.name" ).equals( "Linux" );

    private static SysTrayAccess interfaze = loadInterface();

    // check availability
    static boolean isAvailable()
    {
        if( interfaze != null ) return interfaze.isAvailable();

        return false;
    }

    // create a new main menu
    static void addMainMenu( SysTrayMenu menu )
    {
        if( interfaze == null || interfaze instanceof NativeSysTray && !isAvailable() )
        {
            interfaze = new NotAvailable();
        }

        interfaze.addMainMenu( menu, menu.icon.iconFile.getAbsolutePath(), menu.toolTip );

        for( int i = 0; i < menu.getItemCount(); i++ )
        {
            addItem( menu.id, i, menu.getItemAt( i ) );
        }
    }

    // create a new submenu
    static void addSubMenu( SubMenu menu )
    {
        if( interfaze == null || interfaze instanceof NativeSysTray && !isAvailable() )
        {
            interfaze = new NotAvailable();
        }

        interfaze.addSubMenu( menu );

        for( int i = 0; i < menu.getItemCount(); i++ )
        {
            addItem( menu.id, i, menu.getItemAt( i ) );
        }
    }

    static void setToolTip( int menuId, String newTip )
    {
        interfaze.setToolTip( menuId, newTip );
    }

    static void showIcon( int menuId, boolean show )
    {
        interfaze.showIcon( menuId, show );
    }

    static void setIcon( int menuId, String newFileName )
    {
        interfaze.setIcon( menuId, newFileName );
    }

    static void enableItem( int menuId, int itemIndex, boolean enable )
    {
        interfaze.enableItem( menuId, itemIndex, enable );
    }

    static void checkItem( int menuId, int itemIndex, boolean enable )
    {
        interfaze.checkItem( menuId, itemIndex, enable );
    }

    static void addItem( int menuId, int itemIndex, Object item )
    {
        if( item instanceof SysTrayMenuItem )
        {
            boolean checkable = false;
            boolean checked = false;
            if( item instanceof CheckableMenuItem )
            {
                checkable = true;
                CheckableMenuItem checkableItem = ( CheckableMenuItem ) item;
                checked = checkableItem.getState();
            }

            if( item instanceof SubMenu )
            {
                SubMenu subMenu = ( SubMenu ) item;
                String sub = "#SUB<" + subMenu.id + "><" + subMenu.label + ">";
                interfaze.addItem( menuId, itemIndex, sub, checkable, checked, subMenu.enabled );
            }
            else
            {
                SysTrayMenuItem menuItem = ( SysTrayMenuItem ) item;
                interfaze.addItem( menuId, itemIndex, menuItem.label, checkable, checked,
                    menuItem.enabled );
            }
        }
        else interfaze.addItem( menuId, itemIndex, "#SEP", false, false, true );
    }

    static void removeItem( int menuId, int itemIndex )
    {
        interfaze.removeItem( menuId, itemIndex );
    }

    static void setItemLabel( int menuId, int itemIndex, String label )
    {
        interfaze.setItemLabel( menuId, itemIndex, label );
    }

    static void replaceItems( int menuId, Vector items )
    {
        interfaze.removeAll( menuId );

        for( int i = 0; i < items.size(); i++ )
        {
            addItem( menuId, i, items.get( i ) );
        }
    }

    static void dispose()
    {
        interfaze.dispose();
        interfaze = null;
    }

    private static SysTrayAccess loadInterface()
    {
        if( isWindows || isLinux ) return new NativeSysTray();

        return null;
    }
}
