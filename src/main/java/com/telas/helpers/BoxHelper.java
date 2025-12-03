package com.telas.helpers;

import com.telas.dtos.response.BoxMonitorAdResponseDto;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.entities.Box;
import com.telas.entities.BoxAddress;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.BoxAddressRepository;
import com.telas.services.BucketService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BoxHelper {
    private final BoxAddressRepository boxAddressRepository;
    private final BucketService bucketService;

    @Value("${TOKEN_SECRET}")
    private String API_KEY;

    @Transactional(readOnly = true)
    public BoxAddress getBoxAddress(UUID boxAddressId) {
        return boxAddressRepository.findById(boxAddressId)
                .orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_ADDRESS_NOT_FOUND));
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
