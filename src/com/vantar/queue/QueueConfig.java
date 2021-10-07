package com.vantar.queue;

import org.aeonbits.owner.Config;


public interface QueueConfig {

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
}
