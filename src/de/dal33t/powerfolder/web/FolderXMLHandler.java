package de.dal33t.powerfolder.web;

import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;

public class FolderXMLHandler extends AbstractVeloHandler implements Handler {

    public FolderXMLHandler(Controller controller) {
        super(controller);
    }

    public void doRequest(HTTPRequest httpRequest) {
        context.put("controller", getController());
        FolderRepository repo = getController().getFolderRepository();
        context.put("folderRepository", repo);
        Map<String, String> params = httpRequest.getQueryParams();
        List<Folder> folders = repo.getFoldersAsSortedList();
        if (params != null && params.containsKey("FolderID")) {
            for (Folder folder : folders) {
                if (folder.getId().equals(params.get("FolderID"))) {
                    context.put("folder", folder);
                    if (params.containsKey("directory")) {
                        String dirStr = params.get("directory");
                        Directory directory = folder.getDirectory();
                        if (!dirStr.equals("/")) {
                            directory = directory.getSubDirectory(dirStr);
                        }
                        if (directory == null) {
                            context.put("directory", folder.getDirectory());
                        } else {
                            context.put("directory", directory);
                        }
                    } else {
                        context.put("directory", folder.getDirectory());
                    }
                    log().debug("folder " + params.get("FolderID") + " found");
                    break;
                }
            }
        }
    }

    public String getTemplateFilename() {
        return "folder.xml";
    }

}
