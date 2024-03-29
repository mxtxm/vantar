package com.vantar.service.messaging;

import com.vantar.admin.model.*;
import com.vantar.common.*;
import com.vantar.exception.ServiceException;
import com.vantar.queue.*;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.service.log.*;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;


public class ServiceMessaging {

    private static final Logger log = LoggerFactory.getLogger(ServiceMessaging.class);

    private volatile boolean serviceOn = false;
    private String workerTag;
    private Event event;


    public void start() {
        if (!Services.isUp(Queue.class)) {
            return;
        }
        serviceOn = true;
        Beat.set(this.getClass(), "start");
        receive();
    }

    public void stop() {
        if (!Services.isUp(Queue.class)) {
            return;
        }
        serviceOn = false;
        Beat.set(this.getClass(), "stop");
        Queue.cancelTake(workerTag);
    }

    public void broadcast(int type) {
        broadcast(type, "");
    }

    public void broadcast(int type, String message) {
        if (!Services.isUp(Queue.class)) {
            return;
        }
        Beat.set(ServiceMessaging.class, "broadcast");
        Queue.emmit(VantarParam.QUEUE_NAME_MESSAGE_BROADCAST, new Packet(new Message(message), type));
        log.debug(" > broadcast({}, {})", type, message);
    }

    private void receive() {
        if (!Services.isUp(Queue.class)) {
            return;
        }

        TakeCallback takeCallback = new TakeCallback() {

            @Override
            public boolean getItem(Packet packet, int takerId) {
                Beat.set(ServiceMessaging.class, "receive");
                if (!serviceOn) {
                    return false;
                }

                int type = packet.getType();
                Message message = packet.getObject();
                switch (type) {

                    case VantarParam.MESSAGE_SERVICES_START:
                        if (!Services.ID.equals(message.serverId)) {
                            if (message.getBoolean()) {
                                Services.startServer();
                            } else {
                                Services.startServices();
                            }
                        }
                        break;

                    case VantarParam.MESSAGE_SERVICES_STOP:
                        if (!Services.ID.equals(message.serverId)) {
                            if (message.getBoolean()) {
                                Services.stop();
                            } else {
                                Services.stopServices();
                            }
                        }
                        break;

                    case VantarParam.MESSAGE_SERVICE_STARTED:
                        if (!Services.ID.equals(message.serverId)) {
                            try {
                                Services.onServiceStarted(message.serverId, message.getString());
                            } catch (Exception e) {
                                log.error(" !! MESSAGE_SERVICE_STARTED ({})\n", message, e);
                            }
                        }
                        break;

                    case VantarParam.MESSAGE_SERVICE_STOPPED:
                        if (!Services.ID.equals(message.serverId)) {
                            try {
                                Services.onServiceStopped(message.serverId, message.getString());
                            } catch (Exception e) {
                                log.error(" !! MESSAGE_SERVICE_STOPPED ({})\n", message, e);
                            }
                        }
                        break;

//                    case VantarParam.MESSAGE_SERVICE_ENABLED_COUNT:
//                        Services.setTotalServiceCount(message.getInteger(), message.serverId);
//                        break;

                    case VantarParam.MESSAGE_SETTINGS_UPDATED:
                        if (!Services.ID.equals(message.serverId)) {
                            AdminSettings.reloadSettings();
                        }
                        break;

                    case VantarParam.MESSAGE_UPDATE_SETTINGS:
                        if (!Services.ID.equals(message.serverId)) {
                            AdminSettings.updateProperties(message.getString(), Settings.config, Settings.configClass);
                        }
                        break;

                    case VantarParam.MESSAGE_DATABASE_UPDATED:
                        if (!Services.ID.equals(message.serverId)) {
                            try {
                                Services.getService(ServiceDtoCache.class).update(message.getString());
                            } catch (ServiceException e) {
                                log.error(" !! failed to update cache\n", e);
                            }
                        }
                        break;
                }

                if (event != null) {
                    event.onReceive(type, message);
                }

                return true;
            }

            @Override
            public void cancel(String queueName, int workerId) {
                log.info(" Queue({}, {}) shutdown", queueName, workerId);
            }

            @Override
            public void shutDown(String queueName, int workerId) {
                log.info(" Queue({}, {}) shutdown", queueName, workerId);
            }
        };

        workerTag = Queue.receive(VantarParam.QUEUE_NAME_MESSAGE_BROADCAST, takeCallback);
    }

    public void setEvent(Event event) {
        this.event = event;
    }


    public static class Message {

        public String serverId;
        public Object message;

        public Message() {

        }

        public Message(String message) {
            serverId = Services.ID;
            this.message = message;
        }

        public String getString() {
            if (message == null) {
                return null;
            }
            return message.toString();
        }

        public Integer getInteger() {
            if (message == null) {
                return null;
            }
            return message instanceof Number ? ((Number) message).intValue() : StringUtil.toInteger(message.toString());
        }

        public Boolean getBoolean() {
            if (message == null) {
                return null;
            }
            return message instanceof Boolean ? (Boolean) message : StringUtil.toBoolean(message.toString());
        }

        public String toString() {
            return ObjectUtil.toStringViewable(this);
        }
    }


    public interface Event {

        void onReceive(int type, Message message);

        void onMessageQueueFail(String queue);
    }
}