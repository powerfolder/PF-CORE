
package snoozesoft.systray4j.test;

import java.io.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import snoozesoft.systray4j.*;

class Controller implements SysTrayMenuListener, ActionListener, ChangeListener
{
    Vector icons;
    HashMap mains;
    HashMap mainSub;
    HashMap subs;
    int mainIds;
    int subIds;
    Interface ui;

    Controller()
    {
        icons = new Vector();
        loadIcons();

        mains = new HashMap();
        mainSub = new HashMap();
        subs = new HashMap();
        mainIds = 1;
        subIds = 1;

        ui = new Interface( this );
    }

    public static void main( String[] args ) throws Exception
    {
        //JFrame.setDefaultLookAndFeelDecorated( true );
        //JDialog.setDefaultLookAndFeelDecorated( true );

        UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

        new Controller();
    }

    public void actionPerformed( ActionEvent e )
    {
        if( e.getActionCommand().equals( "add main" ) )
        {
            SysTrayMenuIcon icon =
                new SysTrayMenuIcon( ui.comboIcons.getSelectedItem().toString() );

            icon.addSysTrayMenuListener( this );

            SysTrayMenu menu =
                new SysTrayMenu( icon, format( ui.txtTip.getText() ) );

            int id = mainIds++;
            String key = String.valueOf( id );
            mains.put( key, menu );

            ui.comboMains.addItem( key );
            ui.comboMains.setEnabled( true );
            ui.btnShowIcon.setEnabled( true );
            ui.btnHideIcon.setEnabled( true );
            ui.comboIcons2.setEnabled( true );
            ui.btnSetIcon.setEnabled( true );
            ui.txtTip2.setEnabled( true );
            ui.btnSetTip.setEnabled( true );
            ui.txtLabel.setEnabled( true );
            ui.txtAction.setEnabled( true );
            ui.checkIndex.setEnabled( true );
            ui.checkCheckable.setEnabled( true );
            ui.btnAddItem.setEnabled( true );
            ui.btnAddSub.setEnabled( true );
            ui.btnAddSep.setEnabled( true );
            ui.checkIndex2.setEnabled( true );

            ui.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        }
        else if( e.getActionCommand().equals( "main" ) )
        {
            updateSub();
            stateChanged( null );
        }
        else if( e.getActionCommand().equals( "sub" ) )
        {
            ui.comboSubs.setEnabled( ui.checkSub.isSelected() );
        }
        else if( e.getActionCommand().equals( "index 1" ) )
        {
            ui.txtIndex.setEnabled( ui.checkIndex.isSelected() );
        }
        else if( e.getActionCommand().equals( "index 2" ) )
        {
            ui.txtIndex2.setEnabled( ui.checkIndex2.isSelected() );
        }
        else if( e.getActionCommand().equals( "show icon" ) )
        {
            SysTrayMenu menu = ( SysTrayMenu ) getMenu( false );
            menu.showIcon();
        }
        else if( e.getActionCommand().equals( "hide icon" ) )
        {
            SysTrayMenu menu = ( SysTrayMenu ) getMenu( false );
            menu.hideIcon();
        }
        else if( e.getActionCommand().equals( "set icon" ) )
        {
            SysTrayMenu menu = ( SysTrayMenu ) getMenu( false );

            SysTrayMenuIcon icon =
                new SysTrayMenuIcon( ui.comboIcons2.getSelectedItem().toString() );

            icon.addSysTrayMenuListener( this );

            menu.setIcon( icon );
        }
        else if( e.getActionCommand().equals( "set tip" ) )
        {
            SysTrayMenu menu = ( SysTrayMenu ) getMenu( false );
            menu.setToolTip( format( ui.txtTip2.getText() ) );
        }
        else if( e.getActionCommand().equals( "add item" ) )
        {
            Object obj = getMenu( true );

            SysTrayMenuItem item = null;
            if( ui.checkCheckable.isSelected() )
            {
                item = new CheckableMenuItem( ui.txtLabel.getText(), ui.txtAction.getText() );
            }
            else item = new SysTrayMenuItem( ui.txtLabel.getText(), ui.txtAction.getText() );
            item.addSysTrayMenuListener( this );

            if( obj instanceof SysTrayMenu )
            {
                SysTrayMenu menu = ( SysTrayMenu ) obj;
                if( ui.checkIndex.isSelected() )
                {
                    menu.addItem( item, Integer.parseInt( ui.txtIndex.getText() ) );
                }
                else menu.addItem( item );
            }
            else
            {
                SubMenu menu = ( SubMenu ) obj;
                if( ui.checkIndex.isSelected() )
                {
                    menu.addItem( item, Integer.parseInt( ui.txtIndex.getText() ) );
                }
                else menu.addItem( item );
            }
        }
        else if( e.getActionCommand().equals( "add sub" ) )
        {
            Object obj = getMenu( true );

            SubMenu item = new SubMenu( ui.txtLabel.getText() );

            if( obj instanceof SysTrayMenu )
            {
                SysTrayMenu menu = ( SysTrayMenu ) obj;
                if( ui.checkIndex.isSelected() )
                {
                    menu.addItem( item, Integer.parseInt( ui.txtIndex.getText() ) );
                }
                else menu.addItem( item );
            }
            else
            {
                SubMenu menu = ( SubMenu ) obj;
                if( ui.checkIndex.isSelected() )
                {
                    menu.addItem( item, Integer.parseInt( ui.txtIndex.getText() ) );
                }
                else menu.addItem( item );
            }

            int id = subIds++;
            String key = String.valueOf( id );
            subs.put( key, item );

            Vector subList;
            Object keyMain = ui.comboMains.getSelectedItem();
            if( mainSub.containsKey( keyMain ) ) subList = ( Vector ) mainSub.get( keyMain );
            else
            {
                subList = new Vector();
                mainSub.put( keyMain, subList );
            }

            subList.add( key );

            updateSub();
        }
        else if( e.getActionCommand().equals( "add sep" ) )
        {
            Object obj = getMenu( true );

            if( obj instanceof SysTrayMenu )
            {
                SysTrayMenu menu = ( SysTrayMenu ) obj;
                if( ui.checkIndex2.isSelected() )
                {
                    menu.addSeparator( Integer.parseInt( ui.txtIndex2.getText() ) );
                }
                else menu.addSeparator();
            }
            else
            {
                SubMenu menu = ( SubMenu ) obj;
                if( ui.checkIndex2.isSelected() )
                {
                    menu.addSeparator( Integer.parseInt( ui.txtIndex2.getText() ) );
                }
                else menu.addSeparator();
            }
        }
        else if( e.getActionCommand().equals( "change item" ) )
        {
            SysTrayMenuItem item = getItemAt( Integer.parseInt( ui.txtIndex3.getText() ) );
            item.setLabel( ui.txtLabel2.getText() );
        }
        else if( e.getActionCommand().equals( "change action" ) )
        {
            SysTrayMenuItem item = getItemAt( Integer.parseInt( ui.txtIndex3.getText() ) );
            item.setActionCommand( ui.txtAction2.getText() );
        }
        else if( e.getActionCommand().equals( "enable item" ) )
        {
            SysTrayMenuItem item = getItemAt( Integer.parseInt( ui.txtIndex3.getText() ) );
            item.setEnabled( true );
        }
        else if( e.getActionCommand().equals( "disable item" ) )
        {
            SysTrayMenuItem item = getItemAt( Integer.parseInt( ui.txtIndex3.getText() ) );
            item.setEnabled( false );
        }
        else if( e.getActionCommand().equals( "check item" ) )
        {
            CheckableMenuItem item =
                ( CheckableMenuItem ) getItemAt( Integer.parseInt( ui.txtIndex3.getText() ) );

            item.setState( true );
        }
        else if( e.getActionCommand().equals( "uncheck item" ) )
        {
            CheckableMenuItem item =
                ( CheckableMenuItem ) getItemAt( Integer.parseInt( ui.txtIndex3.getText() ) );

            item.setState( false );
        }
        else if( e.getActionCommand().equals( "remove item" ) )
        {
            Object obj = getMenu( true );
            SysTrayMenuItem item = null;

            if( obj instanceof SysTrayMenu )
            {
                SysTrayMenu menu = ( SysTrayMenu ) obj;
                menu.removeItemAt( Integer.parseInt( ui.txtIndex4.getText() ) );
            }
            else
            {
                SubMenu menu = ( SubMenu ) obj;
                menu.removeItemAt( Integer.parseInt( ui.txtIndex4.getText() ) );
            }

            if( item instanceof SubMenu )
            {
                removeSub( ( SubMenu ) item );
                updateSub();
            }

            updateRemove();
        }
        else if( e.getActionCommand().equals( "remove all" ) )
        {
            Object obj = getMenu( true );

            if( obj instanceof SysTrayMenu )
            {
                SysTrayMenu menu = ( SysTrayMenu ) obj;
                removeAllMain( menu );
                menu.removeAll();
            }
            else
            {
                SubMenu menu = ( SubMenu ) obj;
                removeAllSub( menu );
                menu.removeAll();
            }

            updateSub();
            updateRemove();
        }
    }

