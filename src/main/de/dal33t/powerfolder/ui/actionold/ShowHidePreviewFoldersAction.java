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
package de.dal33t.powerfolder.ui.actionold;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.Translation;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import com.jgoodies.binding.value.ValueModel;

import javax.swing.*;

/**
 * Action for toggeling whether preview folders should display.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 3.1 $
 */
@SuppressWarnings("serial")
public class ShowHidePreviewFoldersAction extends BaseAction {

    private ValueModel hidePreviewFoldersVM;

    public ShowHidePreviewFoldersAction(ValueModel hidePreviewFoldersVM,
        Controller controller) {
        super("hide_preview", controller);
        if (hidePreviewFoldersVM == null) {
            throw new NullPointerException("hidePreviewFoldersVM is null");
        }
        this.hidePreviewFoldersVM = hidePreviewFoldersVM;
        hidePreviewFoldersVM.addValueChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                changeTitle((Boolean) evt.getNewValue());
            }
        });
    }

    private void changeTitle(Boolean hide) {
        if (hide) {
            putValue(Action.NAME, Translation.getTranslation("show_preview.name"));
            setMnemonicKey(Translation.getTranslation("show_preview.key"));
            putValue(Action.SHORT_DESCRIPTION, Translation.getTranslation("show_preview.description"));
        } else {
            putValue(Action.NAME, Translation.getTranslation("hide_preview.name"));
            setMnemonicKey(Translation.getTranslation("hide_preview.key"));
            putValue(Action.SHORT_DESCRIPTION, Translation.getTranslation("hide_preview.description"));
        }
    }

    public void actionPerformed(ActionEvent e) {
        // Toggle visibility
        hidePreviewFoldersVM.setValue(!((Boolean) hidePreviewFoldersVM.getValue()));
    }
}
