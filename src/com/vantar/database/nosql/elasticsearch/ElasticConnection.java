package com.vantar.database.nosql.elasticsearch;

import com.vantar.common.VantarParam;
import com.vantar.util.string.StringUtil;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import java.io.IOException;


public class ElasticConnection {

    private static RestHighLevelClient client;
    private static ElasticConfig config;
    private static boolean isUp;


    public static boolean isEnabled() {
        return config != null;
    }

    public static boolean isUp() {
        return isUp;
    }

    public static void connect(ElasticConfig config) {
        ElasticConnection.config = config;
        isUp = true;
    }

    protected static synchronized RestHighLevelClient getClient() {
        if (client == null) {
            String[] hostConfigs = StringUtil.splitTrim(config.getElasticHosts(), VantarParam.SEPARATOR_BLOCK);
            HttpHost[] hosts = new HttpHost[hostConfigs.length];
            int i = 0;
            for (String host : hostConfigs) {
                String[] hostParts = StringUtil.splitTrim(host, VantarParam.SEPARATOR_COMMON);
                hosts[i++] = new HttpHost(hostParts[1], StringUtil.toInteger(hostParts[2]), hostParts[0]);
            }
            client = new RestHighLevelClient(RestClient.builder(hosts));
        }
        return client;
    }

    private static synchronized void close() throws IOException {
        client.close();
        client = null;
        isUp = false;
    }

    public static void shutdown() {

    }
}
