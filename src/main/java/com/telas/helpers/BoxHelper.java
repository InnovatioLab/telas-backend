package com.telas.helpers;

import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.dtos.response.BoxMonitorAdResponseDto;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.entities.Box;
import com.telas.entities.BoxAddress;
import com.telas.entities.Monitor;
import com.telas.entities.SubscriptionMonitor;
import com.telas.enums.AdValidationType;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.BoxAddressRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.repositories.SubscriptionMonitorRepository;
import com.telas.services.BucketService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.HttpClientUtil;
import com.telas.shared.utils.MonitorBlocksUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BoxHelper {
    private final Logger log = LoggerFactory.getLogger(BoxHelper.class);
    private final BoxAddressRepository boxAddressRepository;
    private final MonitorRepository monitorRepository;
    private final SubscriptionMonitorRepository subscriptionMonitorRepository;
    private final BucketService bucketService;

    @Value("${TOKEN_SECRET}")
    private String API_KEY;

    @Transactional(readOnly = true)
    public BoxAddress getBoxAddress(UUID boxAddressId) {
        return boxAddressRepository.findById(boxAddressId)
                .orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_ADDRESS_NOT_FOUND));
    }


    @Transactional(readOnly = true)
    public Monitor findMonitorById(UUID monitorId) {
        return monitorRepository.findById(monitorId)
                .orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
    }

    // language: java
    @Transactional(readOnly = true)
    public List<BoxMonitorAdResponseDto> getBoxMonitorAdResponse(Box box) {
        return box.getMonitors().stream()
                .map(monitor -> {
                    List<SubscriptionMonitor> activeSubs = subscriptionMonitorRepository.findByMonitorId(monitor.getId());
                    Map<UUID, SubscriptionMonitor> subsByClientId = activeSubs.stream()
                            .filter(sm -> sm.getSubscription() != null && sm.getSubscription().getClient() != null)
                            .collect(Collectors.toMap(sm -> sm.getSubscription().getClient().getId(), sm -> sm, (a, b) -> a));

                    int totalSubscriptionBlocks = MonitorBlocksUtils.sumSubscriptionBlocks(activeSubs);

                    long unmatchedAdsCount = monitor.getMonitorAds().stream()
                            .filter(ma -> {
                                UUID clientId = ma.getAd() != null && ma.getAd().getClient() != null
                                        ? ma.getAd().getClient().getId()
                                        : null;
                                return clientId == null || !subsByClientId.containsKey(clientId);
                            })
                            .count();

                    int blocksPerAdminAd = MonitorBlocksUtils
                            .blocksPerAdminAd(monitor.getMaxBlocks(), totalSubscriptionBlocks, unmatchedAdsCount);

                    List<MonitorAdResponseDto> adDtos = monitor.getMonitorAds().stream()
                            .map(monitorAd -> {
                                MonitorAdResponseDto dto = new MonitorAdResponseDto(
                                        monitorAd,
                                        bucketService.getLink(AttachmentUtils.format(monitorAd.getAd()))
                                );

                                UUID adClientId = monitorAd.getAd() != null && monitorAd.getAd().getClient() != null
                                        ? monitorAd.getAd().getClient().getId()
                                        : null;

                                if (adClientId != null && subsByClientId.containsKey(adClientId)) {
                                    dto.setBlockQuantity(subsByClientId.get(adClientId).getSlotsQuantity());
                                } else {
                                    dto.setBlockQuantity(blocksPerAdminAd > 0 ? blocksPerAdminAd : SharedConstants.MIN_QUANTITY_MONITOR_BLOCK);
                                }

                                return dto;
                            })
                            .toList();

                    return new BoxMonitorAdResponseDto(adDtos);
                })
                .toList();
    }

}
