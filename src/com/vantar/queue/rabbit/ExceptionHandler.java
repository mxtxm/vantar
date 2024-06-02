package com.vantar.queue.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.TopologyRecoveryException;


public interface ExceptionHandler {

    void handleReturnListenerException(Channel channel, Throwable e);

    void handleChannelRecoveryException(Channel channel, Throwable e);

    void handleConfirmListenerException(Channel channel, Throwable e);

    void handleConnectionRecoveryException(Connection conn, Throwable e);

    void handleConsumerException(Channel channel, Throwable e, Consumer consumer, String consumerTag, String methodName);

    void handleTopologyRecoveryException(Connection conn, Channel channel, TopologyRecoveryException e);

    void handleUnexpectedConnectionDriverException(Connection conn, Throwable e);
}
