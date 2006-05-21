package de.dal33t.powerfolder.web;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Translation;

public class WebInterfaceOptionsDialog extends PFComponent {
    private WebInterface webInterface;
    private JDialog dialog;
    private JDialog parent;
    private JPanel panel;

    private JTextField portField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton okButton;
    private JButton cancelButton;
    
    public WebInterfaceOptionsDialog(WebInterface webInterface, Controller controller, JDialog parent) {
        super(controller);
        this.webInterface = webInterface;
        this.parent = parent;
    }

    public void open() {
        if (dialog == null) {
            dialog = new JDialog(parent, "WebInterface Settings", true);
            dialog.setContentPane(getUIComponent());
            dialog.pack();
            dialog.setResizable(false);
            Component parent = dialog.getOwner();
            int x = parent.getX() + (parent.getWidth() - dialog.getWidth()) / 2;
            int y = parent.getY() + (parent.getHeight() - dialog.getHeight())
                / 2;
            dialog.setLocation(x, y);
        }
        dialog.setVisible(true);
    }

    private JPanel getUIComponent() {
        if (panel == null) {
            initComponents();
            FormLayout layout = new FormLayout("3dlu, pref, 3dlu, pref, 3dlu",
                "3dlu, pref, 3dlu, pref, 3dlu, pref, 5dlu, pref, 3dlu");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(new JLabel("Port"), cc.xy(2, 2));
            builder.add(portField, cc.xy(4, 2));
            builder.add(new JLabel("Username"), cc.xy(2, 4));
            builder.add(usernameField, cc.xy(4, 4));
            builder.add(new JLabel("Password"), cc.xy(2, 6));
            builder.add(passwordField, cc.xy(4, 6));
            builder.add(getButtonBar(), cc.xywh(2, 8, 3, 1));
            panel = builder.getPanel();
        }
        return panel;
    }
    
    private void initComponents() {
        Properties props = getController().getConfig();  
        portField = new JTextField(props.getProperty(WebInterface.PORT_SETTING), 6);      
        usernameField = new JTextField(props.getProperty(WebInterface.USERNAME_SETTING), 12);
        passwordField = new JPasswordField(props.getProperty(WebInterface.PASSWORD_SETTING), 12);
        okButton = new JButton(Translation.getTranslation("general.ok"));
        okButton.setMnemonic(Translation.getTranslation("general.ok.key").charAt(0));
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Properties props = getController().getConfig();                
                props.put(WebInterface.PORT_SETTING, portField.getText());
                props.put(WebInterface.USERNAME_SETTING, usernameField.getText());                
                props.put(WebInterface.PASSWORD_SETTING, new String(passwordField.getPassword()));
                getController().saveConfig();
                webInterface.initProperties();
                webInterface.stop();
                webInterface.start();
                close();
            }            
        });
        cancelButton = new JButton(Translation.getTranslation("general.cancel"));
        cancelButton .setMnemonic(Translation.getTranslation("general.cancel.key").charAt(0));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               close();
            }            
        });

    }
    
    private void close() {
        dialog.setVisible(true);
        dialog.dispose();
    }
    
    private Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }
}
