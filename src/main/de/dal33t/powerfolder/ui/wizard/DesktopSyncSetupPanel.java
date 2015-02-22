/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.ui.wizard;

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.BACKUP_ONLINE_STOARGE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_CREATE_ITEMS;

import java.awt.event.ActionEvent;
import java.util.Collections;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.UserDirectories;
import de.dal33t.powerfolder.util.UserDirectory;

@SuppressWarnings("serial")
public class DesktopSyncSetupPanel extends PFWizardPanel {

    private WizardPanel nextPanel;
    private JTextArea infoLabel;
    private ActionLabel agreeLabel;
    private ActionLabel skipLabel;
    private JCheckBox wallpaperBox;
    private boolean agreed;

    public DesktopSyncSetupPanel(Controller controller, WizardPanel nextPanel) {
        super(controller);
        Reject.ifNull(nextPanel, "Nextpanel is null");
        this.nextPanel = nextPanel;
        this.agreed = false;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public WizardPanel next() {
        if (!agreed) {
            return nextPanel;
        }
        return new FolderCreatePanel(getController());
        // return new SwingWorkerPanel(getController(), new SetupTask(),
        // Translation.getTranslation("wizard.desktop_sync.setting_up"),
        // Translation.getTranslation("wizard.desktop_sync.setting_up.text"),
        // nextPanel);
    }

    @Override
    protected String getTitle() {
        return Translation.getTranslation("wizard.desktop_sync.title");
    }

    @Override
    protected JComponent buildContent() {
        FormLayout layout = new FormLayout("9dlu, 120dlu:grow",
            "pref, 9dlu, pref, 1dlu, pref, 10dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.add(infoLabel, cc.xyw(1, row, 2));
        row += 2;
        builder.add(agreeLabel.getUIComponent(), cc.xyw(1, row, 2));
        row += 2;
        builder.add(wallpaperBox, cc.xyw(2, row, 1));
        row += 2;
        builder.add(skipLabel.getUIComponent(), cc.xyw(2, row, 1));
        row += 2;

        return builder.getPanel();
    }

    @Override
    protected void initComponents() {
        infoLabel = new JTextArea(
            Translation.getTranslation("wizard.desktop_sync.info_text"));
        infoLabel.setEditable(false);
        infoLabel.setCursor(null);
        infoLabel.setOpaque(false);
        infoLabel.setFocusable(false);
        infoLabel.setFont(UIManager.getFont("Label.font"));
        infoLabel.setEditable(false);
        infoLabel.setOpaque(false);
        infoLabel.setWrapStyleWord(true);
        infoLabel.setLineWrap(true);

        agreeLabel = new ActionLabel(getController(), new AgreeAction(
            getController()));
        agreeLabel.convertToBigLabel();

        wallpaperBox = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("wizard.desktop_sync.wallpaper"));
        wallpaperBox.setSelected(true);

        skipLabel = new ActionLabel(getController(), new SkipAction(
            getController()));
        skipLabel.convertToBigLabel();
        skipLabel.setIcon(null);

    }

    private class AgreeAction extends BaseAction {

        protected AgreeAction(Controller controller) {
            super("action_desktop_sync.agree", controller);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            agreed = true;

            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, nextPanel);
            
            getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE, true);
            
            UserDirectory desktopDir = UserDirectories.getUserDirectories()
                .get(Translation.getTranslation("user.dir.desktop"));
            FolderCreateItem item = new FolderCreateItem(
                desktopDir.getDirectory());
            getWizardContext().setAttribute(FOLDER_CREATE_ITEMS,
                Collections.singletonList(item));
            getWizard().next();
        }
    }

    private class SkipAction extends BaseAction {
        protected SkipAction(Controller controller) {
            super("action_desktop_sync.skip", controller);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            getWizard().next();
        }
    }
}
