package com.vantar.queue;

import com.rabbitmq.client.*;
import com.vantar.service.Services;
import com.vantar.service.log.LogEvent;


public class DefaultQueueExceptionHandler implements QueueExceptionHandler {

    public void handleReturnListenerException(Channel channel, Throwable e) {
        LogEvent.fatal(QueueExceptionHandlerBase.class, "handleReturnListenerException", channel, e);
        Services.stop();
        Services.start();
    }

    public void handleChannelRecoveryException(Channel channel, Throwable e) {
        LogEvent.fatal(QueueExceptionHandlerBase.class, "handleChannelRecoveryException", channel, e);
        Services.stop();
        Services.start();
    }

    public void handleConfirmListenerException(Channel channel, Throwable e) {
        LogEvent.fatal(QueueExceptionHandlerBase.class, "handleConfirmListenerException", channel, e);
        Services.stop();
        Services.start();
    }

    public void handleConnectionRecoveryException(Connection conn, Throwable e) {
        LogEvent.fatal(QueueExceptionHandlerBase.class, "handleConnectionRecoveryException", conn, e);
        Services.stop();
        Services.start();
    }

    public void handleConsumerException(Channel channel, Throwable e, Consumer consumer, String consumerTag, String methodName) {
        LogEvent.fatal(QueueExceptionHandlerBase.class, "handleConsumerException", consumerTag, methodName, channel, e);
        Services.stop();
        Services.start();
    }

    public void handleTopologyRecoveryException(Connection conn, Channel channel, TopologyRecoveryException e) {
        LogEvent.fatal(QueueExceptionHandlerBase.class, "handleTopologyRecoveryException", conn, channel, e);
        Services.stop();
        Services.start();
    }

    public void handleUnexpectedConnectionDriverException(Connection conn, Throwable e) {
        LogEvent.fatal(QueueExceptionHandlerBase.class, "handleUnexpectedConnectionDriverException", conn, e);
        Services.stop();
        Services.start();
    }

    public static void queueFail(Class<?> clazz, String queueName, int workerId) {
        LogEvent.fatal(clazz, "queue(" + queueName + ", "  + workerId + ") failed - restarting all services...");
        Services.stop();
        Services.start();
    }
}