    public void stateChanged( ChangeEvent event )
    {
        int index = ui.tab.getSelectedIndex();
        if( index == 1 ) updateChange();
        else if( index == 2 ) updateRemove();
    }

    public void menuItemSelected( SysTrayMenuEvent e )
    {
        if( e.getActionCommand().equals( "exit" ) ) System.exit( 0 );

        JOptionPane.showMessageDialog(
            ui, e.getActionCommand(), "Item Selected", JOptionPane.INFORMATION_MESSAGE );
    }

    public void iconLeftClicked( SysTrayMenuEvent e )
    {
        if( ui.isVisible() ) ui.hide();
        else ui.show();
    }

    public void iconLeftDoubleClicked( SysTrayMenuEvent e )
    {
        JOptionPane.showMessageDialog(
            ui, "Calling SysTrayMenu.dispose()", "Info", JOptionPane.INFORMATION_MESSAGE );

        SysTrayMenu.dispose();
        ui.dispose();
    }

    void loadIcons()
    {
        String[] list = new File( "." ).list();
        for( int i = 0; i < list.length; i++ )
        {
            String fileName = list[ i ];
            if( fileName.endsWith( SysTrayMenuIcon.getExtension() ) )
            {
                icons.add( new File( fileName ) );
            }
        }
    }

