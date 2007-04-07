/* $Id: MemberComparator.java,v 1.12 2006/01/28 02:59:20 totmacherr Exp $
 */
package de.dal33t.powerfolder.util.compare;

import java.util.Comparator;
import java.util.Date;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.TransferStatus;
import de.dal33t.powerfolder.util.Loggable;

/**
 * Comparator for members
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.12 $
 */
public class MemberComparator extends Loggable implements Comparator {
    /** In gui used member sorting */
    public static final MemberComparator IN_GUI = new MemberComparator(0);
    /** Sorts members by last (direct) connection date, latest first */
    public static final MemberComparator BY_LAST_CONNECT_DATE = new MemberComparator(
        1);
    /**
     * Member get compared by their connection to us (localnet, inet), localnets
     * first
     */
    public static final MemberComparator BY_CONNECTION_TYPE = new MemberComparator(
        2);

    /** Sorts nodes by last connection time to the network */
    public static final MemberComparator BY_RECONNECTION_PRIORITY = new MemberComparator(
        3);
    /** Sorts nodes by last connection time to the network */
    public static final MemberComparator BY_UPLOAD_AVAILIBILITY = new MemberComparator(
        4);

    /** Sorts nodes by nickname */
    public static final MemberComparator NICK = new MemberComparator(5);

    /** Sorts nodes by hostname */
    public static final MemberComparator HOSTNAME = new MemberComparator(6);

    /** Sorts nodes by IP */
    public static final MemberComparator IP = new MemberComparator(7);

    private int type;

    private MemberComparator(int type) {
        this.type = type;
    }

    public int compare(Object o1, Object o2) {
        if (o1 instanceof Member && o2 instanceof Member) {
            Member member1 = (Member) o1;
            Member member2 = (Member) o2;
            int result = 0;

            if (type == 0) {
                // Sort, used in the gui
                // myself at top
                if (member1.isMySelf()) {
                    result -= 2000;
                }
                if (member2.isMySelf()) {
                    result += 2000;
                }
                // then connected members
                if (member1.isConnected()) {
                    result -= 500;
                }
                if (member2.isConnected()) {
                    result += 500;
                }
                // then connected to networked
                if (member1.isConnectedToNetwork()) {
                    result -= 100;
                }
                if (member2.isConnectedToNetwork()) {
                    result += 100;
                }
                // friends then
                if (member1.isFriend()) {
                    result -= 1000;
                }
                if (member2.isFriend()) {
                    result += 1000;
                }

                // otherwise sort after nick
                result += member1.getNick().toLowerCase().compareTo(
                    member2.getNick().toLowerCase());
            } else if (type == 1) {
                // Sort by last connect time
                result = compareDates(member1.getInfo().lastConnectTime,
                    member2.getInfo().lastConnectTime);
            } else if (type == 2) {
                // Sort by connection type
                result -= member1.isOnLAN() ? 100 : 0;
                result += member2.isOnLAN() ? 100 : 0;
            } else if (type == 3) {
                // Sort by reconn priority
                result = compareDates(member1.getLastNetworkConnectTime(),
                    member2.getLastNetworkConnectTime());

                if (member1.isFriend()) {
                    result -= 8;
                }
                if (member2.isFriend()) {
                    result += 8;
                }
                if (member1.isSupernode()) {
                    result -= 4;
                }
                if (member2.isSupernode()) {
                    result += 4;
                }
                if (member1.isConnectedToNetwork()) {
                    result -= 2;
                }
                if (member2.isConnectedToNetwork()) {
                    result += 2;
                }
            } else if (type == 4) {
                // Check lan connection
                result += member1.isOnLAN() ? 10000 : 0;
                result -= member2.isOnLAN() ? 10000 : 0;

                // Compare by upload availibility
                long tsresult = compareTransferStatus(member1
                    .getLastTransferStatus(), member2.getLastTransferStatus());

                result += tsresult;

                // log().warn("TS Result between " + member1.getNick() + " and "
                // + member2.getNick() +": " + tsresult);
            } else if (type == 5) { // nickname
                return member1.getNick().toLowerCase().compareTo(
                    member2.getNick().toLowerCase());
            } else if (type == 6) { // hostname
                return member1.getHostName().toLowerCase().compareTo(
                    member2.getHostName().toLowerCase());
            } else if (type == 7) { // ip
                String ip1 = member1.getIP();
                if (ip1 == null) {
                    ip1 = "";
                }
                String ip2 = member2.getIP();
                if (ip2 == null) {
                    ip2 = "";
                }
                return compareIPs(ip1, ip2);
            }

            return result;
        } else if (o1 instanceof MemberInfo && o2 instanceof MemberInfo) {
            if (type == 1) {
                // Sort by last connect time
                return compareDates(((MemberInfo) o1).lastConnectTime,
                    ((MemberInfo) o1).lastConnectTime);
            }
        }
        return 0;
    }

    /**
     * Compares two transfer stati
     * 
     * @param t1
     * @param t2
     * @return
     */
    private int compareTransferStatus(TransferStatus t1, TransferStatus t2) {
        if (t1 == null) {
            return t2 == null ? 0 : -5000;
        } else if (t2 == null) {
            return 5000;
        }
        int result = 0;

        if (t1.getFreeUploadSlots() > 0) {
            result += 2000;
        }
        if (t2.getFreeUploadSlots() > 0) {
            result -= 2000;
        }

        // Calculate available upload cps
        long uploadCPSDiffer = (t1.getAvailbleUploadCPS() - t2
            .getAvailbleUploadCPS());

        // Chop between -1000 and 1000
        result += Math.max(Math.min(uploadCPSDiffer / 10, 1000), -1000);

        return result;
    }

    /**
     * Compares two dates. They may also be null
     * 
     * @param d1
     * @param d2
     * @return
     */
    private int compareDates(Date d1, Date d2) {
        long time1 = d1 != null ? d1.getTime() : 0;
        long time2 = d2 != null ? d2.getTime() : 0;
        if (time1 == time2) {
            return 0;
        }
        return (time1 - time2 > 0 ? -1 : 1);
    }

    /** compare two Strings that represent IP addresses */
    private int compareIPs(String ip1, String ip2) {
        if (ip1.trim().equals("") && ip2.trim().equals("")) {
            return 0;
        }
        if (ip1.trim().equals("")) {
            return -1;
        }        
        if (ip2.trim().equals("")) {
            return -1;
        }
        String[] ip1Array = ip1.split("\\.");
        String[] ip2Array = ip2.split("\\.");        
        for (int i = 0; i <= 3; i++) {
            int part1 = Integer.parseInt(ip1Array[i]);
            int part2 = Integer.parseInt(ip2Array[i]);
            if (part1 == part2) {
                continue;
            }
            if (part1 < part2)
                return -1;
            if (part1 > part2)
                return 1;
        }
        return 0;
    }
}