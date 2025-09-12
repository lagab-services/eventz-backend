package com.lagab.eventz.app.domain.ticket.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Assertions;

import java.util.Base64;

class QrCodeServiceTest {

    @Test
    @DisplayName("generateQrCodeBase64 should return a PNG data URL with valid PNG bytes")
    void generateQrCodeBase64_returnsPngDataUrl() {
        QrCodeService service = new QrCodeService();

        String text = "ticket:12345|user:abc";
        int width = 200;
        int height = 200;

        String dataUrl = service.generateQrCodeBase64(text, width, height);

        // Assert prefix
        String prefix = "data:image/png;base64,";
        Assertions.assertNotNull(dataUrl, "Data URL should not be null");
        Assertions.assertTrue(dataUrl.startsWith(prefix), "Data URL should start with '" + prefix + "'");

        // Decode Base64 part and check PNG signature
        String base64 = dataUrl.substring(prefix.length());
        byte[] bytes = Base64.getDecoder().decode(base64);

        Assertions.assertTrue(bytes.length > 8, "PNG bytes should be longer than the 8-byte signature");

        // PNG signature: 89 50 4E 47 0D 0A 1A 0A
        byte[] pngSig = new byte[] {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        for (int i = 0; i < pngSig.length; i++) {
            Assertions.assertEquals(pngSig[i], bytes[i], "PNG signature byte mismatch at index " + i);
        }
    }

    @Test
    @DisplayName("generateQrCodeBase64 should work with minimal inputs")
    void generateQrCodeBase64_minimalInputs() {
        QrCodeService service = new QrCodeService();

        String dataUrl = service.generateQrCodeBase64("a", 50, 50);

        String prefix = "data:image/png;base64,";
        Assertions.assertTrue(dataUrl.startsWith(prefix));

        String base64 = dataUrl.substring(prefix.length());
        byte[] bytes = Base64.getDecoder().decode(base64);
        Assertions.assertTrue(bytes.length > 8);
    }
}
