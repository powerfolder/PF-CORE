/*********************************************************************************
                                  NotAvailable.java
                                  -----------------
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

import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * A small window in the lower right desktop corner for systray emulation, for
 * unsupported platforms.
 */
class NotAvailable extends JFrame implements SysTrayAccess, MouseListener, ActionListener
{
    private GridBagLayout gridBag;
    private JPanel pnl;
    private HashMap menus; // SysTrayMenu container
    private HashMap pops; // JPopupMenu container
    private HashMap icons; // NAIcon container
    private int idCounter;
    private String helpMsg;

    private static String linuxHelp =
        "You are running SysTray for Java on Linux. If you have KDE3 installed,\n" +
        "this is a supported platform. However, the native library could not be loaded\n" +
        "by the JVM. To fix this, make sure the file libsystray4j.so is accessible from\n" +
        "your java.library.path.";

    private static String win32Help =
        "You are running SysTray for Java on Windows, which is a supported platform.\n" +
        "However, the native library could not be loaded by the JVM. To fix this, make sure\n" +
        "the file systray4j.dll is accessible from your java.library.path.";

    private static String notAvailHelp =
        "SysTray for Java is not available on this platform. Currently\n" +
        "supported platforms are, Linux with KDE3, and Windows.";

    NotAvailable()
    {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if( SysTrayManager.isWindows )
        {
            setBounds( screenSize.width - 140, screenSize.height - 110, 110, 70 );
        }
        else setBounds( screenSize.width - 170, screenSize.height - 130, 110, 70 );

        setDefaultCloseOperation( EXIT_ON_CLOSE );
        setTitle( "SysTray for Java" );

        menus = new HashMap();
        pops = new HashMap();
        icons = new HashMap();
        idCounter = 1;

        gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        pnl = new JPanel( gridBag );

        c.insets = new Insets( 0, 4, 0, 0 );
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.WEST;
        JButton btnHelp = new JButton( "Help" );
        btnHelp.addActionListener( this );
        btnHelp.setActionCommand( "-1" );
        gridBag.setConstraints( btnHelp, c );

        pnl.add( btnHelp );
        getContentPane().add( pnl, BorderLayout.CENTER );

        if( SysTrayManager.isLinux ) helpMsg = linuxHelp;
        else if( SysTrayManager.isWindows ) helpMsg = win32Help;
        else helpMsg = notAvailHelp;

        setVisible(true);
    }

    public boolean isAvailable()
    {
        return false;
    }

    public void addMainMenu( SysTrayMenu menu, String iconFileName, String toolTip )
    {
        JPopupMenu pop = new JPopupMenu();

        GridBagConstraints c = new GridBagConstraints();
        c.weighty = 1.0;
        c.insets = new Insets( 0, 0, 2, 2 );
        c.anchor = GridBagConstraints.SOUTHEAST;
        NAIcon icon = new NAIcon( menu, pop );
        icon.setIcon( new ImageIcon( getClass().getResource( "rocket.gif" ) ) );
        icon.addMouseListener( this );
        icon.setBorder( BorderFactory.createEtchedBorder() );
        icon.setToolTipText( toolTip );
        gridBag.setConstraints( icon, c );
        pnl.add( icon );
        pnl.doLayout();

        Integer key = new Integer( idCounter );
        pops.put( key, pop );
        menus.put( key, menu );
        icons.put( key, icon );

        menu.id = idCounter++;
    }

    public void addSubMenu( SubMenu menu )
    {
        JMenu pop = new JMenu();

        Integer key = new Integer( idCounter );
        pops.put( key, pop );
        menus.put( key, menu );

        menu.id = idCounter++;
    }

    public void setToolTip( int menuId, String tip )
    {
        NAIcon icon = ( NAIcon ) icons.get( new Integer( menuId ) );
        icon.setToolTipText( tip );
    }

