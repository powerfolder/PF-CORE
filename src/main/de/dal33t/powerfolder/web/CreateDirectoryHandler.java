package de.dal33t.powerfolder.web;

import java.io.File;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Util;

public class CreateDirectoryHandler extends PFComponent implements Handler {

    public CreateDirectoryHandler(Controller controller) {
        super(controller);
    }

    public HTTPResponse getPage(HTTPRequest httpRequest) {

        if (httpRequest.getQueryParams() != null
            && httpRequest.getQueryParams().containsKey("createNewDirectoryLocation")
            && httpRequest.getQueryParams().containsKey("newDirectoryName"))
        {
            String where = Util.decodeFromURL(httpRequest.getQueryParams().get(
                "createNewDirectoryLocation"));
            String newDirectoryName = Util.decodeFromURL(httpRequest
                .getQueryParams().get("newDirectoryName"));
            if (where.length() > 0 && newDirectoryName.length() > 0) {
                File directoryLocation = new File(where);
                if (directoryLocation.isDirectory()) {
                    File newDirectory = new File(where, newDirectoryName);
                    if (newDirectory.mkdir()) {
                        return new HTTPResponse("succes");
                    }
                }
            }
        }
        return new HTTPResponse("failed");
    }
}