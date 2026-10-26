package com.flycast.emulator.shell;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RetroAchievementsHostUrlValidatorTest {
    @Test
    public void acceptsHttpAndHttpsProxyUrls() {
        assertTrue(RetroAchievementsHostUrlValidator.isValid("http://127.0.0.1:8080"));
        assertTrue(RetroAchievementsHostUrlValidator.isValid("https://proxy.example.test/api?mode=ra"));
        assertTrue(RetroAchievementsHostUrlValidator.isValid("http://localhost"));
    }

    @Test
    public void rejectsMissingOrUnsupportedUrlParts() {
        assertFalse(RetroAchievementsHostUrlValidator.isValid(null));
        assertFalse(RetroAchievementsHostUrlValidator.isValid(""));
        assertFalse(RetroAchievementsHostUrlValidator.isValid("ftp://proxy.example.test"));
        assertFalse(RetroAchievementsHostUrlValidator.isValid("proxy.example.test"));
        assertFalse(RetroAchievementsHostUrlValidator.isValid("http:///missing-host"));
        assertFalse(RetroAchievementsHostUrlValidator.isValid("http://"));
    }

    @Test
    public void rejectsUserInfoAndInvalidPorts() {
        assertFalse(RetroAchievementsHostUrlValidator.isValid("http://user:pass@proxy.example.test"));
        assertFalse(RetroAchievementsHostUrlValidator.isValid("http://proxy.example.test:0"));
        assertFalse(RetroAchievementsHostUrlValidator.isValid("http://proxy.example.test:65536"));
        assertFalse(RetroAchievementsHostUrlValidator.isValid("http://proxy.example.test:abc"));
    }

    @Test
    public void rejectsControlCharactersWhitespaceAndMalformedUris() {
        assertFalse(RetroAchievementsHostUrlValidator.isValid("http://proxy.example.test\nHostUrl=evil"));
        assertFalse(RetroAchievementsHostUrlValidator.isValid("http://proxy.example.test/path with space"));
        assertFalse(RetroAchievementsHostUrlValidator.isValid("http://[invalid-ipv6"));
    }
}
