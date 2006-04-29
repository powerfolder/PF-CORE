/* $Id: RequestFileListAction.java,v 1.14 2005/10/14 13:32:29 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.message.RequestFileList;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Requests the filelist for the selected unjoined folder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.14 $
 */
public class RequestFileListAction extends SelectionBaseAction {

    public RequestFileListAction(Controller controller,
        SelectionModel selectionModel)
    {
        super("requestfilelist", controller, selectionModel);
    }

    public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
        Object selection = selectionChangeEvent.getSelection();

        if (selection instanceof FolderDetails) {
            FolderDetails foDetails = (FolderDetails) selection;
            // get source for filelist
            Member member = getController().getFolderRepository().getSourceFor(
                foDetails.getFolderInfo(), false);
            // Enabled if source is available for folder
            setEnabled(member != null && member.isConnected());
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object selection = getUIController().getControlQuarter()
            .getSelectedItem();
        if (selection instanceof FolderDetails) {
            final FolderDetails foDetails = (FolderDetails) selection;
            final Member member = getController().getFolderRepository()
                .getSourceFor(foDetails.getFolderInfo(), false);

            if (member != null && member.isConnected()) {
                // try {
                member.addMessageListener(FileList.class,
                    new SwitchToFileListListener(foDetails.getFolderInfo(),
                        member));
                member.sendMessageAsynchron(new RequestFileList(foDetails
                    .getFolderInfo()), "Unable to get filelist for "
                    + foDetails.getFolderInfo().name + "\n" + member.getNick()
                    + " disconnted");
                // } catch (ConnectionException ex) {
                // JOptionPane.showMessageDialog(getUIController()
                // .getMainFrame().getUIComponent(),
                // "Unable to get filelist for " + foInfo.name + "\n"
                // + member.getNick() + " disconnted: "
                // + ex.getMessage(), "Unable to get filelist",
                // JOptionPane.ERROR_MESSAGE);
                // }
                log().debug(
                    "Requested filelist for " + foDetails.getFolderInfo().name);
            } else {
                log().warn(
                    "Unable to request filelist for "
                        + foDetails.getFolderInfo().name
                        + ", no source available");
            }
        }
    }

    /**
     * Waits for the file list and switches the filelist on information quarter
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.14 $
     */
    private class SwitchToFileListListener implements MessageListener {
        private Member from;
        private FolderInfo forFoler;

        private SwitchToFileListListener(FolderInfo forFolder, Member from) {
            this.forFoler = forFolder;
            this.from = from;
        }

        public void handleMessage(Member source, Message message) {
            if (!(message instanceof FileList)) {
                return;
            }
            FileList filelist = (FileList) message;

            // remove listener
            from.removeMessageListener(this);

            if (filelist.folder.equals(forFoler)
                && forFoler.equals(getSelectionModel().getSelection()))
            {
                log().verbose(filelist.folder + ": Received filelist");
                // display if correct folder and the one is still selected
                getUIController().getInformationQuarter()
                    .displayOnePublicFolder(
                        forFoler.getFolderDetails(getController()));
            }
        }
    }

}