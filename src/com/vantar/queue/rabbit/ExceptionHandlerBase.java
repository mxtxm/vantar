package com.vantar.queue.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.TopologyRecoveryException;
import com.rabbitmq.client.impl.StrictExceptionHandler;


public class ExceptionHandlerBase extends StrictExceptionHandler {

    private ExceptionHandler customHandler;


    public ExceptionHandlerBase() {

    }

    public ExceptionHandlerBase(ExceptionHandler customHandler) {
        this.customHandler = customHandler;
    }

    @Override
    public void handleReturnListenerException(Channel channel, Throwable e) {
        if (customHandler != null) {
            customHandler.handleReturnListenerException(channel, e);
        }
    }

    @Override
    public void handleChannelRecoveryException(Channel channel, Throwable e) {
        if (customHandler != null) {
            customHandler.handleChannelRecoveryException(channel, e);
        }
    }

    @Override
    public void handleConfirmListenerException(Channel channel, Throwable e) {
        if (customHandler != null) {
            customHandler.handleConfirmListenerException(channel, e);
        }
    }

    @Override
    public void handleConnectionRecoveryException(Connection conn, Throwable e) {
        if (customHandler != null) {
            customHandler.handleConnectionRecoveryException(conn, e);
        }
    }

    @Override
    public void handleConsumerException(Channel channel, Throwable e, Consumer consumer, String consumerTag, String methodName) {
        if (customHandler != null) {
            customHandler.handleConsumerException(channel, e, consumer, consumerTag, methodName);
        }
    }

    @Override
    public void handleTopologyRecoveryException(Connection conn, Channel channel, TopologyRecoveryException e) {
        if (customHandler != null) {
            customHandler.handleTopologyRecoveryException(conn, channel, e);
        }
    }

    @Override
    public void handleUnexpectedConnectionDriverException(Connection conn, Throwable e) {
        if (customHandler != null) {
            customHandler.handleUnexpectedConnectionDriverException(conn, e);
        }
    }
}
