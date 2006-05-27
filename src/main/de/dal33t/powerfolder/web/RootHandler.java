package de.dal33t.powerfolder.web;

import java.util.List;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;


public class RootHandler extends AbstractVeloHandler implements Handler {

    public RootHandler(Controller controller) {
        super(controller);
    }
    
    public void doRequest(HTTPRequest httpRequest) {
        FolderRepository repo = getController().getFolderRepository();
        List<Folder> folders = repo.getFoldersAsSortedList();
        folders = repo.getFoldersAsSortedList();
        context.put("folderList", folders);
    }
    
    public  String getTemplateFilename() {
        return "index.vm";
    }

}
