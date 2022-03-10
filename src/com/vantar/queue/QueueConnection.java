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
import java.util.concurrent.TimeoutException;


public class QueueConnection {

    private static final Logger log = LoggerFactory.getLogger(QueueConnection.class);
    private Connection connection;
    private Map<String, Deque<Channel>> channels = new HashMap<>(20);
    private final QueueConfig config;
    private final QueueExceptionHandler exceptionHandler;

    public boolean isUp;


    public QueueConnection(QueueConfig config, QueueExceptionHandler exceptionHandler) {
        this.config = config;
        this.exceptionHandler = exceptionHandler;
    }

    private Connection getConnection() {
        if (connection == null || !connection.isOpen()) {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(config.getRabbitMqHost());
            factory.setPort(config.getRabbitPort());
            factory.setUsername(config.getRabbitMqUser());
            factory.setPassword(config.getRabbitMqPassword());
            factory.setVirtualHost("/");
            factory.setRequestedHeartbeat(config.getRabbitMqHeartbeat());
            factory.setAutomaticRecoveryEnabled(true);
            factory.setTopologyRecoveryEnabled(true);
            factory.setWorkPoolTimeout(10000);
            factory.setConnectionTimeout(10000);
            factory.setHandshakeTimeout(10000);
            factory.setRequestedChannelMax(0);
            factory.setExceptionHandler(new QueueExceptionHandlerBase(exceptionHandler));
            try {
                connection = factory.newConnection();
                isUp = true;
                log.info("> rabbitmq connected");
            } catch (TimeoutException | IOException e) {
                log.error("! rabbitmq connect failed", e);
                isUp = false;
                return null;
            }
        }
        return connection;
    }

    public synchronized Channel getChannel(String queueName) {
        Deque<Channel> channels = this.channels.get(queueName);
        if (channels == null) {
            this.channels.put(queueName, new ArrayDeque<>());
        } else if (!channels.isEmpty()) {
            Channel channel = channels.pop();
            if (channel == null) {
                return getChannel(queueName);
            } if (channel.isOpen()) {
                return channel;
            }
            try {
                channel.close();
            } catch (IOException | TimeoutException ignore) {

            }
            return getChannel(queueName);
        }

        Connection connection = getConnection();
        if (connection == null) {
            log.error("! rabbitmq channel to '{}' failed (connection=null)", queueName);
            return null;
        }
        try {
            Channel channel = connection.createChannel();
            if (channel == null) {
                log.error("! rabbitmq channel to '{}' failed", queueName);
                return null;
            }
            channel.queueDeclare(queueName, false, false, false, null);
            log.debug("> rabbitmq created channel to '{}'", queueName);
            return channel;
        } catch (IOException e) {
            log.error("! rabbitmq channel to '{}' failed\n", queueName, e);
            return null;
        }
    }

    public synchronized void removeChannels() {
        for (String queueName : channels.keySet()) {
            removeChannels(queueName);
        }
        channels = new HashMap<>(20);
    }

    public synchronized void removeChannels(String queueName) {
        Deque<Channel> qChannels = channels.remove(queueName);
        while (qChannels.peek() != null) {
            Channel channel = qChannels.pop();
            try {
                channel.abort();
            } catch (IOException ignore) {

            }
            try {
                channel.close();
            } catch (IOException | TimeoutException ignore) {

            }
        }
    }

    public synchronized void putBack(String queueName, Channel channel) {
        if (channel != null && channel.isOpen()) {
            channels.get(queueName).push(channel);
        }
    }

    public void shutdown() {
        try {
            if (config.rabbitmqDestroyQueuesAtShutdown()) {
                Queue.deleteAll();
            }
            removeChannels();
            connection.close();
            isUp = false;
            log.error("> rabbitmq is shutdown");
        } catch (AlreadyClosedException | IOException e) {
            log.error("! rabbitmq failed to shutdown", e);
        }
    }

    public String[] getQueues() {
        return StringUtil.split(config.getRabbitMqQueues(), VantarParam.SEPARATOR_BLOCK);
    }
}
