package de.dal33t.powerfolder.webservice;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.WebServiceClientEvent;
import de.dal33t.powerfolder.event.WebServiceClientListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.RequestBackup;
import de.dal33t.powerfolder.util.ByteSerializer;

/**
 * Client for sending requests to PowerFolder Webservice
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.6 $
 */
public class WebServiceClient extends PFComponent {
    /**
     * The official url to call the request againts. Development on this machine ;)
     */
    private static final String PF_WEBSERVICE_URL = "http://srv01.powerfolder.net:1339/RPC2";
    // private static final String PF_WEBSERVICE_URL =
    // "http://localhost:80/RPC2";

    private XmlRpcClient xmlrpcClient;
    private boolean isAvailable;
    private OwnStatus lastOwnStatus;
    private WebServiceClientListener listernSupport;

    /**
     * Initalizes
     * 
     * @param controller
     */
    public WebServiceClient(Controller controller) {
        super(controller);

        try {
            xmlrpcClient = new XmlRpcClient(PF_WEBSERVICE_URL);
        } catch (MalformedURLException e) {
            log()
                .error(
                    "Unable to initalize webservice client to "
                        + PF_WEBSERVICE_URL);
        }

        // Listener support
        listernSupport = (WebServiceClientListener) ListenerSupportFactory
            .createListenerSupport(WebServiceClientListener.class);

        isAvailable = false;
        // Check if service is available
//        new Thread("WebService availability checker") {
//            public void run() {
//                requestOwnStatus();
//            }
//        }.start();
    }

    /**
     * Answers if the client is correctly initalize
     * 
     * @return
     */
    public boolean isStarted() {
        return xmlrpcClient != null;
    }

    /**
     * Returns the last status of my account @ PowerFolder service
     * @return the status or null if not available
     */
    public OwnStatus getOwnStatus() {
        return lastOwnStatus;
    }

    /**
     * Answers if this folder is booked @ PowerFolder services
     * @param folder
     * @return
     */
    public boolean hasBookedFolder(FolderInfo folder) {
        if (lastOwnStatus == null || lastOwnStatus.folders == null) {
            return false;
        }
        return lastOwnStatus.folders.contains(folder);
    }

    /**
     * Checks the availability of the service
     */
    @SuppressWarnings("unchecked")
    private void requestOwnStatus()
    {
        if (!isStarted()) {
            throw new IllegalStateException(
                "PowerFolder WebService Client is not initalized");
        }

        log().warn("Requesting own status from webserivce");
        try {
            Vector xmlargs = new Vector();
            MemberInfo mySelfInfo = getController().getMySelf().getInfo();
            byte[] memberData = ByteSerializer.serializeStatic(mySelfInfo, true);
            xmlargs.add(memberData);
            // execute
            byte[] data = (byte[]) xmlrpcClient.execute("powerfolder.getInfo",
                xmlargs);
            lastOwnStatus = (OwnStatus) ByteSerializer.deserializeStatic(data, true);
            isAvailable = true;

            // Fire event
            listernSupport.receivedOwnStatus(new WebServiceClientEvent(this,
                lastOwnStatus));
        } catch (ClassCastException e) {
            log().error("Unable to execute request", e);
            isAvailable = false;
        } catch (IOException e) {
            log().error("Unable to execute request", e);
            isAvailable = false;
        } catch (XmlRpcException e) {
            log().error("Problem while executing request", e);
            isAvailable = false;
        } catch (ClassNotFoundException e) {
            log().error("Problem while executing request", e);
            isAvailable = false;
        }
    }

    /**
     * Checks if the service is available
     * 
     * @param foInfo
     */
    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Request the service to join the folder and tries to connect to the given
     * webserivce
     * 
     * @param foInfo
     */
    @SuppressWarnings("unchecked")
    public void requestJoinFolder(FolderInfo foInfo)
    {
        if (!isStarted()) {
            throw new IllegalStateException(
                "PowerFolder WebService Client is not initalized");
        }
        try {
            Vector xmlargs = new Vector();
            MemberInfo mySelfInfo = getController().getMySelf().getInfo();
            byte[] memberData = ByteSerializer.serializeStatic(mySelfInfo, true);
            xmlargs.add(memberData);

            RequestBackup request = new RequestBackup();
            request.folder = foInfo;
            byte[] backupData = ByteSerializer.serializeStatic(request, true);
            xmlargs.add(backupData);

            // execute
            byte[] assigendServerByte = (byte[]) xmlrpcClient.execute(
                "powerfolder.joinFolder", xmlargs);

            if (assigendServerByte != null) {
                MemberInfo assignedServerInfo = (MemberInfo) ByteSerializer
                    .deserializeStatic(assigendServerByte, true);
                Member assigendServer = assignedServerInfo.getNode(
                    getController(), true);
                // Set webservice as friend
                log().warn("Adding webservice to friends: " + assigendServer);
                assigendServer.setFriend(true);
            }

            // Refresh own status
            requestOwnStatus();
        } catch (IOException e) {
            log().error("Unable to execute folder join request", e);
        } catch (XmlRpcException e) {
            log().error("Problem while executing folder join request", e);
        } catch (ClassNotFoundException e) {
            log().error("Problem while executing folder join request", e);
        }
    }

    /**
     * Request the service to leave the folder
     * 
     * @param foInfo
     */
    @SuppressWarnings("unchecked")
    public void requestLeaveFolder(FolderInfo foInfo)
    {
        if (!isStarted()) {
            throw new IllegalStateException(
                "PowerFolder WebService Client is not initalized");
        }
        try {
            Vector xmlargs = new Vector();
            MemberInfo mySelfInfo = getController().getMySelf().getInfo();
            byte[] memberData = ByteSerializer.serializeStatic(mySelfInfo, true);
            xmlargs.add(memberData);

            byte[] data = ByteSerializer.serializeStatic(foInfo, true);
            xmlargs.add(data);

            // execute
            xmlrpcClient.execute("powerfolder.leaveFolder", xmlargs);

            // Refresh own status
            requestOwnStatus();
        } catch (IOException e) {
            log().error("Unable to execute folder leave request", e);
        } catch (XmlRpcException e) {
            log().error("Problem while executing folder leave request", e);
        }
    }
}
