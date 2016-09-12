package de.dal33t.powerfolder.test.encryptedStorage;

import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.test.TestHelper;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/**
 * JUnit Test for file encryption with cryptomator lib and cryptofs.
 *
 * @author Jan Wiegmann <wiegmann@de.dal33t.powerfolder.com>
 * @since <pre>Aug 23, 2016</pre>
 */


public class cryptofsTest {

    private Path unencryptedSource;
    private Path encryptedDestination;
    private Path decryptedDestination;
    private FileSystem fileSystem;

    @Before
    public void setup() throws IOException {

        PathUtils.recursiveDelete(TestHelper.getTestDir());

        // Enencrypted files
        unencryptedSource = TestHelper.getTestDir().resolve("unencryptedSource");
        Files.createDirectory(unencryptedSource);

        // Encrypted files
        encryptedDestination = TestHelper.getTestDir().resolve("encryptedDestination");
        Files.createDirectory(encryptedDestination);

        // Unencrypted files after decrypting process.
        decryptedDestination = TestHelper.getTestDir().resolve("decryptedDestination");
        Files.createDirectory(decryptedDestination);

        // Cryptomator filesystem with cryptolib.
        fileSystem = initFileSystem("78f639876f298793AFAG!!%%12%...ö22öppP");

    }

    private FileSystem initFileSystem(String password) throws IOException {
        return CryptoFileSystemProvider.newFileSystem(
                encryptedDestination,
                CryptoFileSystemProperties.cryptoFileSystemProperties()
                        .withPassphrase(password)
                        .build());
    }

    @After
    public void shutDown() throws IOException {
        fileSystem.close();
    }

    @Test
    public void encryptSingleFile() throws IOException {

        // Create unencrypted test file.
        Path sourceFile = TestHelper.createRandomFile(unencryptedSource);

        // Create encrypted file over cryptofs.
        Path encryptedDirectory = fileSystem.getPath(encryptedDestination.toString());
        Path encFile = encryptedDirectory.resolve(sourceFile.getFileName().toString());

        // Copy into encryptedDestination directory.
        Files.createDirectories(encryptedDirectory);
        Files.copy(sourceFile, encFile);

        // Read from encrypted dir over crypto filesystem in clear text to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(fileSystem.getPath(encryptedDestination.toString()))) {
            listing.forEach(System.out::println);
        }

        // Copy from encrypted destination to decrypted destination.
        Path destFile = decryptedDestination.resolve(sourceFile.getFileName());

        Files.copy(encFile, destFile);

        // Check if unencryptedSource file is the same after decryption.
        assertTrue(TestHelper.compareFiles(sourceFile, destFile));

