/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: CleanupTranslationFiles.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test.util;

import java.util.Properties;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.message.ConfigurationLoadRequest;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.ConfigurationLoader;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Test the loading of the config.
 * 
 * @author sprajc
 */
public class ConfigurationLoadTest extends TwoControllerTestCase {
    private static final String TEST_CONFIG_URL = "download.powerfolder.com/development/junit";
    private Member lisaAtBart;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        makeFriends();
        connectBartAndLisa();
        Member bartAtLisa = getContollerLisa().getNodeManager().getNode(
            getContollerBart().getMySelf().getInfo());
        bartAtLisa.setServer(true);

        lisaAtBart = getContollerBart().getNodeManager().getNode(
            getContollerLisa().getMySelf().getInfo());
    }

    public void testRestartOnConfigReload() throws ConnectionException {
        ConfigurationLoadRequest r = new ConfigurationLoadRequest(
            TEST_CONFIG_URL, false, true);
        lisaAtBart.sendMessage(r);

        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public String message() {
                return "Lisa started? " + getContollerLisa().isStarted();
            }

            public boolean reached() {
                return !getContollerLisa().isStarted()
                    && getContollerLisa().isRestartRequested();
            }
        });
    }

    public void testReloadConfig() throws ConnectionException {
        getContollerLisa().setNetworkingMode(NetworkingMode.LANONLYMODE);

        ConfigurationLoadRequest r = new ConfigurationLoadRequest("http://"
            + TEST_CONFIG_URL, false, false);
        lisaAtBart.sendMessage(r);

        TestHelper.waitMilliSeconds(1000);
        assertEquals(NetworkingMode.LANONLYMODE.toString(),
            ConfigurationEntry.NETWORKING_MODE.getValue(getContollerLisa()));
        // Non existing should have been added.
        assertTrue(ConfigurationEntry.USER_INTERFACE_LOCKED
            .getValueBoolean(getContollerLisa()));

        // Now overwrite values.
        r = new ConfigurationLoadRequest(TEST_CONFIG_URL + "/", true, false);
        lisaAtBart.sendMessage(r);

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public String message() {
                return "Lisas networking mode: "
                    + ConfigurationEntry.NETWORKING_MODE
                        .getValue(getContollerLisa());
            }

            public boolean reached() {
                return ConfigurationEntry.NETWORKING_MODE.getValue(
                    getContollerLisa()).equals(
                    NetworkingMode.SERVERONLYMODE.toString());
            }
        });
    }

    public void testMergePreferences() throws BackingStoreException {
        Preferences p = Preferences
            .userNodeForPackage(ConfigurationLoadTest.class);
        p.put("EXISTING", "existing value");

        Properties preConfig = new Properties();
        preConfig.put("IGNORED", "VALUE");
        preConfig.put("pref.APREF", "XXX");
        preConfig.put("pref.EXISTING", "NU VALUE");
        preConfig.put("pref.int", "4343");
        preConfig.put("pref.intBroken", "4x343");
        preConfig.put("pref.boolean", "true");
        preConfig.put("pref.booleanBroken", "1");
        preConfig.put("pref.long", "2343894738925");

        ConfigurationLoader.mergePreferences(preConfig, p, false);
        assertEquals("XXX", p.get("APREF", null));
        assertEquals("existing value", p.get("EXISTING", null));
        assertEquals(4343, p.getInt("int", -1));
        assertEquals(-1, p.getInt("intBroken", -1));
        assertTrue(p.getBoolean("boolean", false));
        assertFalse(p.getBoolean("booleanBroken", false));
        assertEquals(2343894738925L, p.getLong("long", -1));

        ConfigurationLoader.mergePreferences(preConfig, p, true);
        assertEquals("XXX", p.get("APREF", null));
        assertEquals("NU VALUE", p.get("EXISTING", null));
        assertEquals(4343, p.getInt("int", -1));
        assertEquals(-1, p.getInt("intBroken", -1));
        assertTrue(p.getBoolean("boolean", false));
        assertFalse(p.getBoolean("booleanBroken", false));
        assertEquals(2343894738925L, p.getLong("long", -1));
    }
}
