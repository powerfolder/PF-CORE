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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PFS-5306: Deterministic tests for folder tags on {@link FolderInfo}: the
 * factory mutation (version bump, no-op detection), carry-over through the other
 * factory mutators, JSON parsing and the wire-protocol escalation / backward
 * compatibility (tags only serialize at FolderInfo protocol 103, older peers
 * keep receiving &lt;= 102 streams).
 */
public class FolderInfoTagsTest {

    private static final String TAGS_JSON = "[\"Projekt\",\"2026\"]";

    private FolderInfo topFolder;
    private FolderInfo subFolder;

    @BeforeEach
    void setUp() throws Exception {
        topFolder = FolderInfoFactory.newTopFolderForTest("TopFolder", "TAG-TOP-1");
        subFolder = FolderInfoFactory.newFolder(
            FileInfoFactory.lookupDirectory(topFolder, "sub"));
    }

    // --- Factory: changing the tags bumps the folder version -----------------

    @Test
    public void testFactoryChangeBumpsVersionAndStoresTags() {
        int versionBefore = subFolder.getVersion();

        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);

        assertEquals(versionBefore + 1, tagged.getVersion(),
            "Version must be bumped when the tags change");
        assertEquals(TAGS_JSON, tagged.getTags(),
            "Tags must be stored on the new instance");
        assertEquals(subFolder.getId(), tagged.getId(),
            "Folder identity (id) must be preserved");
    }

    @Test
    public void testFactoryChangeIsNoOpWhenTagsUnchanged() {
        // Untagged (null) -> null must be a no-op.
        FolderInfo same = FolderInfoFactory.changeTags(subFolder, null);
        assertSame(subFolder, same, "Unchanged (null) tags must return the same instance");

        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo sameTagged = FolderInfoFactory.changeTags(tagged, TAGS_JSON);
        assertSame(tagged, sameTagged, "Unchanged tags must return the same instance");
        assertEquals(tagged.getVersion(), sameTagged.getVersion(),
            "Version must not change on a no-op");
    }

    @Test
    public void testFactoryUntagBumpsVersionAndClearsTags() {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo untagged = FolderInfoFactory.changeTags(tagged, null);
        assertEquals(tagged.getVersion() + 1, untagged.getVersion(),
            "Version must be bumped when untagging");
        assertNull(untagged.getTags(), "Tags must be cleared");
        assertTrue(untagged.getTagsList().isEmpty(), "Tag list must be empty when untagged");
    }

    // --- Carry-over through the other factory mutators -----------------------

    @Test
    public void testTagsSurviveRename() {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo renamed = FolderInfoFactory.rename(tagged, "RenamedSub");
        assertEquals(TAGS_JSON, renamed.getTags(), "Rename must not drop the tags");
    }

    @Test
    public void testTagsSurviveChangeParent() {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo moved = FolderInfoFactory.changeParent(tagged,
            FileInfoFactory.lookupDirectory(topFolder, "other/place"));
        assertEquals(TAGS_JSON, moved.getTags(), "Changing the parent must not drop the tags");
    }

    @Test
    public void testTagsSurviveResolveConflict() {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo resolved = FolderInfoFactory.resolveConflict(tagged);
        assertEquals(TAGS_JSON, resolved.getTags(), "Resolving a conflict must not drop the tags");
    }

    @Test
    public void testTagsSurviveChangeInheritsPermissions() {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo interrupted = FolderInfoFactory.changeInheritsPermissions(tagged, false);
        assertEquals(TAGS_JSON, interrupted.getTags(),
            "Changing inheritsPermissions must not drop the tags");
    }

    // --- Parsing --------------------------------------------------------------

    @Test
    public void testGetTagsListParsesJson() {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        assertEquals(Arrays.asList("Projekt", "2026"), tagged.getTagsList());
    }

    @Test
    public void testGetTagsListNeverNull() {
        assertNotNull(subFolder.getTagsList(), "Tag list must never be null");
        assertTrue(subFolder.getTagsList().isEmpty(), "Tag list must be empty when untagged");
    }

    // --- Wire protocol escalation & backward compatibility --------------------

    @Test
    public void testUntaggedFolderStaysAtOldProtocol() throws Exception {
        // No tags -> no need for 103 even towards a new peer.
        assertEquals(101L, writtenProtocolVersion(subFolder, true, false, true));
    }

    @Test
    public void testOldPeerNeverReceivesNewProtocol() throws Exception {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        // Peer negotiated < 116: tags must never escalate the protocol.
        assertEquals(101L, writtenProtocolVersion(tagged, true, false, false));
    }

    @Test
    public void testNewPeerReceivesTags() throws Exception {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        // Peer negotiated >= 116 and the folder is tagged: escalate to 103.
        assertEquals(103L, writtenProtocolVersion(tagged, true, false, true));
    }

    @Test
    public void testTagsRoundTripToNewPeer() throws Exception {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        FolderInfo read = roundTrip(tagged, true, false, true);
        assertEquals(TAGS_JSON, read.getTags(), "Tags must survive the wire to a new peer");
        assertEquals(Arrays.asList("Projekt", "2026"), read.getTagsList());
        assertTrue(read.inheritsPermissions(),
            "Absent interruption flag must be read as inheriting (default)");
    }

    @Test
    public void testOldPeerReadsTaggedFolderAsUntagged() throws Exception {
        FolderInfo tagged = FolderInfoFactory.changeTags(subFolder, TAGS_JSON);
        // Written for an old peer (no tags) and read back: must default to
        // untagged - the safe default, no garbage in the stream.
        FolderInfo read = roundTrip(tagged, true, false, false);
        assertNull(read.getTags(), "Absent tags must be read as untagged");
        assertTrue(read.getTagsList().isEmpty(), "Tag list must be empty when untagged");
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
