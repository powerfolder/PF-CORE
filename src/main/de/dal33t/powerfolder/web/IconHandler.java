package de.dal33t.powerfolder.web;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Icon;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.ImageSupport;
/** serves an image for the file extension, mapping to the system icons 
 * FIXME: needs caching */
public class IconHandler extends PFComponent implements Handler {

    public IconHandler(Controller controller) {
        super(controller);        
    }

    public HTTPResponse getPage(HTTPRequest httpRequest) {
        Map<String, String> params = httpRequest.getQueryParams();
        if (params.containsKey("extension")) {
            log().debug(httpRequest.getFile()+ " " +params.get("extension"));
            Icon icon = Icons.getIconExtension(params.get("extension"));
            Image image = Icons.getImageFromIcon(icon);
            String contentType = null;
            File tempFile = null;
            try {
                synchronized (this) { // prevent write of same file twice
                    if (ImageSupport.isWriteSupportedImage("png")) {
                        contentType = "image/png";
                        tempFile = new File(Controller.getTempFilesLocation(),
                            "temp.png");
                        tempFile.deleteOnExit();
                        ImageIO.write(Icons.toBufferedImage(image), "png",
                            tempFile);
                    } else if (ImageSupport.isWriteSupportedImage("jpg")) {
                        contentType = "image/jpg";
                        tempFile = new File(Controller.getTempFilesLocation(),
                            "temp.jpg");
                        ImageIO.write(Icons.toBufferedImage(image), "jpg",
                            tempFile);
                    }
                    tempFile.deleteOnExit();
                    byte[] contents = new byte[(int) tempFile.length()];
                    InputStream in = new FileInputStream(tempFile);
                    in.read(contents);
                    HTTPResponse response = new HTTPResponse(contents);
                    response.setContentType(contentType);
                    return response;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

}
