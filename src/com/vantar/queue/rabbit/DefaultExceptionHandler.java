package com.vantar.queue.rabbit;

import com.rabbitmq.client.*;
import com.vantar.service.Services;
import com.vantar.service.log.ServiceLog;


public class DefaultExceptionHandler implements ExceptionHandler {

    public void handleReturnListenerException(Channel channel, Throwable e) {
        ServiceLog.fatal(ExceptionHandlerBase.class, "handleReturnListenerException", channel, e);
        Services.stopServices();
        Services.startServices();
    }

    public void handleChannelRecoveryException(Channel channel, Throwable e) {
        ServiceLog.log.error("! queue channel={}", channel, e);
        Services.stopServices();
        Services.startServices();
    }

    public void handleConfirmListenerException(Channel channel, Throwable e) {
        ServiceLog.log.error("! queue channel={}", channel, e);
        Services.stopServices();
        Services.startServices();
    }

    public void handleConnectionRecoveryException(Connection conn, Throwable e) {
        ServiceLog.log.error("! queue connection={}", conn, e);
        Services.stopServices();
        Services.startServices();
    }

    public void handleConsumerException(Channel channel, Throwable e, Consumer consumer, String consumerTag, String methodName) {
        ServiceLog.log.error("! queue consumerTag={} methodName={} channel={}", consumerTag, methodName, channel, e);
        Services.stopServices();
        Services.startServices();
    }

    public void handleTopologyRecoveryException(Connection conn, Channel channel, TopologyRecoveryException e) {
        ServiceLog.log.error("! queue connection={} channel={}", conn, conn, e);
        Services.stopServices();
        Services.startServices();
    }

    public void handleUnexpectedConnectionDriverException(Connection conn, Throwable e) {
        ServiceLog.log.error("! queue connection={}", conn, e);
        Services.stopServices();
        Services.startServices();
    }

    public static void queueFail(Class<?> clazz, String queueName, int workerId) {
        ServiceLog.log.error("! queue({}, {}, failed) ---> restarting all services...", queueName, workerId);
        Services.stopServices();
        Services.startServices();
    }
}