    Object getMenu( boolean sub )
    {
        Object key = ui.comboMains.getSelectedItem();
        if( sub && ui.checkSub.isSelected() && mainSub.containsKey( key ) )
        {
            key = ui.comboSubs.getSelectedItem();

            return subs.get( key );
        }

        return mains.get( key );
    }

    SysTrayMenuItem getItemAt( int index )
    {
        SysTrayMenuItem item = null;
        Object obj = getMenu( true );

        if( obj instanceof SysTrayMenu )
        {
            SysTrayMenu menu = ( SysTrayMenu ) obj;
            item = menu.getItemAt( index );
        }
        else
        {
            SubMenu menu = ( SubMenu ) obj;
            item = menu.getItemAt( index );
        }

        return item;
    }

    void updateSub()
    {
        Object key = ui.comboMains.getSelectedItem();
        if( mainSub.containsKey( key ) )
        {
            Vector subList = ( Vector ) mainSub.get( key );
            if( !subList.isEmpty() )
            {
                ui.comboSubs.removeAllItems();

                Iterator i = subList.iterator();
                while( i.hasNext() ) ui.comboSubs.addItem( i.next() );

                ui.checkSub.setEnabled( true );
                ui.comboSubs.setEnabled( ui.checkSub.isSelected() );

                return;
            }
        }

        ui.checkSub.setEnabled( false );
        ui.comboSubs.setEnabled( false );
    }