    public void showIcon( int menuId, boolean show )
    {
        NAIcon icon = ( NAIcon ) icons.get( new Integer( menuId ) );

        if( show )
        {
            GridBagConstraints c = new GridBagConstraints();
            c.weighty = 1.0;
            c.insets = new Insets( 0, 0, 2, 2 );
            c.anchor = GridBagConstraints.SOUTHEAST;
            gridBag.setConstraints( icon, c );
            pnl.add( icon );
        }
        else pnl.remove( icon );

        pnl.doLayout();
        pnl.repaint();
    }

    public void setIcon( int menuId, String iconFileName )
    {
        System.err.println( "systray4j: method not available" );
    }

    public void enableItem( int menuId, int itemIndex, boolean enable )
    {
        JPopupMenu pop = null;
        Object obj = pops.get( new Integer( menuId ) );
        if( obj instanceof JMenu ) pop = ( ( JMenu ) obj ).getPopupMenu();
        else pop = ( JPopupMenu ) obj;

        Component c = pop.getComponent( pop.getComponentCount() - itemIndex - 1 );
        c.setEnabled( enable );
    }

    public void checkItem( int menuId, int itemIndex, boolean check )
    {
        JPopupMenu pop = null;
        Object obj = pops.get( new Integer( menuId ) );
        if( obj instanceof JMenu ) pop = ( ( JMenu ) obj ).getPopupMenu();
        else pop = ( JPopupMenu ) obj;

        JMenuItem item =
            ( JMenuItem ) pop.getComponent( pop.getComponentCount() - itemIndex - 1 );

        item.setSelected( check );
    }

    public void setItemLabel( int menuId, int itemIndex, String label )
    {
        JPopupMenu pop = null;
        Object obj = pops.get( new Integer( menuId ) );
        if( obj instanceof JMenu ) pop = ( ( JMenu ) obj ).getPopupMenu();
        else pop = ( JPopupMenu ) obj;

        JMenuItem item =
            ( JMenuItem ) pop.getComponent( pop.getComponentCount() - itemIndex - 1 );

        item.setText( label );
    }

    public void addItem( int menuId,
                         int itemIndex,
                         String label,
                         boolean checkable,
                         boolean check,
                         boolean enable )
    {
        Object obj = pops.get( new Integer( menuId ) );
        if( obj instanceof JPopupMenu )
        {
            JPopupMenu pop = ( JPopupMenu ) obj;
            int index = pop.getComponentCount() - itemIndex;
            if( label.equals( "#SEP" ) ) pop.insert( new JPopupMenu.Separator(), index );
            else if( label.startsWith( "#SUB<" ) )
            {
                Integer key = new Integer( label.substring( 5, label.indexOf( ">" ) ) );
                JMenu submenu = ( JMenu ) pops.get( key );
                submenu.setEnabled( enable );
                submenu.setText(
                    label.substring( label.indexOf( "><" ) + 2, label.length() - 1 ) );

                pop.insert( submenu, index );
            }
            else
            {
                JMenuItem item = null;
                if( checkable )
                {
                    item = new JCheckBoxMenuItem( label );
                    item.setSelected( check );
                }
                else item = new JMenuItem( label );

                item.setActionCommand( String.valueOf( menuId ) );
                item.addActionListener( this );
                item.setEnabled( enable );

                pop.insert( item, index );
            }
        }
        else
        {
            JMenu pop = ( JMenu ) obj;
            int index = pop.getItemCount() - itemIndex;
            if( label.equals( "#SEP" ) ) pop.insertSeparator( index );
            else if( label.startsWith( "#SUB<" ) )
            {
                Integer key = new Integer( label.substring( 5, label.indexOf( ">" ) ) );
                JMenu submenu = ( JMenu ) pops.get( key );
                submenu.setEnabled( enable );
                submenu.setText(
                    label.substring( label.indexOf( "><" ) + 2, label.length() - 1 ) );

                pop.insert( submenu, index );
            }
            else
            {
                JMenuItem item = null;
                if( checkable )
                {
                    item = new JCheckBoxMenuItem( label );
                    item.setSelected( check );
                }
                else item = new JMenuItem( label );

                item.setActionCommand( String.valueOf( menuId ) );
                item.addActionListener( this );
                item.setEnabled( enable );

                pop.insert( item, index );
            }
        }
    }

