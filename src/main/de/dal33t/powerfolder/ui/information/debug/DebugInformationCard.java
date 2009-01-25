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
import java.io.IOException;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.TextPanel;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.logging.LoggingManager;

/**
 * Debug panel shows buttons to shutdown/start the FileRquestor, TransferManager
 * and the NodeManager.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.0 $
 */

public class DebugInformationCard extends InformationCard{
	
	private JPanel uiComponent;
    private TextPanel textPanel;

    private JButton shutdownFileRequestorButton;
    private JButton startFileRequestorButton;

    private JButton shutdownTransferManagerButton;
    private JButton startTransferManagerButton;
    private JToggleButton suspendEventsTransferManagerToggleButton;

    private JButton shutdownNodeManagerButton;
    private JButton startNodeManagerButton;
    private JToggleButton suspendEventsNodeManagerToggleButton;
    
    private JButton shutdownFolderRepository;
    private JButton startFolderRepository;
    private JToggleButton suspendEventsFolderRepositoryToggleButton;
    
    private JButton shutdownConnectionListener;
    private JButton startConnectionListener;
        
    private JButton shutdownBroadcastMananger;
    private JButton startBroadcastMananger;
    
    private JButton openDebugDir;
    
    private JComboBox logLevelCombo;
    
    private JCheckBox logToFileCheckBox;

    private JCheckBox scrollLockCheckBox;
    private JCheckBox showDebugReportsCheckBox;

    public static final String showDebugReportsPrefKey = "Debug.showDebugReports";

	public DebugInformationCard(Controller controller) {
		super(controller);
	}

