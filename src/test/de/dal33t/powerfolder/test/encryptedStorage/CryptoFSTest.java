package de.dal33t.powerfolder.test.encryptedStorage;

import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;
import org.apache.commons.codec.digest.DigestUtils;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.CryptoFileSystemUris;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

import static java.nio.file.FileVisitResult.CONTINUE;
import static org.junit.Assert.*;

/**
 * JUnit Test for storage encryption with cryptolib and cryptofs.
 *
 * @author Jan Wiegmann <wiegmann@powerfolder.com>
 */

public class CryptoFSTest {

    private Path unencryptedSource;
    private Path encryptedDestination;
    private Path encryptedDestination2;
    private Path decryptedDestination;
    private FileSystem fileSystem;
    private FileSystem fileSystem2;

    @Before
    public void setUp() throws Exception {

        PathUtils.recursiveDelete(TestHelper.getTestDir());

        // Encrypted files
        unencryptedSource = TestHelper.getTestDir().resolve("unencryptedSource");
        Files.createDirectory(unencryptedSource);

        // Encrypted files
        encryptedDestination = TestHelper.getTestDir().resolve("encryptedDestination");
        Files.createDirectory(encryptedDestination);

        encryptedDestination2 = TestHelper.getTestDir().resolve("encryptedDestination2");
        Files.createDirectory(encryptedDestination2);

        // Unencrypted files after decrypting process.
        decryptedDestination = TestHelper.getTestDir().resolve("decryptedDestination");
        Files.createDirectory(decryptedDestination);

        // Cryptomator filesystems.
        fileSystem = initFileSystem(encryptedDestination, IdGenerator.makeId());
        fileSystem2 = initFileSystem(encryptedDestination2, IdGenerator.makeId());

        // Initialize stuff for FiveControllerTestCase.
        /*super.setUp();
        PreferencesEntry.EXPERT_MODE.setValue(getContollerHomer(), true);
        PreferencesEntry.EXPERT_MODE.setValue(getContollerMarge(), true);
        PreferencesEntry.EXPERT_MODE.setValue(getContollerLisa(), true);
        PreferencesEntry.EXPERT_MODE.setValue(getContollerMaggie(), true);
        PreferencesEntry.EXPERT_MODE.setValue(getContollerBart(), true);
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION, false);*/

    }

    private static FileSystem initFileSystem(Path encDir, String password) throws IOException {
        return CryptoFileSystemProvider.newFileSystem(
                encDir,
                CryptoFileSystemProperties.cryptoFileSystemProperties()
                        .withPassphrase(password)
                        .build());
    }

    @After
    public void shutDown() throws IOException {
        fileSystem.close();
    }

    /**
     * Simple file operations inside the encrypted filesystem and between encrypted filesystem and default filesystem.
     */

    @Test
    public void encryptSingleFile() throws IOException {

        // Create unencrypted test file.
        Path sourceFile = TestHelper.createRandomFile(unencryptedSource);

        // Create encrypted file over cryptofs.
        Path encryptedDirectory = fileSystem.getPath(encryptedDestination.toString());
        Path encFile = encryptedDirectory.resolve(sourceFile.getFileName().toString());

        Path encryptedDirectory2 = fileSystem2.getPath(encryptedDestination2.toString());
        Path encFile2 = encryptedDirectory2.resolve(sourceFile.getFileName().toString());

        // Copy into encryptedDestination directory.
        Files.createDirectories(encryptedDirectory);
        Files.copy(sourceFile, encFile);

        Files.createDirectories(encryptedDirectory2);
        Files.copy(encFile, encFile2);

        // Read from encrypted dir over crypto filesystem in clear text to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(fileSystem.getPath(encryptedDestination.toString()))) {
            listing.forEach(System.out::println);
        }

        // Copy from encrypted destination to decrypted destination.
        Path destFile = decryptedDestination.resolve(sourceFile.getFileName());