    public void removeItem( int menuId, int itemIndex )
    {
        JPopupMenu pop = null;
        Object obj = pops.get( new Integer( menuId ) );
        if( obj instanceof JMenu ) pop = ( ( JMenu ) obj ).getPopupMenu();
        else pop = ( JPopupMenu ) obj;

        pop.remove( pop.getComponentCount() - itemIndex - 1 );
    }

    public void removeAll( int menuId )
    {
        JPopupMenu pop = null;
        Object obj = pops.get( new Integer( menuId ) );
        if( obj instanceof JMenu ) pop = ( ( JMenu ) obj ).getPopupMenu();
        else pop = ( JPopupMenu ) obj;

        pop.removeAll();
    }

    public void dispose()
    {
        dispose();
    }

    public void mouseClicked( MouseEvent event )
    {
        NAIcon icon = ( NAIcon ) event.getSource();

        if( event.getButton() == MouseEvent.BUTTON1 )
        {
            icon.menu.iconLeftClicked( event.getClickCount() == 2 );
        }
        else if( event.getButton() == MouseEvent.BUTTON3 )
        {
            Dimension size = icon.pop.getSize();
            icon.pop.show( icon, event.getX(), event.getY() );
            size = icon.pop.getSize();
            icon.pop.show( icon, event.getX() - size.width, event.getY() - size.height );
        }
    }

    public void actionPerformed( ActionEvent event )
    {
        JPopupMenu pop = null;
        Integer key = new Integer( event.getActionCommand() );
        Object obj = menus.get( key );
        if( obj instanceof SysTrayMenu )
        {
            SysTrayMenu menu = ( SysTrayMenu ) obj;
            pop = ( JPopupMenu ) pops.get( key );

            int index = pop.getComponentCount() -
                pop.getComponentIndex( ( Component ) event.getSource() );

            menu.menuItemSelected( index - 1 );
        }
        else if( obj instanceof SubMenu )
        {
            SubMenu menu = ( SubMenu ) obj;
            pop = ( ( JMenu ) pops.get( key ) ).getPopupMenu();

            int index = pop.getComponentCount() -
                pop.getComponentIndex( ( Component ) event.getSource() );

            menu.menuItemSelected( index - 1 );
        }
        else
        {
            String message = helpMsg;

            if( SysTrayManager.isWindows || SysTrayManager.isLinux )
            {
                String path = "java.library.path = ";
                String token = "";
                int lineLength = path.length();
                String sep = System.getProperty( "path.separator" );
                String pathLong = System.getProperty( "java.library.path" );
                StringTokenizer tok = new StringTokenizer( pathLong, sep, true );
                while( tok.hasMoreTokens() )
                {
                    if( token.equals( sep ) )
                    {
                        if( lineLength > 50 )
                        {
                            path += "\n";
                            lineLength = 0;
                        }
                    }

                    token = tok.nextToken();
                    path += token;
                    lineLength += token.length();
                }

                message += "\n\n" + path;
            }

            DialogFactory.genericDialog(null, "Help", message, GenericDialogType.INFO);
        }
    }

    private class NAIcon extends JLabel
    {
        SysTrayMenu menu;
        JPopupMenu pop;

        NAIcon( SysTrayMenu menu, JPopupMenu pop )
        {
            this.menu = menu;
            this.pop = pop;
        }
    }

    public String getHelp() { return null; }
    public void mouseEntered( MouseEvent event ) {}
    public void mouseExited( MouseEvent event ) {}
    public void mousePressed( MouseEvent event ) {}
    public void mouseReleased( MouseEvent event ) {}
}
