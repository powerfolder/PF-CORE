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

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.event.RecycleBinEvent;
import de.dal33t.powerfolder.event.RecycleBinListener;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

/**
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 */
@SuppressWarnings("serial")
public class EmptyRecycleBinAction extends BaseAction {

    public EmptyRecycleBinAction(Controller controller) {
        super("empty_recycle_bin", controller);
        RecycleBin recycleBin = controller.getRecycleBin();
        recycleBin.addRecycleBinListener(new MyRecycleBinListener());
        setEnabled(getController().getRecycleBin().countAllRecycledFiles() > 0);
    }

    public void actionPerformed(ActionEvent e) {

        int choice = DialogFactory.genericDialog(getUIController()
            .getMainFrame().getUIComponent(), Translation
            .getTranslation("empty_recycle_bin_confimation.title"), Translation
            .getTranslation("empty_recycle_bin_confimation.text"),
            new String[]{
                Translation
                    .getTranslation("empty_recycle_bin_confimation.empty"),
                Translation
                    .getTranslation("empty_recycle_bin_confimation.dont")}, 1,
            GenericDialogType.INFO); // Default = 1 = Dont Empty

        if (choice == 0) { // Empty bin
            setEnabled(false);
            MyActivityVisualizationWorker worker = new MyActivityVisualizationWorker(
                getUIController());
            worker.start();
        }
    }

    public class MyRecycleBinListener implements RecycleBinListener {

        public void fileAdded(RecycleBinEvent e) {
            setEnabled(true);
        }

        public void fileRemoved(RecycleBinEvent e) {
            setEnabled(getController().getRecycleBin().countAllRecycledFiles() > 0);
        }

        public void fileUpdated(RecycleBinEvent e) {
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    public class MyActivityVisualizationWorker extends
        ActivityVisualizationWorker
    {

        public MyActivityVisualizationWorker(UIController uiController) {
            super(uiController, false);
        }

        @Override
        public Object construct() {
            getController().getRecycleBin().emptyRecycleBin(
                getProgressListener());
            setEnabled(true);
            return null;
        }

        @Override
        protected String getTitle() {
            return Translation
                .getTranslation("empty_recycle_bin.working.title");
        }

        @Override
        protected String getWorkingText() {
            return Translation
                .getTranslation("empty_recycle_bin.working.description");
        }
    }

}
