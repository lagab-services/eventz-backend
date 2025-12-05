package com.lagab.eventz.app.domain.event.util;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.lagab.eventz.app.domain.event.model.Address;

import static org.assertj.core.api.Assertions.assertThat;

class AddressUtilTest {

    @Test
    void shouldFormatCompleteAddress() {
        // Given
        Address address = new Address();
        address.setAddress1("123 Rue de la Paix");
        address.setAddress2("Appartement 5");
        address.setZipCode("75001");
        address.setCity("Paris");
        address.setCountry("France");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("123 Rue de la Paix, Appartement 5, 75001 Paris, France");
    }

    @Test
    void shouldFormatAddressWithoutAddress2() {
        // Given
        Address address = new Address();
        address.setAddress1("10 Downing Street");
        address.setZipCode("SW1A 2AA");
        address.setCity("London");
        address.setCountry("United Kingdom");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("10 Downing Street, SW1A 2AA London, United Kingdom");
    }

    @Test
    void shouldFormatAddressWithOnlyCity() {
        // Given
        Address address = new Address();
        address.setCity("Paris");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("Paris");
    }

    @Test
    void shouldFormatAddressWithOnlyZipCode() {
        // Given
        Address address = new Address();
        address.setZipCode("75001");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("75001");
    }

    @Test
    void shouldFormatAddressWithZipCodeAndCity() {
        // Given
        Address address = new Address();
        address.setZipCode("75001");
        address.setCity("Paris");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("75001 Paris");
    }

