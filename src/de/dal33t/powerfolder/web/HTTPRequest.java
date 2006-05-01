package de.dal33t.powerfolder.web;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class HTTPRequest {
    public Map<String, String> cookies;
    public String host;
    public String method;
    public String file;
    public String fileWithParams;
    public Map<String, String> queryParams;
    public Socket socket;
   
    public HTTPRequest(Socket socket, InputStream inputStream) throws Exception
    {
        this.socket = socket;
        try {
            LineNumberReader reader = new LineNumberReader(
                new InputStreamReader(inputStream));
            String requestLine = reader.readLine();
            int indexSpace = requestLine.indexOf(" ");
            method = requestLine.substring(0, indexSpace);
            int indexHTTP = requestLine.lastIndexOf("HTTP");
            fileWithParams = requestLine.substring(indexSpace + 1,
                indexHTTP - 1);
            parse(reader);
            int indexQuestionMark = fileWithParams.indexOf("?");
            if (indexQuestionMark != -1) {
                queryParams = parseQueryString(fileWithParams
                    .substring(indexQuestionMark + 1));
                file = fileWithParams.substring(0, indexQuestionMark);
            } else {
                file = fileWithParams;
                queryParams = null;
            }
        } catch (Exception e) {
            throw new Exception("Invalid request: ", e);
        }
    }

    private void parse(LineNumberReader reader) throws IOException {

        cookies = new HashMap<String, String>();
        String header = reader.readLine();
        while (header != null) {

            try {
                String[] nameValue = header.split(":", 2);
                if (nameValue[0].trim().equalsIgnoreCase("Cookie")) {   
                    String allCookies = nameValue[1];
                    String[] cookiesArray = allCookies.split(";");
                    for (String cookieNameValue : cookiesArray) {
                        String[] cookie = cookieNameValue.split("=");
                        cookies.put(cookie[0].trim(), cookie[1].trim());                        
                    }

                } else if (nameValue[0].trim().equalsIgnoreCase("Host")) {
                    host = nameValue[1].trim();
                    int indexOfSemiColon = host.indexOf(":");
                    if (indexOfSemiColon > 0) {
                        host = host.substring(0, indexOfSemiColon);
                    }                    
                }
                header = reader.readLine().trim();
                if (header.length() == 0) {
                    header = null;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // ignore invalid header
            }
        }
    }

    private Map<String, String> parseQueryString(String querystring) {
        Map<String, String> params = new HashMap<String, String>();
        String[] allNameValues = querystring.split("&");
        for (String nameValue : allNameValues) {
            String[] aNameValueSplitted = nameValue.split("=");
            if (aNameValueSplitted.length > 1) {
                String name = aNameValueSplitted[0];
                try {
                    String value = URLDecoder.decode(aNameValueSplitted[1],
                        "UTF-8");
                    params.put(name, value);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return params;
    }
}