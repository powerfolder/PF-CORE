/* $Id$
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.ui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;

import sun.font.FontManager;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;

/**
 * Main toolbar of the application.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class Toolbar extends PFUIComponent {
    private JComponent toolbar;
    private ValueModel textEnabledModel;

    protected Toolbar(Controller controller) {
        super(controller);
        textEnabledModel = new ValueHolder(Boolean.TRUE, true);
    }

    /**
     * @return the ui component of the toolbar
     */
    public JComponent getUIComponent() {
        if (toolbar == null) {
            toolbar = createToolbar();
        }
        return toolbar;
    }

    /**
     * Creates the toolbar
     * 
     * @return
     */
    private JComponent createToolbar() {
        // Build the toolbar
        JButton wizardButton = createToolbarButton(getUIController()
            .getOpenWizardAction(), Icons.WIZARD_OPEN);

        JButton createFolderButton = createToolbarButton(getUIController()
            .getFolderCreateAction(), Icons.NEW_FOLDER);

        JButton inviteToFolderButton = createToolbarButton(getUIController()
            .createToolbarInvitationAction());

        JButton syncFoldersButton = createSyncNowToolbarButton();

        JButton toggleSilentModeButton = createToolbarButton(getUIController()
            .getToggleSilentModeAction());

        JButton preferencesButton = createToolbarButton(getUIController()
            .getOpenPreferencesAction(), Icons.PREFERENCES);

        JButton aboutButton = createToolbarButton(getUIController()
            .getOpenAboutAction(), Icons.ABOUT);

        ButtonBarBuilder bar2 = ButtonBarBuilder.createLeftToRightBuilder();
        // bar2.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);
        bar2.addFixed(syncFoldersButton);
        bar2.addRelatedGap();
        bar2.addFixed(wizardButton);
        bar2.addRelatedGap();
        bar2.addFixed(createFolderButton);
        bar2.addRelatedGap();
        bar2.addFixed(inviteToFolderButton);
        bar2.addRelatedGap();
        bar2.addFixed(toggleSilentModeButton);
        bar2.addRelatedGap();
        bar2.addFixed(preferencesButton);
        bar2.addRelatedGap();
        bar2.addFixed(aboutButton);

        // Create toolbar
        // JComponent bar = ButtonBarFactory.buildLeftAlignedBar(new JButton[]{
        // wizardButton,
        // // connectButton,
        // createFolderButton, joinLeaverFolderButton, scanFoldersButton,
        // toggleSilentModeButton, preferencesButton, aboutButton});

        // SimpleInternalFrame toolbarFrame = new SimpleInternalFrame(null);
        // Border border = BorderFactory.createCompoundBorder(new
        // SimpleInternalFrame.ShadowBorder(), Borders.DLU4_BORDER);
        // bar.setBorder(border);
        // bar.setBackground(Color.WHITE);
        // bar.setBorder(Borders.DLU4_BORDER);
        // toolbarFrame.add(bar);

        return bar2.getPanel();
    }

    /**
     * Creates a button ready to be used on the toolbar
     * 
     * @param action
     * @param mNemonic
     * @return
     */
    private JButton createToolbarButton(Action action) {
        return createToolbarButton(action, null);
    }

    /**
     * Creates a button ready to be used on the toolbar. Overrides the default
     * icon if set and removes the text
     * 
     * @param action
     * @param mNemonic
     * @return
     */
    private JButton createToolbarButton(final Action action, final Icon icon) {
        final JButton button = new JButton(action);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        if (icon != null) {
            // Override icon
            button.setIcon(icon);
        }

        String tooltip = button.getToolTipText();

        // #FDE592
        String fontname = FontManager.getDefaultPhysicalFont()
            .getPostscriptName();
        tooltip = "<HTML><BODY bgcolor=\"#FFF6DA\"><font size=4 FACE=\""
            + fontname + "\">&nbsp;" + tooltip + "&nbsp;</font></BODY></HTML>";
        button.setToolTipText(tooltip);

        // Toolbar icons do not have texts!
        button.addPropertyChangeListener(new PropertyChangeListener() {
            boolean changeFromHere = false;

            public void propertyChange(PropertyChangeEvent evt) {
                if (!changeFromHere
                    && JButton.ICON_CHANGED_PROPERTY.equals(evt
                        .getPropertyName()))
                {
                    // log().warn("Button change: " + evt);
                    // String newText = (String) evt.getNewValue();
                    // if (!StringUtils.isEmpty(newText)) {
                    // changeFromHere = true;
                    // //button.setText(null);
                    // button.setIcon(icon);
                    // changeFromHere = false;
                    // }

                }
            }
        });
        activeImmediateTooltip(button);

        // Display tooltip immediately instead of waiting the default 1-2
        // seconds that Swing defaults to

        // button.setDisplayedMnemonicIndex()
        boolean lowScreenResolution = Toolkit.getDefaultToolkit()
            .getScreenSize().getWidth() < 800;
        Dimension dims;
        if (!lowScreenResolution) {
            dims = new Dimension(Icons.NEW_FOLDER.getIconWidth() + 50,
                Icons.NEW_FOLDER.getIconHeight() + 25);
        } else {
            dims = new Dimension(Icons.NEW_FOLDER.getIconWidth() + 35,
                Icons.NEW_FOLDER.getIconHeight() + 10);
        }
        button.setPreferredSize(dims);
        
        // Text handling
        if (Boolean.TRUE.equals(textEnabledModel.getValue())) {
            button.setText((String) action.getValue(Action.NAME));
        } else {
            button.setText(null);
        }

        textEnabledModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                button.setText((String) action.getValue(Action.NAME));
            }
        }); 

        return button;
    }

    /**
     * Create the syncnow button. State is adapted from folderepository (if
     * scanning)
     * 
     * @return
     */
    private JButton createSyncNowToolbarButton() {
        final JButton syncNowButton = createToolbarButton(getUIController()
            .getScanAllFoldersAction(), Icons.SYNC_NOW);

        // Adapt state from folder repository
        getController().getFolderRepository().addFolderRepositoryListener(
            new FolderRepositoryListener() {
                public void unjoinedFolderAdded(FolderRepositoryEvent e) {
                }

                public void folderRemoved(FolderRepositoryEvent e) {
                }

                public void folderCreated(FolderRepositoryEvent e) {
                }

                public void scansStarted(FolderRepositoryEvent e) {
                    syncNowButton.setIcon(Icons.SYNC_NOW_ACTIVE);
                }

                public void scansFinished(FolderRepositoryEvent e) {
                    syncNowButton.setIcon(Icons.SYNC_NOW);
                }

                public void unjoinedFolderRemoved(FolderRepositoryEvent e) {
                }
            });

        return syncNowButton;
    }

    // Helper methods *******************************************************

    /**
     * The components tooltip will immediately show up
     * @param comp
     */
    private void activeImmediateTooltip(JComponent comp) {
        ToolTipManager.sharedInstance().registerComponent(comp);
        comp.addMouseListener(new MouseAdapter() {
            private final String SHOW_TOOLTIP = "postTip";

            @Override
            public void mouseEntered(MouseEvent e)
            {
                JComponent c = (JComponent) e.getComponent();
                Action action = c.getActionMap().get(SHOW_TOOLTIP);

                if (action != null) {
                    action.actionPerformed(new ActionEvent(c,
                        ActionEvent.ACTION_PERFORMED, SHOW_TOOLTIP));
                }
            }
        });
    }

}
