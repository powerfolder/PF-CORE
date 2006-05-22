package de.dal33t.powerfolder.web;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;

public class DownloadHandler extends PFComponent implements Handler {

    public DownloadHandler(Controller controller) {
        super(controller);
    }

    public HTTPResponse getPage(HTTPRequest httpRequest) {
        Map<String, String> params = httpRequest.getQueryParams();
        FolderRepository repo = getController().getFolderRepository();
        List<Folder> folders = repo.getFoldersAsSortedList();
        String requestFile = httpRequest.getFile();
        if (requestFile.equals("/download")) {
            // we expect more...
            return null;
        }
        // filename should behind "/download/
        // so the browser will understand the filename
        int index = requestFile.lastIndexOf("/");
        try {
            String downloadFile = URLDecoder.decode(requestFile
                .substring(index + 1), "UTF-8");
            log().debug("trying downloading: " + downloadFile);
            if (params != null && params.containsKey("folderID")) {
                for (Folder folder : folders) {
                    if (folder.getId().equals(params.get("folderID"))) {
                        FileInfo info = folder.getFile(new FileInfo(folder
                            .getInfo(), downloadFile));
                        if (info != null) {
                            File file = folder.getDiskFile(info);
                            if (file.exists()) {
                                log().debug(
                                    "realy downloading: " + downloadFile);
                                return new HTTPResponse(file);
                            }
                        }
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {

        }
        return null;
    }
}
