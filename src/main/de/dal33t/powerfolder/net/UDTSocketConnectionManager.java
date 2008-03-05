package de.dal33t.powerfolder.net;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.UDTMessage;
import de.dal33t.powerfolder.message.UDTMessage.Type;
import de.dal33t.powerfolder.util.Partitions;
import de.dal33t.powerfolder.util.Range;

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
	
	public UDTSocketConnectionManager(Range portRange) {
		ports = new Partitions<PortSlot>(portRange, null);
	}
	
	public ConnectionHandler initUDTConnectionHandler(MemberInfo destination) throws ConnectionException {
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
        int port = selectPortFor(destination);
        if (port < 0) {
        	throw new ConnectionException("UDT port selection failed!");
        }
        UDTMessage syn = new UDTMessage(Type.SYN, getController().getMySelf().getInfo(),
        		destination, port);
        return null;
	}

	/**
	 * Returns a port to use for the given destination.
	 * Each connection requires it's own port on both sides.
	 * @param destination
	 * @return
	 */
	private int selectPortFor(MemberInfo destination) {
		Range res = ports.search(ports.getPartionedRange(), null);
		if (res == null) {
			log().error("No further usable ports for UDT connections!");
			return -1;
		}
		ports.insert(Range.getRangeByNumbers(res.getStart(), res.getStart()),
				new PortSlot(destination) );
		return (int) res.getStart();
	}

	private class PortSlot {
		public MemberInfo member;
		/**  If a port is locked - it's pretty much dead (unusable for PF) */
		public boolean locked;		

		public PortSlot(MemberInfo destination) {
			member = destination;
		}
	}
}