        Files.copy(encFile, destFile);

        // Check if unencryptedSource file is the same after decryption.
        assertTrue(TestHelper.compareFiles(sourceFile, destFile));

        assertTrue(TestHelper.compareFiles(sourceFile, encFile2));

        // Check encrypted output.
        checkEncryptionProcess(null, null);
    }

    @Test
    public void encryptMultiFiles() throws IOException {

        // Define how many test files should be created.
        int testFilesToCreate = 200;

        // Create test files.
        for (int i = 0; i < testFilesToCreate; i++) {
            TestHelper.createRandomFile(unencryptedSource);
        }

        // Create directory listing from all files before encryption.
        List<Path> beforeEncryption = new ArrayList<>();

        Files.walk(unencryptedSource)
                .filter(p -> !Files.isDirectory(p))
                .forEach(p -> beforeEncryption.add(p));

        // Encrypt all files and copy them to encryptedDestination dir.
        Path encryptedDirectory = fileSystem.getPath(encryptedDestination.toString());
        Files.createDirectories(encryptedDirectory);

        for (Path p : Files.newDirectoryStream(unencryptedSource)) {
            Files.copy(p, encryptedDirectory.resolve(p.getFileName().toString()));
        }

        // Read from encrypted dir over crypto filesystem in clear text to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(fileSystem.getPath(encryptedDestination.toString()))) {
            listing.forEach(System.out::println);
        }

        // Decrypt all files and copy them to decryptedDestination.
        for (Path p : Files.newDirectoryStream(fileSystem.getPath(encryptedDestination.toString()))) {
            Files.copy(p, decryptedDestination.resolve(p.getFileName().toString()));
        }

        // Create directory listing after decryption.
        List<Path> afterEncryption = new ArrayList<>();

        Files.walk(unencryptedSource)
                .filter(p -> !Files.isDirectory(p))
                .forEach(p -> afterEncryption.add(p));

        // Check encrypted output.
        checkEncryptionProcess(beforeEncryption, afterEncryption);
    }

    @Test
    public void encryptLargeFile() throws IOException {

        // Create random large file.
        Path largeFile = TestHelper.createRandomFile(unencryptedSource, 1024 /*1000L * 1024 * 1024*/);

        // Encrypt and copy large file to encrypted dir.
        Path encryptedDirectory = fileSystem.getPath(encryptedDestination.toString());
        Files.createDirectories(encryptedDirectory);

        Path encFile = encryptedDirectory.resolve(largeFile.getFileName().toString());
        Files.copy(largeFile, encFile);

        // Read from fileSystem dir over crypto filesystem in clear text to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(fileSystem.getPath(encryptedDestination.toString()))) {
            listing.forEach(System.out::println);
        }

        // Decrypt all files and copy them to decryptedDestination.
        Path decryptedLargeFile = null;
        for (Path p : Files.newDirectoryStream(fileSystem.getPath(encryptedDestination.toString()))) {
            decryptedLargeFile = p;
            Files.copy(p, decryptedDestination.resolve(p.getFileName().toString()));
        }

        // Check if unencryptedSource file is the same after decryption.
        assertTrue(TestHelper.compareFiles(largeFile, decryptedLargeFile));

        // Check encrypted output.
        checkEncryptionProcess(null, null);
    }

    @Test
    public void encryptUTF8NamedFile() throws IOException {

        // Create random file with UTF-8 filename.
        Path utf8File = TestHelper.createRandomFile(unencryptedSource, "\u00fc\u0080\u005EFOOBAR\u005B");

        // Encrypt and copy UTF-8 file to encryptedDestination.
        Path encryptedDirectory = fileSystem.getPath(encryptedDestination.toString());
        Files.createDirectories(encryptedDirectory);
        Files.copy(utf8File, encryptedDirectory.resolve(utf8File.getFileName().toString()));

        // Read from encryptedDestination over cryptofs in cleartext to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(fileSystem.getPath(encryptedDestination.toString()))) {
            listing.forEach(System.out::println);
        }

        // Decrypt all files and copy them to decryptedDestination.
        Path decryptedUTF8File = null;
        for (Path p : Files.newDirectoryStream(fileSystem.getPath(encryptedDestination.toString()))) {
            decryptedUTF8File = p;
            Files.copy(p, decryptedDestination.resolve(p.getFileName().toString()));
        }

        // Check if unencryptedSource file is the same after decryption.
        assertTrue(TestHelper.compareFiles(utf8File, decryptedUTF8File));

        // Check encrypted output.
        checkEncryptionProcess(null, null);
    }

    @Test
    public void encryptEmptyFiles() throws IOException {

        // Create unencrypted empty test file.
        Path sourceFile = TestHelper.createRandomFile(unencryptedSource, 0);
        // Would be nice: Path encFile = fileSystem.resolve(sourceFile.getFileName());

        Path encryptedDirectory = fileSystem.getPath(encryptedDestination.toString());

        Path encFile = encryptedDirectory.resolve(sourceFile.getFileName().toString());
        Path destFile = decryptedDestination.resolve(sourceFile.getFileName());

        // Copy into encryptedDestination directory.
        Files.createDirectories(encryptedDirectory);
        Files.copy(sourceFile, encFile);

        // Read from encrypted dir over crypto filesystem in clear text to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(fileSystem.getPath(encryptedDestination.toString()))) {
            listing.forEach(System.out::println);
        }

        // Copy from fileSystem FS to unencrypted directory.
        Files.copy(encFile, destFile);

        // Optional test: Is the file content identical?
        assertTrue(TestHelper.compareFiles(sourceFile, destFile));

        // Check encrypted output.
        checkEncryptionProcess(null, null);
    }

    @Test
    public void readFilesDateInsideEncryptedFS() throws IOException {

        // Create unencrypted test file.
        Path sourceFile = TestHelper.createRandomFile(unencryptedSource);

        // Create encrypted file over cryptofs.
        Path encryptedDirectory = fileSystem.getPath(encryptedDestination.toString());
        Path encFile = encryptedDirectory.resolve(sourceFile.getFileName().toString());

        // Copy into encryptedDestination directory.
        Files.createDirectories(encryptedDirectory);
        Files.copy(sourceFile, encFile);

        Date fileDate = new Date(Files.getLastModifiedTime(encFile).toMillis());

        assertNotNull(fileDate);
    }

    @Test
    public void readFilesAttributesInsideEncryptedFS() throws IOException {

        // Create unencrypted test file.
        Path sourceFile = TestHelper.createRandomFile(unencryptedSource);

        Map<String, Object> attrsSource;

        attrsSource = Files.readAttributes(sourceFile,
                "size,lastModifiedTime,isDirectory");

        assertNotNull(attrsSource);

        // Create encrypted file over cryptofs.
        Path encryptedDirectory = fileSystem.getPath(encryptedDestination.toString());
        Files.createDirectories(encryptedDirectory);

        Path encFile = encryptedDirectory.resolve(sourceFile.getFileName().toString());

        Files.copy(sourceFile, encFile);

        Map<String, Object> attrs;

        attrs = Files.readAttributes(encFile,
                "size,lastModifiedTime,isDirectory");

        assertNotNull(attrs);

    }

    @Test
    public void moveSubDirectoriesInsideEncryptedFS() throws IOException {

        encryptedDestination = fileSystem.getPath(encryptedDestination.toString());
        encryptedDestination2 = fileSystem2.getPath(encryptedDestination2.toString());

        Files.createDirectories(encryptedDestination.resolve("testDir1"));
        Files.createDirectories(encryptedDestination.resolve("testDir2"));
        Files.createDirectories(encryptedDestination.resolve("testDir3"));

        TestHelper.createRandomFile(encryptedDestination.resolve("testDir1"));
        TestHelper.createRandomFile(encryptedDestination.resolve("testDir2"));
        TestHelper.createRandomFile(encryptedDestination.resolve("testDir3"));
        TestHelper.createRandomFile(encryptedDestination);

        Files.walkFileTree(encryptedDestination, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,

                new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        Path targetDir = encryptedDestination2.resolve(encryptedDestination.relativize(dir).toString());
                        Files.createDirectories(targetDir);
                        try {
                            Files.copy(dir, targetDir);
                        } catch (FileAlreadyExistsException e) {
                            if (!Files.isDirectory(targetDir))
                                System.out.println("Could not move file.");
                        }
                        return CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        Files.move(file, encryptedDestination2.resolve(encryptedDestination.relativize(file).toString()));
                        return CONTINUE;
                    }
                });
    }

    @Test
    public void updateFilesInsideEncryptedFS() throws IOException {

        // Create unencrypted empty test file.
        Path sourceFile = TestHelper.createRandomFile(unencryptedSource, 0);

        Path encryptedDirectory = fileSystem.getPath(encryptedDestination.toString());
        Files.createDirectories(encryptedDirectory);

        Path encFile = encryptedDirectory.resolve(sourceFile.getFileName().toString());

        Path destFile = decryptedDestination.resolve(sourceFile.getFileName());

        // Copy from unencryptedSource into encryptedDestination.
        Files.copy(sourceFile, encFile);

        // Write to encrypted empty test file.
        Files.write(encFile, "foobar".getBytes());

        // Read from encryptedDestination over crypto filesystem in clear text to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(fileSystem.getPath(encryptedDestination.toString()))) {
            listing.forEach(System.out::println);
        }

        // Copy from encryptedDestination to decryptedDestination.
        Files.copy(encFile, destFile);

        // Check if file content is the same.
        assertTrue(TestHelper.compareFiles(encFile, destFile));
    }

    @Test
    public void moveFilesInsideEncryptedFS() throws IOException {

        Path sourceFile = TestHelper.createRandomFile(unencryptedSource, "foobar.txt");

        encryptedDestination = fileSystem.getPath(encryptedDestination.toString());
        decryptedDestination = fileSystem.getPath(decryptedDestination.toString());

        Path fileFrom = unencryptedSource.resolve(sourceFile.getFileName().toString());

        Path encFileTo = encryptedDestination.resolve(sourceFile.getFileName().toString());
        Files.createDirectories(encFileTo);

        Files.copy(fileFrom, encFileTo, StandardCopyOption.REPLACE_EXISTING);

        String md5File1 = DigestUtils.md5Hex(Files.readAllBytes(encFileTo));

        Path encFileMoved = decryptedDestination.resolve(sourceFile.getFileName().toString());
        Files.createDirectories(encFileMoved);

        Files.move(encFileTo, encFileMoved);

        String md5File2 = DigestUtils.md5Hex(Files.readAllBytes(encFileMoved));

        Files.walk(encryptedDestination)
                .forEach(p -> System.out.println(p));

        assertTrue(md5File1.equals(md5File2));

    }

    @Test
    public void copyFilesInsideEncryptedFS() throws IOException {

        Path sourceFile = TestHelper.createRandomFile(unencryptedSource, "foobar.txt");

        unencryptedSource = fileSystem.getPath(encryptedDestination.toString());
        encryptedDestination = fileSystem.getPath(unencryptedSource.toString());

        Path encFileFrom = unencryptedSource.resolve(sourceFile.getFileName().toString());
        Path encFileTo = encryptedDestination.resolve("foobar.txt");

        Files.copy(encFileFrom, encFileTo);

        Files.walk(encryptedDestination)
                .forEach(p -> System.out.println(p));

        assertTrue(TestHelper.compareFiles(encFileFrom, encFileTo));

    }

    @Test
    public void deleteFilesInsideEncryptedFS() throws IOException {

        Path sourceFile = TestHelper.createRandomFile(encryptedDestination, "foobar.txt");

        Path encFile = fileSystem.getPath(sourceFile.toString());

        Files.deleteIfExists(encFile);

        Files.walk(encryptedDestination)
                .forEach(p -> System.out.println(p));

        assertFalse(Files.exists(encFile));

    }

    /**
     * Multithreading test with encrypted filesystem.
     */

    @Test
    public void multiThreadTest() throws IOException {

        encryptedDestination = fileSystem.getPath(encryptedDestination.toString());
        Path encFile = TestHelper.createRandomFile(encryptedDestination, "foobar.txt");

        Runnable writeToTestFile = () -> {
            String foobar = "foobar";
            try {
                Files.write(encFile, foobar.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        Runnable readFromTestFile = () -> {
            try {
                String content = new String(Files.readAllBytes(encFile));
                System.out.println(content);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        writeToTestFile.run();
        readFromTestFile.run();

    }

    /**
     * Method to check the encrypted content from all cryptoFSTest methods.
     */

    public void checkEncryptionProcess(List<Path> beforeEncryption, List<Path> afterEncryption)
            throws IOException {

        if (beforeEncryption != null && afterEncryption != null) {
            for (int i = 0; i < beforeEncryption.size(); i++) {
                assertTrue(beforeEncryption.contains(afterEncryption.get(i)));
            }
        }

        Long[] unencryptedSourceInfo = PathUtils.calculateDirectorySizeAndCount(unencryptedSource);
        Long[] encryptedDestinationInfo = PathUtils.calculateDirectorySizeAndCount(encryptedDestination);
        Long[] unencryptedDestinationInfo = PathUtils.calculateDirectorySizeAndCount(decryptedDestination);

        assertTrue(unencryptedSourceInfo[0] <= encryptedDestinationInfo[0] &&
                unencryptedSourceInfo[1] < encryptedDestinationInfo[1]);

        assertTrue(unencryptedDestinationInfo[0] <= encryptedDestinationInfo[0] &&
                unencryptedDestinationInfo[1] < encryptedDestinationInfo[1]);

        System.out.println("-----------------------");
        System.out.println("1. Unencrypted file size before encryption: " + unencryptedSourceInfo[0] + " Unencrypted files total before encryption: " + unencryptedSourceInfo[1]);
        System.out.println("2. Encrypted file size: " + encryptedDestinationInfo[0] + " Encrypted files total: " + encryptedDestinationInfo[1]);
        System.out.println("3. Unencrypted file size after decryption: " + unencryptedDestinationInfo[0] + " Unencrypted files total before encryption: " + unencryptedDestinationInfo[1]);
        System.out.println("-----------------------");
    }

    /**
     * Several test methods with the encrypted filesystem.
     */

    @Test
    public void createCryptomatorURI() throws IOException {

        // Encrypted path.
        Path encPath = fileSystem.getPath(encryptedDestination.toString());

        // Create crypto URI and refer back to encrypted path instance.
        URI encFolderUri = CryptoFileSystemUris.createUri(encPath, encPath.toString());
        Path encPath2 = FileSystems.getFileSystem(encFolderUri).provider().getPath(encFolderUri);

        // Check if the final path is encrypted.
        assertEquals(encPath, encPath2);

    }

    @Test
    public void closeEncryptedFS() throws IOException {

        Path encPath1 = TestHelper.createRandomFile(encryptedDestination, "foobar1.txt");
        Path encPath2 = TestHelper.createRandomFile(encryptedDestination, "foobar.txt2");

        encPath1 = fileSystem.getPath(encryptedDestination.resolve(encPath1.getFileName()).toString());
        encPath2 = fileSystem2.getPath(encryptedDestination.resolve(encPath2.getFileName()).toString());

        encPath1.getFileSystem().close();

        encPath2 = encPath2.resolve("foo");

    }

}


