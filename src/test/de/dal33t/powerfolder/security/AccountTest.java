/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
 * $Id$
 */
package de.dal33t.powerfolder.security;

import org.json.JSONException;
import org.json.JSONObject;

import junit.framework.TestCase;

public class AccountTest extends TestCase {
    public void testJSONData() throws JSONException {
        Account a = new Account();
        JSONObject o = a.getJSONData();
        o.put("cmpEnabled", true);
        o.put("avangateSubscriptionID", "3DEC58");
        a.setJSONData(o);

        JSONObject r = a.getJSONData();
        assertTrue(r.getBoolean("cmpEnabled"));
        assertEquals("3DEC58", r.get("avangateSubscriptionID"));

        a.put("avangateSubscriptionID", "XASA");

        r = a.getJSONData();
        assertTrue(r.getBoolean("cmpEnabled"));
        assertEquals("XASA", r.get("avangateSubscriptionID"));
    }
}
