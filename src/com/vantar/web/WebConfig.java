package com.vantar.web;

import org.aeonbits.owner.Config;


public interface WebConfig {

    @Config.DefaultValue("false")
    @Config.Key("service.log.request")
    boolean logRequest();

    @Config.DefaultValue("false")
    @Config.Key("service.log.response")
    boolean logResponse();
}
