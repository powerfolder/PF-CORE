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
 * $Id: PauseDialog.java 17944 2012-01-31 12:10:03Z harry $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.util.Translation;

/**
 * Dialog to let the user decide how long to pause for.
 */
public class PauseDialog extends BaseDialog {

    private static final Map<Integer, String> PAUSE_RESUME_VALUES;

    static {
        PAUSE_RESUME_VALUES = new TreeMap<Integer, String>();
        PAUSE_RESUME_VALUES.put(0,
            Translation.getTranslation("pause_dialog.pause.user_inactive"));
        PAUSE_RESUME_VALUES.put(5 * 60,
            Translation.getTranslation("pause_dialog.pause.5minutes"));
        PAUSE_RESUME_VALUES.put(3600,
            Translation.getTranslation("pause_dialog.pause.1hour"));
        PAUSE_RESUME_VALUES.put(8 * 3660,
            Translation.getTranslation("pause_dialog.pause.8hours"));
        PAUSE_RESUME_VALUES.put(Integer.MAX_VALUE,
            Translation.getTranslation("pause_dialog.pause.permanent"));
    }

    private JButton pauseButton;
    private JCheckBox neverAskAgainCB;
    private JComboBox<String> pauseValuesCombo;

    public PauseDialog(Controller controller) {
        super(Senior.NONE, controller, true);
    }

    public String getTitle() {
        return Translation.getTranslation("pause_dialog.title");
    }

    protected Icon getIcon() {
        return null;
    }

    protected JComponent getContent() {
        pauseButton = new JButton(
            Translation.getTranslation("pause_dialog.button.text"));
        pauseButton.setMnemonic(Translation
            .getTranslation("pause_dialog.button.text").trim().charAt(0));
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pause();
            }
        });

        neverAskAgainCB = new JCheckBox(
            Translation.getTranslation("general.neverAskAgain"));
        neverAskAgainCB.setMnemonic(Translation.getTranslation(
            "general.neverAskAgain").charAt(0));

        int pauseResumeSeconds = ConfigurationEntry.PAUSE_RESUME_SECONDS
            .getValueInt(getController());
        pauseValuesCombo = new JComboBox<>();
        int i = 0;
        for (Map.Entry<Integer, String> entry : PAUSE_RESUME_VALUES.entrySet())
        {
            pauseValuesCombo.addItem(entry.getValue());
            if (pauseResumeSeconds == entry.getKey()) {
                pauseValuesCombo.setSelectedIndex(i);
            }
            i++;
        }

        FormLayout layout = new FormLayout("pref, 3dlu, pref:grow",
            "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(
            new JLabel(Translation
                .getTranslation("pause_dialog.pause_label.text")), cc.xy(1, 1));
        builder.add(pauseValuesCombo, cc.xy(3, 1));

        builder.add(neverAskAgainCB, cc.xyw(1, 3, 3));
        return builder.getPanel();
    }

    private void pause() {
        if (neverAskAgainCB.isSelected()) {
            PreferencesEntry.SHOW_ASK_FOR_PAUSE
                .setValue(getController(), false);
        }

        int i = 0;
        for (Integer pauseTime : PAUSE_RESUME_VALUES.keySet()) {
            if (i == pauseValuesCombo.getSelectedIndex()) {
                ConfigurationEntry.PAUSE_RESUME_SECONDS.setValue(
                    getController(), pauseTime);
                getController().saveConfig();
                break;
            }
            i++;
        }

        getController().setPaused(!getController().isPaused());

        dialog.dispose();
    }

    protected JButton getDefaultButton() {
        return pauseButton;
    }

    protected Component getButtonBar() {
        JButton cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        return ButtonBarFactory.buildCenteredBar(pauseButton, cancelButton);
    }

}
