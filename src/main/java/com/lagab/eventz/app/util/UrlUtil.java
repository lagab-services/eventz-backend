package com.lagab.eventz.app.util;

import java.text.Normalizer;

import org.apache.commons.lang3.StringUtils;

public class UrlUtil {
    public static String slugify(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                                      .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        normalized = StringUtils.lowerCase(normalized);

        normalized = normalized.replaceAll("[^a-z0-9\\s-]", "");

        normalized = normalized.replaceAll("[\\s-]+", "-");

        normalized = StringUtils.strip(normalized, "-");

        return normalized;
    }

}
