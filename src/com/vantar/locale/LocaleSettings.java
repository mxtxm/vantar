package com.vantar.locale;

import org.aeonbits.owner.Config;


public interface LocaleSettings {

    @Config.DefaultValue("fa")
    @Config.Key("locale.default")
    String getLocaleDefault();

}
