package com.vantar.queue.rabbit;

import com.rabbitmq.client.*;
import com.vantar.common.VantarParam;
import com.vantar.queue.common.Packet;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.string.StringUtil;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;


public class Rabbit implements Closeable {

    public static ExceptionHandler exceptionHandler;
    private static final int MAX_TRIES = 3;
    private static final int DEFAULT_TAKER_ID = 0;

    private Connection connection;
    private Map<String, Deque<Channel>> channelDeQue = new HashMap<>(20, 1);
    private final RabbitConfig config;

    private boolean isUp = false;
    private boolean isShutdown = false;
    private final Map<String, Channel> channels = new HashMap<>(20, 1);

    // > > > service

    public boolean isUp() {
        return isUp;
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public void shutdown() {
        close();
    }

    // service < < <

    // > > > connection

    public Rabbit(RabbitConfig config) {
        if (exceptionHandler == null) {
            exceptionHandler = new DefaultExceptionHandler();
        }
        this.config = config;
    }

    private Connection getConnection() {
        if (isShutdown) {
            return null;
        }

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
            factory.setWorkPoolTimeout(config.getRabbitMqTimeout());
            factory.setConnectionTimeout(config.getRabbitMqTimeout());
            factory.setHandshakeTimeout(config.getRabbitMqTimeout());
            factory.setRequestedChannelMax(0);
            factory.setExceptionHandler(new ExceptionHandlerBase(exceptionHandler));
            try {
                connection = factory.newConnection();
                isUp = true;
                ServiceLog.log.info(" >> rabbitmq to RabbitMQ");
            } catch (Exception e) {
                ServiceLog.log.error(" !! connect to rabbitmq", e);
                isUp = false;
                return null;
            }
        }
        return connection;
    }

    public synchronized Channel getChannel(String queueName) {
        if (isShutdown) {
            return null;
        }

        Deque<Channel> channels = this.channelDeQue.get(queueName);
        if (channels == null) {
            this.channelDeQue.put(queueName, new ArrayDeque<>());
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
            ServiceLog.log.error("! rabbitmq channel to '{}' failed (connection=null)", queueName);
            return null;
        }
        try {
            Channel channel = connection.createChannel();
            if (channel == null) {
                ServiceLog.log.error("! rabbitmq channel to '{}' failed", queueName);
                return null;
            }
            channel.queueDeclare(queueName, false, false, false, null);
            ServiceLog.log.trace(" > rabbitmq created channel to '{}'", queueName);
            return channel;
        } catch (IOException e) {
            ServiceLog.log.error(" !! rabbitmq channel to '{}' failed\n", queueName, e);
            return null;
        }
    }

    public synchronized void removeChannels() {
        for (String queueName : channelDeQue.keySet()) {
            removeChannelsX(queueName);
        }
        channelDeQue = new HashMap<>(20, 1);
    }

    public synchronized void removeChannels(String queueName) {
        removeChannelsX(queueName);
        channelDeQue.remove(queueName);
    }

    private void removeChannelsX(String queueName) {
        Deque<Channel> qChannels = channelDeQue.get(queueName);
        for (Channel channel : qChannels) {
            try {
                channel.abort();
            } catch (IOException ignore) {

            }
            try {
                channel.close();
            } catch (IOException | AlreadyClosedException | TimeoutException ignore) {

            }
        }
    }

    public synchronized void putBack(String queueName, Channel channel) {
        if (channel != null && channel.isOpen()) {
            channelDeQue.get(queueName).push(channel);
        }
    }

    public String[] getQueues() {
        return StringUtil.splitTrim(config.getRabbitMqQueues(), VantarParam.SEPARATOR_BLOCK);
    }

    @Override
    public void close() {
        for (String tag : channelDeQue.keySet()) {
            cancelTake(tag);
        }
        isShutdown = true;
        try {
            if (config.rabbitmqDestroyQueuesAtShutdown()) {
                deleteAll();
            }
            removeChannels();
            connection.close();
            isUp = false;
            ServiceLog.log.info(" << disconnected from rabbitmq");
        } catch (Exception e) {
            ServiceLog.log.error(" !! failed to disconnected from rabbitmq", e);
        }
        isUp = false;
    }

    // connection < < <

    // delete > > >

