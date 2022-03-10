package com.vantar.web;

import org.aeonbits.owner.Config;


public interface WebConfig {

    @Config.DefaultValue("false")
    @Config.Key("web.log.request")
    boolean logRequest();

    @Config.DefaultValue("false")
    @Config.Key("web.log.response")
    boolean logResponse();

}
