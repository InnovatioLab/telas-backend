package com.telas.services;

import com.telas.dtos.request.BusinessQuestionnaireAnswersRequestDto;
import com.telas.entities.AdMessage;
import com.telas.entities.AdRequest;
import com.telas.entities.BusinessQuestionnaire;
import com.telas.entities.BusinessQuestionnaireAnswer;
import com.telas.entities.BusinessQuestionnaireRevision;
import com.telas.entities.Client;
import com.telas.enums.AdminEmailAlertCategory;
import com.telas.enums.BusinessQuestionKey;
import com.telas.enums.NotificationReference;
import com.telas.enums.Permission;
import com.telas.enums.Role;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.AdMessageRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.BusinessQuestionnaireRepository;
import com.telas.repositories.BusinessQuestionnaireRevisionRepository;
import com.telas.repositories.ClientRepository;
import com.telas.shared.constants.valitation.AdValidationMessages;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessQuestionnaireService {

    private static final int MAX_ANSWER_CHARS = 2000;

    private final BusinessQuestionnaireRepository questionnaireRepository;
    private final BusinessQuestionnaireRevisionRepository revisionRepository;
    private final AdMessageRepository adMessageRepository;
    private final AdRequestRepository adRequestRepository;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;
    private final PermissionService permissionService;
    private final AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;

    @Value("${front.base.url}")
    private String frontBaseUrl;

    @Transactional(readOnly = true)
    public Optional<BusinessQuestionnaireAnswersRequestDto> getDraftAnswers(UUID clientId) {
        return questionnaireRepository.findDraftByClientId(clientId)
                .flatMap(q -> revisionRepository.findTopByQuestionnaire_IdOrderByVersionDesc(q.getId()))
                .map(this::revisionToDto);
    }

    @Transactional
    public void saveDraft(UUID clientId, BusinessQuestionnaireAnswersRequestDto answers) {
        validateAnswers(answers);
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        BusinessQuestionnaire q = questionnaireRepository.findDraftByClientId(clientId)
                .orElseGet(() -> {
                    BusinessQuestionnaire n = new BusinessQuestionnaire();
                    n.setClient(client);
                    n.setAdRequest(null);
                    return questionnaireRepository.save(n);
                });
        q.setUpdatedAt(Instant.now());
        BusinessQuestionnaireRevision rev = revisionRepository.findTopByQuestionnaire_IdOrderByVersionDesc(q.getId())
                .orElse(null);
        if (rev == null || !answersEqual(rev, answers)) {
            int nextVersion = rev == null ? 1 : rev.getVersion() + 1;
            BusinessQuestionnaireRevision newRev = new BusinessQuestionnaireRevision();
            newRev.setQuestionnaire(q);
            newRev.setVersion(nextVersion);
            newRev.setCreatedAt(Instant.now());
            newRev.setCreatedByClient(client);
            applyAnswers(newRev, answers);
            q.getRevisions().add(newRev);
        }
        questionnaireRepository.save(q);
    }

    @Transactional
    public void createQuestionnaireForNewAdRequest(UUID clientId, AdRequest adRequest, BusinessQuestionnaireAnswersRequestDto answers) {
        validateAnswers(answers);
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        Optional<BusinessQuestionnaire> existingDraft = questionnaireRepository.findDraftByClientId(clientId);
        BusinessQuestionnaire q;
        if (existingDraft.isPresent()) {
            q = existingDraft.get();
            q.setAdRequest(adRequest);
            q.setUpdatedAt(Instant.now());
            q.getRevisions().clear();
        } else {
            q = new BusinessQuestionnaire(client, adRequest);
        }
        questionnaireRepository.save(q);
        BusinessQuestionnaireRevision rev = new BusinessQuestionnaireRevision();
        rev.setQuestionnaire(q);
        rev.setVersion(1);
        rev.setCreatedAt(Instant.now());
        rev.setCreatedByClient(client);
        applyAnswers(rev, answers);
        q.getRevisions().add(rev);
        questionnaireRepository.save(q);
        adRequest.setBusinessQuestionnaire(q);
    }

    @Transactional
    public void updateQuestionnaireForAdRequest(UUID clientId, UUID adRequestId, BusinessQuestionnaireAnswersRequestDto answers) {
        validateAnswers(answers);
        AdRequest ar = adRequestRepository.findById(adRequestId)
                .filter(r -> r.getClient().getId().equals(clientId) && r.isActive())
                .orElseThrow(() -> new ForbiddenException(ClientValidationMessages.AD_REQUEST_NOT_FOUND));
        BusinessQuestionnaire q = questionnaireRepository.findByAdRequest_Id(ar.getId())
                .orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_REQUEST_NOT_FOUND));
        BusinessQuestionnaireRevision latest = revisionRepository.findTopByQuestionnaire_IdOrderByVersionDesc(q.getId())
                .orElseThrow(() -> new BusinessRuleException("Questionnaire has no revisions."));
        if (answersEqual(latest, answers)) {
            return;
        }
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        BusinessQuestionnaireRevision rev = new BusinessQuestionnaireRevision();
        rev.setQuestionnaire(q);
        rev.setVersion(latest.getVersion() + 1);
        rev.setCreatedAt(Instant.now());
        rev.setCreatedByClient(client);
        applyAnswers(rev, answers);
        q.getRevisions().add(rev);
        q.setUpdatedAt(Instant.now());
        questionnaireRepository.save(q);
        notifyAdminsQuestionnaireUpdated(client, ar, rev.getVersion());
        if (ar.getAd() != null) {
            String msg = "Business questionnaire updated (revision " + rev.getVersion() + ").";
            adMessageRepository.save(new AdMessage(ar.getAd(), Role.CLIENT, msg));
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportTxtForAdRequest(UUID adRequestId, UUID actorClientId, boolean actorIsPrivileged) {
        BusinessQuestionnaire q = questionnaireRepository.findByAdRequestIdWithRevisionsAndAnswers(adRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_REQUEST_NOT_FOUND));
        if (!actorIsPrivileged && !q.getClient().getId().equals(actorClientId)) {
            throw new ForbiddenException(ClientValidationMessages.AD_REQUEST_NOT_FOUND);
        }
        BusinessQuestionnaireRevision rev = revisionRepository.findTopByQuestionnaire_IdOrderByVersionDesc(q.getId())
                .orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_REQUEST_NOT_FOUND));
        String text = buildPlainText(q.getClient().getBusinessName(), rev);
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public Optional<Integer> findLatestVersionByAdRequestId(UUID adRequestId) {
        return questionnaireRepository.findByAdRequest_Id(adRequestId)
                .flatMap(q -> revisionRepository.findTopByQuestionnaire_IdOrderByVersionDesc(q.getId()))
                .map(BusinessQuestionnaireRevision::getVersion);
    }

    @Transactional(readOnly = true)
    public Optional<Instant> findLatestRevisionCreatedAt(UUID adRequestId) {
        return questionnaireRepository.findByAdRequest_Id(adRequestId)
                .flatMap(q -> revisionRepository.findTopByQuestionnaire_IdOrderByVersionDesc(q.getId()))
                .map(BusinessQuestionnaireRevision::getCreatedAt);
    }

    @Transactional(readOnly = true)
    public Optional<BusinessQuestionnaireAnswersRequestDto> getLatestAnswersForClientAdRequest(UUID clientId, UUID adRequestId) {
        return questionnaireRepository.findByAdRequest_Id(adRequestId)
                .filter(q -> q.getClient().getId().equals(clientId))
                .flatMap(q -> revisionRepository.findTopByQuestionnaire_IdOrderByVersionDesc(q.getId()))
                .map(this::revisionToDto);
    }

    private void notifyAdminsQuestionnaireUpdated(Client client, AdRequest adRequest, int version) {
        String adminLink = frontBaseUrl + "/admin/ads";
        Map<String, String> base = new HashMap<>();
        base.put("clientName", client.getBusinessName());
        base.put("revisionVersion", String.valueOf(version));
        base.put("link", adminLink);
        base.put("adRequestId", adRequest.getId().toString());
        for (Client recipient : clientRepository.findAllAdminsAndDevelopers()) {
            boolean canManageAds = recipient.isDeveloper()
                    || permissionService.hasPermission(recipient, Permission.ADMIN_ADS_MANAGE);
            if (!canManageAds) {
                continue;
            }
            boolean sendEmail = !recipient.isDeveloper()
                    && adminEmailAlertPreferenceService.wantsEmail(
                            recipient.getId(), AdminEmailAlertCategory.ADS_MANAGEMENT);
            notificationService.save(
                    NotificationReference.AD_REQUEST_QUESTIONNAIRE_UPDATED,
                    recipient,
                    new HashMap<>(base),
                    sendEmail
            );
        }
    }

    private void validateAnswers(BusinessQuestionnaireAnswersRequestDto dto) {
        if (dto == null) {
            throw new BusinessRuleException("Business answers are required.");
        }
        Map<BusinessQuestionKey, String> map = toEnumMap(dto);
        for (BusinessQuestionKey k : BusinessQuestionKey.orderedForQuestionnaire()) {
            String v = map.get(k);
            if (v == null || v.isBlank()) {
                throw new BusinessRuleException("All business questions must be answered.");
            }
            if (v.length() > MAX_ANSWER_CHARS) {
                throw new BusinessRuleException("Answer too long for " + k.name());
            }
        }
    }

    private Map<BusinessQuestionKey, String> toEnumMap(BusinessQuestionnaireAnswersRequestDto dto) {
        EnumMap<BusinessQuestionKey, String> m = new EnumMap<>(BusinessQuestionKey.class);
        m.put(BusinessQuestionKey.PRODUCT_OR_SERVICE, dto.getProductOrService());
        m.put(BusinessQuestionKey.IDEAL_CUSTOMER, dto.getIdealCustomer());
        m.put(BusinessQuestionKey.PROBLEM_SOLVED, dto.getProblemSolved());
        m.put(BusinessQuestionKey.DESIRED_RESULT, dto.getDesiredResult());
        m.put(BusinessQuestionKey.CONCERN_BEFORE_CHOOSING, dto.getConcernBeforeChoosing());
        m.put(BusinessQuestionKey.WHY_TRUST, dto.getWhyTrust());
        m.put(BusinessQuestionKey.ONE_MESSAGE_TO_REMEMBER, dto.getOneMessageToRemember());
        m.put(BusinessQuestionKey.NEXT_ACTION, dto.getNextAction());
        m.put(BusinessQuestionKey.VISUAL_HAPPY_OUTCOME, dto.getVisualHappyOutcome());
        m.put(BusinessQuestionKey.AD_TONE, dto.getAdTone());
        return m;
    }

    private void applyAnswers(BusinessQuestionnaireRevision rev, BusinessQuestionnaireAnswersRequestDto dto) {
        Map<BusinessQuestionKey, String> map = toEnumMap(dto);
        for (Map.Entry<BusinessQuestionKey, String> e : map.entrySet()) {
            BusinessQuestionnaireAnswer a = new BusinessQuestionnaireAnswer();
            a.setRevision(rev);
            a.setQuestionKey(e.getKey().name());
            a.setAnswerText(e.getValue().trim());
            rev.getAnswers().add(a);
        }
    }

    private boolean answersEqual(BusinessQuestionnaireRevision rev, BusinessQuestionnaireAnswersRequestDto dto) {
        Map<BusinessQuestionKey, String> wanted = toEnumMap(dto);
        Map<String, String> existing = rev.getAnswers().stream()
                .filter(a -> !BusinessQuestionKey.isLegacyKey(a.getQuestionKey()))
                .collect(Collectors.toMap(BusinessQuestionnaireAnswer::getQuestionKey, BusinessQuestionnaireAnswer::getAnswerText));
        for (BusinessQuestionKey k : BusinessQuestionKey.orderedForQuestionnaire()) {
            if (!Objects.equals(
                    existing.getOrDefault(k.name(), "").trim(),
                    wanted.getOrDefault(k, "").trim())) {
                return false;
            }
        }
        return true;
    }

    private BusinessQuestionnaireAnswersRequestDto revisionToDto(BusinessQuestionnaireRevision rev) {
        Map<String, String> byKey = rev.getAnswers().stream()
                .collect(Collectors.toMap(
                        BusinessQuestionnaireAnswer::getQuestionKey,
                        BusinessQuestionnaireAnswer::getAnswerText,
                        (a, b) -> a));
        BusinessQuestionnaireAnswersRequestDto dto = new BusinessQuestionnaireAnswersRequestDto();
        dto.setProductOrService(byKey.getOrDefault(BusinessQuestionKey.PRODUCT_OR_SERVICE.name(), ""));
        dto.setIdealCustomer(byKey.getOrDefault(BusinessQuestionKey.IDEAL_CUSTOMER.name(), ""));
        dto.setProblemSolved(byKey.getOrDefault(BusinessQuestionKey.PROBLEM_SOLVED.name(), ""));
        dto.setDesiredResult(byKey.getOrDefault(BusinessQuestionKey.DESIRED_RESULT.name(), ""));
        dto.setConcernBeforeChoosing(byKey.getOrDefault(BusinessQuestionKey.CONCERN_BEFORE_CHOOSING.name(), ""));
        dto.setWhyTrust(byKey.getOrDefault(BusinessQuestionKey.WHY_TRUST.name(), ""));
        dto.setOneMessageToRemember(byKey.getOrDefault(BusinessQuestionKey.ONE_MESSAGE_TO_REMEMBER.name(), ""));
        dto.setNextAction(byKey.getOrDefault(BusinessQuestionKey.NEXT_ACTION.name(), ""));
        dto.setVisualHappyOutcome(byKey.getOrDefault(BusinessQuestionKey.VISUAL_HAPPY_OUTCOME.name(), ""));
        dto.setAdTone(byKey.getOrDefault(BusinessQuestionKey.AD_TONE.name(), ""));
        return dto;
    }

    private String buildPlainText(String clientName, BusinessQuestionnaireRevision rev) {
        StringBuilder sb = new StringBuilder();
        sb.append("Telas — Business questionnaire").append(System.lineSeparator());
        sb.append("Client: ").append(clientName).append(System.lineSeparator());
        sb.append("Revision: ").append(rev.getVersion()).append(System.lineSeparator());
        sb.append("Created at (UTC): ").append(rev.getCreatedAt()).append(System.lineSeparator());
        sb.append(System.lineSeparator());
        Map<String, String> byKey = rev.getAnswers().stream()
                .collect(Collectors.toMap(BusinessQuestionnaireAnswer::getQuestionKey, BusinessQuestionnaireAnswer::getAnswerText, (a, b) -> a));
        List<BusinessQuestionnaireAnswer> ordered = rev.getAnswers().stream()
                .sorted((a, b) -> Integer.compare(orderKey(a.getQuestionKey()), orderKey(b.getQuestionKey())))
                .toList();
        int n = 1;
        for (BusinessQuestionnaireAnswer a : ordered) {
            String label = labelForKey(a.getQuestionKey());
            sb.append(n++).append(". ").append(label).append(System.lineSeparator());
            sb.append(byKey.getOrDefault(a.getQuestionKey(), "")).append(System.lineSeparator());
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    private int orderKey(String key) {
        try {
            return BusinessQuestionKey.valueOf(key).ordinal();
        } catch (IllegalArgumentException ignored) {
            return 999;
        }
    }

    private String labelForKey(String key) {
        try {
            return BusinessQuestionKey.valueOf(key).exportLabel();
        } catch (IllegalArgumentException ignored) {
            return key;
        }
    }
}
