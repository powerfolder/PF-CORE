package de.dal33t.powerfolder.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.UDTMessage;
import de.dal33t.powerfolder.message.UDTMessage.Type;
import de.dal33t.powerfolder.util.Partitions;
import de.dal33t.powerfolder.util.Range;
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
	private Semaphore pendingConnections = new Semaphore(Constants.MAX_PENDING_UDT_CONNECTIONS);

	public UDTSocketConnectionManager(Range portRange) {
		ports = new Partitions<PortSlot>(portRange, null);
	}
	
	/**
	 * Checks if UDT sockets are supported on this PF instance.
	 * @return
	 */
	public static boolean isSupported() {
		return UDTSocket.isSupported();
	}
	
	public ConnectionHandler initUDTConnectionHandler(MemberInfo destination) throws ConnectionException {
        if (getController().getMySelf().getInfo().equals(destination)) {
            throw new ConnectionException(
                "Illegal relayed loopback connection detection to myself");
        }
        
        if (pendingConnections.availablePermits() == 0) {
        	log().error("Maximum number of pending UDT connections reached!");
        }
        try {
        	// Aquire a permit for this pending connection
			pendingConnections.acquire();
		} catch (InterruptedException e) {
			log().error(e);
			return null;
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
        relay.sendMessage(syn);
        Future<UDTMessage> waiter = futureFor(destination);
        UDTMessage reply = null;
		try {
			reply = waiter.get(Constants.TO_UDT_CONNECTION, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			log().verbose(e);
			return null;
		} catch (ExecutionException e) {
			throw new ConnectionException(e);
		} catch (TimeoutException e) {
			throw new ConnectionException(e);
		} finally {
	        // Release a permit, so another blocked pending connection can act
	        pendingConnections.release();
		}

        if (reply != null && reply.getType() == UDTMessage.Type.ACK) {
	        ConnectionHandler handler = getController()
	        	.getIOProvider()
	        	.getConnectionHandlerFactory()
	        	.createUDTSocketConnectionHandler(getController(), slot.socket);
	        handler.init();
	        return handler;
        }
        
        return null;
	}

	private Future<UDTMessage> futureFor(MemberInfo destination) {
		// TODO Auto-generated method stub
		return null;
	}

	public void handleUDTMessage(UDTMessage msg) {
		switch (msg.getType()) {
		case SYN:
			break;
		case ACK:
		case NACK:
			break;
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
		while (true) {
			res = ports.search(ports.getPartionedRange(), null);
			if (res == null) {
				log().error("No further usable ports for UDT connections!");
				try {
					slot.socket.close();
				} catch (IOException e) {
					log().error(e);
				}
				return null;
			}
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
	public void freeSlot(int port) {
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
