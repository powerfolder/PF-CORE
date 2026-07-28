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
package de.dal33t.powerfolder.light;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

/**
 * PFS-5306: Deterministic tests for workspace tags on {@link FolderInfo}: the
 * factory mutation (version bump, no-op detection), carry-over through the other
 * factory mutators, JSON parsing and the wire-protocol escalation / backward
 * compatibility (tags only serialize at FolderInfo protocol 103, older peers
 * keep receiving &lt;= 102 streams).
 */
public class FolderInfoTagsTest extends TestCase {

    private static final String TAGS_JSON = "[\"Projekt\",\"2026\"]";

    private FolderInfo topFolder;
    private FolderInfo subFolder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        topFolder = FolderInfoFactory.newTopFolderForTest("TopFolder", "TAG-TOP-1");
        subFolder = FolderInfoFactory.newFolder(
            FileInfoFactory.lookupDirectory(topFolder, "sub"));
    }

    // --- Factory: changing the tags bumps the folder version -----------------

    public void testFactoryChangeBumpsVersionAndStoresTags() {
        int versionBefore = subFolder.getVersion();

        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);

        assertEquals("Version must be bumped when the tags change",
            versionBefore + 1, tagged.getVersion());
        assertEquals("Tags must be stored on the new instance",
            TAGS_JSON, tagged.getTags());
        assertEquals("Folder identity (id) must be preserved",
            subFolder.getId(), tagged.getId());
    }

    public void testFactoryChangeIsNoOpWhenTagsUnchanged() {
        // Untagged (null) -> null must be a no-op.
        FolderInfo same = FolderInfoFactory.changeTags(subFolder, null);
        assertSame("Unchanged (null) tags must return the same instance", subFolder, same);

        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo sameTagged = FolderInfoFactory.changeTags(tagged, TAGS_JSON);
        assertSame("Unchanged tags must return the same instance", tagged, sameTagged);
        assertEquals("Version must not change on a no-op",
            tagged.getVersion(), sameTagged.getVersion());
    }

    public void testFactoryUntagBumpsVersionAndClearsTags() {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo untagged = FolderInfoFactory.changeTags(tagged, null);
        assertEquals("Version must be bumped when untagging",
            tagged.getVersion() + 1, untagged.getVersion());
        assertNull("Tags must be cleared", untagged.getTags());
        assertTrue("Tag list must be empty when untagged", untagged.getTagsList().isEmpty());
    }

    // --- Carry-over through the other factory mutators -----------------------

    public void testTagsSurviveRename() {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo renamed = FolderInfoFactory.rename(tagged, "RenamedSub");
        assertEquals("Rename must not drop the tags", TAGS_JSON, renamed.getTags());
    }

    public void testTagsSurviveChangeParent() {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo moved = FolderInfoFactory.changeParent(tagged,
            FileInfoFactory.lookupDirectory(topFolder, "other/place"));
        assertEquals("Changing the parent must not drop the tags", TAGS_JSON, moved.getTags());
    }

    public void testTagsSurviveResolveConflict() {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo resolved = FolderInfoFactory.resolveConflict(tagged);
        assertEquals("Resolving a conflict must not drop the tags", TAGS_JSON, resolved.getTags());
    }

    public void testTagsSurviveChangeInheritsPermissions() {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo interrupted = FolderInfoFactory.changeInheritsPermissions(tagged, false);
        assertEquals("Changing inheritsPermissions must not drop the tags",
            TAGS_JSON, interrupted.getTags());
    }

    // --- Parsing --------------------------------------------------------------

    public void testGetTagsListParsesJson() {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        assertEquals(Arrays.asList("Projekt", "2026"), tagged.getTagsList());
    }

    public void testGetTagsListNeverNull() {
        assertNotNull("Tag list must never be null", subFolder.getTagsList());
        assertTrue("Tag list must be empty when untagged", subFolder.getTagsList().isEmpty());
    }

    // --- Wire protocol escalation & backward compatibility --------------------

    public void testUntaggedFolderStaysAtOldProtocol() throws Exception {
        // No tags -> no need for 103 even towards a new peer.
        assertEquals(101L, writtenProtocolVersion(subFolder, true, false, true));
    }

    public void testOldPeerNeverReceivesNewProtocol() throws Exception {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        // Peer negotiated < 116: tags must never escalate the protocol.
        assertEquals(101L, writtenProtocolVersion(tagged, true, false, false));
    }

    public void testNewPeerReceivesTags() throws Exception {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        // Peer negotiated >= 116 and the folder is tagged: escalate to 103.
        assertEquals(103L, writtenProtocolVersion(tagged, true, false, true));
    }

    public void testTagsRoundTripToNewPeer() throws Exception {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo read = roundTrip(tagged, true, false, true);
        assertEquals("Tags must survive the wire to a new peer", TAGS_JSON, read.getTags());
        assertEquals(Arrays.asList("Projekt", "2026"), read.getTagsList());
        assertTrue("Absent interruption flag must be read as inheriting (default)",
            read.inheritsPermissions());
    }

    public void testOldPeerReadsTaggedFolderAsUntagged() throws Exception {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        // Written for an old peer (no tags) and read back: must default to
        // untagged - the safe default, no garbage in the stream.
        FolderInfo read = roundTrip(tagged, true, false, false);
        assertNull("Absent tags must be read as untagged", read.getTags());
        assertTrue("Tag list must be empty when untagged", read.getTagsList().isEmpty());
    }

    // --- helpers ---------------------------------------------------------------

    private byte[] serialize(FolderInfo fi, boolean inclVersionAndParent,
        boolean inclInheritsPermissions, boolean inclTags) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            fi.writeExternal(oos, inclVersionAndParent, inclInheritsPermissions, inclTags);
        }
        return bos.toByteArray();
    }

    private FolderInfo roundTrip(FolderInfo fi, boolean inclVersionAndParent,
        boolean inclInheritsPermissions, boolean inclTags) throws Exception
    {
        byte[] bytes = serialize(fi, inclVersionAndParent, inclInheritsPermissions, inclTags);
        try (ObjectInputStream ois =
                 new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return FolderInfo.readExt(ois);
        }
    }

    /** @return the leading protocol version FolderInfo wrote to the stream. */
    private long writtenProtocolVersion(FolderInfo fi, boolean inclVersionAndParent,
        boolean inclInheritsPermissions, boolean inclTags) throws Exception
    {
        byte[] bytes = serialize(fi, inclVersionAndParent, inclInheritsPermissions, inclTags);
        try (ObjectInputStream ois =
                 new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return ois.readLong();
        }
    }
}
