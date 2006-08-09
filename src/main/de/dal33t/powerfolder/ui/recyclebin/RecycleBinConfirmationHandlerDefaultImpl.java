package de.dal33t.powerfolder.ui.recyclebin;

import java.io.File;
import java.util.Date;

import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.RecycleBinConfirmEvent;
import de.dal33t.powerfolder.disk.RecycleBinConfirmationHandler;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

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
        String text = Translation.getTranslation(
            "recyclebin.confirmation.overwrite.on.restore.overwrite.filename",
            target.getName())
            + "\n";
        text += Translation.getTranslation(
            "recyclebin.confirmation.overwrite.on.restore.changed.date", Format
                .formatDate(new Date(target.lastModified())))
            + "\n";

        text += Translation.getTranslation(
            "recyclebin.confirmation.overwrite.on.restore.size.bytes", Format
                .formatBytes(target.length()))
            + "\n";
        text += Translation
            .getTranslation("recyclebin.confirmation.overwrite.on.restore.with.file.from.recyclebin")
            + "\n";
        text += Translation.getTranslation(
            "recyclebin.confirmation.overwrite.on.restore.changed.date", Format
                .formatDate(new Date(source.lastModified())))
            + "\n";

        text += Translation.getTranslation(
            "recyclebin.confirmation.overwrite.on.restore.size.bytes", Format
                .formatBytes(source.length()))
            + "\n";

        int returnValue = getUIController()
            .showOKCancelDialog(
                null,
                Translation
                    .getTranslation("recyclebin.confirmation.overwrite.on.restore.title"),
                text);
        return returnValue == JOptionPane.OK_OPTION;
    }
}
