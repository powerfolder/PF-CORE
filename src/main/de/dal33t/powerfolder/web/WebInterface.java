package de.dal33t.powerfolder.web;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

import javax.swing.JDialog;

import org.apache.velocity.app.Velocity;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.plugin.AbstractPFPlugin;

/**
 * Entry point for the webserver. It will delegate the requests to "Handlers".
 * Register the Handlers in initHandlers. If no handler found for the URL the
 * FileHandler is tryed, it will try to find the requested page/file in the
 * PowerFolder.jar.<BR>
 * The DownloadHandler is a special case, that one will be called if the URL
 * starts with "/download".<BR>
 * Before any page is returned the loginHandler is checked if there is a valid
 * session. If not the loginHandler will be called.<BR>
 * Some references used in this class: <BR>
 * http://en.wikipedia.org/wiki/HTTP_cookie<BR>
 * cookie: http://rfc.net/rfc2109.html<BR>
 * cookie 2: http://www.ietf.org/rfc/rfc2965.txt<BR>
 * http://www.w3.org/Protocols/HTTP/1.1/rfc2616.txt.gz
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class WebInterface extends AbstractPFPlugin {
    public final static String PORT_SETTING = "plugin.webinterface.port";
    public final static String USERNAME_SETTING = "plugin.webinterface.username";
    public final static String PASSWORD_SETTING = "plugin.webinterface.password";

    private static final int DEFAULT_PORT = 80;
    private int port = DEFAULT_PORT;
    private FileHandler fileHandler = new FileHandler();
    private FileNotFoundHandler fileNotFoundHandler = new FileNotFoundHandler();
    private DownloadHandler downloadHandler;
    private LoginHandler loginHandler;

    /** Worker threads that are idle */
    private static List<WebWorker> workerPool = new LinkedList<WebWorker>();
    /** Where processing workers are */
    private static List<WebWorker> activeWorkers = new LinkedList<WebWorker>();
    private static final int MAX_WORKERS = 3;

    private ServerSocket serverSocket;

    private HashMap<String, Handler> handlers = new HashMap<String, Handler>();

    public WebInterface(Controller controller) {
        super(controller);
    }

    private void initHandlers() {
        downloadHandler = new DownloadHandler(getController());
        handlers.put("/", new RootHandler(getController()));
        handlers.put("/folderlist", new FolderListHandler(getController()));
        handlers.put("/folder", new FolderHandler(getController()));
        handlers.put("/login", loginHandler);
        handlers.put("/404", new FileNotFoundHandler());
        handlers.put("/leavefolder", new LeaveFolderHandler(getController()));
        handlers.put("/pastepowerfolderlink", new PastePowerFolderLinkHandler(
            getController()));
        handlers.put("/folderdetails",
            new FolderDetailsHandler(getController()));
        handlers.put("/setsyncprofile", new SetSyncProfileHandler(
            getController()));
        handlers.put("/icon", new IconHandler(getController()));
        handlers.put("/remoteDownload", new RemoteDownloadHandler(
            getController()));
        handlers.put("/getbasefolder",  new GetBaseFolderHandler(
            getController()));
        handlers.put("/getsubdirectories",  new GetSubDirsHandler(
            getController()));
        handlers.put("/createdirectory",  new CreateDirectoryHandler(
            getController()));
        
        
    }

    void initProperties() {
        // new loginHandler if properties (username /password) have changed
        loginHandler = new LoginHandler(getController());
        Properties props = getController().getConfig();
        try {
            String portStr = props.getProperty(PORT_SETTING);
            if (portStr != null && portStr.trim().length() > 0) {
                port = Integer.parseInt(portStr);
            }
        } catch (Exception e) {
            // using default
        }
    }

    /** sets Velocity to use the classpath for finding templates */
    private void initVelocity() {
        try {
            Properties p = new Properties();
            p.setProperty("resource.loader", "class");
            p.setProperty("class.resource.loader.description",
                "Velocity Classpath Resource Loader");
            p
                .setProperty("class.resource.loader.class",
                    "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            Velocity.init(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Handler getHandler(String file) {
        if (handlers.containsKey(file)) {
            Handler handler = handlers.get(file);
            log().debug(
                "request: " + file + " handled by "
                    + handler.getClass().getName());
            return handler;
        }
        if (file.startsWith("/download")) {
            log().debug(
                "request: " + file + " handled by "
                    + downloadHandler.getClass().getName());
            return downloadHandler;
        }
        return null;
    }

    public String getName() {
        return "WebInterface";
    }

    public String getDescription() {
        return "Adds a WebInterface to your PowerFolder, access PowerFolder in your browser.";
    }

    public void start() {
        initProperties();
        initVelocity();
        initHandlers();
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // start workers
        for (int i = 0; i < MAX_WORKERS; ++i) {
            WebWorker worker = new WebWorker();
            (new Thread(worker, "WebInterface worker #" + i)).start();
            workerPool.add(worker);
        }

        Runnable runner = new Runnable() {
            public void run() {
                try {
                    while (true) {
                        //accept incomming requests 
                        Socket socket = serverSocket.accept();
                        socket.setKeepAlive(false);
                        socket.setTcpNoDelay(true);
                        socket.setSoTimeout(1000);
                        WebWorker worker = null;
                        synchronized (workerPool) {
                            if (workerPool.isEmpty()) {
                                WebWorker ws = new WebWorker();
                                ws.setSocket(socket);
                                (new Thread(ws,
                                    "Additional WebInterface worker")).start();
                            } else {
                                worker = workerPool.get(0);
                                workerPool.remove(0);
                                activeWorkers.add(worker);
                                worker.setSocket(socket);
                            }
                        }
                    }
                } catch (SocketException se) {
                    // socket probaly closed on shutdown
                    // se.printStackTrace();
                } catch (IOException e) {
                    log().error(e);
                }
            }
        };
        Thread thread = new Thread(runner, "WebInterface");
        thread.start();
        log().debug(this + " started");
    }

    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        synchronized (activeWorkers) {
            for (WebWorker worker : activeWorkers) {
                worker.shutdown();
            }
            activeWorkers.clear();
        }
        synchronized (workerPool) {
            for (WebWorker worker : workerPool) {
                worker.shutdown();
            }
            workerPool.clear();
        }
        log().debug(this + " stopped");
    }

    public boolean hasOptionsDialog() {
        return true;
    }

    public void showOptionsDialog(JDialog parent) {
        WebInterfaceOptionsDialog dialog = new WebInterfaceOptionsDialog(this,
            getController(), parent);
        dialog.open();
    }

    private class WebWorker implements Runnable {

        private Socket socket;
        private boolean stop = false;

        public synchronized void run() {
            while (true) {
                if (socket == null) {
                    /* nothing to do */
                    try {
                        wait();
                        if (stop) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        /* should not happen */
                        continue;
                    }
                }
                try {
                    handleClient();
                } catch (Exception e) {
                    log().error("error when handeling a request", e);
                }
                /*
                 * go back in workerPool if there's fewer than MAX_WORKERS
                 * connections.
                 */
                socket = null;
                List<WebWorker> pool = WebInterface.workerPool;
                synchronized (pool) {
                    activeWorkers.remove(this);
                    if (pool.size() >= WebInterface.MAX_WORKERS) {
                        /* too many threads, exit this one */
                        return;
                    }
                    pool.add(this);
                }
            }
        }

        private synchronized void setSocket(Socket socket) {
            this.socket = socket;
            notify();
        }

        private void shutdown() {
            stop = true;
            synchronized (this) {
                notify();
            }
        }

        private void handleClient() {
            InputStream inputStream = null;
            PrintStream printStream = null;
            try {
                inputStream = new BufferedInputStream(socket.getInputStream());
                printStream = new PrintStream(socket.getOutputStream());
                handle(inputStream, printStream);
                printStream.flush();
                printStream.close();
                inputStream.close();
            } catch (Exception e) {
                // return an internal server error?
                log().error("error when handeling a request", e);
            } finally {
                try {
                    if (printStream != null) {
                        printStream.flush();
                        printStream.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    socket.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        private void handle(InputStream inputStream, PrintStream printStream)
            throws Exception
        {
            HTTPRequest httpRequest = new HTTPRequest(socket, inputStream);
            // log().debug("handle... '" + httpRequest.file + "'");
            HTTPResponse response = null;
            // the only page to show if not logged on is the login page
            if (httpRequest.getFile().startsWith("/login")) {
                response = loginHandler.getPage(httpRequest);
            }

            // check if logged on
            if (!loginHandler.checkSession(httpRequest.getCookies(), socket
                .getInetAddress()))
            {
                 //log().debug("session not valid: " +httpRequest.getCookies());
                // no valid session
                response = loginHandler.getPage(httpRequest);
            }

            if (response == null) {
                //log().debug("session valid");
                if (httpRequest.getMethod().equals(HTTPConstants.HTTP_GET)) {
                    response = handleGET(httpRequest);
                } else if (httpRequest.getMethod().equals(
                    HTTPConstants.HTTP_HEAD))
                {
                    // return only the headers not the contents,
                    // should be treated as GET
                    response = handleGET(httpRequest);
                    response.setReturnValue(false);
                } else {
                    /* we don't support this method */
                    printStream.print("HTTP/1.1 "
                        + HTTPConstants.HTTP_BAD_METHOD
                        + " unsupported method type: ");
                    printStream.print(httpRequest.getMethod());
                    printStream.write(HTTPConstants.EOL);
                    printStream.flush();
                    socket.close();
                    return;
                }
            }

            if (response == null) {
                Handler velo = handlers.get("/404");
                response = velo.getPage(httpRequest);
            }
            if (response.getResponseCode() == HTTPConstants.HTTP_NOT_FOUND) {
                response = fileNotFoundHandler.getPage(httpRequest);
            }
            writePage(printStream, response, httpRequest);
        }

        /**
         * 
         */
        private void writePage(PrintStream printStream, HTTPResponse response,
            HTTPRequest httpRequest) throws IOException
        {
            printStream.print("HTTP/1.1 " + response.getResponseCode() + " OK");
            printStream.write(HTTPConstants.EOL);
            printStream.print("Server: PowerFolder WebInterface/"
                + Controller.PROGRAM_VERSION);
            printStream.write(HTTPConstants.EOL);
            printStream.print("Date: " + (new Date()));
            printStream.write(HTTPConstants.EOL);

            printStream.print("Content-length: " + response.getContentLength());
            printStream.write(HTTPConstants.EOL);
            printStream.print("Last Modified: " + response.getLastModified());
            printStream.write(HTTPConstants.EOL);
            printStream.print("Content-type: " + response.getContentType());
            printStream.write(HTTPConstants.EOL);

            if (response.getCookies() != null) {
                // insert all cookies
                printStream.print(response.getCookiesAsHTTPString(httpRequest
                    .getHost()));
            }
            // extra newline for end of headers / content follows
            printStream.write(HTTPConstants.EOL);
            if (response.shouldReturnValue()) { // GET
                InputStream input = response.getInputStream();
                if (input == null) {
                    // nothing to return!
                    throw new IllegalStateException(
                        "Must be something to return");
                }
                byte[] buffer;
                if (response.getContentLength() > 0 && response.getContentLength() < (10*1024)) {
                    buffer = new byte[(int)response.getContentLength()];
                } else {                       
                    buffer = new byte[10*1024];
                }
                while (true) {
                    int actualBytes = input.read(buffer);
                    if (actualBytes == -1) {
                        break;
                    }
                    printStream.write(buffer, 0, actualBytes);
                }
                input.close();
            } // else HEAD (should not return value)
            printStream.flush();
        }

        private HTTPResponse handleGET(HTTPRequest request) {
            String file = request.getFile();
            Handler handler = getHandler(file);
            if (handler == null) {
                return fileHandler.doGet(request);
            }
            return handler.getPage(request);
        }
    }
}