        // Check encrypted output.
        checkEncryptionProcess(null, null);
    }

    @Test
    public void multiFiles() throws IOException {

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
        Path encryptedLargeFile = encryptedDirectory.resolve(largeFile.getFileName().toString());
        Files.createDirectories(encryptedDirectory);
        Files.copy(largeFile, encryptedLargeFile);

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
    public void encryptSubDirectories() throws IOException {

        Files.createDirectory(unencryptedSource.resolve("testDir1"));
        Files.createDirectory(unencryptedSource.resolve("testDir2"));
        Files.createDirectory(unencryptedSource.resolve("testDir3"));

        TestHelper.createRandomFile(unencryptedSource.resolve("testDir1"));
        TestHelper.createRandomFile(unencryptedSource.resolve("testDir2"));
        TestHelper.createRandomFile(unencryptedSource.resolve("testDir3"));
        TestHelper.createRandomFile(unencryptedSource);

        // Get a file list from all unencrypted files with subdirs.
        List<Path> unencryptedFiles = new ArrayList<>();

        Files.walk(unencryptedSource)
                .filter(Files::isRegularFile)
                .forEach(p -> unencryptedFiles.add(p));

        // Encrypt and copy unencrypted files with subdirs.
        for (Path p : unencryptedFiles){

            Path fileName = p.getFileName();
            String fileAndMissingSubDirs = p.toString().replace(unencryptedSource.toString(), "");
            String onlyMissingSubDirs = fileAndMissingSubDirs.replace(fileName.toString(), "");

            if (onlyMissingSubDirs.startsWith("/")){
                onlyMissingSubDirs = onlyMissingSubDirs.replaceFirst("/", "");
            }

            Path encryptedDirectory = fileSystem.getPath(encryptedDestination.toString());
            Path encryptedDirectoryWithMissingSubDirs = encryptedDirectory.resolve(onlyMissingSubDirs);

            if (!Files.exists(encryptedDirectoryWithMissingSubDirs)) {
                Files.createDirectories(encryptedDirectoryWithMissingSubDirs);
            }

            Files.copy(p, encryptedDirectoryWithMissingSubDirs.resolve(fileName.toString()));
        }

        // Get a file list from all encrypted files and subdirs.
        List<Path> encryptedFiles = new ArrayList<>();

        Files.walk(fileSystem.getPath(encryptedDestination.toString()))
                .filter(Files::isRegularFile)
                .forEach(p -> encryptedFiles.add(p));

        // Decrypt and copy encrypted files with subdirs.
        for (Path p : encryptedFiles){

            Path fileName = p.getFileName();
            String fileAndMissingSubDirs = p.toString().replace(encryptedDestination.toString(), "");
            String onlyMissingSubDirs = fileAndMissingSubDirs.replace(fileName.toString(), "");

            if (onlyMissingSubDirs.startsWith("/")){
                onlyMissingSubDirs = onlyMissingSubDirs.replaceFirst("/", "");
            }

            Path decryptedDirectory = Paths.get(decryptedDestination.toString());
            Path decryptedDirectoryWithMissingSubDirs = decryptedDirectory.resolve(onlyMissingSubDirs);

            if (!Files.exists(decryptedDirectoryWithMissingSubDirs)) {
                Files.createDirectories(decryptedDirectoryWithMissingSubDirs);
            }

            Files.copy(p, decryptedDirectoryWithMissingSubDirs.resolve(fileName.toString()));
        }

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
    public void updateEncryptedFiles() throws IOException {

        // Create unencrypted empty test file.
        Path sourceFile = TestHelper.createRandomFile(unencryptedSource, 0);

        Path encryptedDirectory = fileSystem.getPath(encryptedDestination.toString());
        Path encFile = encryptedDirectory.resolve(sourceFile.getFileName().toString());

        Path destFile = decryptedDestination.resolve(sourceFile.getFileName());

        // Copy from unencryptedSource into encryptedDestination.
        Files.createDirectories(encryptedDirectory);
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
    public void moveEncryptedFiles() throws IOException {

        // Create unencrypted test file.
        Path sourceFile = TestHelper.createRandomFile(unencryptedSource);
        // Would be nice: Path encFile = fileSystem.resolve(sourceFile.getFileName());

        Path encryptedDirectory = fileSystem.getPath(encryptedDestination.toString());

        Path encFile = encryptedDirectory.resolve(sourceFile.getFileName().toString());
        Path encFile2 = encryptedDirectory.resolve(sourceFile.getFileName().toString() + "2");
        Path destFile = decryptedDestination.resolve(sourceFile.getFileName());

        // Copy from unencryptedSource into encryptedDestination.
        Files.createDirectories(encryptedDirectory);
        Files.move(sourceFile, encFile);

        // Also move an file inside the encryptedDestination.
        Files.move(encFile, encFile2);

        // Read from encryptedDestination over crypto filesystem in clear text to ensure the files are really encrypted.
        try (Stream<Path> listing = Files.list(fileSystem.getPath(encryptedDestination.toString()))) {
            listing.forEach(System.out::println);
        }

        // Copy from encryptedDestination to unencryptedDestination.
        Files.move(encFile, destFile);
    }

    public void checkEncryptionProcess(List<Path> beforeEncryption, List<Path> afterEncryption)
            throws IOException {

        // TO-DO: Check every single file -> unencryptedSource to decryptedDestination!
        // TO-DO: Check if clear-text filename is in encrypted directory.

        if (beforeEncryption != null && afterEncryption != null){
            for (int i = 0; i < beforeEncryption.size(); i++){
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

}


