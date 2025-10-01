package com.telas.shared.constants;

public final class SharedConstants {
    public static final String DAILY_CRON = "0 0 3 * * *";
    public static final String EXPIRY_SUBSCRIPTION_CRON = "0 0 7 * * *";
    public static final String ZONE_ID = "America/New_York";
    public static final String PROJECT_NAME = "Telas";
    public static final String PERMISSIONS = "permissions";
    public static final String USD = "usd";
    public static final String CARD = "card";

    public static final String EMAIL_SENDER = "support@telas-ads.com";
    public static final String EMAIL_SUBJECT_CONTACT_VERIFICATION = "Registry Confirmation - Telas";
    public static final String TEMPLATE_EMAIL_CONTACT_VERIFICATION = "email_contact_confirmation.ftlh";
    public static final String TEMPLATE_EMAIL_RESET_PASSWORD = "email_reset_password.ftlh";
    public static final String EMAIL_SUBJECT_RESET_PASSWORD = "Password Reset - Telas";
    public static final String TEMPLATE_EMAIL_FIRST_SUBSCRIPTION = "email_first_subscription.ftlh";
    public static final String EMAIL_SUBJECT_FIRST_SUBSCRIPTION = "Your Ad service Purchase is Confirmed";
    public static final String TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING_REMINDER = "email_subscription_about_expire_reminder.ftlh";
    public static final String EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING_REMINDER = "15 Days Before Expiration";
    public static final String TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING_LAST_DAY = "email_subscription_about_expire_last_day.ftlh";
    public static final String EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING_LAST_DAY = "Last Day of Service";


    public static final String REGEX_ONLY_NUMBERS = "^\\d+$";
    public static final String REGEX_ALPHANUMERIC = "^[A-Za-zÀ-ÿ0-9\\s]*$";
    public static final String REGEX_PASSWORD = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]+$";
    public static final String REGEX_ONLY_LETTERS = "^[a-zA-ZÀ-ÖØ-öø-ÿ\\s]*$";
    public static final String REGEX_ZIP_CODE = "\\d{5}";
    public static final String REGEX_IP = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    public static final String REGEX_ATTACHMENT_NAME = ".*\\.(jpg|jpeg|png|gif|svg|bmp|tiff)$";
    public static final String REGEX_ATTACHMENT_TYPE = "image/(jpg|jpeg|png|gif|svg|bmp|tiff)";

    public static final int ZERO = 0;
    public static final int ATTACHMENT_LINK_EXPIRY_TIME = 7 * 24 * 60 * 60;
    public static final int MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024;
    public static final int MAX_ATTACHMENT_PER_CLIENT = 5;
    public static final int AD_DISPLAY_TIME_IN_SECONDS = 5;
    public static final int TOTAL_SECONDS_IN_A_MINUTE = 60;
    public static final int TOTAL_SECONDS_IN_A_DAY = 24 * 60 * 60;


    public static final int MAX_MONITOR_ADS = 17;
    public static final int MAX_ADS_VALIDATION = 3;
    public static final int MIN_QUANTITY_MONITOR_BLOCK = 1;
    public static final int MAX_QUANTITY_MONITOR_BLOCK = 3;

    public static final int TAMANHO_NOME_ANEXO = 255;
    public static final int MAX_ADS_PER_CLIENT = 1;

    public static final long MAX_BILLING_CYCLE_ANCHOR = 30L * 24 * 60 * 60;

}
