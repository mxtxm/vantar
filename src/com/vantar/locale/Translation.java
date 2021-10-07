package com.vantar.locale;


public interface Translation {

    String getLangKey();

    String getString(LangKey key);

    String getString(String key);
}
