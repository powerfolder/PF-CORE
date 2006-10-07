
package snoozesoft.systray4j.test;

import java.awt.*;
import javax.swing.*;

class Interface extends JFrame
{
    JComboBox comboIcons;
    JTextField txtTip;
    JComboBox comboMains;
    JCheckBox checkSub;
    JComboBox comboSubs;
    JButton btnShowIcon;
    JButton btnHideIcon;
    JComboBox comboIcons2;
    JButton btnSetIcon;
    JTextField txtTip2;
    JButton btnSetTip;
    JTabbedPane tab;
    JTextField txtLabel;
    JButton btnAddItem;
    JTextField txtIndex;
    JTextField txtAction;
    JCheckBox checkCheckable;
    JCheckBox checkIndex;
    JButton btnAddSub;
    JButton btnAddSep;
    JCheckBox checkIndex2;
    JTextField txtIndex2;
    JTextField txtIndex3;
    JTextField txtLabel2;
    JButton btnChangeItem;
    JTextField txtAction2;
    JButton btnChangeAction;
    JButton btnEnableItem;
    JButton btnDisableItem;
    JButton btnCheckItem;
    JButton btnUncheckItem;
    JTextField txtIndex4;
    JButton btnRemoveItem;
    JButton btnRemoveAll;

    Interface( Controller controller )
    {
        setBounds( 100, 60, 570, 460 );
        setDefaultCloseOperation( EXIT_ON_CLOSE );
        setTitle( "SysTray for Java Test" );

        GridBagLayout layoutMain = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout( layoutMain );

        /////////////////////////////// add menu ////////////////////////////////

        c.insets = new Insets( 8, 6, 4, 2 );
        c.anchor = GridBagConstraints.EAST;
        JLabel lbl = new JLabel( "Tooltip:" );
        layoutMain.setConstraints( lbl, c );
        getContentPane().add( lbl );

        c.weightx = 1.0;
        c.insets = new Insets( 8, 2, 4, 0 );
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.WEST;

        if( System.getProperty( "os.name" ).equals( "Linux" ) )
        {
            txtTip = new JTextField( "  ~~~~~~~~~~~~~~\\n | SysTray for Java |\\n  ~~~~~~~~~~~~~~" );
        }
        else txtTip = new JTextField( " ~~~~~~~~~~~\\n| SysTray for Java |\\n ~~~~~~~~~~~" );

        layoutMain.setConstraints( txtTip, c );
        getContentPane().add( txtTip );

        c.weightx = 0;
        c.gridwidth = 1;
        c.insets = new Insets( 8, 12, 4, 2 );
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        lbl = new JLabel( "Icon:" );
        layoutMain.setConstraints( lbl, c );
        getContentPane().add( lbl );

        c.insets = new Insets( 8, 2, 4, 6 );
        c.anchor = GridBagConstraints.WEST;
        comboIcons = new JComboBox( controller.icons );
        layoutMain.setConstraints( comboIcons, c );
        getContentPane().add( comboIcons );

        c.insets = new Insets( 8, 6, 4, 6 );
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = GridBagConstraints.REMAINDER;
        JButton btnAddMain = new JButton( "Add Menu" );
        btnAddMain.addActionListener( controller );
        btnAddMain.setActionCommand( "add main" );
        layoutMain.setConstraints( btnAddMain, c );
        getContentPane().add( btnAddMain );

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets( 4, 4, 4, 4 );
        JSeparator sep = new JSeparator();
        layoutMain.setConstraints( sep, c );
        getContentPane().add( sep );

        /////////////////////////////// select menu ////////////////////////////////

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.gridwidth = 1;
        c.insets = new Insets( 4, 6, 2, 2 );
        c.anchor = GridBagConstraints.EAST;
        lbl = new JLabel( "Menu:" );
        layoutMain.setConstraints( lbl, c );
        getContentPane().add( lbl );

        c.insets = new Insets( 4, 0, 2, 6 );
        c.anchor = GridBagConstraints.WEST;
        comboMains = new JComboBox();
        comboMains.setEnabled( false );
        comboMains.setActionCommand( "main" );
        comboMains.addActionListener( controller );
        layoutMain.setConstraints( comboMains, c );
        getContentPane().add( comboMains );

        c.insets = new Insets( 4, 6, 2, 2 );
        c.anchor = GridBagConstraints.EAST;
        checkSub = new JCheckBox( "Submenu:" );
        checkSub.setEnabled( false );
        checkSub.setActionCommand( "sub" );
        checkSub.addActionListener( controller );
        layoutMain.setConstraints( checkSub, c );
        getContentPane().add( checkSub );

        c.insets = new Insets( 4, 0, 2, 6 );
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        comboSubs = new JComboBox();
        comboSubs.setEnabled( false );
        layoutMain.setConstraints( comboSubs, c );
        getContentPane().add( comboSubs );

        ///////////////////////// change icon and tooltip ////////////////////////

        c.insets = new Insets( 4, 6, 4, 6 );
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        JPanel pnlIcon = new JPanel();
        pnlIcon.setBorder( BorderFactory.createTitledBorder( "Icon" ) );
        layoutMain.setConstraints( pnlIcon, c );
        getContentPane().add( pnlIcon );

        GridBagLayout layoutIcon = new GridBagLayout();
        pnlIcon.setLayout( layoutIcon );

        c.insets = new Insets( 4, 4, 4, 2 );
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 2;
        btnShowIcon = new JButton( "Show" );
        btnShowIcon.addActionListener( controller );
        btnShowIcon.setActionCommand( "show icon" );
        btnShowIcon.setEnabled( false );
        layoutIcon.setConstraints( btnShowIcon, c );
        pnlIcon.add( btnShowIcon );

        c.gridwidth = 1;
        c.insets = new Insets( 4, 2, 4, 6 );
        btnHideIcon = new JButton( "Hide" );
        btnHideIcon.addActionListener( controller );
        btnHideIcon.setActionCommand( "hide icon" );
        btnHideIcon.setEnabled( false );
        layoutIcon.setConstraints( btnHideIcon, c );
        pnlIcon.add( btnHideIcon );

        c.insets = new Insets( 4, 2, 4, 2 );
        comboIcons2 = new JComboBox( controller.icons );
        comboIcons2.setEnabled( false );
        layoutIcon.setConstraints( comboIcons2, c );
        pnlIcon.add( comboIcons2 );

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        btnSetIcon = new JButton( "Set" );
        btnSetIcon.addActionListener( controller );
        btnSetIcon.setActionCommand( "set icon" );
        btnSetIcon.setEnabled( false );
        layoutIcon.setConstraints( btnSetIcon, c );
        pnlIcon.add( btnSetIcon );

        c.weightx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets( 4, 4, 4, 2 );
        lbl = new JLabel( "Tooltip:" );
        layoutIcon.setConstraints( lbl, c );
        pnlIcon.add( lbl );

        c.weightx = 1.0;
        c.gridwidth = 4;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets( 4, 2, 4, 2 );
        txtTip2 = new JTextField();
        txtTip2.setEnabled( false );
        layoutIcon.setConstraints( txtTip2, c );
        pnlIcon.add( txtTip2 );

        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        btnSetTip = new JButton( "Set" );
        btnSetTip.addActionListener( controller );
        btnSetTip.setActionCommand( "set tip" );
        btnSetTip.setEnabled( false );
        layoutIcon.setConstraints( btnSetTip, c );
        pnlIcon.add( btnSetTip );

        /////////////////////////////// tabbed pane ////////////////////////////////

        c.insets = new Insets( 2, 6, 4, 6 );
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        tab = new JTabbedPane();
        layoutMain.setConstraints( tab, c );
        getContentPane().add( tab );

        /////////////////////////////// add item ////////////////////////////////

        JPanel pnlAddItem = new JPanel();
        tab.add( "Add Item", pnlAddItem );

        GridBagLayout layoutAddItem = new GridBagLayout();
        pnlAddItem.setLayout( layoutAddItem );

        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets( 12, 8, 4, 2 );
        lbl = new JLabel( "Label:" );
        layoutAddItem.setConstraints( lbl, c );
        pnlAddItem.add( lbl );

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets( 12, 2, 4, 6 );
        txtLabel = new JTextField();
        txtLabel.setEnabled( false );
        layoutAddItem.setConstraints( txtLabel, c );
        pnlAddItem.add( txtLabel );

        c.weightx = 0;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets( 4, 8, 4, 2 );
        lbl = new JLabel( "Action Command:" );
        layoutAddItem.setConstraints( lbl, c );
        pnlAddItem.add( lbl );

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        //c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets( 4, 2, 4, 0 );
        txtAction = new JTextField( 12 );
        txtAction.setEnabled( false );
        layoutAddItem.setConstraints( txtAction, c );
        pnlAddItem.add( txtAction );

        c.weightx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets( 4, 8, 4, 2 );
        checkCheckable = new JCheckBox ( "Checkable" );
        checkCheckable.setEnabled( false );
        layoutAddItem.setConstraints( checkCheckable, c );
        pnlAddItem.add( checkCheckable );

        c.gridwidth = 2;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets( 4, 8, 4, 0 );
        checkIndex = new JCheckBox( "Index:" );
        checkIndex.setEnabled( false );
        checkIndex.setActionCommand( "index 1" );
        checkIndex.addActionListener( controller );
        layoutAddItem.setConstraints( checkIndex, c );
        pnlAddItem.add( checkIndex );

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets( 0, 0, 0, 0 );
        txtIndex = new JTextField( "0", 2 );
        txtIndex.setEnabled( false );
        layoutAddItem.setConstraints( txtIndex, c );
        pnlAddItem.add( txtIndex );

        c.gridwidth = 2;
        c.insets = new Insets( 8, 8, 4, 0 );
        //c.weightx = 0;
        //c.fill = GridBagConstraints.NONE;
        //c.anchor = GridBagConstraints.CENTER;
        btnAddItem = new JButton( "Add" );
        btnAddItem.addActionListener( controller );
        btnAddItem.setActionCommand( "add item" );
        btnAddItem.setEnabled( false );
        layoutAddItem.setConstraints( btnAddItem, c );
        pnlAddItem.add( btnAddItem );

        c.insets = new Insets( 8, 2, 4, 8 );
        c.gridwidth = GridBagConstraints.REMAINDER;
        btnAddSub = new JButton( "Add As Submenu" );
        btnAddSub.addActionListener( controller );
        btnAddSub.setActionCommand( "add sub" );
        btnAddSub.setEnabled( false );
        layoutAddItem.setConstraints( btnAddSub, c );
        pnlAddItem.add( btnAddSub );

        c.gridwidth = 4;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets( 16, 8, 8, 4 );
        btnAddSep = new JButton( "Add Separator" );
        btnAddSep.addActionListener( controller );
        btnAddSep.setActionCommand( "add sep" );
        btnAddSep.setEnabled( false );
        layoutAddItem.setConstraints( btnAddSep, c );
        pnlAddItem.add( btnAddSep );

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.insets = new Insets( 17, 4, 8, 0 );
        checkIndex2 = new JCheckBox( "At Index:" );
        checkIndex2.setEnabled( false );
        checkIndex2.setActionCommand( "index 2" );
        checkIndex2.addActionListener( controller );
        layoutAddItem.setConstraints( checkIndex2, c );
        pnlAddItem.add( checkIndex2 );

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets( 19, 0, 8, 6 );
        txtIndex2 = new JTextField( "0", 2 );
        txtIndex2.setEnabled( false );
        layoutAddItem.setConstraints( txtIndex2, c );
        pnlAddItem.add( txtIndex2 );

        /////////////////////////////// change item ////////////////////////////////

        JPanel pnlChangeItem = new JPanel();
        tab.add( "Change Item", pnlChangeItem );
        tab.addChangeListener( controller );

        GridBagLayout layoutChangeItem = new GridBagLayout();
        pnlChangeItem.setLayout( layoutChangeItem );

        c.weighty = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets( 12, 8, 4, 2 );
        lbl = new JLabel( "Index:" );
        layoutChangeItem.setConstraints( lbl, c );
        pnlChangeItem.add( lbl );

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets( 12, 2, 4, 8 );
        txtIndex3 = new JTextField( "0", 2 );
        txtIndex3.setEnabled( false );
        layoutChangeItem.setConstraints( txtIndex3, c );
        pnlChangeItem.add( txtIndex3 );

        c.gridwidth = 1;
        c.insets = new Insets( 8, 8, 4, 2 );
        c.anchor = GridBagConstraints.EAST;
        lbl = new JLabel( "Label:" );
        layoutChangeItem.setConstraints( lbl, c );
        pnlChangeItem.add( lbl );

        c.gridwidth = 4;
        c.insets = new Insets( 8, 2, 4, 8 );
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        txtLabel2 = new JTextField();
        txtLabel2.setEnabled( false );
        layoutChangeItem.setConstraints( txtLabel2, c );
        pnlChangeItem.add( txtLabel2 );

        c.insets = new Insets( 8, 0, 4, 8 );
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0;
        btnChangeItem = new JButton( "Change" );
        btnChangeItem.addActionListener( controller );
        btnChangeItem.setActionCommand( "change item" );
        btnChangeItem.setEnabled( false );
        layoutChangeItem.setConstraints( btnChangeItem, c );
        pnlChangeItem.add( btnChangeItem );

        c.gridwidth = 3;
        c.insets = new Insets( 4, 8, 4, 2 );
        c.anchor = GridBagConstraints.EAST;
        lbl = new JLabel( "Action Command:" );
        layoutChangeItem.setConstraints( lbl, c );
        pnlChangeItem.add( lbl );

        c.gridwidth = 1;
        c.insets = new Insets( 4, 2, 4, 8 );
        //c.fill = GridBagConstraints.HORIZONTAL;
        //c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        txtAction2 = new JTextField( 12 );
        txtAction2.setEnabled( false );
        layoutChangeItem.setConstraints( txtAction2, c );
        pnlChangeItem.add( txtAction2 );

        c.insets = new Insets( 4, 0, 4, 8 );
        //c.anchor = GridBagConstraints.CENTER;
        //c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        //c.weightx = 0;
        btnChangeAction = new JButton( "Change" );
        btnChangeAction.addActionListener( controller );
        btnChangeAction.setActionCommand( "change action" );
        btnChangeAction.setEnabled( false );
        layoutChangeItem.setConstraints( btnChangeAction, c );
        pnlChangeItem.add( btnChangeAction );

        c.gridwidth = 2;
        //c.weighty = 1.0;
        c.insets = new Insets( 10, 8, 4, 2 );
        c.anchor = GridBagConstraints.EAST;
        btnEnableItem = new JButton( "Enable" );
        btnEnableItem.addActionListener( controller );
        btnEnableItem.setActionCommand( "enable item" );
        btnEnableItem.setEnabled( false );
        layoutChangeItem.setConstraints( btnEnableItem, c );
        pnlChangeItem.add( btnEnableItem );

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets( 10, 2, 4, 2 );
        btnDisableItem = new JButton( "Disable" );
        btnDisableItem.addActionListener( controller );
        btnDisableItem.setActionCommand( "disable item" );
        btnDisableItem.setEnabled( false );
        layoutChangeItem.setConstraints( btnDisableItem, c );
        pnlChangeItem.add( btnDisableItem );

        c.gridwidth = 2;
        c.weighty = 1.0;
        c.insets = new Insets( 4, 8, 4, 2 );
        c.anchor = GridBagConstraints.NORTHEAST;
        c.fill = GridBagConstraints.HORIZONTAL;
        btnCheckItem = new JButton( "Check" );
        btnCheckItem.addActionListener( controller );
        btnCheckItem.setActionCommand( "check item" );
        btnCheckItem.setEnabled( false );
        layoutChangeItem.setConstraints( btnCheckItem, c );
        pnlChangeItem.add( btnCheckItem );

        //c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets( 4, 2, 4, 2 );
        btnUncheckItem = new JButton( "Uncheck" );
        btnUncheckItem.addActionListener( controller );
        btnUncheckItem.setActionCommand( "uncheck item" );
        btnUncheckItem.setEnabled( false );
        layoutChangeItem.setConstraints( btnUncheckItem, c );
        pnlChangeItem.add( btnUncheckItem );

        /////////////////////////////// remove item ////////////////////////////////

        JPanel pnlRemoveItem = new JPanel();
        tab.add( "Remove Item", pnlRemoveItem );

        GridBagLayout layoutRemoveItem = new GridBagLayout();
        pnlRemoveItem.setLayout( layoutRemoveItem );

        c.weighty = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets( 12, 8, 4, 2 );
        lbl = new JLabel( "Index:" );
        layoutRemoveItem.setConstraints( lbl, c );
        pnlRemoveItem.add( lbl );

        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets( 12, 2, 4, 8 );
        txtIndex4 = new JTextField( "0", 2 );
        txtIndex4.setEnabled( false );
        layoutRemoveItem.setConstraints( txtIndex4, c );
        pnlRemoveItem.add( txtIndex4 );

        c.weightx = 1.0;
        c.insets = new Insets( 12, 0, 4, 8 );
        c.gridwidth = GridBagConstraints.REMAINDER;
        btnRemoveItem = new JButton( "Remove" );
        btnRemoveItem.addActionListener( controller );
        btnRemoveItem.setActionCommand( "remove item" );
        btnRemoveItem.setEnabled( false );
        layoutRemoveItem.setConstraints( btnRemoveItem, c );
        pnlRemoveItem.add( btnRemoveItem );

        c.weighty = 1.0;
        c.insets = new Insets( 8, 8, 4, 8 );
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.NORTHWEST;
        btnRemoveAll = new JButton( "Remove All" );
        btnRemoveAll.addActionListener( controller );
        btnRemoveAll.setActionCommand( "remove all" );
        btnRemoveAll.setEnabled( false );
        layoutRemoveItem.setConstraints( btnRemoveAll, c );
        pnlRemoveItem.add( btnRemoveAll );

        setVisible(true);

        btnDisableItem.setPreferredSize( btnUncheckItem.getSize() );
    }
}
