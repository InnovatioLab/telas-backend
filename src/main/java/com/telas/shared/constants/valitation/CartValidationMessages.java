package com.telas.shared.constants.valitation;

import com.telas.shared.constants.SharedConstants;

public final class CartValidationMessages {
    public static final String MONITOR_ID_REQUIRED = "Monitor ID is required.";
    public static final String MIN_QUANTITY_MONITOR_BLOCK = "Minimum quantity of monitor blocks is " + SharedConstants.MIN_QUANTITY_MONITOR_BLOCK + ".";
    public static final String MAX_QUANTITY_MONITOR_BLOCK = "Maximum quantity of monitor blocks is " + SharedConstants.MAX_QUANTITY_MONITOR_BLOCK + ".";
    public static final String ITEMS_REQUIRED = "At least one item is required in the cart.";
    public static final String CART_NOT_FOUND = "Cart not found.";
    public static final String CART_ALREADY_EXISTS = "Cart already exists for this client.";
}
