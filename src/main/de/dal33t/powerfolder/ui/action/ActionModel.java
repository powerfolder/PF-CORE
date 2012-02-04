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
 * $Id: ActionModel.java 5419 2008-09-29 12:18:20Z harry $
 */
package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.security.ChangePreferencesPermission;
import de.dal33t.powerfolder.security.FolderCreatePermission;

/**
 * Holder of all simple singleton actions.
 */
public class ActionModel extends PFComponent {

    public ActionModel(Controller controller) {
        super(controller);
    }

    private FolderWizardAction folderWizardAction;
    private NewFolderAction newFolderAction;
    private FindComputersAction findComputersAction;
    private OpenPreferencesAction openPreferencesAction;
   // private OpenStatsChartsAction openStatsChartsAction;
    //private OpenAboutBoxAction openAboutBoxAction;
    private ConnectAction connectAction;
    private OpenDownloadsInformationAction openDownloadsInformationAction;
    private OpenUploadsInformationAction openUploadsInformationAction;
    private ViewNoticesAction viewNoticesAction;
    private OpenDebugInformationAction openDebugInformationAction;

    public NewFolderAction getNewFolderAction() {
        if (newFolderAction == null) {
            newFolderAction = new NewFolderAction(getController());
            newFolderAction.allowWith(FolderCreatePermission.INSTANCE);
        }
        return newFolderAction;
    }

    public FolderWizardAction getFolderWizardAction() {
        if (folderWizardAction == null) {
            folderWizardAction = new FolderWizardAction(getController());
            folderWizardAction.allowWith(FolderCreatePermission.INSTANCE);
        }
        return folderWizardAction;
    }

    public FindComputersAction getFindComputersAction() {
        if (findComputersAction == null) {
            findComputersAction = new FindComputersAction(getController());
        }
        return findComputersAction;
    }

    public OpenPreferencesAction getOpenPreferencesAction() {
        if (openPreferencesAction == null) {
            openPreferencesAction = new OpenPreferencesAction(getController());
            openPreferencesAction
                .allowWith(ChangePreferencesPermission.INSTANCE);
        }
        return openPreferencesAction;
    }

//    public OpenStatsChartsAction getOpenStatsChartsAction() {
//        if (openStatsChartsAction == null) {
//            openStatsChartsAction = new OpenStatsChartsAction(getController());
//        }
//        return openStatsChartsAction;
//    }

    public OpenDownloadsInformationAction getOpenDownloadsInformationAction() {
        if (openDownloadsInformationAction == null) {
            openDownloadsInformationAction = new OpenDownloadsInformationAction(
                getController());
        }
        return openDownloadsInformationAction;
    }

    public OpenUploadsInformationAction getOpenUploadsInformationAction() {
        if (openUploadsInformationAction == null) {
            openUploadsInformationAction = new OpenUploadsInformationAction(
                getController());
        }
        return openUploadsInformationAction;
    }

//    public OpenAboutBoxAction getOpenAboutBoxAction() {
//        if (openAboutBoxAction == null) {
//            openAboutBoxAction = new OpenAboutBoxAction(getController());
//        }
//        return openAboutBoxAction;
//    }
//
    public ConnectAction getConnectAction() {
        if (connectAction == null) {
            connectAction = new ConnectAction(getController());
        }
        return connectAction;
    }

    public ViewNoticesAction getViewNoticesAction() {
        if (viewNoticesAction == null) {
            viewNoticesAction = new ViewNoticesAction(getController());
        }
        return viewNoticesAction;
    }

    public OpenDebugInformationAction getOpenDebugInformationAction() {
        if (openDebugInformationAction == null) {
            openDebugInformationAction = new OpenDebugInformationAction(
                getController());
        }
        return openDebugInformationAction;
    }
}
