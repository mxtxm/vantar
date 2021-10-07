package com.vantar.queue;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.vantar.common.VantarParam;
import com.vantar.util.string.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;


public class QueueConnection {

    private static final Logger log = LoggerFactory.getLogger(QueueConnection.class);
    private Connection connection;
    private final Map<String, Deque<Channel>> channels = new HashMap<>();
    private final QueueConfig config;
    private final QueueExceptionHandler exceptionHandler;

    public boolean isUp;


    public QueueConnection(QueueConfig config) {
        this(config, null);
    }

    public QueueConnection(QueueConfig config, QueueExceptionHandler exceptionHandler) {
        this.config = config;
        this.exceptionHandler = exceptionHandler;
        Connection connection = getConnection();
        if (connection == null) {
            log.error("! FAILED TO INIT RABBIT CHANNEL POOL (connection=null)");
            return;
        }
        log.info("success > connection to rabbit");

        for (String s : StringUtil.split(config.getRabbitMqQueues(), VantarParam.SEPARATOR_BLOCK)) {
            String[] queueInitChannels = StringUtil.split(s, VantarParam.SEPARATOR_COMMON);

            if (queueInitChannels.length == 2) {
                String queueName = queueInitChannels[0];
                int connectionPoolSize = StringUtil.toInteger(queueInitChannels[1]);
                Deque<Channel> channels = new ArrayDeque<>();

                for (int i = 0; i < connectionPoolSize; ++i) {
                    try {
                        Channel channel = connection.createChannel();
                        channel.queueDeclare(queueName, false, false, false, null);
                        channels.push(channel);
                        log.info("created queue channel({})", queueName);
                    } catch (IOException e) {
                        log.error("! FAILED TO CREATE RABBIT CHANNEL ({}, {})", queueName, connectionPoolSize, e);
                    }
                }

                this.channels.put(queueName, channels);
            }
        }
        isUp = true;
    }

    public synchronized Channel get(String queueName) {
        Deque<Channel> channels = this.channels.get(queueName);
        if (channels == null) {
            this.channels.put(queueName, new ArrayDeque<>());
        } else if (!channels.isEmpty()) {
            Channel channel = channels.pop();
            if (channel != null && channel.isOpen()) {
                return channel;
            } else {
                return get(queueName);
            }
        }

        Connection connection = getConnection();
        if (connection == null) {
            log.error("! FAILED TO CREATE RABBIT CHANNEL (connection=null)");
            return null;
        }

        try {
            Channel channel = connection.createChannel();
            if (channel == null) {
                log.error("! FAILED TO CREATE RABBIT CHANNEL({})", queueName);
                return null;
            }
            channel.queueDeclare(queueName, false, false, false, null);
            log.debug("created queue channel({})", queueName);
            return channel;
        } catch (IOException e) {
            log.error("! FAILED TO CREATE RABBIT CHANNEL({})", queueName, e);
            return null;
        }
    }

    public synchronized int getChannelCount(String queueName) {
        Deque<Channel> channels = this.channels.get(queueName);
        return channels == null ? 0 : channels.size();
    }

    public synchronized void putBack(String queueName, Channel channel) {
        channels.get(queueName).push(channel);
    }

    public void shutdown() {
        try {
            if (config.rabbitmqDestroyQueuesAtShutdown()) {
                Queue.deleteAll();
            }
            connection.close();
            isUp = false;
            log.info("closed rabbit connection");
        } catch (AlreadyClosedException | IOException e) {
            log.error("! FAILED TO CLOSE RABBIT CONNECTION", e);
        }
    }

    public String[] getQueues() {
        return StringUtil.split(config.getRabbitMqQueues(), VantarParam.SEPARATOR_BLOCK);
    }

    private Connection getConnection() {
        if (connection == null || !connection.isOpen()) {
            connection = connect();
        }
        return connection;
    }

    private synchronized Connection connect() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.getRabbitMqHost());
        factory.setPort(config.getRabbitPort());
        factory.setUsername(config.getRabbitMqUser());
        factory.setPassword(config.getRabbitMqPassword());
        factory.setVirtualHost("/");
        factory.setRequestedHeartbeat(20);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);

        factory.setWorkPoolTimeout(10000);
        factory.setConnectionTimeout(10000);
        factory.setHandshakeTimeout(10000);
        factory.setRequestedChannelMax(0);

        factory.setExceptionHandler(new QueueExceptionHandlerBase(exceptionHandler));
        ExecutorService executor = Executors.newFixedThreadPool(20);

        try {
            return factory.newConnection(executor);
        } catch (TimeoutException | IOException e) {
            log.error("! ", e);
            return null;
        }
    }
}
