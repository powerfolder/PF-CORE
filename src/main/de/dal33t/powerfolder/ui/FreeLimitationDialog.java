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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JButton;

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

/**
 * A dialog that gets displayed when the free version hits its limits.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FreeLimitationDialog extends BaseDialog {

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
        return Translation.getTranslation("freelimitdialog.title");
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
            .getTranslation("freelimitdialog.heavyusagedetected"), cc
            .xy(1, row));
        row += 2;
        builder.addLabel(Translation.getTranslation("freelimitdialog.reason",
            3, 10), cc.xy(1, row));
        row += 2;
        builder
            .addLabel(Translation
                .getTranslation("freelimitdialog.buyrecommendation"), cc.xy(1,
                row));
        row += 2;
        builder.addLabel(Translation
            .getTranslation("freelimitdialog.buyrecommendation2"), cc
            .xy(1, row));
        row += 2;
        builder.addLabel(Translation
            .getTranslation("freelimitdialog.buyrecommendation3"), cc
            .xy(1, row));
        row += 2;
        LinkLabel linkLabel = new LinkLabel(Translation
            .getTranslation("freelimitdialog.whatispro"),
            Constants.POWERFOLDER_PRO_URL);
        linkLabel.setBorder(Borders.createEmptyBorder("0, 1px, 0, 0"));
        builder.add(linkLabel, cc.xy(1, row));

        return builder.getPanel();
    }

    private Component buildToolbar() {
        JButton buyProButton = new JButton(Translation
            .getTranslation("freelimitdialog.buypro"));
        buyProButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    BrowserLauncher.openURL(Constants.POWERFOLDER_PRO_URL);
                } catch (IOException e1) {
                    log().error(e1);
                }
            }
        });

        JButton reduceButton = new JButton(Translation
            .getTranslation("freelimitdialog.reduce"));
        reduceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        return ButtonBarFactory.buildCenteredBar(buyProButton, reduceButton);
    }

}
