package com.vantar.locale;

import org.aeonbits.owner.Config;


public interface LocaleConfig {

    @Config.DefaultValue("en")
    @Config.Key("locale.default")
    String getLocaleDefault();
}
