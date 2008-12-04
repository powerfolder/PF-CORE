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
* $Id: ChatPanel.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.chat;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import java.awt.Color;

/**
 * Class to show a chat session with a member.
 */
public class ChatPanel extends PFComponent {

    private JPanel uiComponent;
    private JTextArea chatInput;
    private JTextPane chatOutput;
    private JScrollPane outputScrollPane;
    private JScrollPane inputScrollPane;
    private JPanel toolBar;

    /**
     * Constructor
     *
     * @param controller
     */
    public ChatPanel(Controller controller) {
        super(controller);
    }

    /**
     * Create the ui if required and return.
     *
     * @return
     */
    public JPanel getUiComponent() {
        if (uiComponent == null) {
            initialize();
            buildUiComponent();
        }
        return uiComponent;
    }

    /**
     * Build the ui.
     */
    private void buildUiComponent() {

        FormLayout layout = new FormLayout("fill:0:grow",
            "3dlu, pref, 3dlu, pref, 3dlu, fill:0:grow, 3dlu, pref, 3dlu, pref, 3dlu");
        //         tools       sep         me                 sep         you

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(toolBar, cc.xy(1, 2));
        builder.addSeparator(null, cc.xy(1, 4));
        builder.add(outputScrollPane, cc.xy(1, 6));
        builder.addSeparator(null, cc.xy(1, 8));
        builder.add(inputScrollPane, cc.xy(1, 10));
        uiComponent = builder.getPanel();
    }

    /**
     * Initialize the ui.
     */
    private void initialize() {
        createToolBar();
        createOutputComponents();
        createInputComponents();
    }

    /**
     * Create the tool bar.
     */
    private void createToolBar() {
        toolBar = new JPanel();
    }

    /**
     * Create the output components.
     */
    private void createOutputComponents() {
        chatOutput = new JTextPane();
        chatOutput.setEditable(false);
        outputScrollPane = new JScrollPane(chatOutput);
        chatOutput.getParent().setBackground(Color.WHITE);
        UIUtil.removeBorder(outputScrollPane);
    }

    /**
     * Create the input components.
     */
    private void createInputComponents() {
        chatInput = new JTextArea(5, 30);
        chatInput.setEditable(true);
        inputScrollPane = new JScrollPane(chatInput);
        UIUtil.removeBorder(inputScrollPane);
    }
}
