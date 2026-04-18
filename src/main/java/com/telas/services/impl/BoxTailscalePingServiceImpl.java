package com.telas.services.impl;

import com.telas.services.BoxTailscalePingOutcome;
import com.telas.services.BoxTailscalePingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class BoxTailscalePingServiceImpl implements BoxTailscalePingService {

    private static final Pattern IPV4 =
            Pattern.compile(
                    "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    @Value("${monitoring.ping.enabled:true}")
    private boolean pingEnabled;

    @Value("${monitoring.ping.timeout-seconds:3}")
    private int timeoutSeconds;

    @Override
    public BoxTailscalePingOutcome pingBoxAddressIp(String ipFromBoxAddress) {
        if (!pingEnabled) {
            return BoxTailscalePingOutcome.notAttempted("ping_disabled");
        }
        if (!StringUtils.hasText(ipFromBoxAddress)) {
            return BoxTailscalePingOutcome.notAttempted("no_ip");
        }
        String ip = ipFromBoxAddress.trim();
        if (!IPV4.matcher(ip).matches()) {
            return BoxTailscalePingOutcome.attempted(
                    false, "expected_ipv4_literal_like_tailscale");
        }
        try {
            return runSystemPing(ip);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return BoxTailscalePingOutcome.attempted(false, "interrupted");
        } catch (Exception e) {
            return fallbackIsReachable(ip, "ping_process:" + e.getMessage());
        }
    }

    private BoxTailscalePingOutcome runSystemPing(String ip) throws IOException, InterruptedException {
        List<String> cmd = buildPingCommand(ip);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        long waitMs = (long) timeoutSeconds * 1000L + 1500L;
        boolean finished = p.waitFor(waitMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            return BoxTailscalePingOutcome.attempted(false, "ping_timeout");
        }
        byte[] out = p.getInputStream().readAllBytes();
        String snippet =
                new String(out, StandardCharsets.UTF_8)
                        .replace("\r", " ")
                        .replace("\n", " ")
                        .trim();
        if (snippet.length() > 200) {
            snippet = snippet.substring(0, 200) + "…";
        }
        int code = p.exitValue();
        if (code == 0) {
            return BoxTailscalePingOutcome.attempted(true, StringUtils.hasText(snippet) ? snippet : "ok");
        }
        return BoxTailscalePingOutcome.attempted(
                false, "exit_" + code + (StringUtils.hasText(snippet) ? " " + snippet : ""));
    }

    private List<String> buildPingCommand(String ip) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        List<String> cmd = new ArrayList<>();
        if (os.contains("win")) {
            cmd.add("ping");
            cmd.add("-n");
            cmd.add("1");
            cmd.add("-w");
            cmd.add(String.valueOf(Math.max(1000, timeoutSeconds * 1000)));
            cmd.add(ip);
        } else {
            cmd.add("ping");
            cmd.add("-c");
            cmd.add("1");
            cmd.add("-W");
            cmd.add(String.valueOf(Math.max(1, timeoutSeconds)));
            cmd.add(ip);
        }
        return cmd;
    }

    private BoxTailscalePingOutcome fallbackIsReachable(String ip, String prefix) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            boolean ok = addr.isReachable(Math.max(1000, timeoutSeconds * 1000));
            return BoxTailscalePingOutcome.attempted(
                    ok, (ok ? "java_isReachable_ok" : "java_isReachable_fail") + " (" + prefix + ")");
        } catch (IOException e) {
            return BoxTailscalePingOutcome.attempted(false, prefix + ";java:" + e.getMessage());
        }
    }
}
