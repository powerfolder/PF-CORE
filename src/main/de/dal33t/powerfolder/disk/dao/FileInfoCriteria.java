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
package de.dal33t.powerfolder.disk.dao;

import de.dal33t.powerfolder.DocumentType;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;

import java.util.*;

import static de.dal33t.powerfolder.util.StringUtils.isBlank;

/**
 * Object that holds criterias to select {@link FileInfo}s from a
 * {@link FileInfoDAO}
 *
 * @author sprajc
 */
public class FileInfoCriteria {
    private List<String> domains = new LinkedList<>();
    private String path;
    private boolean recursive;
    private Type type = Type.FILES_AND_DIRECTORIES;
    private Set<String> keyWords = new HashSet<>();
    private int maxResults = -1;
    private boolean includeDeleted = false;
    private String fileName;
    private final Set<String> extensions = new LinkedHashSet<>();
    private String modifiedBy;
    private String modifiedByAccountId;
    private String modifiedByDeviceId;
    private String modifiedByDeviceName;
    private Date modifiedAfter;
    private Date modifiedBefore;
    private Long minSize;
    private Long maxSize;
    private final Set<String> categories = new LinkedHashSet<>();
    private SortField sortField;
    private boolean sortDescending;
    private final Set<String> tags = new LinkedHashSet<>();

    /**
     * @return the domain(s) to search in.
     */
    public List<String> getDomains() {
        return domains;
    }

    /**
     * @param domain
     *            a domain to search in for files.
     */
    public void addDomain(String domain) {
        if (!this.domains.contains(domain)) {
            this.domains.add(domain);
        }
    }

    /**
     * @param member
     *            the member to add to the selection criteria.
     */
    public void addMember(Member member) {
        addDomain(member.getId());
    }

    /**
     * Adds myself
     *
     * @param folder
     */
    public void addMySelf(Folder folder) {
        addMember(folder.getController().getMySelf());
    }

    /**
     * Adds all fully connected {@link Member}s and myself to the selection
     * criteria.
     *
     * @param folder
     */
    public void addConnectedAndMyself(Folder folder) {
        addMySelf(folder);
        for (Member member : folder.getMembersAsCollection()) {
            if (member.isCompletelyConnected()) {
                addMember(member);
            }
        }
    }

    /**
     * Adds all fully connected {@link Member}s, that have right to write and
     * myself to the selection criteria.
     *
     * @param folder
     */
    public void addWriteMembersAndMyself(Folder folder) {
        addMySelf(folder);
        for (Member member : folder.getMembersAsCollection()) {
            if (member.isCompletelyConnected()
                && folder.hasWritePermission(member))
            {
                addMember(member);
            }
        }
    }

    /**
     * Clears all selected domains.
     */
    public void clearDomains() {
        domains.clear();
    }

    /**
     * @param keyWord
     *            the keywords to add as filter.
     */
    public void addKeyWord(String keyWord) {
        if (isBlank(keyWord)) {
            return;
        }
        keyWords.add(keyWord.trim().toLowerCase());
    }

    /**
     * @return the keywords to filter the result for.
     */
    public Set<String> getKeyWords() {
        return Collections.unmodifiableSet(keyWords);
    }

    /**
     * @return the number of maximum returned items. -1 for unlimited
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * @param maxResults
     *            the number of maximum returned items. -1 for unlimited
     */
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    /**
     * @return path the path/relative name of the sub directory.
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path
     *            the path/relative name of the sub directory.
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @param dirInfo
     *            the path/relative name of the sub directory.
     */
    public void setPath(DirectoryInfo dirInfo) {
        setPath(dirInfo != null ? dirInfo.getRelativeName() : null);
    }

    /**
     * Prepends the given subfolder path to this criteria's path.
     * This modifies the current instance and returns it for chaining.
     *
     * Ensures there is exactly one "/" between both segments and
     * the resulting path does NOT start with a "/".
     *
     * @param subfolderPath The subfolder path to prepend (e.g. "docs")
     */
    public void mapToSubFolderPath(String subfolderPath) {
        Reject.ifNull(subfolderPath, "subfolderPath");
        Reject.ifTrue(subfolderPath.startsWith("/"), "subfolderPath must not start with slash: " + subfolderPath);

        if (isBlank(this.path)) {
            this.path = subfolderPath;
        } else {
            String base = this.path.endsWith("/") ? this.path.substring(0, this.path.length() - 1) : this.path;
            this.path = subfolderPath + "/" + base;
        }

        // Remove leading slash if one somehow remains (safety)
        if (this.path.startsWith("/")) {
            this.path = this.path.substring(1);
        }
    }


