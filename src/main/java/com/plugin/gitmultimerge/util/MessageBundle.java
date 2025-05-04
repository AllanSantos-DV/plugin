package com.plugin.gitmultimerge.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Message provider for internationalization support.
 * Implements access to localized resources.
 */
public final class MessageBundle {
    private static final String BUNDLE = "messages.GitMultiMergeBundle";

    private MessageBundle() {
        // Utility class, não instanciar
    }

    /**
     * Gets a message from the resource bundle.
     *
     * @param key    The key of the message.
     * @param params The parameters for the message.
     * @return The formatted message.
     */
    public static @NotNull String message(@NotNull String key, @NotNull Object... params) {
        String value = findStringByKey(key);
        if (value == null) {
            return missingMessage(key);
        }

        if (params.length > 0) {
            return MessageFormat.format(value, params);
        }

        return value;
    }

    private static @Nullable String findStringByKey(@NotNull String key) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, Locale.getDefault());
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            try {
                // Tentar com o bundle padrão se não encontrar no bundle específico
                ResourceBundle defaultBundle = ResourceBundle.getBundle(BUNDLE, Locale.ROOT);
                return defaultBundle.getString(key);
            } catch (MissingResourceException ex) {
                return null;
            }
        }
    }

    private static @NotNull String missingMessage(@NotNull String key) {
        return "!" + key + "!";
    }
}