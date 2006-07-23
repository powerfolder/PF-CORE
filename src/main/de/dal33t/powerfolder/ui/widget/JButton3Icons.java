package de.dal33t.powerfolder.ui.widget;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JButton;

    
public class JButton3Icons extends JButton {
    
    public JButton3Icons(Icon normalIcon, Icon hoverIcon, Icon pushIcon) {
        super(normalIcon);
        setOpaque(false);
        setBorder(null);
        setBackground(Color.WHITE);
        setPressedIcon(pushIcon);
        setRolloverIcon(hoverIcon);
        
    }
}