    public Type getType() {
        return type;
    }

    /**
     * @param type
     *            the type of objects in the result
     */
    public void setType(Type type) {
        Reject.ifNull(type, "Type is null");
        this.type = type;
    }

    /**
     * @return true to recursively include all files from subdirectory too.
     */
    public boolean isRecursive() {
        return recursive;
    }

    /**
     * @param recursive
     *            true to recursively include all files from subdirectory too.
     */
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public boolean includeDeleted() {
        return includeDeleted;
    }

    public void setIncludeDeleted(boolean includeDeleted) {
        this.includeDeleted = includeDeleted;
    }

    /** @return the text the file name must contain, independent of the keywords. */
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the extensions a file may have, e.g. doc and pdf. Several are OR-combined, like the
     *         {@link #getCategories() categories}: one question with more than one acceptable answer.
     */
    public Set<String> getExtensions() {
        return Collections.unmodifiableSet(extensions);
    }

    public void addExtension(String extension) {
        if (isBlank(extension)) {
            return;
        }
        extensions.add(extension.trim().toLowerCase(Locale.ROOT));
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getModifiedByAccountId() {
        return modifiedByAccountId;
    }

    public void setModifiedByAccountId(String modifiedByAccountId) {
        this.modifiedByAccountId = modifiedByAccountId;
    }

    public String getModifiedByDeviceId() {
        return modifiedByDeviceId;
    }

    public void setModifiedByDeviceId(String modifiedByDeviceId) {
        this.modifiedByDeviceId = modifiedByDeviceId;
    }

    /** @return the text the name of the device that changed the file last must contain. */
    public String getModifiedByDeviceName() {
        return modifiedByDeviceName;
    }

    public void setModifiedByDeviceName(String modifiedByDeviceName) {
        this.modifiedByDeviceName = modifiedByDeviceName;
    }

    public Date getModifiedAfter() {
        return modifiedAfter;
    }

    public void setModifiedAfter(Date modifiedAfter) {
        this.modifiedAfter = modifiedAfter;
    }

    public Date getModifiedBefore() {
        return modifiedBefore;
    }

    public void setModifiedBefore(Date modifiedBefore) {
        this.modifiedBefore = modifiedBefore;
    }

    public Long getMinSize() {
        return minSize;
    }

    public void setMinSize(Long minSize) {
        this.minSize = minSize;
    }

    public Long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * @return the categories a file may belong to, e.g. image and document. Several are OR-combined: one
     *         question ("what kind of file") with more than one acceptable answer, unlike the tags, where
     *         every one given has to be present.
     */
    public Set<String> getCategories() {
        return Collections.unmodifiableSet(categories);
    }

    public void addCategory(String category) {
        if (isBlank(category)) {
            return;
        }
        categories.add(category.trim().toLowerCase(Locale.ROOT));
    }

    public SortField getSortField() {
        return sortField;
    }

    public void setSortField(SortField sortField) {
        this.sortField = sortField;
    }

    public boolean isSortDescending() {
        return sortDescending;
    }

    public void setSortDescending(boolean sortDescending) {
        this.sortDescending = sortDescending;
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public void addTag(String tag) {
        if (StringUtils.isBlank(tag)) {
            return;
        }
        tags.add(tag.trim());
    }

    /**
     * Tests a single {@link FileInfo} against all content criteria: name, extension, editor, modification
     * date, size, category and tags. The structural criteria - domains, path, type, deleted state and max
     * results - stay with the DAO that walks the index.
     */
    public boolean matches(FileInfo fileInfo) {
        return matchesName(fileInfo, keyWords) && matchesFileName(fileInfo, fileName)
                && matchesExtension(fileInfo, extensions)
                && matchesModifiedBy(fileInfo, modifiedBy)
                && matchesModifiedDate(fileInfo, modifiedAfter, modifiedBefore)
                && matchesSize(fileInfo, minSize, maxSize)
                && matchesModifiedById(fileInfo, modifiedByAccountId, modifiedByDeviceId)
                && matchesDeviceName(fileInfo, modifiedByDeviceName)
                && matchesCategory(fileInfo, categories) && matchesTags(fileInfo, tags);
    }

    private static boolean matchesName(FileInfo fileInfo, Set<String> keyWords) {
        if (keyWords.isEmpty()) {
            return true;
        }
        String name = fileInfo.getFilenameOnly().toLowerCase();
        for (String keyWord : keyWords) {
            if (!name.contains(keyWord)) {
                return false;
            }
        }
        return true;
    }

    /**
     * PFS-5653: the name: filter. Every word of the value has to appear in the file name - the keywords of
     * a search also reach the path and, in the index, the content, this one does not.
     */
    private static boolean matchesFileName(FileInfo fileInfo, String fileName) {
        List<String> words = nameWords(fileName);
        if (words.isEmpty()) {
            return true;
        }
        String name = fileInfo.getFilenameOnly();
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        for (String word : words) {
            if (!lower.contains(word)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The words a "name:" value is compared by, lower cased. The index tokenizes a file name on everything
     * that is neither a letter nor a digit, so "!urgent!" is stored as "urgent" - a
     * value has to be cut the same way, otherwise the punctuation the user typed matches nothing. A value
     * of nothing but punctuation leaves no word at all and therefore filters nothing.
     */
    public static List<String> nameWords(String value) {
        if (isBlank(value)) {
            return Collections.emptyList();
        }
        List<String> words = new ArrayList<>();
        for (String word : value.toLowerCase().replaceAll("[^\\p{L}\\p{N}\\s._\\-]", " ").split("\\s+")) {
            /* PFS-5306: the tokenizer keeps a dot only between alphanumerics, so a trailing one - as in
             * "29.7." - would be searched for but never indexed. */
            while (word.endsWith(".")) {
                word = word.substring(0, word.length() - 1);
            }
            if (hasLetterOrDigit(word)) {
                words.add(word);
            }
        }
        return words;
    }

    /** Dots, hyphens and underscores stay inside a word, but on their own they are no word at all. */
    private static boolean hasLetterOrDigit(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (Character.isLetterOrDigit(word.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesExtension(FileInfo fileInfo, Set<String> extensions) {
        if (extensions.isEmpty()) {
            return true;
        }
        String actual = fileInfo.getExtension();
        return actual != null && extensions.contains(actual.toLowerCase(Locale.ROOT));
    }

    private static boolean matchesModifiedBy(FileInfo fileInfo, String modifiedBy) {
        List<String> words = nameWords(modifiedBy);
        if (words.isEmpty()) {
            return true;
        }
        AccountInfo account = fileInfo.getModifiedByAccount();
        if (account == null) {
            return false;
        }
        /* Kept apart by blanks: a word may sit in the display name, the username or the device nick, but
         * never across the seam between two of them. */
        StringBuilder content = new StringBuilder()
                .append(account.getDisplayName()).append(' ').append(account.getUsername());
        MemberInfo member = fileInfo.getModifiedBy();
        if (member != null) {
            content.append(' ').append(member.nick);
        }
        return containsAllWords(content.toString(), words);
    }

    private static boolean matchesModifiedDate(FileInfo fileInfo, Date after, Date before) {
        if (after == null && before == null) {
            return true;
        }
        Date modified = fileInfo.getModifiedDate();
        if (modified == null) {
            return false;
        }
        if (after != null && modified.before(after)) {
            return false;
        }
        /* Both bounds are inclusive: "before:2024-12-31" means up to the last millisecond of that day. */
        return before == null || !modified.after(before);
    }

    private static boolean matchesSize(FileInfo fileInfo, Long minSize, Long maxSize) {
        if (minSize == null && maxSize == null) {
            return true;
        }
        long size = fileInfo.getSize();
        if (minSize != null && size < minSize) {
            return false;
        }
        return maxSize == null || size <= maxSize;
    }

    private static boolean matchesModifiedById(FileInfo fileInfo, String accountId, String deviceId) {
        if (isBlank(accountId) && isBlank(deviceId)) {
            return true;
        }
        if (StringUtils.isNotBlank(accountId)) {
            AccountInfo account = fileInfo.getModifiedByAccount();
            if (account == null || !accountId.trim().equals(account.getOID())) {
                return false;
            }
        }
        if (StringUtils.isNotBlank(deviceId)) {
            MemberInfo member = fileInfo.getModifiedBy();
            if (member == null || !deviceId.trim().equals(member.id)) {
                return false;
            }
        }
        return true;
    }

    /** PFS-5653: the device: filter - the name of the device a file was changed on. */
    private static boolean matchesDeviceName(FileInfo fileInfo, String deviceName) {
        List<String> words = nameWords(deviceName);
        if (words.isEmpty()) {
            return true;
        }
        MemberInfo member = fileInfo.getModifiedBy();
        return member != null && member.nick != null && containsAllWords(member.nick, words);
    }

    /** True when every word - already lower cased by {@link #nameWords(String)} - sits in the text. */
    private static boolean containsAllWords(String text, List<String> words) {
        String lower = text.toLowerCase();
        for (String word : words) {
            if (!lower.contains(word)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesCategory(FileInfo fileInfo, Set<String> categories) {
        if (categories.isEmpty()) {
            return true;
        }
        String actual = fileInfo.isDiretory()
                ? DocumentType.FOLDER : DocumentType.categoryOf(fileInfo.getExtension());
        return categories.contains(actual);
    }

    /**
     * Tags are always matched case-insensitively, in whatever case they were typed when tagging or when
     * searching. Compared directly against the (few) file tags, without any per-file allocation.
     */
    private static boolean matchesTags(FileInfo fileInfo, Set<String> wantedTags) {
        if (wantedTags.isEmpty()) {
            return true;
        }
        List<String> fileTags = fileInfo.getTagsList();
        if (fileTags.size() < wantedTags.size()) {
            return false;
        }
        for (String wanted : wantedTags) {
            boolean found = false;
            for (String tag : fileTags) {
                if (tag.equalsIgnoreCase(wanted)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * PFS-5653: true when the criteria carry something no folder can answer. Asking for documents, for
     * PDFs, for something larger than 10 MB or changed by someone is asking about files - a name or a tag
     * is not, folders carry both.
     */
    public boolean describesFilesOnly() {
        boolean fileCategory = !categories.isEmpty() && !categories.contains(DocumentType.FOLDER);

        return fileCategory
                || !extensions.isEmpty()
                || minSize != null
                || maxSize != null
                || modifiedAfter != null
                || modifiedBefore != null
                || StringUtils.isNotBlank(modifiedBy)
                || StringUtils.isNotBlank(modifiedByDeviceName);
    }

    public boolean hasSearchCriteria() {
        return !keyWords.isEmpty()
                || StringUtils.isNotBlank(fileName)
                || !extensions.isEmpty()
                || StringUtils.isNotBlank(modifiedBy)
                || StringUtils.isNotBlank(modifiedByAccountId)
                || StringUtils.isNotBlank(modifiedByDeviceId)
                || StringUtils.isNotBlank(modifiedByDeviceName)
                || modifiedAfter != null
                || modifiedBefore != null
                || minSize != null
                || maxSize != null
                || !categories.isEmpty()
                || !tags.isEmpty();
    }

    @Override
    public String toString() {
        return "FileInfoCriteria [domains=" + domains + ", type=" + type
            + ", path=" + path + ", keyWords=" + keyWords + ", fileName=" + fileName + ", recursive="
            + recursive + ", maxResults=" + maxResults + ", modifiedAfter="
            + modifiedAfter + ", modifiedBefore=" + modifiedBefore + ", minSize="
            + minSize + ", maxSize=" + maxSize + ", modifiedByAccountId="
            + modifiedByAccountId + ", modifiedByDeviceId=" + modifiedByDeviceId
            + ", extensions=" + extensions + ", categories=" + categories + "]";
    }

    public enum Type {
        FILES_AND_DIRECTORIES, FILES_ONLY, DIRECTORIES_ONLY
    }

    /**
     * PFS-5653: the orders a result can be sorted by. Single source of truth for every spelling a caller may
     * use - the "sort:" search operator, the sortname request parameter - so that the Lucene sort and the
     * comparator applied afterwards can never disagree on what "date" means.
     */
    public enum SortField {
        NAME, SIZE, DATE;

        /**
         * @param value the name of a field, in any case - "date", "DATE", " Date ".
         * @return the field it names, or null if it names none.
         */
        public static SortField parse(String value) {
            if (StringUtils.isBlank(value)) {
                return null;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
