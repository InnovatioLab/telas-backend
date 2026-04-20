package com.telas.shared.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class Ipv4CidrUtil {

    private static final Pattern IPV4 =
            Pattern.compile(
                    "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    private Ipv4CidrUtil() {}

    public static boolean isIpv4Slash24(String cidr) {
        if (cidr == null || !cidr.endsWith("/24")) {
            return false;
        }
        int slash = cidr.indexOf('/');
        if (slash <= 0) {
            return false;
        }
        String ipPart = cidr.substring(0, slash).trim();
        return IPV4.matcher(ipPart).matches();
    }

    public static List<String> listHostIpsInSlash24(String cidr) {
        if (!isIpv4Slash24(cidr)) {
            return List.of();
        }
        int slash = cidr.indexOf('/');
        String ip = cidr.substring(0, slash).trim();
        String[] o = ip.split("\\.");
        if (o.length != 4) {
            return List.of();
        }
        String prefix = o[0] + "." + o[1] + "." + o[2];
        List<String> out = new ArrayList<>(254);
        for (int h = 1; h <= 254; h++) {
            out.add(prefix + "." + h);
        }
        return out;
    }

    public static List<String> filterIpv4Slash24Cidrs(Iterable<String> cidrs) {
        Set<String> out = new LinkedHashSet<>();
        if (cidrs == null) {
            return List.of();
        }
        for (String c : cidrs) {
            if (c == null) {
                continue;
            }
            String s = c.trim();
            int slash = s.indexOf('/');
            if (slash <= 0) {
                continue;
            }
            if (!"/24".equals(s.substring(slash))) {
                continue;
            }
            String ipPart = s.substring(0, slash);
            if (!IPV4.matcher(ipPart).matches()) {
                continue;
            }
            out.add(ipPart + "/24");
        }
        return List.copyOf(out);
    }
}
