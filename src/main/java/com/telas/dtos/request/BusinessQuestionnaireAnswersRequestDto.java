package com.telas.dtos.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.shared.constants.BusinessQuestionnaireConstants;
import com.telas.shared.utils.TrimStringDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessQuestionnaireAnswersRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = BusinessQuestionnaireConstants.MAX_ANSWER_LENGTH)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String productOrService;

    @NotBlank
    @Size(max = BusinessQuestionnaireConstants.MAX_ANSWER_LENGTH)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String idealCustomer;

    @NotBlank
    @Size(max = BusinessQuestionnaireConstants.MAX_ANSWER_LENGTH)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String problemSolved;

    @NotBlank
    @Size(max = BusinessQuestionnaireConstants.MAX_ANSWER_LENGTH)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String desiredResult;

    @NotBlank
    @Size(max = BusinessQuestionnaireConstants.MAX_ANSWER_LENGTH)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String concernBeforeChoosing;

    @NotBlank
    @Size(max = BusinessQuestionnaireConstants.MAX_ANSWER_LENGTH)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String whyTrust;

    @NotBlank
    @Size(max = BusinessQuestionnaireConstants.MAX_ANSWER_LENGTH)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String oneMessageToRemember;

    @NotBlank
    @Size(max = BusinessQuestionnaireConstants.MAX_ANSWER_LENGTH)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String nextAction;

    @NotBlank
    @Size(max = BusinessQuestionnaireConstants.MAX_ANSWER_LENGTH)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String visualHappyOutcome;

    @NotBlank
    @Size(max = BusinessQuestionnaireConstants.MAX_ANSWER_LENGTH)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String adTone;
}
