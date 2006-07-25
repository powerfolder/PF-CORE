package de.dal33t.powerfolder.light;

import java.io.*;
import java.util.*;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.util.Convert;

/**
 * Container for saving and loading nodes.
 * This class is not Thread-safe.
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */
public class NodeList {
    private List<MemberInfo> nodeList;
    private Set<MemberInfo> friendsSet;
    

    public NodeList() {
    }

    /**
     * Initializes this NodeList with the current state of the NodeManager.
     * @param nm
     * @param onlySuperNodes true, if only supernodes should be included
     */
    public NodeList(NodeManager nm, boolean onlySuperNodes) {
        nodeList = new ArrayList<MemberInfo>();
        friendsSet = new HashSet<MemberInfo>();
        
        synchronized (nm) {
            for (Member m: nm.getNodes()) {
                if (!onlySuperNodes || m.isSupernode()) {
                    nodeList.add(m.getInfo());
                }
            }
            friendsSet.addAll(Arrays.asList(
                Convert.asMemberInfos(nm.getFriends())));
        }
    }
    
    /**
     * Returns the Set containing the friends.
     * @return
     */
    public Set<MemberInfo> getFriendsSet() {
        if (friendsSet == null) { 
            friendsSet = new HashSet<MemberInfo>();
        }

        return friendsSet;
    }

    /**
     * Returns the List containing the supernodes.
     * @return
     */
    public List<MemberInfo> getNodeList() {
        if (nodeList == null) {
            nodeList = new ArrayList<MemberInfo>();
        }

        return nodeList;
    }
    
    /**
     * Initializes this NodeList with the data from the given InputStream.
     * @param in
     * @throws IOException
     */
    public void load(InputStream in) throws IOException {
        ObjectInputStream oin = new ObjectInputStream(
            new BufferedInputStream(in));
        
        try {
            // Create new Lists/Sets instead of using those loaded.
            // This makes the use of this class independed from the implementation of
            // the saved objects.
            nodeList = new ArrayList<MemberInfo>((List<MemberInfo>) oin.readObject());
            
            friendsSet = new HashSet<MemberInfo>((Set<MemberInfo>) oin.readObject());
        } catch (ClassNotFoundException e) {
            throw new IOException(e.toString());
        }
    }
    
    /**
     * Saves this NodeLists contents to the given OutputStream.
     * @param out
     * @throws IOException
     */
    public void save(OutputStream out) throws IOException {
        ObjectOutputStream oout = new ObjectOutputStream(out);
        
        oout.writeObject(nodeList);
        oout.writeObject(friendsSet);
        
        oout.flush();
    }
    
    /**
     * Initializes this NodeList with the data from the given File.
     * @param file
     * @throws IOException
     */
    public void load(File file) throws IOException {
        InputStream in = new FileInputStream(file); 
        load(in);
        in.close();
    }
    
    /**
     * Saves this NodeLists contents to the given File.
     * @param file
     * @throws IOException
     */
    public void save(File file) throws IOException {
        OutputStream out = new FileOutputStream(file); 
        save(out);
        out.close();
    }
}
