package com.marketingproject.shared.constants;

import java.util.List;

public final class SharedConstants {
    public static final String PHONE_PREFIX = "";
    public static final String PROJECT_NAME = "Marketing-Project";
    public static final String DESTINATARIO = "Recipient";
    public static final String PERMISSIONS = "permissions";

    public static final String EMAIL_SENDER = "victoremmanuelmn@gmail.com";
    public static final String EMAIL_SUBJECT_CONTACT_VERIFICATION = "Registry Confirmation - Marketing Project";
    public static final String TEMPLATE_EMAIL_CONTACT_VERIFICATION = "email_contact_confirmation.ftlh";
    public static final String TEMPLATE_EMAIL_RESET_PASSWORD = "email_reset_password.ftlh";
    public static final String EMAIL_SUBJECT_RESET_PASSWORD = "Password Reset - Marketing Project";

    public static final int TAMANHO_PADRAO_NOME_RAZAO_SOCIAL = 100;
    public static final int ZERO = 0;

    public static final List<String> FORMATOS_PERMITIDOS_ANEXO = List.of("jpeg", "png");

    public static final String REGEX_ONLY_NUMBERS = "^\\d+$";
    public static final String REGEX_IDENTIFICATION_NUMBER_CLIENT = "^\\d{9}$";
    public static final String REGEX_IDENTIFICATION_NUMBER_OWNER = "^(?!666|000|9\\d{2})\\d{9}$";
    public static final String REGEX_ALPHANUMERIC = "^[A-Za-zÀ-ÿ0-9\\s]*$";
    public static final String REGEX_PASSWORD = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]+$";
    public static final String REGEX_ONLY_LETTERS = "^[a-zA-ZÀ-ÖØ-öø-ÿ\\s]*$";
    public static final String REGEX_ONLY_LETTERS_NUMBERS = "[\\p{L}0-9\\s-.,]+";

    public static final String REGEX_ATTACHMENT_NAME = ".*\\.(jpg|jpeg|png|gif|svg|bmp|tiff)$";
    public static final String REGEX_ATTACHMENT_TYPE = "image/(jpg|jpeg|png|gif|svg|bmp|tiff)";

    public static final int ATTACHMENT_LINK_EXPIRY_TIME = 7 * 24 * 60 * 60;
    public static final int MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024;

    public static final int QUANTIDADE_MAXIMA_FOTO_PRODUTO = 5;
    public static final int QUANTIDADE_MINIMA_PRODUTO = 1;
    public static final int TAMANHO_MAXIMO_DIGITOS_PRECO = 8;
    public static final int TAMANHO_MAXIMO_FRACTION_PRECO = 2;

    public static final int TAMANHO_NOME_ANEXO = 255;
}
