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
package de.dal33t.powerfolder.ui.notification;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Class representing the message notification form.
 */
public class NotificationForm extends JPanel {

    /**
     * Constructor. Displays a panel with title and message. Also accept and
     * optional cancel button.
     *
     * @param titleText
     * @param messageText
     * @param acceptOptionLabel
     * @param acceptAction
     * @param cancelOptionLabel
     * @param cancelAction
     */
    NotificationForm(String titleText, String messageText, String acceptOptionLabel,
                     Action acceptAction, String cancelOptionLabel,
                     Action cancelAction, boolean showAccept) {
        setLayout(new BorderLayout());
        JPanel jPanel = createPanel(titleText, messageText, acceptOptionLabel,
                acceptAction, cancelOptionLabel, cancelAction, showAccept);
        add(jPanel, BorderLayout.CENTER);
        setBorder(new LineBorder(Color.lightGray, 1));
    }

    /**
     * Create the UI for notification form
     */
    private static JPanel createPanel(String titleText, String msgText,
                                      String acceptOptionLabel,
                                      Action acceptAction,
                                      String cancelOptionLabel,
                                      Action cancelAction,
                                      boolean showAccept) {
        JPanel jPanel = new JPanel();
        jPanel.setBackground(Color.WHITE);
        CellConstraints cc = new CellConstraints();

        JButton button = new JButton();
        button.setAction(acceptAction);
        button.setText(acceptOptionLabel);
        FormLayout formLayout;
        int internalWidth;
        int[] cols;
        if (cancelOptionLabel == null) {
            formLayout = new FormLayout(
                    "fill:7px:none, fill:85px:none, fill:80px:none, fill:85px:none, fill:7px:none",
                    "center:40px:none, center:70px:none, center:12px:none, center:default:none, center:default:none");
            internalWidth = 3;
            jPanel.setLayout(formLayout);
            if (showAccept) {
                jPanel.add(button, cc.xy(3, 4));
            }
            cols = new int[]{2, 3, 4};
        } else {
            formLayout = new FormLayout(
                    "fill:7px:none, fill:30px:none, fill:80px:none, fill:30px:none, fill:80px:none, fill:30px:none, fill:7px:none",
                    "center:40px:none, center:70px:none, center:12px:none, center:default:none, center:default:none");
            internalWidth = 5;
            jPanel.setLayout(formLayout);
            jPanel.add(button, cc.xy(3, 4));

            if (showAccept) {
                button = new JButton();
                button.setAction(cancelAction);
                button.setText(cancelOptionLabel);
                jPanel.add(button, cc.xy(5, 4));
            }
            cols = new int[]{2, 3, 4, 5, 6};
        }

        jPanel.add(createHeaderPanel(titleText), cc.xywh(2, 1, internalWidth, 1));

        JTextArea textArea = new JTextArea(msgText);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        jPanel.add(textArea, new CellConstraints(2, 2, internalWidth, 1,
                CellConstraints.DEFAULT, CellConstraints.TOP));

        addFillComponents(jPanel, cols, new int[]{1, 2, 3, 4, 5});
        return jPanel;
    }

    private static void addFillComponents(Container panel, int[] cols, int[] rows) {
        Dimension filler = new Dimension(10, 10);

        boolean doneColumnOne = false;
        CellConstraints cc = new CellConstraints();
        if (cols.length > 0 && rows.length > 0) {
            if (cols[0] == 1 && rows[0] == 1) {
                /** add a rigid area */
                panel.add(Box.createRigidArea(filler), cc.xy(1, 1));
                doneColumnOne = true;
            }
        }

        for (int col : cols) {
            if (col == 1 && doneColumnOne) {
                continue;
            }
            panel.add(Box.createRigidArea(filler), cc.xy(col, 1));
        }

        for (int row : rows) {
            if (row == 1 && doneColumnOne) {
                continue;
            }
            panel.add(Box.createRigidArea(filler), cc.xy(1, row));
        }

    }

    /**
     * Create header subpanel with icon and text
     */
    private static JPanel createHeaderPanel(String title) {
        JPanel jPanel = new JPanel();
        jPanel.setBackground(null);
        FormLayout formlayout1 = new FormLayout("fill:default:none, fill:7px:none, fill:default:none",
                "center:default:none");
        CellConstraints cc = new CellConstraints();
        jPanel.setLayout(formlayout1);

        JLabel jLabel = new JLabel();
        jLabel.setText(Translation.getTranslation("notification_form.title",
            title));
        jLabel.setHorizontalAlignment(SwingConstants.LEFT);
        jPanel.add(jLabel, cc.xy(3, 1));

        JLabel logo = new JLabel(Icons.SMALL_LOGO);
        logo.setSize(new Dimension(
            Icons.SMALL_LOGO.getIconWidth(),
            Icons.SMALL_LOGO.getIconHeight()));
        jPanel.add(logo, cc.xy(1, 1));

        addFillComponents(jPanel, new int[]{1, 2}, new int[]{1});
        return jPanel;
    }
}
