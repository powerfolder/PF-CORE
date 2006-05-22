package de.dal33t.powerfolder.web;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.JDialog;

import org.apache.velocity.app.Velocity;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.plugin.AbstractPFPlugin;

/**
 * http://en.wikipedia.org/wiki/HTTP_cookie cookie: http://rfc.net/rfc2109.html
 * cookie 2: http://www.ietf.org/rfc/rfc2965.txt
 * http://www.w3.org/Protocols/HTTP/1.1/rfc2616.txt.gz
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
    private static ArrayList<WebWorker> workerPool = new ArrayList<WebWorker>();
    /** Where processing workers are */
    private static ArrayList<WebWorker> activeWorkers = new ArrayList<WebWorker>();
    private static final int MAX_WORKERS = 3;

    private ServerSocket serverSocket;

    private HashMap<String, Handler> veloHandlers = new HashMap<String, Handler>();

    public WebInterface(Controller controller) {
        super(controller);
        initProperties();
        initVelocity();
        initVelocityHandlers();
    }

    private void initVelocityHandlers() {
        downloadHandler = new DownloadHandler(getController());
        veloHandlers.put("/", new RootHandler(getController()));
        veloHandlers.put("/folderlist.xml", new FolderListXMLHandler(
            getController()));
        veloHandlers.put("/folder.xml", new FolderXMLHandler(getController()));
        veloHandlers.put("/login", loginHandler);
        veloHandlers.put("/404", new FileNotFoundHandler());
        veloHandlers.put("/leavefolder",
            new LeaveFolderHandler(getController()));
        veloHandlers.put("/pastepowerfolderlink",
            new PastePowerFolderLinkHandler(getController()));
        veloHandlers.put("/folderdetails", new FolderDetailsHandler(
            getController()));
        veloHandlers.put("/setsyncprofile", new SetSyncProfileHandler(
            getController()));
        veloHandlers.put("/icon", new IconHandler(getController()));

        veloHandlers.put("/download", downloadHandler);
        veloHandlers.put("/remoteDownload", new RemoteDownloadHandler(
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

    private void initVelocity() {
        try {
            // set Velocity to use the classpath for finding templates
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
        if (veloHandlers.containsKey(file)) {
            Handler handler = veloHandlers.get(file);
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
                        Socket socket = serverSocket.accept();
                        socket.setKeepAlive(true);
                        socket.setTcpNoDelay(true);
                        socket.setSoTimeout(20000);
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
                    e.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(runner, "WebInterface");
        thread.start();
    }

    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (WebWorker worker : activeWorkers) {
            worker.shutdown();
        }
        activeWorkers.clear();

        for (WebWorker worker : workerPool) {
            worker.shutdown();
        }
        workerPool.clear();
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
        public final String COOKIE_EXPIRATION_DATE_FORMAT = "EEE',' dd-MMM-yyyy HH:mm:ss 'GMT'";

        private final byte[] EOL = {(byte) '\r', (byte) '\n'};
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
                    e.printStackTrace();
                }
                /*
                 * go back in workerPool if there's fewer than MAX_WORKERS
                 * connections.
                 */
                socket = null;
                ArrayList<WebWorker> pool = WebInterface.workerPool;
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
            try {
                InputStream inputStream = new BufferedInputStream(socket
                    .getInputStream());
                PrintStream printStream = new PrintStream(socket
                    .getOutputStream());
                handle(inputStream, printStream);
            } catch (Exception e) {
                log().error(e);
            } finally {
                try {
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
                // log().debug("session not valid");
                // no valid session
                response = loginHandler.getPage(httpRequest);
            }

            if (response == null) {
                // log().debug("session valid");
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
                    printStream.write(EOL);
                    printStream.flush();
                    socket.close();
                    return;
                }
            }

            if (response == null) {
                Handler velo = veloHandlers.get("/404");
                response = velo.getPage(httpRequest);
            }
            if (response.getResponseCode() == HTTPConstants.HTTP_NOT_FOUND) {
                response = fileNotFoundHandler.getPage(httpRequest);
            }
            writePage(printStream, response, httpRequest);
        }

        /**
         * Set-Cookie: favColor=blue; expires=Sun, 17-Jan-2038 19:14:07 GMT;
         * path=/; domain=example.com
         */
        private void writePage(PrintStream printStream, HTTPResponse response,
            HTTPRequest httpRequest) throws IOException
        {
            printStream.print("HTTP/1.1 " + response.getResponseCode() + " OK");
            printStream.write(EOL);
            printStream.print("Server: PowerFolder WebInterface/"
                + Controller.PROGRAM_VERSION);
            printStream.write(EOL);
            printStream.print("Date: " + (new Date()));
            printStream.write(EOL);

            printStream.print("Content-length: " + response.getContentLength());
            printStream.write(EOL);
            printStream.print("Last Modified: " + response.getLastModified());
            printStream.write(EOL);
            printStream.print("Content-type: " + response.getContentType());
            printStream.write(EOL);

            if (response.getCookies() != null) {
                // insert all cookies
                for (String name : response.getCookies().keySet()) {
                    String value = response.getCookies().get(name);
                    Calendar calNow = new GregorianCalendar();
                    calNow.add(Calendar.HOUR, 24); // one day expiration

                    DateFormat formatter = new SimpleDateFormat(
                        COOKIE_EXPIRATION_DATE_FORMAT, Locale.US);
                    String expirationDate = formatter.format(calNow.getTime());
                    String cookie = "Set-Cookie: " + name + "=" + value
                        + "; expires=" + expirationDate + "; path=/; domain="
                        + httpRequest.getHost();

                    printStream.print(cookie);
                    printStream.write(EOL);
                }
            }
            // extra newline for end of headers / content follows
            printStream.write(EOL);
            if (response.shouldReturnValue()) {
                InputStream input = response.getInputStream();                
                if (input == null) {
                    // nothing to return!
                    throw new IllegalStateException(
                        "Must be something to return");
                }
                byte[] buffer = new byte[1024];
                while (true) {
                    int actualBytes = input.read(buffer);
                    if (actualBytes == -1) {
                        break;
                    }
                    printStream.write(buffer, 0, actualBytes);
                }
                input.close();
            }
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