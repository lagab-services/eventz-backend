package com.lagab.eventz.app.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UrlUtilTest {

    @Test
    void shouldConvertSimpleTextToSlug() {
        // Given
        String text = "Hello World";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("hello-world");
    }

    @Test
    void shouldRemoveAccentsFromText() {
        // Given
        String text = "Café à Paris";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("cafe-a-paris");
    }

    @Test
    void shouldHandleFrenchAccents() {
        // Given
        String text = "Événement à Montréal";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("evenement-a-montreal");
    }

    @Test
    void shouldRemoveSpecialCharacters() {
        // Given
        String text = "Hello@World#2024!";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("helloworld2024");
    }

    @Test
    void shouldReplaceMultipleSpacesWithSingleDash() {
        // Given
        String text = "Hello    World    Test";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("hello-world-test");
    }

    @Test
    void shouldReplaceMultipleDashesWithSingleDash() {
        // Given
        String text = "Hello---World";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("hello-world");
    }

    @Test
    void shouldTrimLeadingAndTrailingDashes() {
        // Given
        String text = "-Hello World-";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("hello-world");
    }

    @Test
    void shouldHandleNumbersInText() {
        // Given
        String text = "Event 2024 Day 1";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("event-2024-day-1");
    }

    @Test
    void shouldConvertToLowerCase() {
        // Given
        String text = "HELLO WORLD";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("hello-world");
    }

    @Test
    void shouldHandleMixedCaseWithAccents() {
        // Given
        String text = "ConcERT à PARIS 2024";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("concert-a-paris-2024");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "\t", "\n" })
    void shouldReturnEmptyStringForBlankInput(String input) {
        // When
        String result = UrlUtil.slugify(input);

        // Then
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "'Festival de Musique 2024', 'festival-de-musique-2024'",
            "'Théâtre & Cinéma', 'theatre-cinema'",
            "'Concert: Rock & Roll!', 'concert-rock-roll'",
            "'Expo d''Art Moderne', 'expo-dart-moderne'",
            "'Soirée Dansante été 2024', 'soiree-dansante-ete-2024'"
    })
    void shouldSlugifyVariousEventNames(String input, String expected) {
        // When
        String result = UrlUtil.slugify(input);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldHandleGermanUmlauts() {
        // Given
        String text = "Über München";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("uber-munchen");
    }

    @Test
    void shouldHandleSpanishCharacters() {
        // Given
        String text = "Año Español";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("ano-espanol");
    }

    @Test
    void shouldHandleOnlySpecialCharacters() {
        // Given
        String text = "@#$%^&*()";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleOnlyDashes() {
        // Given
        String text = "---";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleVeryLongText() {
        // Given
        String text = "This is a very long event name that contains many words and should be properly slugified";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("this-is-a-very-long-event-name-that-contains-many-words-and-should-be-properly-slugified");
    }

    @Test
    void shouldHandleTextWithUnderscores() {
        // Given
        String text = "Hello_World_Test";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("helloworldtest");
    }

    @Test
    void shouldPreserveExistingDashes() {
        // Given
        String text = "Hello-World";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("hello-world");
    }

    @Test
    void shouldHandleComplexMixedContent() {
        // Given
        String text = "  --Événement@2024: Café & Thé-- !! ";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("evenement2024-cafe-the");
    }

    @Test
    void shouldHandleEmojiAndUnicodeCharacters() {
        // Given
        String text = "Festival 🎵 2024";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        assertThat(result).isEqualTo("festival-2024");
    }

    @Test
    void shouldHandleCyrillicCharacters() {
        // Given
        String text = "Привет World";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        // Cyrillic characters are removed as they're not in [a-z0-9]
        assertThat(result).isEqualTo("world");
    }

    @Test
    void shouldHandleChineseCharacters() {
        // Given
        String text = "你好 World 2024";

        // When
        String result = UrlUtil.slugify(text);

        // Then
        // Chinese characters are removed
        assertThat(result).isEqualTo("world-2024");
    }
}
