package de.dal33t.powerfolder.web;

import java.util.Date;
import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;

public class RemoteDownloadHandler extends PFComponent implements Handler {

    public RemoteDownloadHandler(Controller controller) {
        super(controller);
    }

    public HTTPResponse getPage(HTTPRequest httpRequest) {
        Map<String, String> params = httpRequest.getQueryParams();
        FolderRepository repo = getController().getFolderRepository();
        List<Folder> folders = repo.getFoldersAsSortedList();
        if (params != null && params.containsKey("folderID")
            && params.containsKey("file") && params.containsKey("size"))
        {            
            for (Folder folder : folders) {
                if (folder.getId().equals(params.get("folderID"))) {
            
                    FileInfo info = new FileInfo(folder.getInfo(), params.get("file"));
                    info.setSize(Long.parseLong(params.get("size")));
                    info.setModifiedInfo(null, new Date(Long.parseLong(params.get("modifiedDate"))));
                    info.setDeleted(Boolean.parseBoolean(params.get("isDeleted")));
                    //this code is duplicated from DowloadFileAction maybe should move to transfermanager?
                    if (info.isDeleted()) {
                        folder.reDownloadFile(info);
                    } 
                    if (!info.isDownloading(getController())) {
                        if (info.isDeleted() || info.isExpected(repo)
                            || info.isNewerAvailable(repo))
                        {
                            Member member = getController().getTransferManager().downloadNewestVersion(
                                info);
                            if (member != null) {
                                HTTPResponse response = new HTTPResponse("download started from " + member.getNick());                            
                                return response; 
                            }
                        }
                    }                            
                    
                }
            }
        }
        return null;
    }
}
