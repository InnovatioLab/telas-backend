package com.telas.services.impl;

import com.telas.dtos.request.filters.ClientFilterRequestDto;
import com.telas.dtos.request.filters.FilterAdRequestDto;
import com.telas.dtos.response.*;
import com.telas.entities.Ad;
import com.telas.entities.AdRequest;
import com.telas.entities.Client;
import com.telas.entities.RefusedAd;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.services.QuestionnaireLatestMeta;
import com.telas.shared.constants.valitation.AdValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
import com.telas.enums.AdRequestOrigin;
import com.telas.enums.AdValidationType;
import com.telas.enums.DefaultStatus;
import com.telas.enums.PartnerSubmissionMode;
import com.telas.enums.Permission;
import com.telas.enums.Role;
import com.telas.services.ad.AdUploadService;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.BusinessQuestionnaireService;
import com.telas.services.ClientAdminQueryService;
import com.telas.services.PermissionService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.jpa.SpecificationFactory;
import com.telas.shared.utils.PaginationFilterUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientAdminQueryServiceImpl implements ClientAdminQueryService {

    private final ClientRepository repository;
    private final AdRequestRepository adRequestRepository;
    private final AdRepository adRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final PermissionService permissionService;
    private final AdUploadService adUploadService;
    private final BusinessQuestionnaireService businessQuestionnaireService;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<ClientMinResponseDto>> findAllFilters(ClientFilterRequestDto request) {
        authenticatedUserService.validateAdmin();
        Client actor = authenticatedUserService.getLoggedUser().client();
        Sort order = request.setOrdering();

        Pageable pageable = PaginationFilterUtil.getPageable(request, order);
        Specification<Client> roleRestriction = actor.isDeveloper()
                ? (root, query, criteriaBuilder) -> criteriaBuilder.conjunction()
                : (root, query, criteriaBuilder) -> criteriaBuilder.notEqual(root.get("role"), Role.ADMIN);
        Specification<Client> statusVisibilityForPanel = (root, query, criteriaBuilder) -> {
            if (actor.isDeveloper()) {
                return criteriaBuilder.conjunction();
            }
            List<Predicate> allowed = new ArrayList<>();
            allowed.add(criteriaBuilder.equal(root.get("status"), DefaultStatus.ACTIVE));
            if (permissionService.hasPermission(actor, Permission.ADMIN_CLIENTS_VIEW_INACTIVE)) {
                allowed.add(criteriaBuilder.equal(root.get("status"), DefaultStatus.INACTIVE));
            }
            if (permissionService.hasPermission(actor, Permission.ADMIN_CLIENTS_VIEW_DELETED)) {
                allowed.add(criteriaBuilder.equal(root.get("status"), DefaultStatus.DELETED));
            }
            return criteriaBuilder.or(allowed.toArray(new Predicate[0]));
        };
        Specification<Client> filter = PaginationFilterUtil.addSpecificationFilter(
                Specification.where(roleRestriction).and(statusVisibilityForPanel),
                request.getGenericFilter(), this::filterClients);

        Page<Client> page = repository.findAll(filter, pageable);
        boolean canDeactivate = permissionService.hasPermission(actor, Permission.ADMIN_CLIENTS_DEACTIVATE);
        boolean canReactivate = permissionService.hasPermission(actor, Permission.ADMIN_CLIENTS_REACTIVATE);
        boolean canRestoreDeleted =
                permissionService.hasPermission(actor, Permission.ADMIN_CLIENTS_RESTORE_DELETED);
        UUID viewerId = actor.getId();

        List<Client> clients = page.getContent();
        List<UUID> clientIds = clients.stream()
                .map(Client::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<UUID, Integer> approvedAdsCountByClientId = new HashMap<>();
        if (!clientIds.isEmpty()) {
            adRepository.countApprovedAdsByClientIds(clientIds).forEach(row ->
                    approvedAdsCountByClientId.put(
                            row.getClientId(),
                            Math.toIntExact(row.getApprovedCount())));
        }

        List<ClientMinResponseDto> response = clients.stream()
                .map(c -> new ClientMinResponseDto(
                        c,
                        viewerId,
                        canDeactivate,
                        canReactivate,
                        canRestoreDeleted,
                        approvedAdsCountByClientId.get(c.getId())
                ))
                .toList();
        return PaginationResponseDto.fromResult(response, (int) page.getTotalElements(), page.getTotalPages(),
                request.getPage());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<AdRequestAdminResponseDto>> findPendingAdRequest(FilterAdRequestDto request) {
        authenticatedUserService.validateAdminOrAdsManageAccess();
        Sort order = request.setOrdering();

        Pageable pageable = PaginationFilterUtil.getPageable(request, order);
        Specification<AdRequest> filter = PaginationFilterUtil.addSpecificationFilter((root, query, criteriaBuilder) -> {
            query.distinct(true);

            Join<AdRequest, Client> clientJoin = root.join("client", JoinType.INNER);
            Join<AdRequest, Ad> adJoin = root.join("ad", JoinType.LEFT);

            Predicate activeClient = criteriaBuilder.equal(clientJoin.get("status"), DefaultStatus.ACTIVE);

            Predicate noAdYet = criteriaBuilder.isNull(adJoin.get("id"));
            Predicate rejectedAd =
                    criteriaBuilder.equal(adJoin.get("validation"), AdValidationType.REJECTED);
            Predicate pendingValidation =
                    criteriaBuilder.equal(adJoin.get("validation"), AdValidationType.PENDING);

            Predicate adminUploadNeeded = criteriaBuilder.or(noAdYet, rejectedAd);

            Predicate pendingAdvertiserReview = criteriaBuilder.and(
                    pendingValidation,
                    criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("requestOrigin"), AdRequestOrigin.CLIENT),
                            criteriaBuilder.and(
                                    criteriaBuilder.equal(root.get("requestOrigin"), AdRequestOrigin.PARTNER),
                                    criteriaBuilder.or(
                                            criteriaBuilder.isNull(root.get("submissionMode")),
                                            criteriaBuilder.notEqual(
                                                    root.get("submissionMode"),
                                                    PartnerSubmissionMode.PARTNER_FINISHED_CREATIVE
                                            )
                                    )
                            )
                    )
            );

            Predicate adminDirectApproval = criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("submissionMode"), PartnerSubmissionMode.PARTNER_FINISHED_CREATIVE),
                    pendingValidation
            );

            Predicate visibleInAdminQueue = criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("isActive"), true),
                    criteriaBuilder.or(
                            adminUploadNeeded,
                            pendingAdvertiserReview,
                            adminDirectApproval
                    )
            );

            Subquery<Long> refusedCountSq = query.subquery(Long.class);
            Root<RefusedAd> refusedRoot = refusedCountSq.from(RefusedAd.class);
            refusedCountSq.select(criteriaBuilder.count(refusedRoot));
            refusedCountSq.where(criteriaBuilder.equal(refusedRoot.get("ad"), adJoin));

            Predicate refusalHistoryOk = criteriaBuilder.or(
                    noAdYet,
                    criteriaBuilder.and(
                            criteriaBuilder.isNotNull(adJoin.get("id")),
                            criteriaBuilder.lessThanOrEqualTo(
                                    refusedCountSq,
                                    (long) SharedConstants.MAX_ADS_VALIDATION
                            )
                    )
            );

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(activeClient);
            predicates.add(visibleInAdminQueue);
            predicates.add(refusalHistoryOk);

            if (request.getRequestOrigin() != null) {
                predicates.add(criteriaBuilder.equal(root.get("requestOrigin"), request.getRequestOrigin()));
            }
            if (request.getSubmissionMode() != null) {
                predicates.add(criteriaBuilder.equal(root.get("submissionMode"), request.getSubmissionMode()));
            }
            if (request.getClientRole() != null) {
                predicates.add(criteriaBuilder.equal(clientJoin.get("role"), request.getClientRole()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, request.getGenericFilter(), this::filterAdRequests);

        Page<AdRequest> page = adRequestRepository.findAll(filter, pageable);
        List<AdRequest> adRequests = page.getContent();
        List<UUID> adRequestIds = adRequests.stream().map(AdRequest::getId).toList();
        Map<UUID, QuestionnaireLatestMeta> questionnaireByAdRequestId =
                businessQuestionnaireService.findLatestMetadataByAdRequestIds(adRequestIds);

        List<AdRequestAdminResponseDto> response = adRequests.stream()
                .map(adRequest -> {
                    QuestionnaireLatestMeta meta = questionnaireByAdRequestId.get(adRequest.getId());
                    Integer version = meta != null ? meta.version() : null;
                    Instant updatedAt = meta != null ? meta.updatedAt() : null;
                    int attachmentCount = ValidateDataUtils.countCsvIds(adRequest.getAttachmentIds());
                    boolean partnerRemovalRequested = adRequest.getAd() != null
                            && adRequest.getAd().getPartnerRemovalRequestedAt() != null;
                    return new AdRequestAdminResponseDto(
                            adRequest,
                            version,
                            updatedAt,
                            attachmentCount,
                            partnerRemovalRequested);
                })
                .toList();
        return PaginationResponseDto.fromResult(response, (int) page.getTotalElements(), page.getTotalPages(),
                request.getPage());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<AdRequestAdminResponseDto>> findMyPartnerAdRequests(FilterAdRequestDto request) {
        Client partner = authenticatedUserService.validatePartner().client();
        Sort order = request.setOrdering();
        Pageable pageable = PaginationFilterUtil.getPageable(request, order);

        Specification<AdRequest> base = (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("client").get("id"), partner.getId()),
                criteriaBuilder.equal(root.get("requestOrigin"), AdRequestOrigin.PARTNER)
        );

        Specification<AdRequest> filter = PaginationFilterUtil.addSpecificationFilter(
                base,
                request.getGenericFilter(),
                this::filterPartnerAdRequests
        );

        Page<AdRequest> page = adRequestRepository.findAll(filter, pageable);
        List<AdRequest> adRequests = page.getContent();
        List<UUID> adRequestIds = adRequests.stream().map(AdRequest::getId).toList();
        Map<UUID, QuestionnaireLatestMeta> questionnaireByAdRequestId =
                businessQuestionnaireService.findLatestMetadataByAdRequestIds(adRequestIds);

        List<AdRequestAdminResponseDto> response = adRequests.stream()
                .map(adRequest -> {
                    QuestionnaireLatestMeta meta = questionnaireByAdRequestId.get(adRequest.getId());
                    Integer version = meta != null ? meta.version() : null;
                    Instant updatedAt = meta != null ? meta.updatedAt() : null;
                    int attachmentCount = ValidateDataUtils.countCsvIds(adRequest.getAttachmentIds());
                    boolean partnerRemovalRequested = adRequest.getAd() != null
                            && adRequest.getAd().getPartnerRemovalRequestedAt() != null;
                    return new AdRequestAdminResponseDto(
                            adRequest,
                            version,
                            updatedAt,
                            attachmentCount,
                            partnerRemovalRequested);
                })
                .toList();
        return PaginationResponseDto.fromResult(response, (int) page.getTotalElements(), page.getTotalPages(),
                request.getPage());
    }

    @Override
    @Transactional(readOnly = true)
    public AdRequestMediaResponseDto findAdRequestMedia(UUID adRequestId) {
        authenticatedUserService.validateAdminOrAdsManageAccess();
        AdRequest adRequest = adRequestRepository.findById(adRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_REQUEST_NOT_FOUND));
        Map<String, Object> linkData = adUploadService.getAdRequestData(adRequest);
        LinkResponseDto ad = (LinkResponseDto) linkData.get("ad");
        @SuppressWarnings("unchecked")
        List<LinkResponseDto> attachments = (List<LinkResponseDto>) linkData.get("attachments");
        return new AdRequestMediaResponseDto(ad, attachments != null ? attachments : List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<PendingAdAdminValidationResponseDto>> findPendingAds(FilterAdRequestDto request) {
        authenticatedUserService.validateAdminOrAdsManageAccess();
        Sort order = sortForPendingAds(request);
        Pageable pageable = PaginationFilterUtil.getPageable(request, order);
        Specification<Ad> base = (root, query, criteriaBuilder) -> {
            Join<Ad, Client> clientJoin = root.join("client", JoinType.INNER);
            Predicate activeClient = criteriaBuilder.equal(clientJoin.get("status"), DefaultStatus.ACTIVE);
            Predicate pending = criteriaBuilder.equal(root.get("validation"), AdValidationType.PENDING);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(activeClient);
            predicates.add(pending);
            if (request.getClientRole() != null) {
                predicates.add(criteriaBuilder.equal(clientJoin.get("role"), request.getClientRole()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        Specification<Ad> filter = PaginationFilterUtil.addSpecificationFilter(base,
                request.getGenericFilter(), this::filterPendingAds);
        Page<Ad> page = adRepository.findAll(filter, pageable);
        List<PendingAdAdminValidationResponseDto> response = page.stream()
                .map(ad -> new PendingAdAdminValidationResponseDto(
                        ad,
                        adUploadService.getStringLinkFromAd(ad),
                        adUploadService.buildClientReferencesForAd(ad)))
                .toList();
        return PaginationResponseDto.fromResult(response, (int) page.getTotalElements(), page.getTotalPages(),
                request.getPage());
    }

    private Sort sortForPendingAds(FilterAdRequestDto request) {
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "";
        boolean desc = "desc".equalsIgnoreCase(request.getSortDir());
        return switch (sortBy) {
            case "clientName" ->
                    Sort.by(desc ? Sort.Order.desc("client.businessName").ignoreCase()
                            : Sort.Order.asc("client.businessName").ignoreCase());
            case "clientRole" ->
                    Sort.by(desc ? Sort.Order.desc("client.role") : Sort.Order.asc("client.role"));
            case "name" ->
                    Sort.by(desc ? Sort.Order.desc("name").ignoreCase() : Sort.Order.asc("name").ignoreCase());
            case "submissionDate", "waitingDays" ->
                    Sort.by(desc ? Sort.Order.desc("createdAt") : Sort.Order.asc("createdAt"));
            default -> Sort.by(Sort.Order.desc("createdAt"));
        };
    }

    private Specification<Ad> filterPendingAds(Specification<Ad> specification, String genericFilter) {
        if (genericFilter == null || genericFilter.isBlank()) {
            return specification;
        }
        return specification.and((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String filter = "%" + genericFilter.toLowerCase() + "%";
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("client").get("businessName")), filter));
            predicates.add(criteriaBuilder.like(root.get("client").get("contact").get("email"), filter));
            predicates.add(criteriaBuilder.equal(root.get("client").get("contact").get("phone"), genericFilter));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("client").get("role")), filter));
            SpecificationFactory.addDatePredicates(predicates, criteriaBuilder, root, genericFilter, "createdAt");
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        });
    }

    private Specification<Client> filterClients(Specification<Client> specification, String genericFilter) {
        String filter = "%" + genericFilter.toLowerCase() + "%";

        return specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("businessName")), filter),
                criteriaBuilder.like(root.get("contact").get("email"), filter),
                criteriaBuilder.equal(root.get("contact").get("phone"), genericFilter),
                criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), genericFilter.toLowerCase()),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("role")), filter)));
    }

    private Specification<AdRequest> filterAdRequests(Specification<AdRequest> specification, String genericFilter) {
        return specification.and((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String filter = "%" + genericFilter.toLowerCase() + "%";

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("client").get("businessName")), filter));
            predicates.add(criteriaBuilder.like(root.get("client").get("contact").get("email"), filter));
            predicates.add(criteriaBuilder.equal(root.get("client").get("contact").get("phone"), genericFilter));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("client").get("role")), filter));

            SpecificationFactory.addDatePredicates(predicates, criteriaBuilder, root, genericFilter, "createdAt");

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        });
    }

    private Specification<AdRequest> filterPartnerAdRequests(
            Specification<AdRequest> specification,
            String genericFilter) {
        if (genericFilter == null || genericFilter.isBlank()) {
            return specification;
        }
        return specification.and((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String filter = "%" + genericFilter.toLowerCase() + "%";
            Join<?, ?> monitorJoin = root.join("targetMonitor", JoinType.LEFT);
            Join<?, ?> addressJoin = monitorJoin.join("address", JoinType.LEFT);

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("slogan")), filter));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(addressJoin.get("street")), filter));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(addressJoin.get("city")), filter));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(addressJoin.get("state")), filter));
            SpecificationFactory.addDatePredicates(predicates, criteriaBuilder, root, genericFilter, "createdAt");
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        });
    }
}
