package de.dal33t.powerfolder.util;

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
