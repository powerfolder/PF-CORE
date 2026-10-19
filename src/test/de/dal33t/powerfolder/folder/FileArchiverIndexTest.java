package de.dal33t.powerfolder.folder;

import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.FileArchiverImpl;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SubFolderFileArchiverProxy;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.light.MemberInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PFS-5529: The archive answers "has this file a version" by exact name and extension, and "is there anything
 * below this directory" - both out of what it remembers about its directories, forgotten on every write.
 */
public class FileArchiverIndexTest {

    private Path base;
    private FolderInfo folderInfo;
    private FileArchiver archiver;

    @BeforeEach
    public void setUp() throws IOException {
        base = Files.createTempDirectory("pfs5529");
        folderInfo = FolderInfoFactory.newTopFolderForTest("archived", "archived-id");
        archiver = new FileArchiverImpl(base.resolve("archive"), new MemberInfo("node", "node-id", null));
    }

    @Test
    public void aVersionIsFoundByExactNameOnly() throws IOException {
        archiveVersionOf("report-final.txt");

        assertTrue(archiver.hasArchivedFileInfo(fileInfo("report-final.txt")), "The archived file has a version");
        assertFalse(archiver.hasArchivedFileInfo(fileInfo("report.txt")),
            "report.txt does not borrow the versions of report-final.txt - the old prefix match did");
        assertFalse(archiver.hasArchivedFileInfo(fileInfo("report-final.doc")), "Same name, other extension");
    }

    @Test
    public void anythingBelowIsAnsweredForDirectories() throws IOException {
        assertFalse(archiver.hasArchivedFilesBelow(""), "An empty archive holds nothing");

        archiveVersionOf("sub/deep/inside.txt");

        assertTrue(archiver.hasArchivedFilesBelow(""));
        assertTrue(archiver.hasArchivedFilesBelow("sub"));
        assertTrue(archiver.hasArchivedFilesBelow("sub/deep"));
        assertFalse(archiver.hasArchivedFilesBelow("other"), "A directory the archive does not know");
    }

    @Test
    public void aWriteOfTheArchiverIsSeenAtOnce() throws IOException {
        FileInfo first = fileInfo("first.txt");
        assertFalse(archiver.hasArchivedFileInfo(first), "Remembered: no version yet");

        archiveVersionOf("first.txt");
        assertTrue(archiver.hasArchivedFileInfo(first), "The archive forgot what it remembered when it wrote");
        assertTrue(archiver.hasArchivedFilesBelow(""));
    }

    @Test
    public void aDirectoryHasNoVersionOfItsOwn() throws IOException {
        archiveVersionOf("sub/inside.txt");

        DirectoryInfo sub = FileInfoFactory.lookupDirectory(folderInfo, "sub");
        assertFalse(archiver.hasArchivedFileInfo(sub), "A directory is never a version - the old prefix match said yes");
        assertTrue(archiver.hasArchivedFilesBelow(sub.getRelativeName()), "What keeps a deleted directory alive");
    }

    @Test
    public void aPurgeThroughTheSubfolderProxyIsSeenAtOnce() throws IOException {
        archiveVersionOf("sub/inside.txt");
        FolderInfo subInfo = FolderInfoFactory.newFolder(FileInfoFactory.lookupDirectory(folderInfo, "sub"));
        SubFolderFileArchiverProxy proxy = new SubFolderFileArchiverProxy((FileArchiverImpl) archiver, subInfo);
        Folder subFolder = mock(Folder.class);
        when(subFolder.getFileArchiver()).thenReturn(proxy);
        FileInfo inside = FileInfoFactory.lookupInstance(subInfo, "inside.txt");
        assertTrue(proxy.hasArchivedFileInfo(inside), "Remembered through the proxy");

        proxy.purge(inside, subFolder, null);

        assertFalse(proxy.hasArchivedFileInfo(inside), "The proxy deletes in the archive, so the archive forgets");
        assertFalse(proxy.hasArchivedFilesBelow(""), "Nothing left below the subfolder");
    }

    private void archiveVersionOf(String relativeName) throws IOException {
        Path source = base.resolve("source").resolve(relativeName);
        Files.createDirectories(source.getParent());
        Files.write(source, "content".getBytes(StandardCharsets.UTF_8));
        archiver.archive(fileInfo(relativeName), source, true);
    }

    private FileInfo fileInfo(String relativeName) {
        return FileInfoFactory.lookupInstance(folderInfo, relativeName);
    }
}
