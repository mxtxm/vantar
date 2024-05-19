package com.vantar.queue;

import com.rabbitmq.client.*;
import com.vantar.service.Services;
import org.slf4j.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;


public class Queue {

    public enum Engine implements Services.DataSources  {
        QUEUE,
    }


    private static final Logger log = LoggerFactory.getLogger(Queue.class);
    private static final int MAX_TRIES = 3;
    private static final Map<String, Channel> channels = new HashMap<>();

    public static final int DEFAULT_TAKER_ID = 0;
    public static QueueConnection connection;


    public static boolean isEnabled() {
        return connection != null;
    }

    public static boolean isUp() {
        return connection != null && connection.isUp();
    }

    public static void connect(QueueConfig config) {
        connection = new QueueConnection(config, new DefaultQueueExceptionHandler());
    }

    public static void connect(QueueConfig config, QueueExceptionHandler exceptionHandler) {
        connection = new QueueConnection(config, exceptionHandler);
    }

    public static void shutdown() {
        for (String tag : channels.keySet()) {
            cancelTake(tag);
        }
        connection.shutdown();
    }

    public static boolean delete(String queueName) {
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = connection.getChannel(queueName);
            if (channel == null) {
                continue;
            }
            try {
                channel.queueDelete(queueName);
                log.info(" >> rabbitmq deleted '{}'", queueName);
                return true;
            } catch (AlreadyClosedException | IOException e) {
                log.error(" !! rabbitmq delete failed {} times. queue({})\n", MAX_TRIES-i, queueName, e);
            } finally {
                connection.putBack(queueName, channel);
            }
        }
        return false;
    }

    public static Map<String, Boolean> deleteAll() {
        Map<String, Boolean> result = new HashMap<>();
        for (String queueName : connection.getQueues()) {
            result.put(queueName, delete(queueName));
        }
        return result;
    }

    public static boolean empty(String queueName) {
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = connection.getChannel(queueName);
            if (channel == null) {
                continue;
            }
            try {
                channel.queuePurge(queueName);
                log.info(" >> rabbitmq emptied '{}'", queueName);
                return true;
            } catch (AlreadyClosedException | IOException e) {
                log.error(" !! rabbitmq empty failed {} times. queue({})\n", MAX_TRIES-i, queueName, e);
            } finally {
                connection.putBack(queueName, channel);
            }
        }
        return false;
    }

    public static long count(String queueName) {
        Channel channel = connection.getChannel(queueName);
        if (channel == null) {
            return -7;
        }
        try {
            return channel.queueDeclarePassive(queueName).getMessageCount();
        } catch (AlreadyClosedException | IOException x) {
            return -14;
        } finally {
            connection.putBack(queueName, channel);
        }
    }

    public static void add(String queueName, Packet item) {
        add(queueName, 1, item);
    }
    public static void add(String queueName, int workerId, Packet packet) {
        if (!packet.isValid()) {
            log.error(" !! invalid packet '{}'", queueName);
            return;
        }
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = connection.getChannel(queueName);
            if (channel == null) {
                continue;
            }
            try {
                channel.basicPublish("", queueName, null, packet.getString().getBytes());
                return;
            } catch (AlreadyClosedException | IOException e) {
                log.error(" !! rabbitmq add failed {} times. queue({}, {})\n", MAX_TRIES-i, queueName, workerId, e);
            } finally {
                connection.putBack(queueName, channel);
            }
        }
    }

    /**
     * On demand take all items out of the q
     */
    public static List<Packet> takeAllItems(String queueName) {
        List<Packet> messages = new ArrayList<>();
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = connection.getChannel(queueName);
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
                        log.error(" !! rabbitmq invalid packet queue({})", queueName);
                    }
                }
            } catch (AlreadyClosedException | IOException e) {
                log.error(" !! rabbitmq takeAll failed. queue({}, {})\n", MAX_TRIES-i, queueName, e);
            } finally {
                connection.putBack(queueName, channel);
            }
        }
        return messages;
    }

    /**
     * get one items out of the q
     */
    public static Packet take(String queueName) {
        Channel channel = connection.getChannel(queueName);
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
            log.error(" !! invalid packet queue({})", queueName);
        } catch (AlreadyClosedException | IOException e) {
            log.error(" !! rabbitmq get failed. queue({})\n", queueName, e);
        } finally {
            connection.putBack(queueName, channel);
        }
        return null;
    }

    /**
     * on demand Listener/threaded/blocking take
     */
    public static void takeUntilEmpty(String queueName, TakeCallback take, ReachedEmptyCallback reachedEmptyCallback) {
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = connection.getChannel(queueName);
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
                        log.error(" !! invalid packet queue({})", queueName);
                    }
                }
                reachedEmptyCallback.reached();
                return;
            } catch (AlreadyClosedException | IOException e) {
                log.error(" !! rabbitmq takeUntilEmpty failed. queue({}, {})\n", MAX_TRIES-i, queueName, e);
            } finally {
                connection.putBack(queueName, channel);
            }
        }
    }

    /**
     * Listener/threaded/blocking take
     * takerId is a param that can be sent when taking an item from a queue
     * the job which takes items from the queue may have takers thread, so the is is just a convenient to identify the
     * thread for example when loging something the id will help to know which thread caused the log
     */
    public static String take(String queueName, TakeCallback take) {
        return take(queueName, take, DEFAULT_TAKER_ID);
    }
    public static String take(String queueName, TakeCallback take, int takerId) {
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = connection.getChannel(queueName);
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
                                    log.error(" !! rabbitmq invalid packet queue({})", queueName);
                                    return;
                                }
                                if (take.getItem(packet, takerId)) {
                                    channel.basicAck(envelope.getDeliveryTag(), false);
                                } else {
                                    channel.basicReject(envelope.getDeliveryTag(), true);
                                }
                            } catch (AlreadyClosedException | IOException e) {
                                log.error(" !! rabbitmq take ack failed. queue({}, {})\n", queueName, takerId, e);
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
                log.error(" !! rabbitmq take failed. queue({}, {}, {})\n", MAX_TRIES-i, queueName, takerId, e);
            }
        }
        return null;
    }

    /**
     * broadcast message to all
     */
    public static void emmit(String exchangeName, Packet packet) {
        emmit(exchangeName, 1, packet);
    }
    public static void emmit(String exchangeName, int workerId, Packet packet) {
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = connection.getChannel(exchangeName);
            if (channel == null) {
                continue;
            }
            try {
                channel.exchangeDeclare(exchangeName, "fanout");
                channel.basicPublish(exchangeName, "", null, packet.getString().getBytes(StandardCharsets.UTF_8));
                return;
            } catch (AlreadyClosedException | IOException e) {
                log.error(" !! rabbitmq emmit failed. exchange({}, {}, {})\n", MAX_TRIES-i, exchangeName, workerId, e);
            } finally {
                connection.putBack(exchangeName, channel);
            }
        }
    }

    /**
     * get emitted (broad casted to all) message
     */
    public static String receive(String exchangeName, TakeCallback take) {
        return receive(exchangeName, take, DEFAULT_TAKER_ID);
    }
    public static String receive(String exchangeName, TakeCallback take, int takerId) {
        for (int i = MAX_TRIES; i > 0; --i) {
            Channel channel = connection.getChannel(exchangeName);
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
                            log.error(" !! rabbitmq invalid packet queue({})", queueName);
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
                log.error(" !! rabbitmq receive failed. exchange({}, {}, {})\n", MAX_TRIES-i, exchangeName, takerId, e);
            }
        }
        return null;
    }

    public static void cancelTake(String workerTag) {
        Channel channel = channels.get(workerTag);
        if (channel != null) {
            try {
                channel.basicCancel(workerTag);
            } catch (AlreadyClosedException ignore) {

            } catch (IOException e) {
                log.error(" !! rabbitmq channel basicCancel abort failed\n", e);
            }
            try {
                channel.abort();
            } catch (AlreadyClosedException | IOException ignore) {

            }
            try {
                channel.close();
            } catch (AlreadyClosedException ignore) {

            } catch (IOException | TimeoutException e) {
                log.error(" !! rabbitmq channel close failed\n", e);
            }
            channels.remove(workerTag);
        }
    }
}
