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
 * $Id: DebugPanel.java 6135 2008-12-24 08:04:17Z harry $
 */
package de.dal33t.powerfolder.ui.information.debug;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.ui.information.InformationCardType;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.logging.LoggingManager;

/**
 * Debug panel shows buttons to shutdown/start the FileRquestor, TransferManager
 * and the NodeManager.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.0 $
 */

public class DebugInformationCard extends InformationCard {

    private JPanel uiComponent;
    private TextPanel textPanel;

    private JButton openDebugDir;
    private JButton dumpThreads;

    private JComboBox<Level> logLevelCombo;

    private JCheckBox logToFileCheckBox;

    private JCheckBox scrollLockCheckBox;

    public DebugInformationCard(Controller controller) {
        super(controller);
    }

    public InformationCardType getInformationCardType() {
        return InformationCardType.DEBUG;
    }

    @Override
    public Image getCardImage() {
        return Icons.getImageById(Icons.DEBUG);
    }

    @Override
    public String getCardTitle() {
        return Translation.getTranslation("debug_information_card.title");
    }

    @Override
    public JComponent getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    private void initialize() {
        textPanel = new TextPanel();
        textPanel.setText(LoggingManager.getLogBuffer(), true);
        logLevelCombo = new JComboBox<>();
        logLevelCombo.addItem(Level.OFF);
        logLevelCombo.addItem(Level.SEVERE);
        logLevelCombo.addItem(Level.WARNING);
        logLevelCombo.addItem(Level.INFO);
        logLevelCombo.addItem(Level.FINE);
        logLevelCombo.addItem(Level.FINER);
        logLevelCombo.setSelectedItem(LoggingManager.getDocumentLoggingLevel());

        logToFileCheckBox = new JCheckBox("Write log files");
        scrollLockCheckBox = new JCheckBox("Scroll lock");

        updateBoxes();

        ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getSource() == logLevelCombo) {
                    Object selectedItem = logLevelCombo.getSelectedItem();
                    LoggingManager.setDocumentLogging((Level) selectedItem,
                        getController());
                } else if (e.getSource() == logToFileCheckBox) {
                    if (logToFileCheckBox.isSelected()) {
                        LoggingManager.setFileLogging(LoggingManager
                            .getDocumentLoggingLevel(),
                            ConfigurationEntry.LOG_LEVEL_CONSOLE
                                .getValueBoolean(getController()));// need to
                                                                   // check
                    }
                } else if (e.getSource() == scrollLockCheckBox) {
                    textPanel.setAutoScroll(!scrollLockCheckBox.isSelected());
                }
            }
        };

        logLevelCombo.addItemListener(itemListener);
        logToFileCheckBox.addItemListener(itemListener);
        scrollLockCheckBox.addItemListener(itemListener);

        openDebugDir = new JButton("Open Logs");
        openDebugDir.setEnabled(true);
        openDebugDir.setToolTipText("Send log files to Support Team");

        dumpThreads = new JButton("CPU dump");
        dumpThreads.setToolTipText("Dumps the current CPU activity to logs");

        openDebugDir.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        PathUtils.openFile(LoggingManager.getDebugDir());
                        BrowserLauncher.openURL(getController(),
                            ConfigurationEntry.PROVIDER_SUPPORT_FILE_TICKET_URL
                                .getValue(getController()));
                    }
                };
                getController().getIOProvider().startIO(r);
            }
        });

        dumpThreads.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (((Level) logLevelCombo.getSelectedItem()).intValue() > Level.INFO
                    .intValue())
                {
                    logLevelCombo.setSelectedItem(Level.INFO);
                }
                String dump = Debug.dumpCurrentStacktraces(false);
                if (StringUtils.isNotBlank(dump)) {
                    logInfo("Active threads:\n\n" + dump);
                } else if (isInfo()) {
                    logInfo("No threads active");
                }
            }
        });
    }

    private void updateBoxes() {
        logToFileCheckBox.setSelected(LoggingManager.isLogToFile());
        scrollLockCheckBox.setSelected(!textPanel.isAutoScroll());
    }

    public void buildUIComponent() {

        FormLayout layout = new FormLayout(
            // 2 4 6 8 10 12 14 16 18 20 22
            "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 8dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, fill:pref:grow",
            "3dlu, pref, 3dlu, fill:pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createToolBar(), cc.xywh(2, 2, 23, 1));
        builder.add(textPanel.getUIComponent(), cc.xywh(1, 4, 24, 1));

        uiComponent = builder.getPanel();

    }

    private JPanel createToolBar() {
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addFixed(openDebugDir);
        bar.addRelatedGap();
        bar.addFixed(new JLabel("Level"));
        bar.addRelatedGap();
        bar.addFixed(logLevelCombo);
        bar.addRelatedGap();
        bar.addFixed(logToFileCheckBox);
        bar.addRelatedGap();
        bar.addFixed(scrollLockCheckBox);
        bar.addUnrelatedGap();
        bar.addFixed(dumpThreads);
        return bar.getPanel();
    }

}