    public boolean delete(String queueName) {
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = getChannel(queueName);
            if (channel == null) {
                continue;
            }
            try {
                channel.queueDelete(queueName);
                ServiceLog.log.info(" >> rabbitmq deleted q({})", queueName);
                return true;
            } catch (AlreadyClosedException | IOException e) {
                ServiceLog.log.error(" !! rabbitmq q delete failed {} times. q({})", MAX_TRIES-i, queueName, e);
            } finally {
                putBack(queueName, channel);
            }
        }
        return false;
    }

    public Map<String, Boolean> deleteAll() {
        Map<String, Boolean> result = new HashMap<>(10, 1);
        for (String queueName : getQueues()) {
            result.put(queueName, delete(queueName));
        }
        return result;
    }

    // delete < < <

    //  > > > general

    public boolean empty(String queueName) {
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = getChannel(queueName);
            if (channel == null) {
                continue;
            }
            try {
                channel.queuePurge(queueName);
                ServiceLog.log.info(" >> rabbitmq emptied q({})", queueName);
                return true;
            } catch (AlreadyClosedException | IOException e) {
                ServiceLog.log.error(" !! rabbitmq empty failed {} times. q({})", MAX_TRIES-i, queueName, e);
            } finally {
                putBack(queueName, channel);
            }
        }
        return false;
    }

    public long count(String queueName) {
        Channel channel = getChannel(queueName);
        if (channel == null) {
            return -7;
        }
        try {
            return channel.queueDeclarePassive(queueName).getMessageCount();
        } catch (AlreadyClosedException | IOException x) {
            return -14;
        } finally {
            putBack(queueName, channel);
        }
    }

    public void cancelTake(String workerTag) {
        Channel channel = channels.get(workerTag);
        if (channel != null) {
            try {
                channel.basicCancel(workerTag);
            } catch (AlreadyClosedException ignore) {

            } catch (IOException e) {
                ServiceLog.log.error(" !! rabbitmq channel basicCancel abort failed", e);
            }
            try {
                channel.abort();
            } catch (AlreadyClosedException | IOException ignore) {

            }
            try {
                channel.close();
            } catch (AlreadyClosedException ignore) {

            } catch (IOException | TimeoutException e) {
                ServiceLog.log.error(" !! rabbitmq channel close failed", e);
            }
            channels.remove(workerTag);
        }
    }

    // general < < <

    //  > > > add

    public void add(String queueName, Packet item) {
        add(queueName, 1, item);
    }
    public void add(String queueName, int workerId, Packet packet) {
        if (!packet.isValid()) {
            ServiceLog.log.error(" !! invalid packet q({})", queueName);
            return;
        }
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = getChannel(queueName);
            if (channel == null) {
                continue;
            }
            try {
                channel.basicPublish("", queueName, null, packet.getString().getBytes());
                return;
            } catch (AlreadyClosedException | IOException e) {
                ServiceLog.log.error(" !! rabbitmq add failed {} times. q({}, {})\n", MAX_TRIES-i, queueName, workerId, e);
            } finally {
                putBack(queueName, channel);
            }
        }
    }

    // add < < <

    //  > > > take

    /**
     * On demand take all items out of the q
     */
    public List<Packet> takeAllItems(String queueName) {
        List<Packet> messages = new ArrayList<>();
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = getChannel(queueName);
            if (channel == null) {
                continue;
            }
            try {
                while (true) {
                    GetResponse response = channel.basicGet(queueName, true);
                    if (response == null) {
                        break;
                    }
                    Packet packet = new Packet(response.getBody());
                    if (packet.isValid()) {
                        messages.add(packet);
                    } else {
                        ServiceLog.log.error(" !! rabbitmq invalid packet q({})", queueName);
                    }
                }
            } catch (AlreadyClosedException | IOException e) {
                ServiceLog.log.error(" !! rabbitmq takeAll failed. q({}, {})\n", MAX_TRIES-i, queueName, e);
            } finally {
                putBack(queueName, channel);
            }
        }
        return messages;
    }

    /**
     * get one items out of the q
     */
    public Packet take(String queueName) {
        Channel channel = getChannel(queueName);
        if (channel == null) {
            return null;
        }
        try {
            GetResponse response = channel.basicGet(queueName, true);
            if (response == null) {
                return null;
            }
            Packet packet = new Packet(response.getBody());
            if (packet.isValid()) {
                return packet;
            }
            ServiceLog.log.error(" !! invalid packet q({})", queueName);
        } catch (AlreadyClosedException | IOException e) {
            ServiceLog.log.error(" !! rabbitmq get failed. q({})\n", queueName, e);
        } finally {
            putBack(queueName, channel);
        }
        return null;
    }

    /**
     * on demand Listener/threaded/blocking take
     */
    public void takeUntilEmpty(String queueName, TakeCallback take, ReachedEmptyCallback reachedEmptyCallback) {
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = getChannel(queueName);
            if (channel == null) {
                continue;
            }
            try {
                while (true) {
                    GetResponse response = channel.basicGet(queueName, true);
                    if (response == null) {
                        break;
                    }
                    Packet packet = new Packet(response.getBody());
                    if (packet.isValid()) {
                        take.getItem(packet, 0);
                    } else {
                        ServiceLog.log.error(" !! invalid packet q({})", queueName);
                    }
                }
                reachedEmptyCallback.reached();
                return;
            } catch (AlreadyClosedException | IOException e) {
                ServiceLog.log.error(" !! rabbitmq takeUntilEmpty failed. q({}, {})\n", MAX_TRIES-i, queueName, e);
            } finally {
                putBack(queueName, channel);
            }
        }
    }

    /**
     * Listener/threaded/blocking take
     * takerId is a param that can be sent when taking an item from a queue
     * the job which takes items from the queue may have takers thread, so the is is just a convenient to identify the
     * thread for example when loging something the id will help to know which thread caused the log
     */
    public String take(String queueName, TakeCallback take) {
        return take(queueName, take, DEFAULT_TAKER_ID);
    }
    public String take(String queueName, TakeCallback take, int takerId) {
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = getChannel(queueName);
            if (channel == null) {
                continue;
            }
            try {
                channel.basicQos(1);
                String tag = channel.basicConsume(
                    queueName,
                    false,
                    new DefaultConsumer(channel) {

                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                            try {
                                Packet packet = new Packet(body);
                                if (!packet.isValid()) {
                                    ServiceLog.log.error(" !! rabbitmq invalid packet q({})", queueName);
                                    return;
                                }
                                if (take.getItem(packet, takerId)) {
                                    channel.basicAck(envelope.getDeliveryTag(), false);
                                } else {
                                    channel.basicReject(envelope.getDeliveryTag(), true);
                                }
                            } catch (AlreadyClosedException | IOException e) {
                                ServiceLog.log.error(" !! rabbitmq take ack failed. q({}, {})\n", queueName, takerId, e);
                            }
                        }

                        @Override
                        public void handleCancel(String consumerTag) {
                            take.cancel(queueName, takerId);
                        }

                        @Override
                        public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                            take.shutDown(queueName, takerId);
                        }
                    }
                );

                channels.put(tag, channel);
                return tag;
            } catch (AlreadyClosedException | IOException e) {
                ServiceLog.log.error(" !! rabbitmq take failed. q({}, {}, {})\n", MAX_TRIES-i, queueName, takerId, e);
            }
        }
        return null;
    }

    // take < < <

    //  > > > broadcast

    /**
     * broadcast message to all
     */
    public void emmit(String exchangeName, Packet packet) {
        emmit(exchangeName, 1, packet);
    }
    public void emmit(String exchangeName, int workerId, Packet packet) {
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = getChannel(exchangeName);
            if (channel == null) {
                continue;
            }
            try {
                channel.exchangeDeclare(exchangeName, "fanout");
                channel.basicPublish(exchangeName, "", null, packet.getString().getBytes(StandardCharsets.UTF_8));
                return;
            } catch (AlreadyClosedException | IOException e) {
                ServiceLog.log.error(" !! rabbitmq emmit failed. exchange({}, {}, {})\n", MAX_TRIES-i, exchangeName, workerId, e);
            } finally {
                putBack(exchangeName, channel);
            }
        }
    }

    /**
     * get emitted (broad casted to all) message
     */
    public String receive(String exchangeName, TakeCallback take) {
        return receive(exchangeName, take, DEFAULT_TAKER_ID);
    }
    public String receive(String exchangeName, TakeCallback take, int takerId) {
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = getChannel(exchangeName);
            if (channel == null) {
                continue;
            }
            try {
                channel.exchangeDeclare(exchangeName, "fanout");
                String queueName = channel.queueDeclare().getQueue();
                channel.queueBind(queueName, exchangeName, "");
                channel.basicQos(1);
                String tag = channel.basicConsume(queueName, true, new DefaultConsumer(channel) {

                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                        Packet packet = new Packet(body);
                        if (!packet.isValid()) {
                            ServiceLog.log.error(" !! rabbitmq invalid packet queue({})", queueName);
                            return;
                        }
                        take.getItem(packet, takerId);
                    }

                    @Override
                    public void handleCancel(String consumerTag) {
                        take.cancel(queueName, takerId);
                    }

                    @Override
                    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                        take.shutDown(queueName, takerId);
                    }
                });

                channels.put(tag, channel);
                return tag;
            } catch (AlreadyClosedException | IOException e) {
                ServiceLog.log.error(" !! rabbitmq receive failed. exchange({}, {}, {})\n", MAX_TRIES-i, exchangeName, takerId, e);
            }
        }
        return null;
    }

    // broadcast < < <
}
