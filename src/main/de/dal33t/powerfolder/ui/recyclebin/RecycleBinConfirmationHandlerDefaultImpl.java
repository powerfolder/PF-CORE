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
package de.dal33t.powerfolder.ui.recyclebin;

import java.io.File;
import java.util.Date;

import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.RecycleBinConfirmEvent;
import de.dal33t.powerfolder.event.RecycleBinConfirmationHandler;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

public class RecycleBinConfirmationHandlerDefaultImpl extends PFUIComponent
    implements RecycleBinConfirmationHandler
{

    public RecycleBinConfirmationHandlerDefaultImpl(Controller controller) {
        super(controller);
    }

    public boolean confirmOverwriteOnRestore(
        RecycleBinConfirmEvent recycleBinConfirmEvent)
    {
        File target = recycleBinConfirmEvent.getTargetFile();
        File source = recycleBinConfirmEvent.getSourceFile();
        StringBuilder sb = new StringBuilder(Translation.getTranslation(
            "recyclebin.confirmation.overwrite.on.restore.overwrite.filename",
            target.getName())
            + '\n');
        sb.append(Translation.getTranslation(
            "recyclebin.confirmation.overwrite.on.restore.changed.date", Format
                .formatDate(new Date(target.lastModified())))
            + '\n');
        sb.append(Translation.getTranslation(
            "recyclebin.confirmation.overwrite.on.restore.size.bytes", Format
                .formatBytes(target.length()))
            + '\n');
        sb.append(Translation
            .getTranslation("recyclebin.confirmation.overwrite.on.restore.with.file.from.recyclebin")
            + '\n');
        sb.append(Translation.getTranslation(
            "recyclebin.confirmation.overwrite.on.restore.changed.date", Format
                .formatDate(new Date(source.lastModified())))
            + '\n');

        sb.append(Translation.getTranslation(
            "recyclebin.confirmation.overwrite.on.restore.size.bytes", Format
                .formatBytes(source.length()))
            + '\n');

        int returnValue = DialogFactory.genericDialog(
                getController().getUIController().getMainFrame().getUIComponent(),
                Translation.getTranslation("recyclebin.confirmation.overwrite.on.restore.title"),
                sb.toString(),
                new String[]{
                        Translation.getTranslation("general.continue"),
                        Translation.getTranslation("general.cancel")},
                0, GenericDialogType.QUESTION); // Continue default
        return returnValue == 0;
    }
}
