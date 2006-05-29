package de.dal33t.powerfolder.web;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.FileInfoComparator;
import de.dal33t.powerfolder.util.ReverseComparator;

public class FolderHandler extends AbstractVeloHandler implements Handler {

    private static final String DESCENDING = "descending";
    private static final String ASCENDING = "ascending";

    public FolderHandler(Controller controller) {
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
                            log().debug(dirStr);
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
        
        context.put("sortColumn", params.get("sortColumn"));
        boolean sortAscending = params.get("sortOrder").equals(ASCENDING);
        if (sortAscending) {
            context.put("sortImage", "/images/" + ASCENDING + ".gif");
            context.put("sortOrder", ASCENDING);
            context.put("nextSortOrder", DESCENDING); 
        } else {
            context.put("sortImage", "/images/" + DESCENDING + ".gif");
            context.put("sortOrder", DESCENDING);
            context.put("nextSortOrder", ASCENDING);
        }
        Directory directory = (Directory) context.get("directory");
        if (directory != null) {
            FileInfoComparator comparator = null;
            String sortColumn = params.get("sortColumn");
            if (sortColumn.equals("name")) {
                comparator = FileInfoComparator
                    .getComparator(FileInfoComparator.BY_NAME);
            } else if (sortColumn.equals("size")) {
                comparator = FileInfoComparator
                    .getComparator(FileInfoComparator.BY_SIZE);
            } else if (sortColumn.equals("date")) {
                comparator = FileInfoComparator
                    .getComparator(FileInfoComparator.BY_MODIFIED_DATE);
            }
            List<FileInfo> files = directory.getValidFiles();
            if (sortAscending) {
                Collections.sort(files, comparator);
            } else {
                Collections.sort(files, new ReverseComparator(comparator));
            }
            context.put("files", files);
        }
    }

    public String getTemplateFilename() {
        return "folder.vm";
    }

}
