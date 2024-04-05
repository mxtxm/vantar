package com.vantar.service.messaging;

import com.vantar.admin.setting.AdminSettings;
import com.vantar.admin.service.AdminService;
import com.vantar.common.*;
import com.vantar.queue.*;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.service.log.*;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;


public class ServiceMessaging {

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

    public void setEvent(Event event) {
        this.event = event;
    }

    public void broadcast(int type) {
        broadcast(type, "");
    }

    public void broadcast(int type, Object... message) {
        if (!Services.isUp(Queue.class)) {
            return;
        }
        Queue.emmit(VantarParam.QUEUE_NAME_MESSAGE_BROADCAST, new Packet(new Message(message), type));
        Beat.set(ServiceMessaging.class, "broadcast");
        ServiceLog.log.trace(" > broadcast({})", type);
    }

    private void receive() {
        if (!Services.isUp(Queue.class)) {
            return;
        }

        TakeCallback takeCallback = new TakeCallback() {

            @Override
            public boolean getItem(Packet packet, int takerId) {
                if (!serviceOn) {
                    return false;
                }

                int type = packet.getType();
                Message message = packet.getObject();

                try {
                    // > > > custom app messages
                    if (!Services.ID.equals(message.serverId)) {
                        switch (type) {

                            case VantarParam.MESSAGE_DATABASE_UPDATED:
                                Services.getService(ServiceDtoCache.class).update(message.getString());
                                break;

                            case VantarParam.MESSAGE_SERVICES_ACTION:
                                String[] p = message.getArray();
                                if (p.length == 5) {
                                    AdminService.serviceAction(
                                        null, //ui
                                        p[0], //action
                                        p[1], //service
                                        StringUtil.toInteger(p[2]), //delay
                                        StringUtil.toInteger(p[3]) //tries
                                    );
                                } else {
                                    ServiceLog.error(getClass(), "invalid param length. type={} msg={}", type, message.getString());
                                }
                                break;

                            case VantarParam.MESSAGE_SERVICE_STARTED:
                                Services.onServiceStarted(message.serverId, message.getString());
                                break;

                            case VantarParam.MESSAGE_SERVICE_STOPPED:
                                Services.onServiceStopped(message.serverId, message.getString());
                                break;

                            case VantarParam.MESSAGE_SETTINGS_UPDATED:
                                AdminSettings.reloadSettings();
                                break;

                            case VantarParam.MESSAGE_UPDATE_SETTINGS:
                                AdminSettings.updateProperties(message.getString(), Settings.config, Settings.configClass);
                                break;
                        }
                    }

                    // > > > custom app messages
                    if (event != null) {
                        event.onReceive(type, message);
                    }
                } catch (Exception e) {
                    ServiceLog.error(getClass(), "failed. type={} msg={}", type, message.getString(), e);
                }

                Beat.set(ServiceMessaging.class, "receive");
                return true;
            }

            @Override
            public void cancel(String queueName, int workerId) {
                ServiceLog.log.info(" > Queue({}, {}) shutdown", queueName, workerId);
            }

            @Override
            public void shutDown(String queueName, int workerId) {
                ServiceLog.log.info(" > Queue({}, {}) shutdown", queueName, workerId);
            }
        };

        workerTag = Queue.receive(VantarParam.QUEUE_NAME_MESSAGE_BROADCAST, takeCallback);
    }


    public static class Message {

        public String serverId;
        public String message;

        public Message() {

        }

        public Message(Object... message) {
            serverId = Services.ID;
            this.message = CollectionUtil.join(message, VantarParam.SEPARATOR_COMMON_COMPLEX);
        }

        public String getString() {
            return message;
        }

        public String[] getArray() {
            return message == null ? null : StringUtil.split(message, VantarParam.SEPARATOR_COMMON_COMPLEX);
        }

        public Integer getInteger() {
            return StringUtil.toInteger(message);
        }

        public Long getLong() {
            return StringUtil.toLong(message);
        }

        public Double getDouble() {
            return StringUtil.toDouble(message);
        }

        public Boolean getBoolean() {
            return StringUtil.toBoolean(message);
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