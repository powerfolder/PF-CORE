/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;

/**
 * use DialogFactory to display this.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 */
public class ScrollableOkCancelDialog extends BaseDialog {
    private int choice = JOptionPane.CANCEL_OPTION;
    private String title;
    private String message;
    private String longText;
    private Icon icon;

    public ScrollableOkCancelDialog(Controller controller, boolean modal,
        boolean border, String title, String message, String longText, Icon icon)
    {
        super(controller, modal, border);
        this.title = title;
        this.message = message;
        this.longText = longText;
        this.icon = icon;
    }

    /**
     * @return the choice of the user, this is JOptionPane competible, either
     *         JOptionPane.OK_OPTION or JOptionPane.CANCEL_OPTION.
     */
    public int getChoice() {
        return choice;
    }

    @Override
    protected Component getButtonBar()
    {
        JButton okButton = createOKButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                choice = JOptionPane.OK_OPTION;
                setVisible(false);
            }
        });
        JButton cancelButton = createCancelButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                choice = JOptionPane.CANCEL_OPTION;
                setVisible(false);
            }
        });
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    @Override
    protected Component getContent()
    {
        JTextArea textArea = new JTextArea(longText, 10, 30);
        textArea.setBackground(Color.WHITE);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        FormLayout layout = new FormLayout("pref", "pref, pref");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();
        builder.add(LinkedTextBuilder.build(getController(), message).getPanel(),
                cc.xy(1, 1));
        builder.add(scrollPane, cc.xy(1, 2));
        return builder.getPanel();
    }

    @Override
    protected Icon getIcon()
    {
        return icon;
    }

    @Override
    public String getTitle()
    {
        return title;
    }
}
