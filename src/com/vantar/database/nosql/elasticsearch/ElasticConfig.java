package com.vantar.database.nosql.elasticsearch;

import org.aeonbits.owner.Config;


public interface ElasticConfig {

    @Config.DefaultValue("http,localhost,9200")
    @Config.Key("elastic.hosts")
    String getElasticHosts();

}
