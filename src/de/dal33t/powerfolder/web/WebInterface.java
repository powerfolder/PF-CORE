package de.dal33t.powerfolder.web;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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
 * http://en.wikipedia.org/wiki/HTTP_cookie
 * cookie: http://rfc.net/rfc2109.html
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
    private LoginHandler loginHandler;

    /** Worker threads that are idle */
    private static ArrayList<WebWorker> workerPool = new ArrayList<WebWorker>();
    /** Where processing workers are */
    private static ArrayList<WebWorker> activeWorkers = new ArrayList<WebWorker>();
    private static final int MAX_WORKERS = 3;

    private ServerSocket serverSocket;

    private HashMap<String, VeloHandler> veloHandlers = new HashMap<String, VeloHandler>();

    public WebInterface(Controller controller) {
        super(controller);       
        initProperties();
        initVelocity();
        initVelocityHandlers();        
    }
    
    void initProperties() {
        //new loginHandler if properties (username /password) have changed 
        loginHandler = new LoginHandler(getController());
        Properties props = getController().getConfig();
        try {
            String portStr = props.getProperty(PORT_SETTING);
            if (portStr != null && portStr.trim().length() > 0) {
                port = Integer.parseInt(portStr);
            }
        } catch (Exception e) {
            //using default
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

    private void initVelocityHandlers() {
        veloHandlers.put("/", new RootHandler());
        veloHandlers.put("/login", loginHandler);
        veloHandlers.put("/404", new FileNotFoundHandler());
        veloHandlers.put("/pastepowerfolderlink",
            new PastePowerFolderLinkHandler(getController()));
    }

    private VeloHandler getVeloHandler(String file) {
        if (veloHandlers.containsKey(file)) {
            return veloHandlers.get(file);
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
    }

    public boolean hasOptionsDialog() {
        return true;
    }

    public void showOptionsDialog(JDialog parent) {        
        WebInterfaceOptionsDialog dialog = new WebInterfaceOptionsDialog(this, getController(), parent);
        dialog.open();        
    }

    private class WebWorker implements Runnable, HTTPConstants {
        public final String COOKIE_EXPIRATION_DATE_FORMAT = "EEE',' dd-MMM-yyyy HH:mm:ss 'GMT'";

        public final byte[] EOL = {(byte) '\r', (byte) '\n'};
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
            notify();
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
            //log().debug("handle... '" + httpRequest.file + "'");
            HTTPResponse response = null;
            // the only page to show if not logged on is the login page
            if (httpRequest.file.startsWith("/login")) {
                response = loginHandler.getPage(httpRequest);
            }

            // check if logged on
            if (!loginHandler.checkSession(httpRequest.cookies, socket
                .getInetAddress()))
            {
                //log().debug("session not valid");
                // no valid session
                response = loginHandler.getPage(httpRequest);
            }

            if (response == null) {
                //log().debug("session valid");
                if (httpRequest.method.equals(HTTP_GET)) {
                    response = handleGET(httpRequest);
                } else if (httpRequest.method.equals(HTTP_HEAD)) {
                    // return only the headers not the contents,
                    // should be treated as GET
                    response = handleGET(httpRequest);
                    response.returnValue = false;
                } else {
                    /* we don't support this method */
                    printStream.print("HTTP/1.1 " + HTTP_BAD_METHOD
                        + " unsupported method type: ");
                    printStream.print(httpRequest.method);
                    printStream.write(EOL);
                    printStream.flush();
                    socket.close();
                    return;
                }
            }

            if (response == null) {
                VeloHandler velo = veloHandlers.get("/404");
                response = velo.getPage(httpRequest);
                response.responseCode = HTTP_NOT_FOUND;
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
            printStream.print("HTTP/1.1 " + response.responseCode + " OK");
            printStream.write(EOL);
            // PFWI is short for PowerFolder WebInterface
            printStream.print("Server: PFWI/" + Controller.PROGRAM_VERSION);
            printStream.write(EOL);
            printStream.print("Date: " + (new Date()));
            printStream.write(EOL);

            printStream.print("Content-length: " + response.contents.length);
            printStream.write(EOL);
            printStream.print("Last Modified: " + response.lastModified);
            printStream.write(EOL);
            printStream.print("Content-type: " + response.contentType);
            printStream.write(EOL);

            if (response.cookies != null) {
                // insert all cookies
                for (String name : response.cookies.keySet()) {
                    String value = response.cookies.get(name);
                    Calendar calNow = new GregorianCalendar();
                    calNow.add(Calendar.HOUR, 24); // one day expiration

                    DateFormat formatter = new SimpleDateFormat(
                        COOKIE_EXPIRATION_DATE_FORMAT, Locale.US);
                    String expirationDate = formatter.format(calNow.getTime());
                    String cookie = "Set-Cookie: " + name + "=" + value
                        + "; expires=" + expirationDate + "; path=/; domain="
                        + httpRequest.host;
                    
                    printStream.print(cookie);
                    printStream.write(EOL);
                }
            }
            // extra newline for end of headers / content follows
            printStream.write(EOL);
            if (response.returnValue) {
                printStream.write(response.contents);
            }
            printStream.flush();
        }

        private HTTPResponse handleGET(HTTPRequest request) {
            String file = request.file;
            VeloHandler veloHandler = getVeloHandler(file);
            if (veloHandler == null) {
                HTTPResponse response = fileHandler.doGet(request);
                return response;
            }
            return veloHandler.getPage(request);
        }
    }
}