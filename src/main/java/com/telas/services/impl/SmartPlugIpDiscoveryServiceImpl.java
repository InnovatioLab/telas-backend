package com.telas.services.impl;

import com.telas.monitoring.entities.BoxSubnetRouteEntity;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.PlugReading;
import com.telas.monitoring.plug.SmartPlugClient;
import com.telas.monitoring.plug.SmartPlugCredentials;
import com.telas.monitoring.repositories.BoxSubnetRouteEntityRepository;
import com.telas.monitoring.repositories.SmartPlugCheckRunRepository;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import com.telas.services.SmartPlugCredentialsResolver;
import com.telas.services.SmartPlugIpDiscoveryService;
import com.telas.shared.utils.Ipv4CidrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SmartPlugIpDiscoveryServiceImpl implements SmartPlugIpDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(SmartPlugIpDiscoveryServiceImpl.class);

    private static final Set<String> RETRY_ERRORS =
            Set.of(
                    "missing_host",
                    "timeout",
                    "sidecar_error",
                    "kasa_discovery_error",
                    "kasa_update_error",
                    "mac_mismatch",
                    "unreachable");

    private final SmartPlugEntityRepository smartPlugEntityRepository;
    private final BoxSubnetRouteEntityRepository boxSubnetRouteEntityRepository;
    private final SmartPlugCheckRunRepository smartPlugCheckRunRepository;
    private final SmartPlugClient smartPlugClient;
    private final SmartPlugCredentialsResolver credentialsResolver;
    private final SmartPlugIpDiscoveryPersistence smartPlugIpDiscoveryPersistence;
    private final ExecutorService discoveryExecutor;

    public SmartPlugIpDiscoveryServiceImpl(
            SmartPlugEntityRepository smartPlugEntityRepository,
            BoxSubnetRouteEntityRepository boxSubnetRouteEntityRepository,
            SmartPlugCheckRunRepository smartPlugCheckRunRepository,
            SmartPlugClient smartPlugClient,
            SmartPlugCredentialsResolver credentialsResolver,
            SmartPlugIpDiscoveryPersistence smartPlugIpDiscoveryPersistence,
            @Qualifier("smartPlugDiscoveryExecutor") ExecutorService discoveryExecutor) {
        this.smartPlugEntityRepository = smartPlugEntityRepository;
        this.boxSubnetRouteEntityRepository = boxSubnetRouteEntityRepository;
        this.smartPlugCheckRunRepository = smartPlugCheckRunRepository;
        this.smartPlugClient = smartPlugClient;
        this.credentialsResolver = credentialsResolver;
        this.smartPlugIpDiscoveryPersistence = smartPlugIpDiscoveryPersistence;
        this.discoveryExecutor = discoveryExecutor;
    }

    @Value("${monitoring.kasa.discovery.enabled:false}")
    private boolean discoveryEnabled;

    @Value("${monitoring.kasa.discovery.max-parallel:20}")
    private int maxParallel;

    @Value("${monitoring.kasa.discovery.per-plug-max-attempts:762}")
    private int perPlugMaxAttempts;

    @Override
    public Map<String, Object> runDiscoveryCycle() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("discoveryAttempted", false);
        if (!discoveryEnabled) {
            summary.put("discoverySkipped", "disabled");
            return summary;
        }
        summary.put("discoveryAttempted", true);
        List<SmartPlugEntity> plugs = smartPlugEntityRepository.findAllEnabledForChecks();
        Map<UUID, SmartPlugCheckRunRepository.SmartPlugLastReadingRow> lastByPlug =
                indexLast(smartPlugCheckRunRepository.findLastReadingsForAllPlugs());
        int skipped = 0;
        int eligible = 0;
        int resolved = 0;
        for (SmartPlugEntity plug : plugs) {
            SmartPlugCheckRunRepository.SmartPlugLastReadingRow last = lastByPlug.get(plug.getId());
            if (!needsDiscovery(plug, last)) {
                skipped++;
                continue;
            }
            eligible++;
            if (tryDiscoverAndPersist(plug)) {
                resolved++;
            }
        }
        summary.put("discoveryPlugsTotal", plugs.size());
        summary.put("discoverySkipped", skipped);
        summary.put("discoveryEligible", eligible);
        summary.put("discoveryResolved", resolved);
        return summary;
    }

    private static Map<UUID, SmartPlugCheckRunRepository.SmartPlugLastReadingRow> indexLast(
            List<SmartPlugCheckRunRepository.SmartPlugLastReadingRow> rows) {
        Map<UUID, SmartPlugCheckRunRepository.SmartPlugLastReadingRow> m = new HashMap<>();
        for (SmartPlugCheckRunRepository.SmartPlugLastReadingRow r : rows) {
            if (r.getSmartPlugId() != null) {
                m.put(r.getSmartPlugId(), r);
            }
        }
        return m;
    }

    private boolean needsDiscovery(SmartPlugEntity plug, SmartPlugCheckRunRepository.SmartPlugLastReadingRow last) {
        if (!StringUtils.hasText(plug.getLastSeenIp())) {
            return true;
        }
        if (last == null) {
            return false;
        }
        if (Boolean.TRUE.equals(last.getSuccess())) {
            return false;
        }
        String err = last.getErrorMessage();
        if (err == null || err.isBlank()) {
            return true;
        }
        if (RETRY_ERRORS.contains(err)) {
            return true;
        }
        return err.startsWith("http_");
    }

    public boolean tryDiscoverAndPersist(SmartPlugEntity plug) {
        UUID plugId = plug.getId();
        SmartPlugEntity managed = smartPlugEntityRepository.findById(plugId).orElse(null);
        if (managed == null) {
            return false;
        }
        UUID boxId = resolveBoxId(managed);
        if (boxId == null) {
            log.info("smartplug.discovery.skip plugId={} reason=no_box", plugId);
            return false;
        }
        List<BoxSubnetRouteEntity> routes = boxSubnetRouteEntityRepository.findByBox_IdOrderByCidrAsc(boxId);
        if (routes.isEmpty()) {
            log.info("smartplug.discovery.skip plugId={} boxId={} reason=no_routes", plugId, boxId);
            return false;
        }
        SmartPlugCredentials credentials = credentialsResolver.resolve(managed);
        if (credentials == null) {
            log.info("smartplug.discovery.skip plugId={} reason=no_credentials", plugId);
            return false;
        }
        String existingIp = managed.getLastSeenIp();
        if (StringUtils.hasText(existingIp)) {
            PlugReading quick = smartPlugClient.readAtHost(managed, existingIp.trim(), credentials);
            if (quick.reachable()) {
                log.debug("smartplug.discovery.skip plugId={} reason=last_ip_ok ip={}", plugId, existingIp);
                return false;
            }
        }
        long t0 = System.nanoTime();
        AtomicReference<String> found = new AtomicReference<>();
        Semaphore budget = new Semaphore(perPlugMaxAttempts);
        int batch = Math.max(1, maxParallel);
        outer:
        for (BoxSubnetRouteEntity route : routes) {
            String cidr = route.getCidr();
            if (!Ipv4CidrUtil.isIpv4Slash24(cidr)) {
                log.debug("smartplug.discovery.cidr_skip plugId={} cidr={}", plugId, cidr);
                continue;
            }
            List<String> hosts = Ipv4CidrUtil.listHostIpsInSlash24(cidr);
            for (int i = 0; i < hosts.size(); i += batch) {
                if (budget.availablePermits() == 0) {
                    break outer;
                }
                if (found.get() != null) {
                    break outer;
                }
                int end = Math.min(i + batch, hosts.size());
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (int j = i; j < end; j++) {
                    if (budget.availablePermits() == 0) {
                        break;
                    }
                    if (found.get() != null) {
                        break;
                    }
                    String ip = hosts.get(j);
                    futures.add(
                            CompletableFuture.runAsync(
                                    () -> {
                                        if (found.get() != null) {
                                            return;
                                        }
                                        if (!budget.tryAcquire()) {
                                            return;
                                        }
                                        PlugReading r = smartPlugClient.readAtHost(managed, ip, credentials);
                                        if (r.reachable()) {
                                            found.compareAndSet(null, ip);
                                        }
                                    },
                                    discoveryExecutor));
                }
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }
        }
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
        int probesDone = perPlugMaxAttempts - budget.availablePermits();
        if (found.get() != null) {
            smartPlugIpDiscoveryPersistence.updateLastSeenIp(plugId, found.get());
            log.info(
                    "smartplug.discovery.ok plugId={} boxId={} ip={} attempts={} elapsedMs={}",
                    plugId,
                    boxId,
                    found.get(),
                    probesDone,
                    elapsedMs);
            return true;
        }
        log.info(
                "smartplug.discovery.miss plugId={} boxId={} attempts={} elapsedMs={} reason=no_host_found",
                plugId,
                boxId,
                probesDone,
                elapsedMs);
        return false;
    }

    private static UUID resolveBoxId(SmartPlugEntity plug) {
        if (plug.getMonitor() != null && plug.getMonitor().getBox() != null) {
            return plug.getMonitor().getBox().getId();
        }
        if (plug.getBox() != null) {
            return plug.getBox().getId();
        }
        return null;
    }

}
