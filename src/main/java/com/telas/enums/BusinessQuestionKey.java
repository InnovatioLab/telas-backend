package com.telas.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public enum BusinessQuestionKey {
    PRODUCT_OR_SERVICE,
    IDEAL_CUSTOMER,
    PROBLEM_SOLVED,
    DESIRED_RESULT,
    CONCERN_BEFORE_CHOOSING,
    WHY_TRUST,
    ONE_MESSAGE_TO_REMEMBER,
    NEXT_ACTION,
    VISUAL_HAPPY_OUTCOME,
    AD_TONE;

    public static List<BusinessQuestionKey> orderedForQuestionnaire() {
        return Arrays.stream(values())
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
    }

    public String exportLabel() {
        return switch (this) {
            case PRODUCT_OR_SERVICE -> "What product or service do you offer?";
            case IDEAL_CUSTOMER -> "Who is your ideal customer?";
            case PROBLEM_SOLVED -> "What problem do you help them solve?";
            case DESIRED_RESULT -> "What result do they want after working with you?";
            case CONCERN_BEFORE_CHOOSING ->
                    "What concern or frustration do they usually have before choosing a business like yours?";
            case WHY_TRUST -> "Why should customers trust you?";
            case ONE_MESSAGE_TO_REMEMBER -> "What is the one message you want people to remember?";
            case NEXT_ACTION -> "What action should they take next?";
            case VISUAL_HAPPY_OUTCOME -> "What visual best represents the happy outcome?";
            case AD_TONE -> "What tone should the ad have?";
        };
    }

    public static boolean isLegacyKey(String key) {
        return "LEGACY_SLOGAN".equals(key) || "LEGACY_BRAND_GUIDELINE_URL".equals(key);
    }
}
