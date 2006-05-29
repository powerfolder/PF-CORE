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

/**
 * Tool to get a file from a folder. The source folder is found by folderID.
 * file info is found by te filename that is part on the request file. <code>
 * /download/filename.ext?folderID=aIDstring
 * </code>
 * This solution is choosen so the browser will know the filename when saving
 * the file.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
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
        //log().debug(httpRequest.getFile());
        //log().debug(params);
        //remove leading "/" :
        requestFile = requestFile.substring(1);
        // filename and subdirs should behind "/download/
        // so the browser will understand the filename
        int index = requestFile.indexOf("/");
       // try {
            String downloadFile = requestFile
                .substring(index + 1);
            if (params != null && params.containsKey("folderID")) {
                for (Folder folder : folders) {
                    if (folder.getId().equals(params.get("folderID"))) {
                        FileInfo info = folder.getFile(new FileInfo(folder
                            .getInfo(), downloadFile));
                        if (info != null) {
                            File file = folder.getDiskFile(info);
                            if (file.exists()) {
                                log().debug("downloading: " + downloadFile);
                                return new HTTPResponse(file);
                            }
                        }
                    }
                }
            }
        //} catch (UnsupportedEncodingException e) {

        //}
        return null;
    }
}
