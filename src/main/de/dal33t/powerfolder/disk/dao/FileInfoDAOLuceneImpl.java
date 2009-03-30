/*
 * Copyright 2004 - 20089 Christian Sprajc. All rights reserved.
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
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.disk.dao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Profiling;
import de.dal33t.powerfolder.util.ProfilingEntry;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * DAO on the FileInfo lucene index.
 * <p>
 * TODO Optimize performance:
 * http://wiki.apache.org/jakarta-lucene/BasicsOfPerformance
 * 
 * @author sprajc
 */
public class FileInfoDAOLuceneImpl extends PFComponent implements FileInfoDAO {
    private static final String FIELD_NAME_DOMAIN = "domain";
    private static final int MAX_ITEMS = 400000;

    private boolean ignoreFileNameCase;
    private volatile boolean started;
    private Directory directory;
    private IndexWriter iwriter;
    private IndexSearcherPool searcherPool;

    /**
     * @param controller the controller
     * @param indexDir the directory where to store the index information
     * @throws IOException
     */
    public FileInfoDAOLuceneImpl(Controller controller, File indexDir) throws IOException
    {
        super(controller);
        Analyzer analyzer = new StandardAnalyzer();
        directory = FSDirectory.getDirectory(indexDir);
        // directory = new RAMDirectory();
        iwriter = new IndexWriter(directory, analyzer, true,
            MaxFieldLength.LIMITED);
        iwriter.setMergeFactor(2);
        // iwriter.setMaxFieldLength(25000);
        searcherPool = new IndexSearcherPool();
        ignoreFileNameCase = !OSUtil.isWindowsSystem();
//        controller.getThreadPool().scheduleWithFixedDelay(new Runnable() {
//
//            public void run() {
//                if (started) {
//                    try {
//                        iwriter.optimize();
//                        iwriter.commit();
//                    } catch (Exception e) {
//                        logSevere("Unable to optimize folder db index: " + e, e);
//                    }
//                }
//            }
//        }, 0, 10, TimeUnit.SECONDS);
        started = true;
    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.disk.lucene.FileInfoDAO#stop()
     */
    public void stop() {
        started = false;
        searcherPool.close();
        try {
            iwriter.close();
        } catch (Exception e) {
            logSevere(e);
        }
    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.disk.lucene.FileInfoDAO#store(java.lang.String, de.dal33t.powerfolder.light.FileInfo)
     */
    public void store(String domain, FileInfo... fInfos) {
        store(domain, Arrays.asList(fInfos));
    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.disk.lucene.FileInfoDAO#store(java.lang.String, java.util.Collection)
     */
    public void store(String domain, Collection<FileInfo> fInfos) {
        Reject.ifNull(fInfos, "FileInfos is null");
        if (fInfos.isEmpty()) {
            return;
        }
        FileInfo last = null;
        try {
            for (FileInfo fInfo : fInfos) {
                last = fInfo;
                Reject.ifNull(fInfo, "FileInfo is null");

                deleteDocument(fInfo.getName(), fInfo.getFolderInfo().id,
                    domain, false);
                Document doc = FileInfoDocumentConverter
                    .convertToDocument(fInfo);
                doc.add(new Field(FIELD_NAME_DOMAIN, domain != null
                    ? domain
                    : "", Field.Store.YES, Field.Index.NOT_ANALYZED));
                iwriter.addDocument(doc);
                // iwriter.optimize();
                // logWarning("Indexing on " + domain + ": " + fInfo);
            }
            commitWriterAndCloseSearcher();
            // iwriter.close();
            // for (FileInfo fInfo : fInfos) {
            // if (find(fInfo.getName(), fInfo.getFolderInfo().id,
            // domain != null ? domain : "") == null)
            // {
            // logSevere("Unable to find after index: " + fInfo);
            // }
            // }
        } catch (Exception e) {
            throw new RuntimeException(
                "Exception while adding FileInfo(s) to index: " + last + " of "
                    + fInfos.size() + " total FileInfos. " + e.toString(), e);
        }
    }

    private void commitWriterAndCloseSearcher() throws CorruptIndexException,
        IOException
    {
        iwriter.commit();
        searcherPool.invalidateActiveSearcher();
    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.disk.lucene.FileInfoDAO#findNewestVersion(de.dal33t.powerfolder.light.FileInfo, java.lang.String)
     */
    public FileInfo findNewestVersion(FileInfo fInfo, String... domains) {
        Reject.ifNull(fInfo, "FolderId is blank");

        IndexSearcher searcher = searcherPool.get();
        try {
            BooleanQuery domainQuery = new BooleanQuery();
            for (int i = 0; i < domains.length; i++) {
                String domain = domains[i];
                Query domainSingleQuery = new TermQuery(new Term(
                    FIELD_NAME_DOMAIN, domain != null ? domain : ""));
                domainQuery.add(domainSingleQuery, BooleanClause.Occur.SHOULD);
            }
            Query folderIdQuery = new TermQuery(new Term(
                FileInfoDocumentConverter.FIELDNAME_FOLDER_ID, fInfo
                    .getFolderInfo().id));
            BooleanQuery bq = new BooleanQuery();
            bq.add(createFileNameQuery(fInfo.getName()),
                BooleanClause.Occur.MUST);
            bq.add(folderIdQuery, BooleanClause.Occur.MUST);
            bq.add(domainQuery, BooleanClause.Occur.MUST);

            Sort byVersion = new Sort();
            byVersion.setSort(FileInfo.PROPERTYNAME_VERSION, true);
            TopDocs docs = searcher.search(bq, null, 10, byVersion);
            if (docs.totalHits == 0) {
                return null;
            }
            // System.out.println("Found: " + docs.totalHits + " docs");
            return convertToFile(searcher.doc(docs.scoreDocs[0].doc));
        } catch (Exception e) {
            throw new RuntimeException(
                "Exception while finding newest FileInfo in index: "
                    + fInfo.toDetailString() + ", domains "
                    + Arrays.asList(domains) + ": " + e.toString(), e);
        } finally {
            searcherPool.returnToPool(searcher);

        }
    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.disk.lucene.FileInfoDAO#find(de.dal33t.powerfolder.light.FileInfo, java.lang.String)
     */
    public FileInfo find(FileInfo fInfo, String domain) {
        return find(fInfo.getName(), fInfo.getFolderInfo().id, domain);
    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.disk.lucene.FileInfoDAO#find(java.lang.String, java.lang.String, java.lang.String)
     */
    public FileInfo find(String fileName, String folderId, String domain) {
        Reject.ifBlank(fileName, "Filename is blank");
        Document doc = findDocument(createFileNameQuery(fileName), folderId,
            domain);
        if (doc == null) {
            return null;
        }
        return convertToFile(doc);
    }

    private Query createFileNameQuery(String fileName) {
        Query fileNameQuery;
        if (ignoreFileNameCase) {
            fileNameQuery = new TermQuery(new Term(
                FileInfoDocumentConverter.FIELDNAME_FILE_NAME_LOWER_CASE,
                fileName.toLowerCase()));
        } else {
            fileNameQuery = new TermQuery(new Term(
                FileInfo.PROPERTYNAME_FILE_NAME, fileName));
        }
        return fileNameQuery;
    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.disk.lucene.FileInfoDAO#findByNameIgnoreCase(java.lang.String, java.lang.String, java.lang.String)
     */

    public FileInfo findByNameIgnoreCase(String fileName, String folderId,
        String domain)
    {
        Reject.ifBlank(fileName, "Filename is blank");
        // Search on the lower case field of the file.
        Query fileNameQuery = new TermQuery(new Term(
            FileInfoDocumentConverter.FIELDNAME_FILE_NAME_LOWER_CASE, fileName
                .toLowerCase()));
        Document doc = findDocument(fileNameQuery, folderId, domain);
        if (doc == null) {
            return null;
        }
        return convertToFile(doc);
    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.disk.lucene.FileInfoDAO#delete(java.lang.String, de.dal33t.powerfolder.light.FileInfo)
     */
    public void delete(String domain, FileInfo fInfo) {
        deleteDocument(fInfo.getName(), fInfo.getFolderInfo().id, domain, true);

    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.disk.lucene.FileInfoDAO#deleteDomain(java.lang.String)
     */
    public void deleteDomain(String domain) {
        try {
            Query domainQuery = new TermQuery(new Term(FIELD_NAME_DOMAIN,
                domain != null ? domain : ""));
            iwriter.deleteDocuments(domainQuery);
            commitWriterAndCloseSearcher();
        } catch (Exception e) {
            throw new RuntimeException("Exception while deleting Domain "
                + domain + ": " + e.toString(), e);
        }
    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.disk.lucene.FileInfoDAO#findAll(java.lang.String)
     */
    public List<FileInfo> findAll(String domain) {
        IndexSearcher searcher = searcherPool.get();
        try {
            Query domainQuery = new TermQuery(new Term(FIELD_NAME_DOMAIN,
                domain != null ? domain : ""));

            TopDocs docs = searcher.search(domainQuery, null, MAX_ITEMS);
            if (docs.scoreDocs.length == 0) {
                return Collections.emptyList();
            }
            List<FileInfo> fInfos = new ArrayList<FileInfo>(
                docs.scoreDocs.length);
            for (int i = 0; i < docs.scoreDocs.length; i++) {
                Document doc = searcher.doc(docs.scoreDocs[i].doc);
                fInfos.add(convertToFile(doc));
            }
            return fInfos;
        } catch (Exception e) {
            throw new RuntimeException(
                "Exception while finding all FileInfo in index for domain: "
                    + domain + ": " + e.toString(), e);
        } finally {
            searcherPool.returnToPool(searcher);
        }
    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.disk.lucene.FileInfoDAO#count(java.lang.String)
     */
    public int count(String domain) {
        IndexSearcher searcher = searcherPool.get();
        try {
            Query domainQuery = new TermQuery(new Term(FIELD_NAME_DOMAIN,
                domain != null ? domain : ""));
            TopDocs docs = searcher.search(domainQuery, null, 1);
            return docs.totalHits;
        } catch (Exception e) {
            throw new RuntimeException(
                "Exception while finding all FileInfo in index for domain: "
                    + domain + ": " + e.toString(), e);
        } finally {
            searcherPool.returnToPool(searcher);
        }
    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.disk.lucene.FileInfoDAO#findAllAsMap(java.lang.String)
     */
    public Map<FileInfo, FileInfo> findAllAsMap(String domain) {
        IndexSearcher searcher = searcherPool.get();
        try {
            Query domainQuery = new TermQuery(new Term(FIELD_NAME_DOMAIN,
                domain != null ? domain : ""));
            TopDocs docs = searcher.search(domainQuery, null, MAX_ITEMS);
            ConcurrentHashMap<FileInfo, FileInfo> fInfos = new ConcurrentHashMap<FileInfo, FileInfo>();
            // docs.scoreDocs.length);
            for (int i = 0; i < docs.scoreDocs.length; i++) {
                Document doc = searcher.doc(docs.scoreDocs[i].doc);
                FileInfo fInfo = convertToFile(doc);
                fInfos.put(fInfo, fInfo);
            }
            return fInfos;
        } catch (Exception e) {
            throw new RuntimeException(
                "Exception while finding all FileInfo in index for domain: "
                    + domain + ": " + e.toString(), e);
        } finally {
            searcherPool.returnToPool(searcher);
        }
    }

    // public void optimize() {
    // iwriter.optimize();
    // iwriter.close();
    // }

    // Internal methods *******************************************************

    private FileInfo convertToFile(Document doc) {
        return FileInfoDocumentConverter
            .convertToFileInfo(getController(), doc);
    }

    private void deleteDocument(String fileName, String folderId,
        String domain, boolean commit)
    {
        Reject.ifBlank(folderId, "FolderId is blank");
        Reject.ifBlank(fileName, "FileName is blank");

        try {
            Query folderIdQuery = new TermQuery(new Term(
                FileInfoDocumentConverter.FIELDNAME_FOLDER_ID, folderId));
            Query domainQuery = new TermQuery(new Term(FIELD_NAME_DOMAIN,
                domain != null ? domain : ""));
            BooleanQuery bq = new BooleanQuery();
            bq.add(createFileNameQuery(fileName), BooleanClause.Occur.MUST);
            bq.add(folderIdQuery, BooleanClause.Occur.MUST);
            bq.add(domainQuery, BooleanClause.Occur.MUST);

            iwriter.deleteDocuments(bq);
            if (commit) {
                commitWriterAndCloseSearcher();
            }
        } catch (Exception e) {
            throw new RuntimeException(
                "Exception while deleting FileInfo in index: " + fileName
                    + " in " + folderId + ", domain " + domain + ": "
                    + e.toString(), e);
        }
    }

    int i = 0;

    private Document findDocument(Query fileNameQuery, String folderId,
        String domain)
    {
        Reject.ifBlank(folderId, "FolderId is blank");
        Reject.ifNull(fileNameQuery, "FileName is blank");

        ProfilingEntry pe = Profiling.start("findDocument");
        IndexSearcher searcher = searcherPool.get();
        try {
            Query folderIdQuery = new TermQuery(new Term(
                FileInfoDocumentConverter.FIELDNAME_FOLDER_ID, folderId));
            Query domainQuery = new TermQuery(new Term(FIELD_NAME_DOMAIN,
                domain != null ? domain : ""));
            BooleanQuery bq = new BooleanQuery();
            bq.add(fileNameQuery, BooleanClause.Occur.MUST);
            bq.add(folderIdQuery, BooleanClause.Occur.MUST);
            bq.add(domainQuery, BooleanClause.Occur.MUST);

            TopDocs docs = searcher.search(bq, null, 1);
            if (docs.totalHits == 0) {
                return null;
            }
            if (docs.totalHits > 1) {
                logSevere("Found multiple FileInfo in index: " + fileNameQuery
                    + " in " + folderId + ", domain " + domain);
            }
            return searcher.doc(docs.scoreDocs[0].doc);

        } catch (Exception e) {
            throw new RuntimeException(
                "Exception while finding FileInfo in index: " + fileNameQuery
                    + " in " + folderId + ", domain " + domain + ": "
                    + e.toString(), e);
        } finally {
            searcherPool.returnToPool(searcher);
            Profiling.end(pe);
            i++;
            if (i % 2000 == 0) {
                System.out.println(Profiling.dumpStats());
            }
        }
    }

    private class IndexSearcherPool {
        private IndexSearcher activeSearcher;
        private Map<IndexSearcher, AtomicInteger> pool = new HashMap<IndexSearcher, AtomicInteger>();

        private synchronized void close() {
            if (activeSearcher != null && pool.get(activeSearcher).get() == 0) {
                try {
                    System.out.println("Close active: " + activeSearcher);
                    activeSearcher.close();
                } catch (IOException e) {
                    logSevere("Unable to close folder db index searcher: " + e,
                        e);
                }
                activeSearcher = null;
            }
        }

        private synchronized void invalidateActiveSearcher() {
            // if (activeSearcher != null && pool.get(activeSearcher).get() ==
            // 0) {
            // try {
            // System.out.println("Close active: " + activeSearcher);
            // activeSearcher.close();
            // } catch (IOException e) {
            // logSevere("Unable to close folder db index searcher: " + e,
            // e);
            // }
            // }
            if (activeSearcher != null && pool.get(activeSearcher).get() == 0) {
                try {
                    activeSearcher.close();
                    activeSearcher = null;
                } catch (IOException e) {
                    logSevere(
                        "Unable to reopen active folder db index reader: " + e,
                        e);

                }
               
                // TODO: optimize thru reopening reader
                // IndexReader ireader = activeSearcher.getIndexReader();
                // try {
                // if (!ireader.isCurrent()) {
                // System.out.println("Reopen active: " + activeSearcher);
                // IndexReader old = ireader.reopen();
                // old.close();
                // }
                // } catch (IOException e) {
                // logSevere(
                // "Unable to reopen active folder db index reader: " + e,
                // e);
                //
                // }

            }
        }

        private synchronized IndexSearcher get() {
            try {
                IndexSearcher searcher;
                if (started) {
                    if (activeSearcher == null) {
                        activeSearcher = new IndexSearcher(directory);
                        pool.put(activeSearcher, new AtomicInteger(0));
                    }
                    searcher = activeSearcher;
                } else {
                    // Fallback for use in non-started mode. don't cache active
                    // searcher.
                    logWarning("Handing out index searcher, but already shut down");
                    searcher = new IndexSearcher(directory);
                    pool.put(searcher, new AtomicInteger(0));
                }
                // logWarning("Created searcher:" + isearcher);
                pool.get(searcher).incrementAndGet();
                // System.out.println("get: " + searcher);
                return searcher;
            } catch (Exception e) {
                throw new RuntimeException(
                    "Exception while getting IndexSearcher: " + e.toString(), e);
            }
        }

        private synchronized void returnToPool(IndexSearcher searcher) {
            AtomicInteger users = pool.get(searcher);
            if (users == null) {
                logSevere("An unknown index searcher was returend to pool: "
                    + searcher);
                return;
            }

            int usersNow = users.decrementAndGet();
            // System.out
            // .println("Returned: " + searcher + ", users: " + usersNow);
            if (usersNow == 0 && searcher != activeSearcher) {
                // Close searcher if no users on this and not active searcher.
                try {
                    System.out.println("Close: " + searcher);
                    searcher.close();
                } catch (IOException e) {
                    logSevere("Unable to close folder db index searcher: " + e,
                        e);
                }
                pool.remove(searcher);
            }
        }
    }
}
