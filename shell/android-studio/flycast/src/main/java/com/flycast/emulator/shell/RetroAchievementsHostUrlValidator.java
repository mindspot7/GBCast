package com.flycast.emulator.shell;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Syntax validation for RetroAchievements proxy base URLs received across the app boundary.
 *
 * This validates URL shape only. It does not authenticate the broadcast sender or decide
 * whether a remote destination is trusted; that policy is tracked separately.
 */
public final class RetroAchievementsHostUrlValidator {
    private RetroAchievementsHostUrlValidator() {}

    public static boolean isValid(String hostUrl) {
        if (hostUrl == null || hostUrl.isEmpty()) return false;

        for (int i = 0; i < hostUrl.length(); i++) {
            if (Character.isISOControl(hostUrl.charAt(i)) || Character.isWhitespace(hostUrl.charAt(i))) {
                return false;
            }
        }

        try {
            URI uri = new URI(hostUrl);
            String scheme = uri.getScheme();
            if (!uri.isAbsolute() || uri.isOpaque()
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getHost().isEmpty()
                    || uri.getRawUserInfo() != null) {
                return false;
            }

            int port = uri.getPort();
            return port == -1 || (port >= 1 && port <= 65535);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
