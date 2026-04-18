package com.telas.services.impl;

import com.telas.services.BoxTailscalePingOutcome;
import com.telas.services.BoxTailscalePingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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

    @Value("${monitoring.ping.tcp-probe-enabled:true}")
    private boolean tcpProbeEnabled;

    @Value("${monitoring.ping.tcp-probe-ports:8081}")
    private String tcpProbePortsRaw;

    @Value("${monitoring.ping.tcp-probe-timeout-ms:3000}")
    private int tcpProbeTimeoutMs;

    @Value("${monitoring.ping.icmp-enabled:true}")
    private boolean icmpEnabled;

    @Value("${monitoring.ping.java-reachable-fallback-enabled:true}")
    private boolean javaReachableFallbackEnabled;

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
        if (tcpProbeEnabled) {
            BoxTailscalePingOutcome tcp = tryTcpProbes(ip);
            if (tcp != null) {
                return tcp;
            }
        }
        if (icmpEnabled) {
            try {
                return runSystemPing(ip);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return BoxTailscalePingOutcome.attempted(false, "interrupted");
            } catch (Exception e) {
                if (javaReachableFallbackEnabled) {
                    return fallbackIsReachable(ip, "ping_process:" + e.getMessage());
                }
                return BoxTailscalePingOutcome.attempted(
                        false, "icmp_failed:" + e.getMessage());
            }
        }
        if (javaReachableFallbackEnabled) {
            return fallbackIsReachable(ip, "probes_exhausted");
        }
        return BoxTailscalePingOutcome.attempted(false, "unreachable_no_connectivity_probe_ok");
    }

    private BoxTailscalePingOutcome tryTcpProbes(String ip) {
        int[] ports = resolveTcpPorts();
        if (ports.length == 0) {
            return null;
        }
        String lastFailureDetail = null;
        for (int port : ports) {
            TcpProbeResult r = tcpProbe(ip, port);
            if (r.reachable()) {
                return BoxTailscalePingOutcome.attempted(true, r.detail());
            }
            lastFailureDetail = r.detail();
        }
        return BoxTailscalePingOutcome.attempted(
                false, lastFailureDetail != null ? lastFailureDetail : "tcp_probe_failed");
    }

    private int[] resolveTcpPorts() {
        if (!StringUtils.hasText(tcpProbePortsRaw)) {
            return new int[] {8081};
        }
        int[] parsed =
                Arrays.stream(tcpProbePortsRaw.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .mapToInt(this::parsePortOrDefault)
                        .filter(p -> p > 0 && p <= 65535)
                        .toArray();
        return parsed.length > 0 ? parsed : new int[] {8081};
    }

    private int parsePortOrDefault(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private record TcpProbeResult(boolean reachable, String detail) {}

    private TcpProbeResult tcpProbe(String ip, int port) {
        InetSocketAddress address = new InetSocketAddress(ip, port);
        try (Socket socket = new Socket()) {
            socket.connect(address, Math.max(200, tcpProbeTimeoutMs));
            return new TcpProbeResult(true, "tcp_connect_ok:" + port);
        } catch (SocketTimeoutException e) {
            return new TcpProbeResult(false, "tcp_timeout:" + port);
        } catch (ConnectException e) {
            String m = e.getMessage();
            if (m != null
                    && (m.contains("Connection refused")
                            || m.toLowerCase(Locale.ROOT).contains("refused"))) {
                return new TcpProbeResult(
                        true,
                        "tcp_peer_reachable_connection_refused:" + port);
            }
            return new TcpProbeResult(false, "tcp_connect_ex:" + port + ":" + m);
        } catch (IOException e) {
            return new TcpProbeResult(false, "tcp_io:" + port + ":" + e.getMessage());
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
