package com.telas.shared.utils;

import com.telas.enums.BoxScriptVersionStatus;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class BoxScriptVersionUtils {

    private BoxScriptVersionUtils() {}

    public static BoxScriptVersionStatus resolveStatus(String reportedVersion, String targetVersion) {
        if (!StringUtils.hasText(targetVersion)) {
            return BoxScriptVersionStatus.UNKNOWN;
        }
        String reported = reportedVersion != null ? reportedVersion.trim() : "";
        String target = targetVersion.trim();
        if (!StringUtils.hasText(reported)) {
            return BoxScriptVersionStatus.UNKNOWN;
        }
        int cmp = compareSemverLoose(reported, target);
        if (cmp < 0) {
            return BoxScriptVersionStatus.BEHIND;
        }
        return BoxScriptVersionStatus.MATCH;
    }

    public static int compareSemverLoose(String a, String b) {
        List<Integer> pa = parseVersionParts(a);
        List<Integer> pb = parseVersionParts(b);
        int n = Math.max(pa.size(), pb.size());
        for (int i = 0; i < n; i++) {
            int va = i < pa.size() ? pa.get(i) : 0;
            int vb = i < pb.size() ? pb.get(i) : 0;
            if (va != vb) {
                return Integer.compare(va, vb);
            }
        }
        return 0;
    }

    private static List<Integer> parseVersionParts(String v) {
        String[] segments = v.split("[.-]", -1);
        List<Integer> out = new ArrayList<>();
        for (String seg : segments) {
            if (!StringUtils.hasText(seg)) {
                continue;
            }
            int end = 0;
            while (end < seg.length() && Character.isDigit(seg.charAt(end))) {
                end++;
            }
            if (end == 0) {
                out.add(0);
            } else {
                try {
                    out.add(Integer.parseInt(seg.substring(0, end)));
                } catch (NumberFormatException e) {
                    out.add(0);
                }
            }
        }
        if (out.isEmpty()) {
            out.add(0);
        }
        return out;
    }
}
