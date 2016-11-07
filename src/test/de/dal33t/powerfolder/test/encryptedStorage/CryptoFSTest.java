package de.dal33t.powerfolder.test.encryptedStorage;

import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.PathUtils;
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
import java.nio.file.attribute.UserPrincipal;
import java.util.*;
import java.util.stream.Stream;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.Assert.*;

/**
 * Cryptomator cryptolib and cryptofs JUnit tests.
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
        encryptedDestination = TestHelper.getTestDir().resolve("encryptedDestination.crypto");
        Files.createDirectory(encryptedDestination);

        encryptedDestination2 = TestHelper.getTestDir().resolve("encryptedDestination2.crypto");
        Files.createDirectory(encryptedDestination2);

        // Unencrypted files after decrypting process.
        decryptedDestination = TestHelper.getTestDir().resolve("decryptedDestination");
        Files.createDirectory(decryptedDestination);

        // Cryptomator filesystems.
        fileSystem = initFileSystem(encryptedDestination, IdGenerator.makeId());
        fileSystem2 = initFileSystem(encryptedDestination2, IdGenerator.makeId());

        encryptedDestination = fileSystem.getPath("/testDir");
        Files.createDirectories(encryptedDestination);

        encryptedDestination2 = fileSystem2.getPath("/testDir");
        Files.createDirectories(encryptedDestination2);

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
        fileSystem2.close();
    }

    /**
     * Simple file operations inside the encrypted filesystem and between encrypted filesystem and default filesystem.
     */

    @Test
    public void encryptSingleFile() throws IOException {

        // Create unencrypted test file.
        Path sourceFile = TestHelper.createRandomFile(unencryptedSource);

        // Create encrypted file over cryptofs.
        
        Path encFile = encryptedDestination.resolve(sourceFile.getFileName().toString());
        Path encFile2 = encryptedDestination2.resolve(sourceFile.getFileName().toString());

        // Copy into encryptedDestination directory.
        Files.copy(sourceFile, encFile);
        Files.copy(encFile, encFile2);

        // Read from encrypted dir over crypto filesystem in clear text to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(encryptedDestination)) {
            listing.forEach(System.out::println);
        }

        // Copy from encrypted destination to decrypted destination.
        Path destFile = decryptedDestination.resolve(sourceFile.getFileName());

        Files.copy(encFile, destFile);

        // Check if unencryptedSource file is the same after decryption.
        assertTrue(TestHelper.compareFiles(sourceFile, destFile));

        assertTrue(TestHelper.compareFiles(sourceFile, encFile2));

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

        for (Path p : Files.newDirectoryStream(unencryptedSource)) {
            Files.copy(p, encryptedDestination.resolve(p.getFileName().toString()));
        }

        // Read from encrypted dir over crypto filesystem in clear text to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(encryptedDestination)) {
            listing.forEach(System.out::println);
        }

        // Decrypt all files and copy them to decryptedDestination.
        for (Path p : Files.newDirectoryStream(encryptedDestination)) {
            Files.copy(p, decryptedDestination.resolve(p.getFileName().toString()));
        }

        // Create directory listing after decryption.
        List<Path> afterEncryption = new ArrayList<>();

        Files.walk(unencryptedSource)
                .filter(p -> !Files.isDirectory(p))
                .forEach(p -> afterEncryption.add(p));

        // Check encrypted output.
        assertEquals(beforeEncryption.size(), afterEncryption.size());
    }

    @Test
    public void encryptLargeFile() throws IOException {

        // Create random large file.
        Path largeFile = TestHelper.createRandomFile(unencryptedSource, 1024 /*1000L * 1024 * 1024*/);

        // Encrypt and copy large file to encrypted dir.

        Path encFile = encryptedDestination.resolve(largeFile.getFileName().toString());
        Files.copy(largeFile, encFile);

        // Read from fileSystem dir over crypto filesystem in clear text to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(encryptedDestination)) {
            listing.forEach(System.out::println);
        }

        // Decrypt all files and copy them to decryptedDestination.
        Path decryptedLargeFile = null;
        for (Path p : Files.newDirectoryStream(encryptedDestination)) {
            decryptedLargeFile = p;
            Files.copy(p, decryptedDestination.resolve(p.getFileName().toString()));
        }

        // Check if unencryptedSource file is the same after decryption.
        assertTrue(TestHelper.compareFiles(largeFile, decryptedLargeFile));

    }

    @Test
    public void encryptUTF8NamedFile() throws IOException {

        // Create random file with UTF-8 filename.
        Path utf8File = TestHelper.createRandomFile(unencryptedSource, "\u00fc\u0080\u005EFOOBAR\u005B");

        // Encrypt and copy UTF-8 file to encryptedDestination.
        Files.copy(utf8File, encryptedDestination.resolve(utf8File.getFileName().toString()));

        // Read from encryptedDestination over cryptofs in cleartext to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(encryptedDestination)) {
            listing.forEach(System.out::println);
        }

        // Decrypt all files and copy them to decryptedDestination.
        Path decryptedUTF8File = null;
        for (Path p : Files.newDirectoryStream(encryptedDestination)) {
            decryptedUTF8File = p;
            Files.copy(p, decryptedDestination.resolve(p.getFileName().toString()));
        }

        // Check if unencryptedSource file is the same after decryption.
        assertTrue(TestHelper.compareFiles(utf8File, decryptedUTF8File));

    }

    @Test
    public void encryptEmptyFiles() throws IOException {

        // Create unencrypted empty test file.
        Path sourceFile = TestHelper.createRandomFile(unencryptedSource, 0);
        // Would be nice: Path encFile = fileSystem.resolve(sourceFile.getFileName());

        Path encFile = encryptedDestination.resolve(sourceFile.getFileName().toString());
        Path destFile = decryptedDestination.resolve(sourceFile.getFileName());

        // Copy into encryptedDestination directory.
        Files.copy(sourceFile, encFile);

        // Read from encrypted dir over crypto filesystem in clear text to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(encryptedDestination)) {
            listing.forEach(System.out::println);
        }

        // Copy from fileSystem FS to unencrypted directory.
        Files.copy(encFile, destFile);

        // Optional test: Is the file content identical?
        assertTrue(TestHelper.compareFiles(sourceFile, destFile));

    }

    @Test
    public void readFilesDateInsideEncryptedFS() throws IOException {

        // Create unencrypted test file.
        Path sourceFile = TestHelper.createRandomFile(unencryptedSource);

        // Create encrypted file over cryptofs.
        
        Path encFile = encryptedDestination.resolve(sourceFile.getFileName().toString());

        // Copy into encryptedDestination directory.
        Files.copy(sourceFile, encFile);

        Date fileDate = new Date(Files.getLastModifiedTime(encFile).toMillis());

        assertNotNull(fileDate);
    }

    @Test
    public void readFilesAttributesInsideEncryptedFS() throws IOException {

        Path sourceFile = TestHelper.createRandomFile(unencryptedSource);
        Map<String, Object> attrsSource;
        attrsSource = Files.readAttributes(sourceFile,
                "size,lastModifiedTime,isDirectory");
        assertNotNull(attrsSource);

        Path encFile = encryptedDestination.resolve(sourceFile.getFileName().toString());
        Files.copy(sourceFile, encFile);
        Map<String, Object> attrs;
        attrs = Files.readAttributes(encFile,
                "size,lastModifiedTime,isDirectory");
        assertNotNull(attrs);

        Path encSubDir = encryptedDestination.resolve("sub");
        Files.createDirectory(encSubDir);
        assertTrue(Files.exists(encSubDir));

    }

    @Test
    public void getFileOwnerFromEncryptedFile() throws IOException {

        // Create unencrypted empty test file.
        Path sourceFile = TestHelper.createRandomFile(unencryptedSource);
        Path encFile = encryptedDestination.resolve(sourceFile.getFileName().toString());

        // Copy into encryptedDestination directory.
        Files.copy(sourceFile, encFile);

        UserPrincipal fileOwner = null;
        fileOwner = Files.getOwner(encFile);

    }

    @Test
    public void testDirectoryStream() throws IOException {

        Path sub = unencryptedSource.resolve("sub");
        Files.createDirectories(sub);

        try (DirectoryStream<Path> stream = Files
                .newDirectoryStream(unencryptedSource)) {

            Iterator<Path> it = stream.iterator();
            while (it.hasNext()) {
                Path path = it.next();
                assertTrue(Files.exists(path));
            }
        }

        sub = encryptedDestination.resolve("sub");
        Files.createDirectories(sub);

        try (DirectoryStream<Path> stream = Files
                .newDirectoryStream(encryptedDestination)) {

            Iterator<Path> it = stream.iterator();
            while (it.hasNext()) {
                Path path = it.next();
                assertTrue(Files.exists(path));
            }
        }


    }

    @Test
    public void moveSubDirectoriesInsideEncryptedFS() throws IOException {

        // Test dirs.
        Files.createDirectories(encryptedDestination.resolve("testDir1"));
        Files.createDirectories(encryptedDestination.resolve("testDir2"));
        Files.createDirectories(encryptedDestination.resolve("testDir3"));

        // Test files.
        TestHelper.createRandomFile(encryptedDestination.resolve("testDir1"));
        TestHelper.createRandomFile(encryptedDestination.resolve("testDir2"));
        TestHelper.createRandomFile(encryptedDestination.resolve("testDir3"));
        TestHelper.createRandomFile(encryptedDestination);

        List<Path> beforeMoving = new ArrayList<>();

        Files.walk(encryptedDestination)
                .forEach(p -> beforeMoving.add(p));

        Files.walkFileTree(encryptedDestination, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,

                new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        Path targetDir = encryptedDestination2.resolve(encryptedDestination.relativize(dir)
                                .toString());
                        if (!Files.exists(targetDir)) {
                            Files.createDirectories(targetDir);
                        }
                        try {
                            Files.copy(dir, targetDir, REPLACE_EXISTING);
                        } catch (FileAlreadyExistsException e) {
                            if (!Files.isDirectory(targetDir))
                                System.out.println("Could not move file.");
                        }
                        return CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        Files.move(file, encryptedDestination2.resolve(encryptedDestination.relativize(file)
                                .toString()), REPLACE_EXISTING);
                        return CONTINUE;
                    }
                });

        List<Path> afterMoving = new ArrayList<>();

        Files.walk(encryptedDestination2)
                .forEach(p -> afterMoving.add(p));

        assertEquals(beforeMoving.size(), afterMoving.size());

    }

    @Test
    public void updateFilesInsideEncryptedFS() throws IOException {

        // Create unencrypted empty test file.
        Path sourceFile = TestHelper.createRandomFile(unencryptedSource, 0);

        Path encFile = encryptedDestination.resolve(sourceFile.getFileName().toString());

        Path destFile = decryptedDestination.resolve(sourceFile.getFileName());

        // Copy from unencryptedSource into encryptedDestination.
        Files.copy(sourceFile, encFile);

        // Write to encrypted empty test file.
        Files.write(encFile, "foobar".getBytes());

        // Read from encryptedDestination over crypto filesystem in clear text to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(encryptedDestination)) {
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

        encryptedDestination = encryptedDestination;
        decryptedDestination = fileSystem.getPath(decryptedDestination.toString());

        Path fileFrom = unencryptedSource.resolve(sourceFile.getFileName().toString());
        Path encFileTo = encryptedDestination.resolve(sourceFile.getFileName().toString());
        Files.createDirectories(encFileTo);

        Files.copy(fileFrom, encFileTo, REPLACE_EXISTING);

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

        encryptedDestination2 = fileSystem2.getPath(encryptedDestination2.toString());

        decryptedDestination = Paths.get(decryptedDestination.toString());
        Files.createDirectories(decryptedDestination);

        Path encFileTo = encryptedDestination.resolve(sourceFile.getFileName().toString());
        Files.copy(sourceFile, encFileTo, REPLACE_EXISTING);

        Path encFileFinal = encryptedDestination2.resolve(sourceFile.getFileName().toString());
        Files.copy(encFileTo, encFileFinal, REPLACE_EXISTING);

        Path decryptedFile = decryptedDestination.resolve(sourceFile.getFileName().toString());
        Files.copy(encFileFinal, decryptedFile, REPLACE_EXISTING);

        assertEquals(Files.size(sourceFile), Files.size(decryptedFile));

    }

    @Test
    public void deleteFilesInsideEncryptedFS() throws IOException {

        Path sourceFile = encryptedDestination.resolve("foobar.txt");
        Files.deleteIfExists(sourceFile);

         sourceFile = TestHelper.createRandomFile(encryptedDestination, "foobar.txt");

        Files.deleteIfExists(sourceFile);
        Files.deleteIfExists(sourceFile);

        Files.walk(encryptedDestination)
                .forEach(p -> System.out.println(p));

        assertFalse(Files.exists(sourceFile));

    }

    /**
     * Multithreading test with encrypted filesystem.
     */

    @Test
    public void multiThreadTest() throws IOException, InterruptedException {

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

        Thread t1 = new Thread(writeToTestFile);
        Thread t2 = new Thread(readFromTestFile);

        t1.start();
        t2.start();

        TestHelper.waitMilliSeconds(1000);
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

    @Test
    public void closeEncryptedFS() throws IOException {

        Path encPath1 = TestHelper.createRandomFile(encryptedDestination, "foobar1.txt");
        Path encPath2 = TestHelper.createRandomFile(encryptedDestination, "foobar.txt2");

        encPath1 = fileSystem.getPath(encryptedDestination.resolve(encPath1.getFileName()).toString());
        encPath2 = fileSystem2.getPath(encryptedDestination.resolve(encPath2.getFileName()).toString());

        encPath1.getFileSystem().close();

    }

}


