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
 */
package de.dal33t.powerfolder.light;

import de.dal33t.powerfolder.Constants;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FolderInfoTest {

    @Test
    public void testGetMetaInfo() {
        FolderInfo foInfo = FolderInfoFactory.newTopFolderForTest("Name of folder");
        assertFalse(foInfo.isMetaFolder(), foInfo.toString());
        assertFalse(foInfo.id.contains(Constants.METAFOLDER_ID_PREFIX),
            foInfo.id);
        assertFalse(foInfo.getName().contains(Constants.METAFOLDER_ID_PREFIX),
            foInfo.getName());

        FolderInfo metaFolder = foInfo.getMetaFolderInfo();
        assertTrue(metaFolder.isMetaFolder(), metaFolder.toString());
        assertTrue(metaFolder.id.contains(Constants.METAFOLDER_ID_PREFIX),
            metaFolder.id);
        assertTrue(metaFolder.getName().contains(Constants.METAFOLDER_ID_PREFIX),
            metaFolder.getName());

        assertEquals(foInfo, metaFolder.lookupContentFolderInfo());
        assertEquals(metaFolder, foInfo.getMetaFolderInfo());

        // Fallback stuff if something really is wrong in the code:
        assertEquals(metaFolder, metaFolder.getMetaFolderInfo());
        assertEquals(foInfo, foInfo.lookupContentFolderInfo());
        assertFalse(metaFolder.equals(foInfo));
    }

    /**
     * PFS-5510: exhaustive coverage of the structure-based
     * {@link FolderInfo#findEnclosingSubFolder(java.util.Collection, FolderInfo, String)} - it powers
     * Account/Group.getAllowedAccess(folder, path) and FolderRepository.findEnclosingSubFolder.
     */
    @Test
    public void testFindEnclosingSubFolder() {
        FolderInfo top = FolderInfoFactory.newTopFolderForTest("TopFolder");
        FolderInfo otherTop = FolderInfoFactory.newTopFolderForTest("OtherTop");

        /*
         * Shares within "TopFolder":
         *
         * /TopFolder
         * ├── projects                      (projects)
         * │   └── team
         * │       └── reports               (projectsTeamReports)
         * ├── archive                       (archive, sibling)
         * ├── department/hr/contracts       (departmentHrContracts)
         * └── doc                           (doc - prefix of the UNSHARED "documents")
         *
         * /OtherTop
         * └── projects                      (projectsInOtherTop - same path, other top folder)
         */
        FolderInfo projects = newSubFolder(top, "projects");
        FolderInfo projectsTeamReports = newSubFolder(top, "projects/team/reports");
        FolderInfo archive = newSubFolder(top, "archive");
        FolderInfo departmentHrContracts = newSubFolder(top, "department/hr/contracts");
        FolderInfo doc = newSubFolder(top, "doc");
        FolderInfo projectsInOtherTop = newSubFolder(otherTop, "projects");

        // Top folders and null entries in the candidates must be ignored gracefully
        List<FolderInfo> candidates = Arrays.asList(projects, projectsTeamReports,
            archive, departmentHrContracts, doc, projectsInOtherTop,
            top, null);

        // --- Exact share roots resolve to themselves ---
        assertEquals(projects, FolderInfo.findEnclosingSubFolder(candidates, top, "projects"));
        assertEquals(archive, FolderInfo.findEnclosingSubFolder(candidates, top, "archive"));
        assertEquals(departmentHrContracts,
            FolderInfo.findEnclosingSubFolder(candidates, top, "department/hr/contracts"));

        // --- Locations deeper inside a share resolve to it, also multi-level unknown paths ---
        assertEquals(projects, FolderInfo.findEnclosingSubFolder(candidates, top, "projects/team"));
        assertEquals(projects,
            FolderInfo.findEnclosingSubFolder(candidates, top, "projects/misc/2026/plan.txt"));
        assertEquals(departmentHrContracts,
            FolderInfo.findEnclosingSubFolder(candidates, top, "department/hr/contracts/2026/new-hire.pdf"));

        // --- The innermost (nested) share wins over the outer one ---
        assertEquals(projectsTeamReports,
            FolderInfo.findEnclosingSubFolder(candidates, top, "projects/team/reports"));
        assertEquals(projectsTeamReports,
            FolderInfo.findEnclosingSubFolder(candidates, top, "projects/team/reports/q1/summary.pdf"));

        // --- Path-segment matching: the "doc" share must not swallow the unshared "documents" ---
        assertEquals(doc, FolderInfo.findEnclosingSubFolder(candidates, top, "doc/readme.txt"));
        assertNull(FolderInfo.findEnclosingSubFolder(candidates, top, "documents/readme.txt"));

        // --- Top root, the unshared levels above a deep share and unrelated paths: no match ---
        assertNull(FolderInfo.findEnclosingSubFolder(candidates, top, ""));
        assertNull(FolderInfo.findEnclosingSubFolder(candidates, top, "department"));
        assertNull(FolderInfo.findEnclosingSubFolder(candidates, top, "department/hr"));
        assertNull(FolderInfo.findEnclosingSubFolder(candidates, top, "unrelated/deep/path"));

        // --- Candidates are scoped to the addressed top folder ---
        assertEquals(projectsInOtherTop,
            FolderInfo.findEnclosingSubFolder(candidates, otherTop, "projects/inside"));
        assertEquals(projects, FolderInfo.findEnclosingSubFolder(candidates, top, "projects/inside"));

        // --- A subfolder as the addressed folder: its path is translated to top coordinates first ---
        assertEquals(projects, FolderInfo.findEnclosingSubFolder(candidates, projects, ""));
        assertEquals(projects, FolderInfo.findEnclosingSubFolder(candidates, projects, "team"));
        assertEquals(projectsTeamReports,
            FolderInfo.findEnclosingSubFolder(candidates, projects, "team/reports"));
        assertEquals(projectsTeamReports,
            FolderInfo.findEnclosingSubFolder(candidates, projects, "team/reports/q1/summary.pdf"));
        // null relativeName counts as the folder's own root
        assertEquals(projects, FolderInfo.findEnclosingSubFolder(candidates, projects, null));

        // --- Null / empty handling ---
        assertNull(FolderInfo.findEnclosingSubFolder(null, top, "projects"));
        assertNull(FolderInfo.findEnclosingSubFolder(candidates, null, "projects"));
        assertNull(FolderInfo.findEnclosingSubFolder(candidates, top, null));
        assertNull(FolderInfo.findEnclosingSubFolder(Collections.<FolderInfo>emptyList(), top, "projects"));
    }

    /**
     * PFC-3543: the barrier overload
     * {@link FolderInfo#findEnclosingSubFolder(java.util.Collection, FolderInfo[], FolderInfo, String)}.
     * Barriers are the subfolders with interrupted permission inheritance; they are handed in
     * separately because an account holds no permission on them and therefore does not carry them in
     * its own candidates. Purely structural here - the inheritance flag itself is not consulted, the
     * caller decides what is a barrier.
     */
    @Test
    public void testFindEnclosingSubFolderWithBarriers() {
        FolderInfo top = FolderInfoFactory.newTopFolderForTest("TopFolder");
        FolderInfo otherTop = FolderInfoFactory.newTopFolderForTest("OtherTop");

        /*
         * /TopFolder
         * ├── projects                      (projects - permitted)
         * │   ├── secret                    (projectsSecret - BARRIER)
         * │   │   └── shared                (projectsSecretShared - permitted BELOW the barrier)
         * │   └── team                      (not shared at all)
         * └── archive                       (archive - BARRIER, nothing permitted above it)
         *
         * /OtherTop
         * └── archive                       (archiveInOtherTop - BARRIER of another top folder)
         */
        FolderInfo projects = newSubFolder(top, "projects");
        FolderInfo projectsSecret = newSubFolder(top, "projects/secret");
        FolderInfo projectsSecretShared = newSubFolder(top, "projects/secret/shared");
        FolderInfo archive = newSubFolder(top, "archive");
        FolderInfo archiveInOtherTop = newSubFolder(otherTop, "archive");

        List<FolderInfo> permitted = Arrays.asList(projects, projectsSecretShared);
        // Top folders and null entries must be ignored gracefully here as well
        FolderInfo[] barriers = {projectsSecret, archive, archiveInOtherTop, top, null};
        FolderInfo[] noBarriers = new FolderInfo[0];

        // --- A barrier below a permitted subfolder wins: evaluation happens AT the barrier ---
        assertEquals(projectsSecret,
            FolderInfo.findEnclosingSubFolder(permitted, barriers, top, "projects/secret"));
        assertEquals(projectsSecret,
            FolderInfo.findEnclosingSubFolder(permitted, barriers, top, "projects/secret/2026/plan.txt"));

        // --- A permitted subfolder BELOW the barrier wins again: access was granted past it ---
        assertEquals(projectsSecretShared,
            FolderInfo.findEnclosingSubFolder(permitted, barriers, top, "projects/secret/shared"));
        assertEquals(projectsSecretShared,
            FolderInfo.findEnclosingSubFolder(permitted, barriers, top, "projects/secret/shared/deep/x.txt"));

        // --- Paths next to the barrier are unaffected ---
        assertEquals(projects, FolderInfo.findEnclosingSubFolder(permitted, barriers, top, "projects"));
        assertEquals(projects, FolderInfo.findEnclosingSubFolder(permitted, barriers, top, "projects/team/x.txt"));
        // "secretly" must not be swallowed by the "secret" barrier (segment-exact matching)
        assertEquals(projects,
            FolderInfo.findEnclosingSubFolder(permitted, barriers, top, "projects/secretly/x.txt"));

        // --- A barrier without anything permitted above it is still found ---
        assertEquals(archive, FolderInfo.findEnclosingSubFolder(permitted, barriers, top, "archive/x.txt"));
        assertEquals(archive, FolderInfo.findEnclosingSubFolder(Collections.<FolderInfo>emptyList(), barriers,
            top, "archive"));

        // --- Barriers are scoped to the addressed top folder, just like candidates ---
        assertEquals(archiveInOtherTop,
            FolderInfo.findEnclosingSubFolder(permitted, barriers, otherTop, "archive/x.txt"));
        assertNull(FolderInfo.findEnclosingSubFolder(permitted, barriers, otherTop, "projects/secret"));

        // --- A subfolder as the addressed folder: barriers match in top coordinates as well ---
        assertEquals(projectsSecret,
            FolderInfo.findEnclosingSubFolder(permitted, barriers, projects, "secret/x.txt"));
        assertEquals(projectsSecretShared,
            FolderInfo.findEnclosingSubFolder(permitted, barriers, projects, "secret/shared/x.txt"));

        // --- No barriers behaves exactly like the candidates-only resolution ---
        assertEquals(projects, FolderInfo.findEnclosingSubFolder(permitted, null, top, "projects/secret"));
        assertEquals(projects, FolderInfo.findEnclosingSubFolder(permitted, noBarriers, top, "projects/secret"));

        // --- Null / empty handling ---
        assertNull(FolderInfo.findEnclosingSubFolder(null, null, top, "projects"));
        assertNull(FolderInfo.findEnclosingSubFolder(null, barriers, null, "archive"));
        // Barriers alone are enough, no candidates needed
        assertEquals(archive, FolderInfo.findEnclosingSubFolder(null, barriers, top, "archive"));
    }

    private static FolderInfo newSubFolder(FolderInfo top, String path) {
        DirectoryInfo location = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(top, path,
            null, 0, null, null, new Date(), 1, null, true, null);
        return FolderInfoFactory.newFolder(location);
    }

}
