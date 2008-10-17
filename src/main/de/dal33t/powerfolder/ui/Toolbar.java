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

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Util;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main toolbar of the application.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class Toolbar extends PFUIComponent {

    private static final Logger log = Logger.getLogger(Toolbar.class.getName());

    /** Amount by which to reduce toolbar icons in small mode. */
    public static final double SMALL_ICON_SCALE_FACTOR = 0.5;

    private JComponent toolbar;
    private ValueModel textEnabledModel;
    private Boolean smallToolbar = PreferencesEntry.SMALL_TOOLBAR
        .getValueBoolean(getController());

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
     * @return the toolbar
     */
    private JComponent createToolbar() {
        JLabel customerIcon = new JLabel();
        if (smallToolbar) {
            customerIcon.setIcon(Icons.getIconById("customer_small.icon"));
        } else {
            customerIcon.setIcon(Icons.getIconById("customer_big.icon"));
        }

        // Build the toolbar
        JButton wizardButton = createToolbarButton(getUIController()
            .getOpenWizardAction(), Icons.WIZARD_OPEN);

        JButton searchNodesButton = createToolbarButton(getUIController()
            .getFindFriendAction(), Icons.SEARCH_NODES);

        JButton syncFoldersButton = createToolbarButton(getUIController()
            .getSyncAllFoldersAction(), Icons.SYNC_NOW_ACTIVE);

        JButton toggleSilentModeButton = createToolbarButton(getUIController()
            .getToggleSilentModeAction(), getController().isSilentMode()
            ? Icons.SLEEP
            : Icons.WAKE_UP);

        JButton preferencesButton = createToolbarButton(getUIController()
            .getOpenPreferencesAction(), Icons.PREFERENCES);

        JButton aboutButton = createToolbarButton(getUIController()
            .getOpenAboutAction(), Icons.ABOUT);

        JComponent buyProButton = null;
        if (!Util.isRunningProVersion() || Util.isTrial(getController())) {
            buyProButton = createBuyNowButton(Icons.BUY_PRO);
        }

        ButtonBarBuilder bar2 = ButtonBarBuilder.createLeftToRightBuilder();
        // bar2.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);
        if (customerIcon.getIcon() != null) {
            bar2.addFixed(customerIcon);
            bar2.addRelatedGap();
        }
        bar2.addFixed(syncFoldersButton);
        bar2.addRelatedGap();
        bar2.addFixed(wizardButton);
        bar2.addRelatedGap();
        bar2.addFixed(searchNodesButton);
        bar2.addRelatedGap();
        bar2.addFixed(toggleSilentModeButton);
        bar2.addRelatedGap();
        bar2.addFixed(preferencesButton);
        bar2.addRelatedGap();
        bar2.addFixed(aboutButton);

        FormLayout layout = new FormLayout("fill:pref, 0:grow, p", "pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(bar2.getPanel(), cc.xy(1, 1));

        if (buyProButton != null) {
            // bar2.addRelatedGap();
            // bar2.addFixed();
            builder.add(buyProButton, cc.xy(3, 1));
        }

        return builder.getPanel();
    }

    /**
     * @param icon
     * @return a clickable Buy now picto component with the given icon.
     */
    private JComponent createBuyNowButton(Icon icon) {
        if (smallToolbar) {
            icon = Icons.scaleIcon((ImageIcon) icon, SMALL_ICON_SCALE_FACTOR);
        }
        JComponent pictoComponent = new JLabel(icon);
        pictoComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    BrowserLauncher.openURL(Constants.POWERFOLDER_PRO_URL);
                } catch (IOException ex) {
                    log.log(Level.SEVERE,  "IOException", ex);
                }
            }
        });
        pictoComponent
            .setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return pictoComponent;
    }

    /**
     * Creates a button ready to be used on the toolbar. Overrides the default
     * icon if set and removes the text
     * 
     * @param action
     * @param icon
     * @return
     */
    private JButton createToolbarButton(final Action action, final Icon icon) {
        final JButton button = new JButton(action) {
            @Override
            public void setToolTipText(String text) {
                super.setToolTipText("<HTML><BODY><font size=4>&nbsp;" + text
                    + "&nbsp;</font></BODY></HTML>");
            }
        };
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        if (icon != null) {
            // Override icon
            button.setIcon(icon);
        }

        // Half-size images for toolbar
        if (smallToolbar) {
            if (button.getIcon() != null) {
                ImageIcon scaledImage = Icons.scaleIcon((ImageIcon) button
                    .getIcon(), SMALL_ICON_SCALE_FACTOR);
                button.setIcon(scaledImage);
            }
        }

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
        double scaleFactor = smallToolbar ? SMALL_ICON_SCALE_FACTOR : 1;
        if (lowScreenResolution) {
            dims = new Dimension((int) (scaleFactor * (Icons.NEW_FOLDER
                .getIconWidth() + 35)), (int) (scaleFactor * (Icons.NEW_FOLDER
                .getIconHeight() + 10)));
        } else {
            dims = new Dimension((int) (scaleFactor * (Icons.NEW_FOLDER
                .getIconWidth() + 50)), (int) (scaleFactor * (Icons.NEW_FOLDER
                .getIconHeight() + 25)));
        }
        button.setPreferredSize(dims);

        // Text handling
        if (Boolean.TRUE.equals(textEnabledModel.getValue()) && !smallToolbar) {
            button.setText((String) action.getValue(Action.NAME));
        } else {
            button.setText(null);
        }

        textEnabledModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (!smallToolbar) {
                    button.setText((String) action.getValue(Action.NAME));
                }
            }
        });

        return button;
    }

    // Helper methods *******************************************************

    /**
     * The components tooltip will immediately show up
     * 
     * @param comp
     */
    private void activeImmediateTooltip(JComponent comp) {
        ToolTipManager.sharedInstance().registerComponent(comp);
        comp.addMouseListener(new MouseAdapter() {
            private final String SHOW_TOOLTIP = "postTip";

            @Override
            public void mouseEntered(MouseEvent e) {
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
