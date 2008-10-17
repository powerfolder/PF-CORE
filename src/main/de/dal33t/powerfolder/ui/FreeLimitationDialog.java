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
package de.dal33t.powerfolder.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;

import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A dialog that gets displayed when the free version hits its limits.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FreeLimitationDialog extends BaseDialog {

    private static final Logger log = Logger.getLogger(FreeLimitationDialog.class.getName());

    protected FreeLimitationDialog(Controller controller) {
        super(controller, false);
    }

    @Override
    protected Icon getIcon()
    {
        return Icons.PRO_LOGO;
    }

    @Override
    public String getTitle()
    {
        return Translation.getTranslation("free_limit_dialog.title");
    }

    @Override
    protected Component getButtonBar()
    {
        return buildToolbar();
    }

    @Override
    protected Component getContent()
    {
        FormLayout layout = new FormLayout("pref:grow",
            "pref, 2dlu, pref, 14dlu, pref, 3dlu, pref, 3dlu, pref, 14dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("0, 0, 14dlu, 0"));

        CellConstraints cc = new CellConstraints();
        int row = 1;
        builder.addLabel(Translation
            .getTranslation("free_limit_dialog.heavy_usage_detected"), cc
            .xy(1, row));
        row += 2;
        builder.addLabel(Translation.getTranslation("free_limit_dialog.reason",
            3, 10), cc.xy(1, row));
        row += 2;
        builder
            .addLabel(Translation
                .getTranslation("free_limit_dialog.buy_recommendation"), cc.xy(1,
                row));
        row += 2;
        builder.addLabel(Translation
            .getTranslation("free_limit_dialog.buy_recommendation2"), cc
            .xy(1, row));
        row += 2;
        builder.addLabel(Translation
            .getTranslation("free_limit_dialog.buy_recommendation3"), cc
            .xy(1, row));
        row += 2;
        LinkLabel linkLabel = new LinkLabel(Translation
            .getTranslation("free_limit_dialog.whatispro"),
            Constants.POWERFOLDER_PRO_URL);
        builder.add(linkLabel, cc.xy(1, row));

        return builder.getPanel();
    }

    private Component buildToolbar() {
        JButton buyProButton = new JButton(Translation
            .getTranslation("free_limit_dialog.buy_pro"));
        buyProButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    BrowserLauncher.openURL(Constants.POWERFOLDER_PRO_URL);
                } catch (IOException e1) {
                    log.log(Level.SEVERE, "IOException", e1);
                }
            }
        });

        JButton reduceButton = new JButton(Translation
            .getTranslation("free_limit_dialog.reduce"));
        reduceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        return ButtonBarFactory.buildCenteredBar(buyProButton, reduceButton);
    }

}
