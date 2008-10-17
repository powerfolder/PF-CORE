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
package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.Toolbar;

import javax.swing.Action;
import javax.swing.ImageIcon;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

/**
 * Action which enables/disables silent mode
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class ToggleSilentModeAction extends BaseAction {

    private static final Logger log = Logger.getLogger(ToggleSilentModeAction.class.getName());

    private boolean smallToolbar;

    public ToggleSilentModeAction(Controller controller) {
        super(null, null, controller);
        smallToolbar = PreferencesEntry.SMALL_TOOLBAR.getValueBoolean(getController());
        // Setup
        adaptForScanSetting(getController().isSilentMode());
        // Bind to property
        getController().addPropertyChangeListener(
            Controller.PROPERTY_SILENT_MODE, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    boolean scansEnabled = (Boolean) evt.getNewValue();
                    adaptForScanSetting(scansEnabled);
                }
            });
    }

    private void adaptForScanSetting(boolean silentMode) {
        // Workaround for toolbar (for toolbar)
        if (silentMode) {
            configureFromActionId("disable_silent_mode");
            if (smallToolbar) {
                ImageIcon scaledImage =
                        Icons.scaleIcon((ImageIcon) Icons.SLEEP,
                        Toolbar.SMALL_ICON_SCALE_FACTOR);
                putValue(Action.SMALL_ICON, scaledImage);
            } else {
                putValue(Action.SMALL_ICON, Icons.SLEEP);
            }
        } else {
            configureFromActionId("enable_silent_mode");
            if (smallToolbar) {
                ImageIcon scaledImage =
                        Icons.scaleIcon((ImageIcon) Icons.WAKE_UP,
                        Toolbar.SMALL_ICON_SCALE_FACTOR);
                putValue(Action.SMALL_ICON, scaledImage);
            } else {
                putValue(Action.SMALL_ICON, Icons.WAKE_UP);
            }
        }

        if (smallToolbar) {
            putValue(Action.NAME, null);
        }
       
    }

    public void actionPerformed(ActionEvent e) {
        // Just toggle
        getController().setSilentMode(!getController().isSilentMode());
        log.finer("Is silentmode: " + getController().isSilentMode());
    }

}