/*
 * Copyright 2004 - 2026 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.search;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.logging.Loggable;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Manages a Lucene index for a specific PowerFolder Folder.
 * The index is stored under each folder’s .PowerFolder/index directory.
 * Each document is uniquely identified by Folder-ID + relative path.
 */
public class LuceneIndexManager extends Loggable {

    private final Folder folder;
    private final Path indexPath;
    private final StandardAnalyzer analyzer;
    private final IndexWriter writer;

    public LuceneIndexManager(Folder folder) throws IOException {
        super();
        this.folder = folder;

        // Use PowerFolder’s system subdirectory for the Lucene index
        this.indexPath = folder.getSystemSubDir().resolve("index");
        Files.createDirectories(indexPath);

        this.analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        this.writer = new IndexWriter(FSDirectory.open(indexPath), config);

        logFine("Lucene index initialized for folder: " + folder.getName() +
                " at " + indexPath.toAbsolutePath());
    }

    /**
     * Adds or updates a single file entry in this folder’s Lucene index.
     */
    public void indexFile(FileInfo fileInfo) {
        try {
            String docId = buildDocId(fileInfo);
            Document doc = new Document();
            doc.add(new StringField("docId", docId, Field.Store.YES));
            doc.add(new StringField("folderId", folder.getId(), Field.Store.YES));
            doc.add(new StringField("folderName", folder.getName(), Field.Store.YES));
            doc.add(new StringField("name", fileInfo.getFilenameOnly(), Field.Store.YES));
            doc.add(new StringField("relativePath", fileInfo.getRelativeName(), Field.Store.YES));
            doc.add(new LongPoint("modified",
                    fileInfo.getModifiedDate() != null ? fileInfo.getModifiedDate().getTime() : 0));
            doc.add(new LongPoint("size", fileInfo.getSize()));

            writer.updateDocument(new Term("docId", docId), doc);
            logInfo("Indexed file " + fileInfo);
        } catch (Exception e) {
            logWarning("Failed to index file " + fileInfo + " in folder " + folder.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Bulk index multiple FileInfo objects in a single commit.
     */
    public void indexFiles(Collection<FileInfo> files) {
        if (files == null || files.isEmpty()) return;
        try {
            for (FileInfo fileInfo : files) {
                indexFile(fileInfo);
            }
            writer.commit();
            logFine("Bulk indexed " + files.size() + " files in folder " + folder.getName());
        } catch (Exception e) {
            logWarning("Bulk indexing failed for folder " + folder.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Removes a single file entry from the Lucene index.
     */
    public void deleteFile(FileInfo fileInfo) {
        try {
            String docId = buildDocId(fileInfo);
            writer.deleteDocuments(new Term("docId", docId));
            logFiner("Removed file " + fileInfo.getFilenameOnly() + " from index of folder " + folder.getName());
        } catch (Exception e) {
            logWarning("Failed to delete file from index: " + e.getMessage());
        }
    }

    /**
     * Bulk delete multiple files from the Lucene index.
     */
    public void deleteFiles(Collection<FileInfo> files) {
        if (files == null || files.isEmpty()) return;
        try {
            for (FileInfo fileInfo : files) {
                writer.deleteDocuments(new Term("docId", buildDocId(fileInfo)));
            }
            writer.commit();
            logFine("Bulk deleted " + files.size() + " files from index of folder " + folder.getName());
        } catch (Exception e) {
            logWarning("Bulk delete failed for folder " + folder.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Updates the Lucene index based on a ScanResult delta.
     */
    public void updateIndex(ScanResult scanResult) {
        Reject.ifNull(scanResult, "ScanResults");

        if (!scanResult.isChangeDetected()) {
            return;
        }

        indexFiles(scanResult.getNewFiles());
        indexFiles(scanResult.getChangedFiles());
        indexFiles(scanResult.getRestoredFiles());
        deleteFiles(scanResult.getDeletedFiles());

        commit();
    }

    /**
     * Rebuild the index from scratch (clears and re-indexes all files).
     */
    public void rebuildIndex(Collection<FileInfo> allFiles) {
        try {
            logFine("Rebuilding Lucene index for folder: " + folder.getName());
            writer.deleteAll();
            if (allFiles != null && !allFiles.isEmpty()) {
                for (FileInfo f : allFiles) {
                    indexFile(f);
                }
            }
            writer.commit();
            logFine("Rebuild completed for folder: " + folder.getName() +
                    " with " + (allFiles != null ? allFiles.size() : 0) + " files.");
        } catch (Exception e) {
            logSevere("Failed to rebuild index for folder " + folder.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Explicitly commits pending changes.
     */
    public void commit() {
        try {
            writer.commit();
        } catch (Exception e) {
            logWarning("Commit failed for folder " + folder.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Closes the Lucene index writer safely.
     */
    public void close() {
        try {
            writer.close();
            logFine("Lucene index closed for folder: " + folder.getName());
        } catch (Exception e) {
            logWarning("Failed to close Lucene index for folder " + folder.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Builds a unique Lucene document ID.
     * Format: <folderId>:<relativePath>
     */
    private String buildDocId(FileInfo fileInfo) {
        if (fileInfo == null) {
            return folder.getId() + ":<null>";
        }
        return folder.getId() + ":" + fileInfo.getRelativeName();
    }
}