    @Test
    void shouldReturnEmptyStringForNullAddress() {
        // When
        String result = AddressUtil.formatAddress(null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyStringForEmptyAddress() {
        // Given
        Address address = new Address();

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldTrimWhitespaceFromFields() {
        // Given
        Address address = new Address();
        address.setAddress1("  123 Main Street  ");
        address.setCity("  Paris  ");
        address.setZipCode("  75001  ");
        address.setCountry("  France  ");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("123 Main Street, 75001 Paris, France");
    }

    @Test
    void shouldIgnoreBlankFields() {
        // Given
        Address address = new Address();
        address.setAddress1("123 Main Street");
        address.setAddress2("   ");
        address.setZipCode("");
        address.setCity("Paris");
        address.setCountry(null);

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("123 Main Street, Paris");
    }

    @Test
    void shouldHandleOnlyAddress1() {
        // Given
        Address address = new Address();
        address.setAddress1("123 Main Street");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("123 Main Street");
    }

    @Test
    void shouldHandleOnlyCountry() {
        // Given
        Address address = new Address();
        address.setCountry("France");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("France");
    }

    @Test
    void shouldFormatAddressWithOnlyAddress1AndCountry() {
        // Given
        Address address = new Address();
        address.setAddress1("123 Main Street");
        address.setCountry("France");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("123 Main Street, France");
    }

    @Test
    void shouldHandleAddressWithNoZipButWithCity() {
        // Given
        Address address = new Address();
        address.setAddress1("123 Main Street");
        address.setCity("Paris");
        address.setCountry("France");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("123 Main Street, Paris, France");
    }

    @Test
    void shouldHandleAddressWithZipButNoCity() {
        // Given
        Address address = new Address();
        address.setAddress1("123 Main Street");
        address.setZipCode("75001");
        address.setCountry("France");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("123 Main Street, 75001, France");
    }

    @ParameterizedTest
    @MethodSource("provideRealWorldAddresses")
    void shouldFormatRealWorldAddresses(Address address, String expected) {
        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> provideRealWorldAddresses() {
        Address frenchAddress = new Address();
        frenchAddress.setAddress1("Tour Eiffel");
        frenchAddress.setAddress2("Champ de Mars");
        frenchAddress.setZipCode("75007");
        frenchAddress.setCity("Paris");
        frenchAddress.setCountry("France");

        Address usAddress = new Address();
        usAddress.setAddress1("1600 Pennsylvania Avenue NW");
        usAddress.setZipCode("20500");
        usAddress.setCity("Washington, DC");
        usAddress.setCountry("United States");

        Address ukAddress = new Address();
        ukAddress.setAddress1("Buckingham Palace");
        ukAddress.setZipCode("SW1A 1AA");
        ukAddress.setCity("London");
        ukAddress.setCountry("United Kingdom");

        Address germanAddress = new Address();
        germanAddress.setAddress1("Brandenburger Tor");
        germanAddress.setAddress2("Pariser Platz");
        germanAddress.setZipCode("10117");
        germanAddress.setCity("Berlin");
        germanAddress.setCountry("Germany");

        Address minimalAddress = new Address();
        minimalAddress.setCity("Barcelona");
        minimalAddress.setCountry("Spain");

        return Stream.of(
                Arguments.of(frenchAddress, "Tour Eiffel, Champ de Mars, 75007 Paris, France"),
                Arguments.of(usAddress, "1600 Pennsylvania Avenue NW, 20500 Washington, DC, United States"),
                Arguments.of(ukAddress, "Buckingham Palace, SW1A 1AA London, United Kingdom"),
                Arguments.of(germanAddress, "Brandenburger Tor, Pariser Platz, 10117 Berlin, Germany"),
                Arguments.of(minimalAddress, "Barcelona, Spain")
        );
    }

    @Test
    void shouldHandleAddressWithAllFieldsBlank() {
        // Given
        Address address = new Address();
        address.setAddress1("   ");
        address.setAddress2("   ");
        address.setZipCode("   ");
        address.setCity("   ");
        address.setCountry("   ");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleAddressWithMixedNullAndBlankFields() {
        // Given
        Address address = new Address();
        address.setAddress1(null);
        address.setAddress2("   ");
        address.setZipCode("75001");
        address.setCity(null);
        address.setCountry("France");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("75001, France");
    }

    @Test
    void shouldHandleVeryLongAddress() {
        // Given
        Address address = new Address();
        address.setAddress1("This is a very long address line that contains many details about the location");
        address.setAddress2("Building name and floor information that is also quite detailed");
        address.setZipCode("75001");
        address.setCity("Paris");
        address.setCountry("France");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).contains("This is a very long address")
                          .contains("Building name and floor")
                          .contains("75001 Paris")
                          .contains("France");
    }

    @Test
    void shouldHandleSpecialCharactersInAddress() {
        // Given
        Address address = new Address();
        address.setAddress1("123 Rue de l'Église");
        address.setAddress2("Bât. A - Porte 5");
        address.setZipCode("75001");
        address.setCity("Paris");
        address.setCountry("France");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("123 Rue de l'Église, Bât. A - Porte 5, 75001 Paris, France");
    }

    @Test
    void shouldHandleNumericOnlyZipCode() {
        // Given
        Address address = new Address();
        address.setZipCode("123456");
        address.setCity("Tokyo");
        address.setCountry("Japan");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("123456 Tokyo, Japan");
    }

    @Test
    void shouldHandleAlphanumericZipCode() {
        // Given
        Address address = new Address();
        address.setZipCode("SW1A 1AA");
        address.setCity("London");
        address.setCountry("United Kingdom");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("SW1A 1AA London, United Kingdom");
    }

    @Test
    void shouldIgnoreOnlineFields() {
        // Given
        Address address = new Address();
        address.setAddress1("123 Main Street");
        address.setCity("Paris");
        address.setCountry("France");
        address.setIsOnline(true);
        address.setOnlineUrl("https://example.com/event");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("123 Main Street, Paris, France");
    }

    @Test
    void shouldIgnoreCoordinates() {
        // Given
        Address address = new Address();
        address.setAddress1("123 Main Street");
        address.setCity("Paris");
        address.setCountry("France");
        address.setLatitude(new java.math.BigDecimal("48.8566"));
        address.setLongitude(new java.math.BigDecimal("2.3522"));

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("123 Main Street, Paris, France");
    }

    @Test
    void shouldIgnoreNameField() {
        // Given
        Address address = new Address();
        address.setName("Venue Name");
        address.setAddress1("123 Main Street");
        address.setCity("Paris");
        address.setCountry("France");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        assertThat(result).isEqualTo("123 Main Street, Paris, France");
    }

    @Test
    void shouldHandleStateField() {
        // Given
        Address address = new Address();
        address.setAddress1("123 Main Street");
        address.setCity("Los Angeles");
        address.setState("California");
        address.setCountry("United States");

        // When
        String result = AddressUtil.formatAddress(address);

        // Then
        // Note: Current implementation doesn't use state field
        assertThat(result).isEqualTo("123 Main Street, Los Angeles, United States");
    }
}
