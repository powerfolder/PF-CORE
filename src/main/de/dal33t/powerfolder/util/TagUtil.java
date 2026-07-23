package de.dal33t.powerfolder.util;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TagUtil {

    private TagUtil() {
    }

    public static List<String> parse(String tagsJson) {
        List<String> tags = new ArrayList<>();
        if (StringUtils.isBlank(tagsJson)) {
            return tags;
        }
        try {
            JSONArray arr = new JSONArray(tagsJson);
            for (int i = 0; i < arr.length(); i++) {
                String tag = clean(arr.optString(i, ""));
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            }
        } catch (JSONException e) {
            String single = clean(tagsJson);
            if (!single.isEmpty()) {
                tags.add(single);
            }
        }
        return tags;
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
        return tag.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
    }
}
