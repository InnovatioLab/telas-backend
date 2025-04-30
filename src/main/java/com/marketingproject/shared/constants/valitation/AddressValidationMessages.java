package com.marketingproject.shared.constants.valitation;

public final class AddressValidationMessages {
    public static final String STREET_REQUIRED = "Street is required";
    public static final String STREET_SIZE = "Street size must be less than or equal to 100 characters.";
    public static final String NUMBER_REQUIRED = "Address number is required";
    public static final String NUMBER_SIZE = "Address number size must be less than or equal to 10 characters.";
    public static final String ZIP_CODE_REQUIRED = "Zip code is required";
    public static final String ZIP_CODE_SIZE = "Zip code size must have five digits.";
    public static final String CITY_REQUIRED = "City is required";
    public static final String CITY_SIZE = "City size must be less than or equal to 50 characters.";
    public static final String STATE_REQUIRED = "State is required";
    public static final String STATE_SIZE = "State size must be two characters";
    public static final String COUNTRY_SIZE = "Country size must be less than or equal to 100 characters.";
    public static final String COMPLEMENT_SIZE = "Complement size must be less than or equal to 100 characters.";
    public static final String NUMBER_REGEX = "Address number must contain only letters and numbers";
    public static final String ZIP_CODE_REGEX = "Zip code must contain only numbers";
    public static final String CITY_REGEX = "City must contain only letters and numbers";
    public static final String STATE_REGEX = "State must contain only letters";
    public static final String COUNTRY_REGEX = "Country must contain only letters";
}
