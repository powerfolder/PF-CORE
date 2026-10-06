package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class TagUtil {

    private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private TagUtil() {
    }

    /**
     * PFS-5911: The directory that carries the tags of a folder. An inheriting subfolder is a directory of its top
     * folder and is tagged there - its FolderInfo holds at most the copy it got when it was shared. The top folder
     * and an interrupted subfolder carry their tags on the FolderInfo (the interrupt moves them over, the restore
     * back). The one rule for everything that writes, lists, searches or suggests the tags of a folder.
     *
     * @return the directory in the top folder, {@code null} when the FolderInfo carries the tags - also when the top
     *         folder is not mounted or has no entry for the directory, which leaves the FolderInfo as the only place
     */
    public static FileInfo directoryOf(FolderInfo foInfo, FolderRepository repository) {
        if (!foInfo.isSubFolder() || !foInfo.inheritsPermissions()) {
            return null;
        }
        Folder top = repository.getFolder(foInfo.getTopFolder());
        FileInfo directory = top != null
            ? top.getFile(FileInfoFactory.lookupDirectory(top.getInfo(), foInfo.locationPath())) : null;
        return directory != null && !directory.isDeleted() ? directory : null;
    }

    /** PFS-5911: The tags of a folder, from wherever they are carried - see {@link #directoryOf}. */
    public static List<String> tagsOf(FolderInfo foInfo, FolderRepository repository) {
        FileInfo directory = directoryOf(foInfo, repository);
        return directory != null ? directory.getTagsList() : foInfo.getTagsList();
    }

    public static List<String> parse(String tagsJson) {
        if (StringUtils.isBlank(tagsJson)) {
            return Collections.emptyList();
        }
        List<String> tags = null;
        try {
            JSONArray arr = new JSONArray(tagsJson);
            for (int i = 0; i < arr.length(); i++) {
                String tag = clean(arr.optString(i, ""));
                if (!tag.isEmpty()) {
                    if (tags == null) {
                        tags = new ArrayList<>(arr.length());
                    }
                    tags.add(tag);
                }
            }
        } catch (JSONException e) {
            String single = clean(tagsJson);
            if (!single.isEmpty()) {
                return Collections.singletonList(single);
            }
        }
        return tags != null ? tags : Collections.emptyList();
    }

    public static List<String> normalize(Iterable<String> rawTags) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        if (rawTags != null) {
            for (String raw : rawTags) {
                String tag = clean(raw);
                if (tag.isEmpty()) {
                    continue;
                }
                if (seen.add(tag.toLowerCase(Locale.ROOT))) {
                    result.add(tag);
                }
            }
        }
        return result;
    }

    public static String toJson(Iterable<String> tags) {
        List<String> normalized = normalize(tags);
        if (normalized.isEmpty()) {
            return null;
        }
        JSONArray arr = new JSONArray();
        for (String tag : normalized) {
            arr.put(tag);
        }
        return arr.toString();
    }

    private static String clean(String tag) {
        if (tag == null) {
            return "";
        }
        return WHITESPACE.matcher(CONTROL_CHARS.matcher(tag).replaceAll(" ")).replaceAll(" ").trim();
    }
}
