package de.dal33t.powerfolder.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import de.dal33t.powerfolder.PFComponent;

public class FileHandler extends PFComponent {
    public HTTPResponse doGet(HTTPRequest request) {
        String filename = request.file;

        if (filename.indexOf("..") != -1) {
            // no path below the root allowed
            return null;
        }
        // strip leading "/"
        if (filename.startsWith("/")) {
            filename = filename.substring(1);
        }
        try {
            URL url = ClassLoader.getSystemResource(filename);
            if (url == null) {
                log().debug("file not found: " + filename);
                return null;
            }
            URLConnection connection = url.openConnection();
            long moddate = connection.getDate();
            long contenstLength = connection.getContentLength();
            log().debug(
                "file found: " + filename + " " + moddate + " "
                    + contenstLength);
            // limit to 1MB
            // FIXME this should be done with a dynamic buffer
            // now creates a buffer the size of the file
            if (contenstLength > -1 && contenstLength < 1024 * 1024) {
                InputStream in = connection.getInputStream();
                byte[] contents = new byte[(int) contenstLength];
                in.read(contents);
                HTTPResponse response = new HTTPResponse(contents);
                response.contentType = response.getMimeType(filename);
                return response;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        log().debug("file not found: " + filename);
        return null;

    }
}
