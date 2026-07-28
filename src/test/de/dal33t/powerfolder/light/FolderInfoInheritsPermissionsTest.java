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

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.security.Permission;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * PFC-3543: Deterministic tests for the "interruption of permission inheritance"
 * flag on {@link FolderInfo}: the feature gate, the effect on permission
 * evaluation, and the wire-protocol negotiation / backward compatibility.
 * <p>
 * The mixed "one side has the feature on, the other off (or is an old peer)"
 * scenario is exercised here at the protocol level by writing for a negotiated
 * remote version of 114 (old) versus &gt;= 115 (new) - no network needed and
 * fully deterministic.
 */
public class FolderInfoInheritsPermissionsTest extends TestCase {

    private FolderInfo topFolder;
    private FolderInfo subFolder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        topFolder = FolderInfoFactory.newTopFolderForTest("TopFolder", "INH-TOP-1");
        subFolder = FolderInfoFactory.newFolder(
            FileInfoFactory.lookupDirectory(topFolder, "sub"));
        // Defined starting state: feature off (also the production default).
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
    }

    @Override
    protected void tearDown() throws Exception {
        // The feature flag is process-wide - never leak it into other tests.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    // --- Feature gate & permission evaluation --------------------------------

    public void testFeatureDisabledAlwaysInherits() {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        // Stored as interrupted, but the disabled feature must ignore it.
        subFolder = withInheritsPermissions(subFolder, false);
        assertTrue("Feature off: subfolder must still inherit",
            subFolder.inheritsPermissions());

        // Top-folder permission still reaches the subfolder.
        Permission topAdmin = FolderPermission.admin(topFolder);
        assertTrue("Feature off: inheritance must not be broken",
            topAdmin.implies(FolderPermission.read(subFolder)));
    }

    public void testFeatureEnabledHonorsInterruption() {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        assertTrue("Default is inheriting", subFolder.inheritsPermissions());

        subFolder = withInheritsPermissions(subFolder, false);
        assertFalse("Feature on: interruption must be honored",
            subFolder.inheritsPermissions());

        // Inheritance broken -> top-folder permission no longer reaches subfolder.
        Permission topAdmin = FolderPermission.admin(topFolder);
        assertFalse("Interrupted subfolder must not inherit top permission",
            topAdmin.implies(FolderPermission.read(subFolder)));
    }

    public void testTopFolderAlwaysInherits() {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        topFolder = withInheritsPermissions(topFolder, false);
        assertTrue("Top folders are the root of inheritance and never interrupted",
            topFolder.inheritsPermissions());
    }

    // --- Factory: changing the flag bumps the folder version ------------------

    public void testFactoryChangeBumpsVersionAndAppliesFlag() {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        int versionBefore = subFolder.getVersion();

        FolderInfo interrupted =
            FolderInfoFactory.changeInheritsPermissions(subFolder, false);

        assertEquals("Version must be bumped when the flag changes",
            versionBefore + 1, interrupted.getVersion());
        assertFalse("Flag must be applied on the new instance",
            interrupted.inheritsPermissions());
        assertEquals("Folder identity (id) must be preserved",
            subFolder.getId(), interrupted.getId());
    }

    public void testFactoryChangeIsNoOpWhenFlagUnchanged() {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        // subFolder inherits by default -> asking for the same must be a no-op.
        FolderInfo same = FolderInfoFactory.changeInheritsPermissions(subFolder, true);
        assertSame("Unchanged flag must return the same instance", subFolder, same);
        assertEquals("Version must not change on a no-op",
            subFolder.getVersion(), same.getVersion());
    }

    // --- Wire protocol negotiation & backward compatibility ------------------

    public void testFeatureOffKeepsOldProtocol() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        subFolder = withInheritsPermissions(subFolder, false);
        // Even towards a >= 115 peer: feature off must never emit protocol 102.
        assertEquals(101L, writtenProtocolVersion(subFolder, true, true));
    }

    public void testOldPeerNeverReceivesNewProtocol() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        subFolder = withInheritsPermissions(subFolder, false);
        // Peer negotiated < 115 (e.g. an old 114 subfolder peer): stays at 101.
        assertEquals(101L, writtenProtocolVersion(subFolder, true, false));
    }

    public void testNewPeerReceivesInterruptionFlag() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        subFolder = withInheritsPermissions(subFolder, false);
        // Peer negotiated >= 115 and the folder is interrupted: escalate to 102.
        assertEquals(102L, writtenProtocolVersion(subFolder, true, true));
    }

    public void testNonInterruptedFolderStaysAtOldProtocol() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        // Not interrupted -> no need for 102 even towards a new peer.
        assertEquals(101L, writtenProtocolVersion(subFolder, true, true));
    }

    public void testInterruptionRoundTripsToNewPeer() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        subFolder = withInheritsPermissions(subFolder, false);
        FolderInfo read = roundTrip(subFolder, true, true);
        assertFalse("Interruption must survive the wire to a new peer",
            read.inheritsPermissions());
    }

    public void testOldPeerReadsInterruptedFolderAsInheriting() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        subFolder = withInheritsPermissions(subFolder, false);
        // Written for an old peer (no flag) and read back: must default to
        // inheriting, i.e. the safe default - no silent wrong interruption.
        FolderInfo read = roundTrip(subFolder, true, false);
        assertTrue("Absent flag must be read as inheriting",
            read.inheritsPermissions());
    }

    // --- helpers -------------------------------------------------------------

    /**
     * Test replacement for the removed in-place setter: same-version copy with the
     * flag applied (FolderInfo is immutable, package-private constructor).
     */
    private static FolderInfo withInheritsPermissions(FolderInfo fi, boolean inherits) {
        return new FolderInfo(fi.getName(), fi.getId(), fi.getVersion(), fi.getParent(),
            fi.storedTags(), inherits);
    }

    private byte[] serialize(FolderInfo fi, boolean inclVersionAndParent,
        boolean inclInheritsPermissions) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            fi.writeExternal(oos, inclVersionAndParent, inclInheritsPermissions);
        }
        return bos.toByteArray();
    }

    private FolderInfo roundTrip(FolderInfo fi, boolean inclVersionAndParent,
        boolean inclInheritsPermissions) throws Exception
    {
        byte[] bytes = serialize(fi, inclVersionAndParent, inclInheritsPermissions);
        try (ObjectInputStream ois =
                 new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return FolderInfo.readExt(ois);
        }
    }

    /** @return the leading protocol version FolderInfo wrote to the stream. */
    private long writtenProtocolVersion(FolderInfo fi, boolean inclVersionAndParent,
        boolean inclInheritsPermissions) throws Exception
    {
        byte[] bytes = serialize(fi, inclVersionAndParent, inclInheritsPermissions);
        try (ObjectInputStream ois =
                 new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return ois.readLong();
        }
    }
}