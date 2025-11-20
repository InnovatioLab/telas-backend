package com.telas.helpers;

import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.dtos.response.BoxMonitorAdResponseDto;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.entities.Box;
import com.telas.entities.BoxAddress;
import com.telas.entities.Monitor;
import com.telas.enums.AdValidationType;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.BoxAddressRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.BucketService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
public class BoxHelper {
    private final Logger log = LoggerFactory.getLogger(BoxHelper.class);
    private final BoxAddressRepository boxAddressRepository;
    private final MonitorRepository monitorRepository;
    private final BucketService bucketService;
    private final HttpClientUtil httpClient;

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

    @Transactional(readOnly = true)
    public List<BoxMonitorAdResponseDto> getBoxMonitorAdResponse(Box box) {
        return box.getMonitors().stream()
                .map(monitor -> new BoxMonitorAdResponseDto(
                        monitor.getMonitorAds().stream()
                                .map(monitorAd -> new MonitorAdResponseDto(
                                        monitorAd,
                                        bucketService.getLink(AttachmentUtils.format(monitorAd.getAd()))
                                ))
                                .toList()
                ))
                .toList();
    }
}
