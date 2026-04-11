package com.telas.services.impl;

import com.telas.dtos.response.ApplicationLogResponseDto;
import com.telas.monitoring.repositories.ApplicationLogEntityRepository;
import com.telas.services.ApplicationLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ApplicationLogQueryServiceImpl implements ApplicationLogQueryService {

    private final ApplicationLogEntityRepository applicationLogEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationLogResponseDto> findAll(
            String source,
            String level,
            Instant from,
            Instant to,
            String q,
            Pageable pageable) {
        String sourceParam = StringUtils.hasText(source) ? source.trim() : null;
        String levelParam = StringUtils.hasText(level) ? level.trim().toUpperCase() : null;
        String qPattern = null;
        if (StringUtils.hasText(q)) {
            String trimmed = q.trim();
            qPattern = "%" + trimmed + "%";
        }
        return applicationLogEntityRepository
                .search(sourceParam, levelParam, from, to, qPattern, pageable)
                .map(ApplicationLogResponseDto::new);
    }
}
