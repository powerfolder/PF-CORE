package de.dal33t.powerfolder.web;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.FolderRepository;

public class GetBaseFolderHandler extends PFComponent implements Handler {

    public GetBaseFolderHandler(Controller controller) {
        super(controller);
    }

    public HTTPResponse getPage(HTTPRequest httpRequest) {
        FolderRepository repo = getController().getFolderRepository();
        
        return new HTTPResponse(repo.getFoldersBasedir());
    }
}