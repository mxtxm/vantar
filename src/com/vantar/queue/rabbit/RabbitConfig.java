package com.vantar.queue.rabbit;

import org.aeonbits.owner.Config;


public interface RabbitConfig {

    @Config.Key("rabbitmq.host")
    String getRabbitMqHost();

    @Config.Key("rabbitmq.port")
    int getRabbitPort();

    @Config.Key("rabbitmq.user")
    String getRabbitMqUser();

    @Config.Key("rabbitmq.password")
    String getRabbitMqPassword();

    @Config.DefaultValue("false")
    @Config.Key("rabbitmq.destroy.queues.at.shutdown")
    boolean rabbitmqDestroyQueuesAtShutdown();

    @Config.Key("rabbitmq.queues")
    String getRabbitMqQueues();

    @Config.DefaultValue("10")
    @Config.Key("rabbitmq.heartbeat")
    int getRabbitMqHeartbeat();

    @Config.DefaultValue("10000")
    @Config.Key("rabbitmq.timeout")
    int getRabbitMqTimeout();
}
