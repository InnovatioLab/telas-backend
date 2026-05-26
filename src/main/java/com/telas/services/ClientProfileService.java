package com.telas.services;

import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.ClientRequestDto;
import com.telas.dtos.request.PermanentDeleteClientRequestDto;
import com.telas.dtos.response.ClientResponseDto;
import com.telas.dtos.response.ClientWorkspaceResponseDto;
import com.telas.dtos.response.PermanentDeletionRequirementsDto;
import com.telas.entities.Client;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.model.PasswordRequestDto;
import com.telas.infra.security.model.PasswordUpdateRequestDto;

import java.util.List;
import java.util.UUID;

public interface ClientProfileService {

    void save(ClientRequestDto request);

    ClientResponseDto findById(UUID id);

    ClientResponseDto findByEmailUnprotected(String email);

    Client findActiveEntityById(UUID id);

    Client findEntityById(UUID id);

    ClientResponseDto getDataFromToken();

    ClientWorkspaceResponseDto getClientWorkspace();

    void validateCode(String email, String codigo);

    void resendCode(String email);

    void createPassword(String email, PasswordRequestDto request);

    void sendResetPasswordCode(String email);

    void resetPassword(String email, PasswordRequestDto request);

    void updatePassword(PasswordUpdateRequestDto request, AuthenticatedUser authClient);

    void update(ClientRequestDto request, UUID id);

    void uploadAttachments(List<AttachmentRequestDto> request);

    void deleteClientAttachment(UUID attachmentId);

    void acceptTermsAndConditions();

    void deactivateClientByDeveloper(UUID clientId);

    void reactivateClientByDeveloper(UUID clientId);

    void softDeleteClientByDeveloper(UUID clientId);

    void restoreSoftDeletedClientByDeveloper(UUID clientId);

    PermanentDeletionRequirementsDto getPermanentDeletionRequirements(UUID clientId);

    void permanentlyDeleteClientByDeveloper(UUID clientId, PermanentDeleteClientRequestDto request);

    void incrementSubscriptionFlow();

    ClientResponseDto buildClientResponse(Client client);
}
