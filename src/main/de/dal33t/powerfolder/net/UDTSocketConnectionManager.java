package de.dal33t.powerfolder.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.UDTMessage;
import de.dal33t.powerfolder.message.UDTMessage.Type;
import de.dal33t.powerfolder.util.Partitions;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import de.dal33t.powerfolder.util.net.UDTSocket;

/**
 * Listens for incoming UDTMessages and either
 * 1) Processes them if the destination is this client or
 * 2) Sends the messages to the destination if possible
 *  
 * @author Dennis "Bytekeeper" Waldherr
 *
 */
public class UDTSocketConnectionManager extends PFComponent {
	private Partitions<PortSlot> ports;
	
	private Object pendReplyMon = new Object();
	private Map<MemberInfo, UDTMessage> replies = new HashMap<MemberInfo, UDTMessage>();
	
	public UDTSocketConnectionManager(Controller controller, Range portRange) {
		super(controller);
		ports = new Partitions<PortSlot>(portRange, null);
	}
	
	/**
	 * Creates and initializes an UDT connection.
	 * The means used for this are similar to that of the RelayedConnectionManager, in that a 
	 * special message is relayed to the target client.
	 * @param destination the destination member to connect to
	 * @return on successfully establishing a connection, null otherwise 
	 * @throws ConnectionException
	 */
	public ConnectionHandler initUDTConnectionHandler(MemberInfo destination) throws ConnectionException {
	    if (!UDTSocket.isSupported()) {
	        throw new ConnectionException("Missing UDT support!");
	    }
        if (getController().getMySelf().getInfo().equals(destination)) {
            throw new ConnectionException(
                "Illegal relayed loopback connection detection to myself");
        }
        
        // Use relay for UDT stuff too
        Member relay = getController().getIOProvider()
        	.getRelayedConnectionManager().getRelay();
        
        if (relay == null) {
            throw new ConnectionException(
                "Unable to open relayed connection to " + destination
                    + ". No relay found!");
        }
        PortSlot slot = selectPortFor(destination);
        if (slot == null) {
        	throw new ConnectionException("UDT port selection failed!");
        }
        UDTMessage syn = new UDTMessage(Type.SYN, getController().getMySelf().getInfo(),
        		destination, slot.port);
        try {
            relay.sendMessage(syn);

	        UDTMessage reply = waitForReply(destination);
	        switch (reply.getType()) {
	            case ACK:
	                log().debug("UDT SYN: Trying to connect...");
	                ConnectionHandler handler = getController()
	                    .getIOProvider()
	                    .getConnectionHandlerFactory()
	                    .createUDTSocketConnectionHandler(getController(), slot.socket, reply.getSource(), reply.getPort());
	                log().debug("UDT SYN: Successfully connected!");
	                return handler;
	            case NACK:
	                throw new ConnectionException("Connection not possible: " + reply);
                default:
                    log().debug("UDT SYN: Received invalid reply:" + reply);
                    throw new ConnectionException("Invalid reply: " + reply);
	        }
		} catch (TimeoutException e) {
			log().verbose(e);
	        throw new ConnectionException(e);
		} catch (InterruptedException e) {
            log().verbose(e);
            throw new ConnectionException(e);
        } finally {
	        // If we failed, release the slot
	        releaseSlot(slot.port);
		}
	}

	private UDTMessage waitForReply(MemberInfo destination) throws TimeoutException, InterruptedException {
		long to = System.currentTimeMillis() + Constants.TO_UDT_CONNECTION;
		synchronized (pendReplyMon) {
			UDTMessage msg = null;
			do {
				msg = replies.get(destination);
				if (msg == null) {
					try {
						pendReplyMon.wait(to - System.currentTimeMillis());
					} catch (InterruptedException e) {
						log().verbose(e);
						throw e;
					}
				}
			} while (msg == null && System.currentTimeMillis() < to);
			if (msg == null) {
				throw new TimeoutException();
			}
			return msg;
		}
	}

