/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OAuth2ClientTest {

    private static final String REDIRECTS = "[\"https://claude.ai/api/mcp/auth_callback\"]";

    @Test
    public void testNewClient() {
        OAuth2Client client = OAuth2Client.newClient("Claude", REDIRECTS, OAuth2Client.SCOPE_MCP);

        assertNotNull(client.getId());
        assertEquals("Claude", client.getClientName());
        assertEquals(REDIRECTS, client.getRedirectURIs());
        assertEquals(OAuth2Client.SCOPE_MCP, client.getScope());
        assertEquals(OAuth2Client.AUTH_METHOD_NONE, client.getTokenEndpointAuthMethod());
        assertTrue(client.isPublicClient());
        assertNotNull(client.getCreated());
        assertNull(client.getLastUsed());
    }

    @Test
    public void testClientIdsAreUnique() {
        OAuth2Client one = OAuth2Client.newClient("Claude", REDIRECTS, OAuth2Client.SCOPE_MCP);
        OAuth2Client two = OAuth2Client.newClient("Claude", REDIRECTS, OAuth2Client.SCOPE_MCP);

        assertNotEquals(one.getId(), two.getId());
    }

    @Test
    public void testTouchSetsLastUsed() {
        OAuth2Client client = OAuth2Client.newClient("Claude", REDIRECTS, OAuth2Client.SCOPE_MCP);

        assertNull(client.getLastUsed());

        client.touch();

        assertNotNull(client.getLastUsed());
    }

    @Test
    public void testRedirectURIsMandatory() {
        try {
            OAuth2Client.newClient("Claude", null, OAuth2Client.SCOPE_MCP);
            fail("Client without redirect URIs must not be created");
        } catch (Exception e) {
        }

        try {
            OAuth2Client.newClient("Claude", "   ", OAuth2Client.SCOPE_MCP);
            fail("Client with blank redirect URIs must not be created");
        } catch (Exception e) {
        }
    }
}
