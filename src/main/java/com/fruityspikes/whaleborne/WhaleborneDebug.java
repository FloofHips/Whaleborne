package com.fruityspikes.whaleborne;

import java.util.Locale;

public final class WhaleborneDebug {

    private static final String TAG = "[whaleborne] ";

    private WhaleborneDebug() {
    }

    public static boolean on() {
        return Whaleborne.LOGGER.isDebugEnabled();
    }

    public static void log(String format, Object... args) {
        if (!on()) {
            return;
        }
        Whaleborne.LOGGER.debug(TAG + String.format(Locale.ROOT, format, args));
    }
}
