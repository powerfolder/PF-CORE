/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test.message;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.net.NodeSearcher;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Tests the NodeSearcher. This test fails because there are also other users in
 * the net ... running this test on "art" finds: Member 'Art' (-disco.-, recon.
 * at 185.74.205.68.cfl.res.rr.com/68.205.74.185:1337) Member 'Eef en Martha'
 * (-disco.-, recon. at fia62-17-100.dsl.hccnet.nl/80.100.17.62:1337) Member
 * 'Bart' (localhost/127.0.0.1:52574) Member 'martin' (-disco.-, recon. at
 * wayhome.ath.cx/82.83.194.183:1337) Member 'Melissa Garth' (-disco.-, recon.
 * at c-24-125-41-64.hsd1.va.comcast.net/24.125.41.64:1337)
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class NodeSearcherTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();

        // Add some users to our protagonists

        MemberInfo maggi = new MemberInfo("Maggi", IdGenerator.makeId(), null);
        maggi.setLastConnectTime(new Date());
        // Marge is very long offline
        MemberInfo marge = new MemberInfo("Marge", IdGenerator.makeId(), null);
        marge.setLastConnectTime(new Date(System.currentTimeMillis()
            - Constants.NODE_TIME_TO_INVALIDATE - 10000));

        getContollerLisa().getNodeManager().addNode(maggi);
        getContollerLisa().getNodeManager().addNode(marge);

        MemberInfo homer = new MemberInfo("Homer", IdGenerator.makeId(), null);
        homer.setLastConnectTime(new Date());
        homer.setConnectAddress(new InetSocketAddress("127.0.0.1", 234));
        MemberInfo flenders = new MemberInfo("Ned Flenders",
            IdGenerator.makeId(), null);
        flenders.setLastConnectTime(new Date());
        flenders.setConnectAddress(new InetSocketAddress("127.0.0.1", 2314));
        MemberInfo moe = new MemberInfo("Moe", IdGenerator.makeId(), null);
        moe.setLastConnectTime(new Date(System.currentTimeMillis()
            - Constants.NODE_TIME_TO_INVALIDATE - 10000));
        moe.setConnectAddress(new InetSocketAddress("127.0.0.1", 333));

        getContollerBart().getNodeManager().addNode(homer);
        getContollerBart().getNodeManager().addNode(flenders);
        getContollerBart().getNodeManager().addNode(moe);

        assertTrue(getContollerBart().getMySelf().isSupernode());
        Member bartAtLisa = getContollerLisa().getNodeManager()
            .getConnectedNodes().iterator().next();
        assertEquals(getContollerBart().getMySelf(), bartAtLisa);
        // assertTrue("Bart is not known as supernode @ Lisa", bartAtLisa
        // .isSupernode());
    }

    /**
     * Tests the search on local node database without the need of sending
     * search requests to supernodes.
     */
    public void testLocalSearch() {
        List<Member> searchResultModel = new ArrayList<Member>();

        // Search for a node, which cannot be found
        NodeSearcher searcher = new NodeSearcher(getContollerLisa(), "xxx",
            searchResultModel, true, false);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertTrue(searchResultModel.isEmpty());

        // Search for "Bart" by nick
        searcher = new NodeSearcher(getContollerLisa(), "art",
            searchResultModel, true, false);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertFalse(searchResultModel.isEmpty());
        System.out.println("members found: ");
        for (Member member : searchResultModel) {
            System.out.println(member);
        }

        assertEquals(1, searchResultModel.size());
        assertEquals(getContollerBart().getMySelf(), searchResultModel.get(0));

        // Search for "Maggi" by nick
        searcher = new NodeSearcher(getContollerLisa(), "MAGGI",
            searchResultModel, true, false);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        // assertFalse(searchResultModel.toString(),
        // searchResultModel.isEmpty());
        assertEquals(searchResultModel.toString(), 1, searchResultModel.size());
        assertEquals("Maggi", searchResultModel.get(0).getNick());

        // Search for "Marge" by nick. Is invalid, but on local database = found
        searcher = new NodeSearcher(getContollerLisa(), "marge",
            searchResultModel, true, false);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertFalse(searchResultModel.isEmpty());
        assertEquals(1, searchResultModel.size());
        assertEquals("Marge", searchResultModel.get(0).getNick());
    }

    /**
     * Tests the search on with the need of sending search requests to
     * supernodes.
     */
    public void testSupernodeSearch() {
        List<Member> searchResultModel = new ArrayList<Member>();

        // Search for "Homer" by nick
        NodeSearcher searcher = new NodeSearcher(getContollerLisa(), "homer",
            searchResultModel, true, false);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertFalse(searchResultModel.isEmpty());
        assertEquals(1, searchResultModel.size());
        assertEquals("Homer", searchResultModel.get(0).getNick());

        // Search for "moe" by nick. Should not be found since he is invalid
        searcher = new NodeSearcher(getContollerLisa(), "MOE",
            searchResultModel, true, false);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertTrue(searchResultModel.isEmpty());
    }

    public void testMixedSearchMultiple() throws Exception {
        for (int i = 0; i < 100; i++) {
            testMixedSearch();
            tearDown();
            setUp();
        }
    }

    /**
     * Both cases with multiple search results.
     */
    public void testMixedSearch() {
        final List<Member> searchResultModel = new ArrayList<Member>();

        // Search for "ar"
        NodeSearcher searcher = new NodeSearcher(getContollerLisa(), "ar",
            searchResultModel, true, false);
        searcher.start();
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return searchResultModel.size() == 2;
            }

            public String message() {
                return "Expected: 2. Found: " + searchResultModel.size() + ": " + searchResultModel.toString();
            }
        });
        searcher.cancelSearch();
        assertFalse(searchResultModel.isEmpty());
        // bARt, homer and mARge, ned flenders, PowerFolder Cloud
        assertEquals(searchResultModel.toString(), 2, searchResultModel.size());

        // Search for "127.0.0.1"
        searcher = new NodeSearcher(getContollerLisa(), "127.0.0.1",
            searchResultModel, true, false);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertFalse(searchResultModel.isEmpty());
        // Bart, Homer, Ned Flenders
        assertEquals(3, searchResultModel.size());

        // Search for hostname (NO LONGER SUPPORTED)
        searcher = new NodeSearcher(getContollerLisa(), "localhost",
            searchResultModel, true, false);
        searcher.start();
        TestHelper.waitMilliSeconds(1000);
        searcher.cancelSearch();
        assertTrue(searchResultModel.isEmpty());
        // None found = not really searched
        assertEquals(0, searchResultModel.size());
    }
}
