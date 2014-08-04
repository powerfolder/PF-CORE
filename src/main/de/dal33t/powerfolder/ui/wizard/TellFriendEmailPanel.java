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
 * $Id: TellFriendPanel.java 9331 2009-09-05 01:40:23Z tot $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;

/**
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0$
 */
public class TellFriendEmailPanel extends PFWizardPanel {

    private JTextArea emailTextArea;
    private JTextArea personalMessageTextArea;

    public TellFriendEmailPanel(Controller controller) {
        super(controller);
    }

    protected JComponent buildContent() {
        FormLayout layout = new FormLayout("140dlu, pref:grow",
            "pref, 3dlu, 40dlu, 6dlu, pref, 3dlu, 40dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();
        int row = 1;

        builder.add(new JLabel(Translation
            .getTranslation("exp.wizard.tell_friend.add_email_address")), cc.xyw(1,
            row, 2));
        row += 2;

        JScrollPane scrollPane = new JScrollPane(emailTextArea);
        scrollPane.setPreferredSize(new Dimension(50, 40));
        builder.add(scrollPane, cc.xy(1, row));
        row += 2;

        builder.add(new JLabel(Translation
            .getTranslation("exp.wizard.tell_friend.personal_message")), cc.xyw(1,
            row, 2));
        row += 2;

        scrollPane = new JScrollPane(personalMessageTextArea);
        scrollPane.setPreferredSize(new Dimension(50, 60));
        builder.add(scrollPane, cc.xy(1, row));

        return builder.getPanel();
    }

    protected String getTitle() {
        return Translation.getTranslation("exp.wizard.tell_friend.title");
    }

    protected void initComponents() {
        emailTextArea = new JTextArea();
        emailTextArea.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                updateButtons();
            }
        });
        personalMessageTextArea = new JTextArea();
    }

    public boolean hasNext() {
        return emailTextArea.getText().contains("@");
    }

    public WizardPanel next() {
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
            Translation.getTranslation("exp.wizard.tell_friend.title"), Translation
                .getTranslation("exp.wizard.tell_friend.success"), true);
        Runnable r = new Runnable() {
            public void run() {
                String[] emails = emailTextArea.getText().split("\n");
                getController().getOSClient().getAccountService().tellFriend(
                    Arrays.asList(emails), personalMessageTextArea.getText());
            }
        };
        return new SwingWorkerPanel(getController(), r, Translation
            .getTranslation("exp.wizard.tell_friend.sending"), Translation
            .getTranslation("exp.wizard.tell_friend.sending"), successPanel);
    }

    public boolean validateNext() {
        String[] emails = emailTextArea.getText().split("\\n");
        for (String email : emails) {
            email = email.trim();
            if (email.length() > 0) {
                if (!email.contains("@")) {
                    DialogFactory
                        .genericDialog(
                            getController(),
                            Translation
                                .getTranslation("exp.wizard.tell_friend.title.warning_title"),
                            Translation.getTranslation(
                                "exp.wizard.tell_friend.title.warning_bad_email",
                                email), GenericDialogType.ERROR);
                    return false;
                }
            }
        }
        return true;
    }
}