    void updateChange()
    {
        boolean enable = false;
        Object obj = getMenu( true );

        if( obj instanceof SysTrayMenu )
        {
            SysTrayMenu menu = ( SysTrayMenu ) obj;
            if( menu.getItemCount() > 0 ) enable = true;
        }
        else if( obj instanceof SubMenu )
        {
            SubMenu menu = ( SubMenu ) obj;
            if( menu.getItemCount() > 0 ) enable = true;
        }

        ui.txtIndex3.setEnabled( enable );
        ui.txtLabel2.setEnabled( enable );
        ui.btnChangeItem.setEnabled( enable );
        ui.txtAction2.setEnabled( enable );
        ui.btnChangeAction.setEnabled( enable );
        ui.btnEnableItem.setEnabled( enable );
        ui.btnDisableItem.setEnabled( enable );
        ui.btnCheckItem.setEnabled( enable );
        ui.btnUncheckItem.setEnabled( enable );
    }

    void updateRemove()
    {
        boolean enable = false;
        Object obj = getMenu( true );

        if( obj instanceof SysTrayMenu )
        {
            SysTrayMenu menu = ( SysTrayMenu ) obj;
            if( menu.getItemCount() > 0 ) enable = true;
        }
        else if( obj instanceof SubMenu )
        {
            SubMenu menu = ( SubMenu ) obj;
            if( menu.getItemCount() > 0 ) enable = true;
        }

        ui.txtIndex4.setEnabled( enable );
        ui.btnRemoveItem.setEnabled( enable );
        ui.btnRemoveAll.setEnabled( enable );
    }

    void removeSub( SubMenu menu )
    {
        SysTrayMenuItem item = null;
        for( int i = 0; i < menu.getItemCount(); i++ )
        {
            item = menu.getItemAt( i );
            if( item instanceof SubMenu ) removeSub( ( SubMenu ) item );
        }

        Object key = null;
        Object value = null;
        Iterator iterator = subs.keySet().iterator();
        while( iterator.hasNext() )
        {
            key = iterator.next();
            value = subs.get( key );
            if( value == menu ) break;
        }

        subs.remove( key );

        Object keyMain = ui.comboMains.getSelectedItem();
        Vector subList = ( Vector ) mainSub.get( keyMain );
        subList.remove( key );
    }


    void removeAllMain( SysTrayMenu menu )
    {
        Object key = ui.comboMains.getSelectedItem();
        if( mainSub.containsKey( key ) )
        {
            Vector subList = ( Vector ) mainSub.get( key );
            if( !subList.isEmpty() )
            {
                for( int i = 0; i < subList.size(); i++ )
                {
                    subs.remove( subList.get( i ) );
                }

                subList.clear();
            }
        }
    }

    void removeAllSub( SubMenu menu )
    {
        SysTrayMenuItem item = null;
        for( int i = 0; i < menu.getItemCount(); i++ )
        {
            item = menu.getItemAt( i );
            if( item instanceof SubMenu ) removeSub( ( SubMenu ) item );
        }
    }
    String format( String tipRaw )
    {
        if( tipRaw.indexOf( '\\' ) > -1 )
        {
            byte[] raw = tipRaw.getBytes();
            byte[] tip = new byte[ raw.length ];

            boolean found = false;

            for( int r = 0, t = 0; r < raw.length; r++ )
            {
                if( found )
                {
                    if( raw[ r ] == 'n' ) tip[ t ] = '\n';
                    else tip[ t ] = raw[ r ];

                    found = false;
                }
                else if( raw[ r ] == '\\' ) found = true;
                else tip[ t ] = raw[ r ];

                if( !found ) ++t;
            }

            return new String( tip );
        }

        return tipRaw;
    }
}
