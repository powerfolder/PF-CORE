package de.dal33t.powerfolder.web;

import java.io.*;
import java.util.Date;
import java.util.Map;

/**
 * This class holds the response to a HTTPRequest. It holdes all the data to
 * return, al header info and the contents itself.
 */
public class HTTPResponse {
    private int responseCode = HTTPConstants.HTTP_OK;
    private Map<String, String> cookies;
    /**
     * if HTTP_HEAD was requested this should be false. true on HTTP_GET
     */
    private boolean returnValue = true;    
    private Date lastModified;
    private String contentType = "text/html";   
    private InputStream inputStream;
    private long size;

    public HTTPResponse() {
                
    }
    
    public HTTPResponse(byte[] contents) {
        setContents(contents);        
    }

    public HTTPResponse(String contents) {
        this(contents.getBytes());
    }

    public HTTPResponse(File file) {
        setFileToReturn(file);
    }

    /** When using this contructor you should set the size and contentType yourself */
    public HTTPResponse(InputStream inputStream) {
        setInputStream(inputStream);
    }
    
    private void setContents(byte[] contents) {
        inputStream = new ByteArrayInputStream(contents);
        setSize(contents.length);
    }

    /** FIXME? This can be done with a HTTP response message**/
    public void redirectToRoot() {
        String page = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \r\n";
        page += "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\r\n";
        page += "<HTML><HEAD><TITLE>:: Powerfolder Webinterface redirect ::</TITLE>\r\n";
        page += "<meta http-equiv=\"refresh\" content=\"0;url=/\">\r\n";
        page += "</HEAD>\r\n";
        page += "<BODY>\r\n";
        page += "</BODY></HTML>";

        setContents(page.getBytes());

    }
    
    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = new Date(lastModified);
    }

    public boolean shouldReturnValue() {
        return returnValue;
    }

    public void setReturnValue(boolean returnValue) {
        this.returnValue = returnValue;
    }

    public void setSize(long size) {
        this.size = size;        
    }
    
    public int getResponseCode() {
        return responseCode;
    }
    
    public long getContentLength() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        setReturnValue(true);
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    private void setFileToReturn(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("file must exists");
        }        
        setReturnValue(true);
        setContentType(getMimeType(file.getName()));
        setLastModified(file.lastModified());
        setSize(file.length());
        try {
            setInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            //we tested that above
        }
    }

    // Returns the MIME type of the specified file.
    // @param file file name whose MIME type is required
    public static String getMimeType(String file) {
        file = file.toUpperCase();

        if (file.endsWith(".HTML") || file.endsWith(".HTM"))
            return "text/html";
        if (file.endsWith(".TXT"))
            return "text/plain";
        if (file.endsWith(".XML"))
            return "text/xml";
        if (file.endsWith(".XSL"))
            return "text/xml";
        if (file.endsWith(".CSS"))
            return "text/css";
        if (file.endsWith(".SGML") || file.endsWith(".SGM"))
            return "text/x-sgml";
        // Image
        if (file.endsWith(".GIF"))
            return "image/gif";
        if (file.endsWith(".JPG") || file.endsWith(".JPEG")
            || file.endsWith(".JPE"))
            return "image/jpeg";
        if (file.endsWith(".PNG"))
            return "image/png";
        if (file.endsWith(".BMP"))
            return "image/bmp";
        if (file.endsWith(".TIF") || file.endsWith(".TIFF"))
            return "image/tiff";
        if (file.endsWith(".RGB"))
            return "image/x-rgb";
        if (file.endsWith(".XPM"))
            return "image/x-xpixmap";
        if (file.endsWith(".XBM"))
            return "image/x-xbitmap";
        if (file.endsWith(".SVG"))
            return "image/svg-xml ";
        if (file.endsWith(".SVGZ"))
            return "image/svg-xml ";
        // Audio
        if (file.endsWith(".AU") || file.endsWith(".SND"))
            return "audio/basic";
        if (file.endsWith(".MID") || file.endsWith(".MIDI")
            || file.endsWith(".RMI") || file.endsWith(".KAR"))
            return "audio/mid";
        if (file.endsWith(".MPGA") || file.endsWith(".MP2")
            || file.endsWith(".MP3"))
            return "audio/mpeg";
        if (file.endsWith(".WAV"))
            return "audio/wav";
        if (file.endsWith(".AIFF") || file.endsWith(".AIFC"))
            return "audio/aiff";
        if (file.endsWith(".AIF"))
            return "audio/x-aiff";
        if (file.endsWith(".RA"))
            return "audio/x-realaudio";
        if (file.endsWith(".RPM"))
            return "audio/x-pn-realaudio-plugin";
        if (file.endsWith(".RAM"))
            return "audio/x-pn-realaudio";
        if (file.endsWith(".SD2"))
            return "audio/x-sd2";
        // Application
        if (file.endsWith(".BIN") || file.endsWith(".DMS")
            || file.endsWith(".LHA") || file.endsWith(".LZH")
            || file.endsWith(".EXE") || file.endsWith(".DLL")
            || file.endsWith(".CLASS"))
            return "application/octet-stream";
        if (file.endsWith(".HQX"))
            return "application/mac-binhex40";
        if (file.endsWith(".PS") || file.endsWith(".AI")
            || file.endsWith(".EPS"))
            return "application/postscript";
        if (file.endsWith(".PDF"))
            return "application/pdf";
        if (file.endsWith(".RTF"))
            return "application/rtf";
        if (file.endsWith(".DOC"))
            return "application/msword";
        if (file.endsWith(".PPT"))
            return "application/powerpoint";
        if (file.endsWith(".FIF"))
            return "application/fractals";
        if (file.endsWith(".P7C"))
            return "application/pkcs7-mime";
        // Application/x
        if (file.endsWith(".JS"))
            return "application/x-javascript";
        if (file.endsWith(".Z"))
            return "application/x-compress";
        if (file.endsWith(".GZ"))
            return "application/x-gzip";
        if (file.endsWith(".TAR"))
            return "application/x-tar";
        if (file.endsWith(".TGZ"))
            return "application/x-compressed";
        if (file.endsWith(".ZIP"))
            return "application/x-zip-compressed";
        if (file.endsWith(".DIR") || file.endsWith(".DCR")
            || file.endsWith(".DXR"))
            return "application/x-director";
        if (file.endsWith(".DVI"))
            return "application/x-dvi";
        if (file.endsWith(".TEX"))
            return "application/x-tex";
        if (file.endsWith(".LATEX"))
            return "application/x-latex";
        if (file.endsWith(".TCL"))
            return "application/x-tcl";
        if (file.endsWith(".CER") || file.endsWith(".CRT")
            || file.endsWith(".DER"))
            return "application/x-x509-ca-cert";
        // Video
        if (file.endsWith(".MPG") || file.endsWith(".MPE")
            || file.endsWith(".MPEG"))
            return "video/mpeg";
        if (file.endsWith(".QT") || file.endsWith(".MOV"))
            return "video/quicktime";
        if (file.endsWith(".AVI"))
            return "video/x-msvideo";
        if (file.endsWith(".MOVIE"))
            return "video/x-sgi-movie";
        // Chemical
        if (file.endsWith(".PDB") || file.endsWith(".XYZ"))
            return "chemical/x-pdb";
        // X-
        if (file.endsWith(".ICE"))
            return "x-conference/x-cooltalk";
        if (file.endsWith(".JNLP"))
            return "application/x-java-jnlp-file";
        if (file.endsWith(".WRL") || file.endsWith(".VRML"))
            return "x-world/x-vrml";
        if (file.endsWith(".WML"))
            return "text/vnd.wap.wml";
        if (file.endsWith(".WMLC"))
            return "application/vnd.wap.wmlc";
        if (file.endsWith(".WMLS"))
            return "text/vnd.wap.wmlscript";
        if (file.endsWith(".WMLSC"))
            return "application/vnd.wap.wmlscriptc";
        if (file.endsWith(".WBMP"))
            return "image/vnd.wap.wbmp";

        return null;
    }
    
}