	@Override
	public Image getCardImage() {
		return Icons.DEBUG_IMAGE;
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
	
	private boolean showDebugReports() {
        Preferences pref = getController().getPreferences();
        return pref.getBoolean(showDebugReportsPrefKey, false);
    }

    private void setShowDebugReports(boolean show) {
        Preferences pref = getController().getPreferences();
        pref.putBoolean(showDebugReportsPrefKey, show);
    }
	
	   private void initialize() {
	        textPanel = new TextPanel();
	        textPanel.setText(LoggingManager.getLogBuffer(), true);
	        logLevelCombo = new JComboBox();
	        logLevelCombo.addItem(Level.OFF);
	        logLevelCombo.addItem(Level.SEVERE);
	        logLevelCombo.addItem(Level.WARNING);
	        logLevelCombo.addItem(Level.INFO);
	        logLevelCombo.addItem(Level.FINE);
	        logLevelCombo.addItem(Level.FINER);
	        logLevelCombo.setSelectedItem(LoggingManager.getDocumentLoggingLevel());

	        logToFileCheckBox = new JCheckBox("Write log files");
	        scrollLockCheckBox = new JCheckBox("Scroll lock");

	        showDebugReportsCheckBox = new JCheckBox("Show debug reports");
	        showDebugReportsCheckBox
	                .setToolTipText("Toggles between Chat and Debut reports if clicked on user in tree");
	        updateBoxes();

	        ItemListener itemListener = new ItemListener() {
	            public void itemStateChanged(ItemEvent e) {
	                if (e.getSource() == logLevelCombo) {
	                    Object selectedItem = logLevelCombo.getSelectedItem();
	                    LoggingManager.setDocumentLogging((Level) selectedItem);
	                } else if (e.getSource() == logToFileCheckBox) {
	                    if(logToFileCheckBox.isSelected()){
	                    	LoggingManager.setFileLogging(LoggingManager.getDocumentLoggingLevel());//need to check
	                    }
	                } else if (e.getSource() == scrollLockCheckBox) {
	                    textPanel.setAutoScroll(!scrollLockCheckBox.isSelected());
	                } else if (e.getSource() == showDebugReportsCheckBox) {
	                    setShowDebugReports(showDebugReportsCheckBox.isSelected());
	                }
	            }
	        };

	        logLevelCombo.addItemListener(itemListener);
	        logToFileCheckBox.addItemListener(itemListener);
	        scrollLockCheckBox.addItemListener(itemListener);
	        showDebugReportsCheckBox.addItemListener(itemListener);

	        shutdownFileRequestorButton = new JButton();
	        shutdownFileRequestorButton.setIcon(Icons.STOP);
	        shutdownFileRequestorButton.setToolTipText("Shutdown FileRequestor");

	        startFileRequestorButton = new JButton();
	        startFileRequestorButton.setIcon(Icons.RUN);
	        startFileRequestorButton.setEnabled(false);
	        startFileRequestorButton.setToolTipText("Start FileRequestor");

	        shutdownTransferManagerButton = new JButton();
	        shutdownTransferManagerButton.setIcon(Icons.STOP);
	        shutdownTransferManagerButton.setToolTipText("Shutdown TransferManager");

	        startTransferManagerButton = new JButton();
	        startTransferManagerButton.setIcon(Icons.RUN);
	        startTransferManagerButton.setEnabled(false);
	        startTransferManagerButton.setToolTipText("Start TransferManager");

	        suspendEventsTransferManagerToggleButton = new JToggleButton();
	        suspendEventsTransferManagerToggleButton.setIcon(Icons.SUSPEND);
	        suspendEventsTransferManagerToggleButton
	                .setToolTipText("Suspend TransferManagerListeners");

	        shutdownNodeManagerButton = new JButton();
	        shutdownNodeManagerButton.setIcon(Icons.STOP);
	        shutdownNodeManagerButton.setToolTipText("Shutdown NodeManager");

	        startNodeManagerButton = new JButton();
	        startNodeManagerButton.setIcon(Icons.RUN);
	        startNodeManagerButton.setEnabled(false);
	        startNodeManagerButton.setToolTipText("Start NodeManager");

	        suspendEventsNodeManagerToggleButton = new JToggleButton();
	        suspendEventsNodeManagerToggleButton.setIcon(Icons.SUSPEND);
	        suspendEventsNodeManagerToggleButton
	                .setToolTipText("Suspend NodeManagerListeners");

	        shutdownFolderRepository = new JButton();
	        shutdownFolderRepository.setIcon(Icons.STOP);
	        shutdownFolderRepository.setToolTipText("Shutdown FolderRepository");

	        startFolderRepository = new JButton();
	        startFolderRepository.setIcon(Icons.RUN);
	        startFolderRepository.setEnabled(false);
	        startFolderRepository.setToolTipText("Start FolderRepository");

	        suspendEventsFolderRepositoryToggleButton = new JToggleButton();
	        suspendEventsFolderRepositoryToggleButton.setIcon(Icons.SUSPEND);
	        suspendEventsFolderRepositoryToggleButton
	                .setToolTipText("Suspend FolderRepositoryListeners");

	        shutdownConnectionListener = new JButton();
	        shutdownConnectionListener.setIcon(Icons.STOP);
	        shutdownConnectionListener.setToolTipText("Shutdown ConnectionListener");

	        startConnectionListener = new JButton();
	        startConnectionListener.setIcon(Icons.RUN);
	        startConnectionListener.setEnabled(false);
	        startConnectionListener.setToolTipText("Start ConnectionListener");

	        shutdownBroadcastMananger = new JButton();
	        shutdownBroadcastMananger.setIcon(Icons.STOP);
	        shutdownBroadcastMananger.setToolTipText("Shutdown BroadcastMananger");

	        startBroadcastMananger = new JButton();
	        startBroadcastMananger.setIcon(Icons.RUN);
	        startBroadcastMananger.setEnabled(false);
	        startBroadcastMananger.setToolTipText("Start BroadcastMananger");
	        
	        openDebugDir = new JButton("Send Logs");
	        openDebugDir.setEnabled(true);
	        openDebugDir.setToolTipText("Send log files to Support Team");


	        shutdownFileRequestorButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                getController().getFolderRepository().getFileRequestor()
	                        .shutdown();
	                shutdownFileRequestorButton.setEnabled(false);
	                startFileRequestorButton.setEnabled(true);
	            }
	        });

	        startFileRequestorButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                getController().getFolderRepository().getFileRequestor()
	                        .start();
	                shutdownFileRequestorButton.setEnabled(true);
	                startFileRequestorButton.setEnabled(false);
	            }
	        });

	        shutdownTransferManagerButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                getController().getTransferManager().shutdown();
	                shutdownTransferManagerButton.setEnabled(false);
	                startTransferManagerButton.setEnabled(true);
	            }
	        });

	        startTransferManagerButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                getController().getTransferManager().start();
	                shutdownTransferManagerButton.setEnabled(true);
	                startTransferManagerButton.setEnabled(false);
	            }
	        });

	        suspendEventsTransferManagerToggleButton
	                .addActionListener(new ActionListener() {
	                    public void actionPerformed(ActionEvent e) {
	                        boolean selected = suspendEventsTransferManagerToggleButton
	                                .isSelected();
	                        getController().getTransferManager().setSuspendFireEvents(
	                                selected);
	                    }
	                });

	        shutdownNodeManagerButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                getController().getNodeManager().shutdown();
	                shutdownNodeManagerButton.setEnabled(false);
	                startNodeManagerButton.setEnabled(true);
	            }
	        });

	        startNodeManagerButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                getController().getNodeManager().start();
	                getController().getReconnectManager().buildReconnectionQueue();
	                shutdownNodeManagerButton.setEnabled(true);
	                startNodeManagerButton.setEnabled(false);
	            }
	        });

	        suspendEventsNodeManagerToggleButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                boolean selected = suspendEventsNodeManagerToggleButton
	                        .isSelected();
	                getController().getNodeManager().setSuspendFireEvents(
	                        selected);
	            }
	        });


	        shutdownFolderRepository.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                getController().getFolderRepository().shutdown();
	                shutdownFolderRepository.setEnabled(false);
	                startFolderRepository.setEnabled(true);
	            }
	        });

	        startFolderRepository.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                getController().getFolderRepository().init();
	                getController().getFolderRepository().start();
	                shutdownFolderRepository.setEnabled(true);
	                startFolderRepository.setEnabled(false);
	            }
	        });

	        suspendEventsFolderRepositoryToggleButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                boolean selected = suspendEventsFolderRepositoryToggleButton
	                        .isSelected();
	                getController().getFolderRepository().setSuspendFireEvents(
	                        selected);
	            }
	        });

	        shutdownConnectionListener.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                getController().getConnectionListener().shutdown();
	                shutdownConnectionListener.setEnabled(false);
	                startConnectionListener.setEnabled(true);
	            }
	        });

	        startConnectionListener.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                try {
	                    getController().getConnectionListener().start();
	                } catch (ConnectionException ce) {
	                    logSevere("Problems starting listener "
	                            + getController().getConnectionListener(), ce);
	                }
	                shutdownConnectionListener.setEnabled(true);
	                startConnectionListener.setEnabled(false);
	            }
	        });

	        shutdownBroadcastMananger.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                getController().getBroadcastManager().shutdown();
	                shutdownBroadcastMananger.setEnabled(false);
	                startBroadcastMananger.setEnabled(true);
	            }
	        });

	        startBroadcastMananger.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                try {
	                    getController().getBroadcastManager().start();
	                } catch (ConnectionException ce) {
	                    logSevere("Problems starting manager "
	                            + getController().getBroadcastManager(), ce);
	                }
	                shutdownBroadcastMananger.setEnabled(true);
	                startBroadcastMananger.setEnabled(false);
	            }
	        });
	        
	        openDebugDir.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                try {
	                    FileUtils.openFile(LoggingManager.getDebugDir());
	                    BrowserLauncher
	                        .openURL(Constants.POWERFOLDER_SUPPORT_FILE_TICKET_URL);
	                } catch (IOException ex) {
	                	logSevere("Problems opening debug directory ", ex);
	                }
	            }
	        });
	    }

	    private void updateBoxes() {
	        logToFileCheckBox.setSelected(LoggingManager.isLogToFile());
	        scrollLockCheckBox.setSelected(!textPanel.isAutoScroll());
	        showDebugReportsCheckBox.setSelected(showDebugReports());
	    }
	    
	    public void buildUIComponent() {
	        
	            FormLayout layout = new FormLayout(
	                    //      2           4            6         8           10           12          14         16            18          20         22
	                    "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 8dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, fill:pref:grow",
	                    "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, fill:pref:grow");
	            PanelBuilder builder = new PanelBuilder(layout);
	            CellConstraints cc = new CellConstraints();

	            builder.add(new JLabel("FileRequestor"), cc.xy(2, 2));
	            builder.add(shutdownFileRequestorButton, cc.xy(4, 2));
	            builder.add(startFileRequestorButton, cc.xy(6, 2));

	            builder.add(new JLabel("TransferManager"), cc.xy(2, 4));
	            builder.add(shutdownTransferManagerButton, cc.xy(4, 4));
	            builder.add(startTransferManagerButton, cc.xy(6, 4));
	            builder.add(suspendEventsTransferManagerToggleButton, cc.xy(8, 4));

	            builder.add(new JLabel("NodeManager"), cc.xy(10, 2));
	            builder.add(shutdownNodeManagerButton, cc.xy(12, 2));
	            builder.add(startNodeManagerButton, cc.xy(14, 2));
	            builder.add(suspendEventsNodeManagerToggleButton, cc.xy(16, 2));

	            builder.add(new JLabel("FolderRepository"), cc.xy(10, 4));
	            builder.add(shutdownFolderRepository, cc.xy(12, 4));
	            builder.add(startFolderRepository, cc.xy(14, 4));
	            builder.add(suspendEventsFolderRepositoryToggleButton, cc.xy(16, 4));

	            builder.add(new JLabel("ConnectionListener"), cc.xy(18, 2));
	            builder.add(shutdownConnectionListener, cc.xy(20, 2));
	            builder.add(startConnectionListener, cc.xy(22, 2));

	            builder.add(new JLabel("BroadcastManager"), cc.xy(18, 4));
	            builder.add(shutdownBroadcastMananger, cc.xy(20, 4));
	            builder.add(startBroadcastMananger, cc.xy(22, 4));

	            builder.add(createToolBar(), cc.xywh(2, 6, 23, 1));
	            builder.add(textPanel.getUIComponent(), cc.xywh(1, 8, 24, 1));
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

	        // Only show the debug reports check box if the debug.reports ConfigurationEntry is enabled.
	        if (ConfigurationEntry.DEBUG_REPORTS.getValueBoolean(getController())) {
	            bar.addFixed(showDebugReportsCheckBox);
	            bar.addRelatedGap();
	        }

	        bar.addFixed(logToFileCheckBox);
	        bar.addRelatedGap();
	        bar.addFixed(scrollLockCheckBox);
	        bar.addRelatedGap();
	        bar.setBorder(Borders.createEmptyBorder("1dlu, 1dlu, 1dlu, 1dlu"));
	        return bar.getPanel();
	    }

}
