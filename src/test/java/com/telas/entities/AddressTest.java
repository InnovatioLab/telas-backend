package com.telas.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressTest {

    @Test
    void getFullAddressFormattedHtml_escapesAndJoinsParts() {
        Address address = new Address();
        address.setStreet("1 Main St");
        address.setCity("Austin");
        address.setState("TX");
        address.setZipCode("78701");
        address.setCountry("US");
        address.setLocationName("<script>");
        address.setAddress2("Suite 2");

        String html = address.getFullAddressFormattedHtml();

        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("1 Main St"));
        assertTrue(html.contains("Suite 2"));
        assertTrue(html.contains("Austin"));
    }
}
