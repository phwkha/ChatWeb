package com.web.backend.config.LocalResolverConfig;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class Translator {

    private static ResourceBundleMessageSource messageSource;

    public Translator(ResourceBundleMessageSource messageSource) {
        Translator.messageSource = messageSource;
    }

    public static String tolocale(@NonNull String msgCode, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(msgCode, args, locale);
    }
}