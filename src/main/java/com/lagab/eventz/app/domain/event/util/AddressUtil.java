package com.lagab.eventz.app.domain.event.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.lagab.eventz.app.domain.event.model.Address;

public class AddressUtil {

    public static String formatAddress(Address address) {
        if (address == null)
            return "";

        List<String> parts = new ArrayList<>();

        // Add address lines
        addIfNotBlank(parts, address.getAddress1());
        addIfNotBlank(parts, address.getAddress2());

        // Add zip code + city (combined)
        String zip = StringUtils.trimToEmpty(address.getZipCode());
        String city = StringUtils.trimToEmpty(address.getCity());
        if (!zip.isEmpty() || !city.isEmpty()) {
            parts.add((zip + " " + city).trim());
        }

        // Add country
        addIfNotBlank(parts, address.getCountry());

        return String.join(", ", parts);
    }

    private static void addIfNotBlank(List<String> parts, String value) {
        if (StringUtils.isNotBlank(value)) {
            parts.add(value.trim());
        }
    }
}
