package com.atablood.iWindoor_api.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectFileServiceSlugTest {

    @Test
    void turkishLettersAreTransliterated() {
        assertEquals("sisli-ofis-calisma", ProjectFileService.asciiSlug("Şişli Ofis Çalışma"));
    }

    @Test
    void uppercaseTurkishAndSpaces() {
        assertEquals("cankaya-gosteri", ProjectFileService.asciiSlug("ÇANKAYA  Gösteri"));
    }

    @Test
    void blankFallsBackToProject() {
        assertEquals("project", ProjectFileService.asciiSlug("   "));
        assertEquals("project", ProjectFileService.asciiSlug(null));
    }
}