	/**
	 * Handles UDT messages.
	 * Based on the content of the message it might get relayed if the destination != mySelf or
	 * processed otherwise. 
	 * @param sender the Member who sent the message
	 * @param msg the message
	 */
	public void handleUDTMessage(final Member sender, final UDTMessage msg) {
		// Are we targeted ?
		if (msg.getDestination().getNode(getController()).isMySelf()) {
            log().debug("Received UDT message for me: " + msg);
		    if (!UDTSocket.isSupported()) {
	            log().warn("UDT sockets not supported on this platform.");
		        return;
		    }
			switch (msg.getType()) {
			case SYN:
				getController().getIOProvider().startIO(
						new Runnable() {
							public void run() {
								Member relay = getController().getIOProvider()
							 		.getRelayedConnectionManager().getRelay();
								if (relay == null) {
									log().error("Relay is null!");
									return;
								}
								PortSlot slot = selectPortFor(sender.getInfo());
								if (slot == null) {
									log().error("UDT port selection failed.");
									try {
										sender.sendMessage(
											new UDTMessage(Type.NACK, getController().getMySelf().getInfo(),
													msg.getSource(), -1));
									} catch (ConnectionException e) {
										log().error(e);
									}
									return;
								}
								try {
									relay.sendMessage(new UDTMessage(Type.ACK, getController().getMySelf().getInfo(),
											msg.getSource(), slot.port));
									ConnectionHandler handler = null;
									try {
							        	log().debug("UDT ACK: Trying to connect...");
										handler = getController().getIOProvider().getConnectionHandlerFactory()
											.createUDTSocketConnectionHandler(getController(), slot.socket, 
												msg.getSource(), msg.getPort());
			                            getController().getNodeManager().acceptConnection(
			                            		handler);
			            		        log().debug("UDT ACK: Successfully connected!");
									} catch (ConnectionException e) {
										if (handler != null) 
											handler.shutdown();
										throw e;
									}
								} catch (ConnectionException e) {
									log().error(e);
									releaseSlot(slot.port);
								}
							}
						});
				break;
			case ACK:
			case NACK:
				synchronized (pendReplyMon) {
					replies.put(msg.getSource(), msg);
					pendReplyMon.notifyAll();
				}
				break;
			}
		} else {
			log().verbose("Relaying UDT message: " + msg);
			// Relay message
			Member dMember = msg.getDestination().getNode(getController());
			if (dMember == null || !dMember.isCompleteyConnected()) {
				UDTMessage failedMsg = new UDTMessage(Type.NACK, msg.getDestination(), 
						msg.getSource(), -1);
				sender.sendMessagesAsynchron(failedMsg);
				return;
			}
			dMember.sendMessagesAsynchron(msg);
		}
	}
	
	/**
	 * Returns a port to use for the given destination.
	 * Each connection requires it's own port on both sides.
	 * The returned slot contains an UDTSocket which is already bound to the selected port.
	 * @param destination
	 * @return
	 */
	public PortSlot selectPortFor(MemberInfo destination) {
		Range res = null;
		// Try to bind port now to avoid surprises later
		PortSlot slot = new PortSlot(destination);
		slot.socket = new UDTSocket();
		try {
            NetworkUtil.setupSocket(slot.socket, destination.getConnectAddress());
        } catch (IOException e1) {
            log().error(e1);
        }
		while (true) {
			synchronized (this) {
				res = ports.search(ports.getPartionedRange(), null);
			}
			if (res == null) {
				log().error("No further usable ports for UDT connections!");
				try {
					slot.socket.close();
				} catch (IOException e) {
					log().error(e);
				}
				return null;
			}
			slot.port = (int) res.getStart();
			try {
				// TODO: Setup InetSocketAddress to bind to the configured address!!
				slot.socket.bind(new InetSocketAddress(slot.port));
				break;
			} catch (IOException e) {
				log().verbose(e);
				
				ports.insert(Range.getRangeByNumbers(res.getStart(), res.getStart()),
						PortSlot.LOCKED);
			}
		}
		ports.insert(Range.getRangeByNumbers(res.getStart(), res.getStart()),
				slot);
		return slot;
	}
	
	/**
	 * Frees a slot taken by selectPortFor
	 * @param port the port slot to free
	 */
	public synchronized void releaseSlot(int port) {
		ports.insert(Range.getRangeByLength(port, 1), null);
	}

	public static class PortSlot {
		/**  If a port is locked - it's pretty much dead (unusable for PF) */
		public static final PortSlot LOCKED = new PortSlot(); 

		private MemberInfo member;
		private UDTSocket socket;
		private int port;

		public PortSlot(MemberInfo destination) {
			member = destination;
		}
		
		private PortSlot() {
		}

		public MemberInfo getMember() {
			return member;
		}

		public UDTSocket getSocket() {
			return socket;
		}

		public int getPort() {
			return port;
		}
	}
}
