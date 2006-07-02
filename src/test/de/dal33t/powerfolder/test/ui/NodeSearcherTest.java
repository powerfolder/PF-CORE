/* $Id$
 */
package de.dal33t.powerfolder.test.ui;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.net.NodeSearcher;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.util.IdGenerator;

/**
 * Tests the NodeSearcher.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class NodeSearcherTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        // Add some users to our protagonists

        MemberInfo maggi = new MemberInfo("Maggi", IdGenerator.makeId());
        maggi.lastConnectTime = new Date();
        // Marge is very long offline
        MemberInfo marge = new MemberInfo("Marge", IdGenerator.makeId());
        marge.lastConnectTime = new Date(System.currentTimeMillis()
            - Constants.NODE_TIME_TO_INVALIDATE - 10000);

        getContollerLisa().getNodeManager().addNode(maggi);
        getContollerLisa().getNodeManager().addNode(marge);

        MemberInfo homer = new MemberInfo("Homer", IdGenerator.makeId());
        homer.lastConnectTime = new Date();
        homer.setConnectAddress(new InetSocketAddress("localhost", 234));
        MemberInfo flenders = new MemberInfo("Ned Flenders", IdGenerator
            .makeId());
        flenders.lastConnectTime = new Date();
        flenders.setConnectAddress(new InetSocketAddress("localhost", 2314));
        MemberInfo moe = new MemberInfo("Moe", IdGenerator.makeId());
        moe.lastConnectTime = new Date(System.currentTimeMillis()
            - Constants.NODE_TIME_TO_INVALIDATE - 10000);
        moe.setConnectAddress(new InetSocketAddress("localhost", 333));

        getContollerBart().getNodeManager().addNode(homer);
        getContollerBart().getNodeManager().addNode(flenders);
        getContollerBart().getNodeManager().addNode(moe);

        assertTrue(getContollerBart().getMySelf().isSupernode());
    }

    /**
     * Tests the search on local node database without the need of sending
     * search requests to supernodes.
     */
    public void testLocalSearch() {
        List searchResultModel = new ArrayList();

        // Search for a node, which cannot be found
        NodeSearcher searcher = new NodeSearcher(getContollerLisa(), "xxx",
            searchResultModel);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertTrue(searchResultModel.isEmpty());

        // Search for "Bart" by nick
        searcher = new NodeSearcher(getContollerLisa(), "art",
            searchResultModel);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertFalse(searchResultModel.isEmpty());
        assertEquals(1, searchResultModel.size());
        assertEquals(getContollerBart().getMySelf(), searchResultModel.get(0));

        // Search for "Maggi" by nick
        searcher = new NodeSearcher(getContollerLisa(), "MAGGI",
            searchResultModel);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertFalse(searchResultModel.isEmpty());
        assertEquals(1, searchResultModel.size());
        assertEquals("Maggi", ((Member) searchResultModel.get(0)).getNick());

        // Search for "Marge" by nick. Is invalid, but on local database = found
        searcher = new NodeSearcher(getContollerLisa(), "marge",
            searchResultModel);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertFalse(searchResultModel.isEmpty());
        assertEquals(1, searchResultModel.size());
        assertEquals("Marge", ((Member) searchResultModel.get(0)).getNick());
    }

    /**
     * Tests the search on with the need of sending search requests to
     * supernodes.
     */
    public void testSupernodeSearch() {
        List searchResultModel = new ArrayList();

        // Search for "Homer" by nick
        NodeSearcher searcher = new NodeSearcher(getContollerLisa(), "homer",
            searchResultModel);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertFalse(searchResultModel.isEmpty());
        assertEquals(1, searchResultModel.size());
        assertEquals("Homer", ((Member) searchResultModel.get(0))
            .getNick());

        // Search for "moe" by nick. Should not be found since he is invalid
        searcher = new NodeSearcher(getContollerLisa(), "MOE",
            searchResultModel);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertTrue(searchResultModel.isEmpty());
    }

    /**
     * Both cases with multiple search results.
     */
    public void testMixedSearch() {
        List searchResultModel = new ArrayList();

        // Search for "r"
        NodeSearcher searcher = new NodeSearcher(getContollerLisa(), "r",
            searchResultModel);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertFalse(searchResultModel.isEmpty());
        // baRt, homeR and maRge, ned flendeRs
        assertEquals(4, searchResultModel.size());

        // Search for "127.0.0.1"
        searcher = new NodeSearcher(getContollerLisa(), "127.0.0.1",
            searchResultModel);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertFalse(searchResultModel.isEmpty());
        // Bart, Homer, Ned Flenders
        assertEquals(3, searchResultModel.size());

        // Search for hostname
        searcher = new NodeSearcher(getContollerLisa(), "localhost",
            searchResultModel);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertFalse(searchResultModel.isEmpty());
        // Bart, Homer, Ned Flenders
        assertEquals(3, searchResultModel.size());
    }
}
