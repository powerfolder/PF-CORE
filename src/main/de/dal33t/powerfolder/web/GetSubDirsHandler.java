package de.dal33t.powerfolder.web;

import java.io.File;

import javax.swing.filechooser.FileSystemView;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Util;

public class GetSubDirsHandler extends AbstractVeloHandler implements Handler {

    public GetSubDirsHandler(Controller controller) {
        super(controller);
    }

    public void doRequest(HTTPRequest httpRequest) {
        String message = "";
        if (httpRequest.getQueryParams() != null
            && httpRequest.getQueryParams().containsKey("dir"))
        {
            String dir = Util.decodeFromURL(httpRequest.getQueryParams().get("dir").trim());
            log().debug("dir: '"+ dir + "'");
            if (dir.length() == 0) {
                message = "param dir is empty";
            }
            
            if (dir.equals("listroots")) {
                context.put("listroots", true);
                context.put("roots", File.listRoots());
                String[] rootNames = new String[File.listRoots().length];
                
                int i =0;
                FileSystemView view = FileSystemView.getFileSystemView();
                for (File aRoot : File.listRoots()) {
                    rootNames[i++] = view.getSystemDisplayName(aRoot);
                    //maybe map to system icons here:
                    //view.getSystemIcon(aRoot);
                    //not needed but just cool ;-)
                }
                context.put("rootNames", rootNames);
                log().debug(rootNames);
            } else {
            
                File dirFile = new File(dir);
                
                if (!dirFile.isDirectory()) {
                    message = "param dir: "+ dir +" is not a valid directory";
                }
                context.put("directory", dirFile);
                if (dirFile.getParentFile() != null) {
                    context.put("parent", dirFile.getParentFile().getAbsolutePath());
                } else {
                    context.put("parent", "listroots");
                }
            }
        } else {
            message = "we need a dir parameter";
        }
        
        if (!message.equals("")) {
            log().error("oops: " + message);
            context.put("message", message);
            context.put("showError", true);
        }
        context.put("VelocityTools", VelocityTools.getInstance());
    }

    public String getTemplateFilename() {
        return "directorylist.vm";
    }
}