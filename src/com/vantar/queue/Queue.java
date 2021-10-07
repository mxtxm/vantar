package com.vantar.queue;

import com.rabbitmq.client.*;
import com.vantar.common.VantarParam;
import com.vantar.util.string.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Queue {

    private static final Logger log = LoggerFactory.getLogger(Queue.class);
    private static final int MAX_TRIES = 3;
    private static QueueConnection connection;
    private static final Map<String, Channel> channels = new HashMap<>();

    public static final int DEFAULT_TAKER_ID = 0;
    public static boolean isUp;


    public static void connect(QueueConfig config) {
        connection = new QueueConnection(config);
        isUp = connection.isUp;
    }

    public static void connect(QueueConfig config, QueueExceptionHandler exceptionHandler) {
        connection = new QueueConnection(config, exceptionHandler);
        isUp = connection.isUp;
    }

    public static void shutdown() {
        connection.shutdown();
        isUp = false;
    }

    public static void abortChannel(String workerTag) {
        Channel channel = channels.get(workerTag);
        if (channel != null) {
            try {
                channel.abort();
            } catch (IOException e) {
                log.error("! channel abort failed", e);
            } finally {
                channels.remove(workerTag);
            }
        }
    }

    public static void abortChannels(Map<String, String> workerTags) {
        workerTags.forEach((workerTag, queueName) -> abortChannel(workerTag));
    }

    public static boolean delete(String queueName) {
        int tryCount = 0;
        int maxTries = 0;
        while (true) {
            Channel channel = connection.get(queueName);
            if (channel == null) {
                log.error("! channel failed queue({})", queueName);
                return false;
            }

            try {
                channel.queueDelete(queueName);
                log.info("> deleted queue({})", queueName);
                return true;
            } catch (AlreadyClosedException | IOException e) {
                if (maxTries == 0) {
                    maxTries = connection.getChannelCount(queueName) + MAX_TRIES;
                } else if (++tryCount == maxTries) {
                    log.error("! rabbit delete failed {} times. queue({})", tryCount, queueName, e);
                    return false;
                }
            }
        }
    }

    public static Map<String, Boolean> deleteAll() {
        Map<String, Boolean> result = new HashMap<>();
        for (String s : connection.getQueues()) {
            String queueName = StringUtil.split(s, VantarParam.SEPARATOR_COMMON)[0];
            result.put(queueName, delete(queueName));
        }
        return result;
    }

    public static boolean empty(String queueName) {
        int tryCount = 0;
        int maxTries = 0;
        while (true) {
            Channel channel = connection.get(queueName);
            if (channel == null) {
                log.error("! channel failed queue({})", queueName);
                return false;
            }

            try {
                channel.queuePurge(queueName);
                log.info("> empty queue({})", queueName);
                return true;
            } catch (AlreadyClosedException | IOException e) {
                if (maxTries == 0) {
                    maxTries = connection.getChannelCount(queueName) + MAX_TRIES;
                } else if (++tryCount == maxTries) {
                    log.error("! rabbit empty failed {} times. queue({})", tryCount, queueName, e);
                    return false;
                }
            }
        }
    }

    public static long count(String queueName) {
        Channel channel = connection.get(queueName);
        if (channel == null) {
            log.error("! channel failed queue({})", queueName);
            return -1;
        }
        try {
            long count = channel.queueDeclarePassive(queueName).getMessageCount();
            connection.putBack(queueName, channel);
            return count;
        } catch (AlreadyClosedException | IOException x) {
            return -2;
        }
    }

    public static void add(String queueName, Packet item) {
        add(queueName, 1, item);
    }
    public static void add(String queueName, int workerId, Packet packet) {
        if (!packet.isValid()) {
            log.error("! invalid packet queue({})", queueName);
            return;
        }

        int tryCount = 0;
        int maxTries = 0;
        while (true) {
            Channel channel = connection.get(queueName);
            if (channel == null) {
                log.error("! channel failed queue({})", queueName);
                return;
            }

            try {
                channel.basicPublish("", queueName, null, packet.getString().getBytes());
                connection.putBack(queueName, channel);
                return;
            } catch (AlreadyClosedException | IOException e) {
                if (maxTries == 0) {
                    maxTries = connection.getChannelCount(queueName) + MAX_TRIES;
                } else if (++tryCount == maxTries) {
                    log.error("! rabbit add failed {} times. queue({}, {})", tryCount, queueName, workerId, e);
                    return;
                }
            }
        }
    }

    /**
     * On demand take all items out of the q
     */
    public static List<Packet> takeAllItems(String queueName) {
        List<Packet> messages = new ArrayList<>();
        int tryCount = 0;
        int maxTries = 0;
        while (true) {
            Channel channel = connection.get(queueName);
            if (channel == null) {
                log.error("! channel failed queue({})", queueName);
                return messages;
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
                        log.error("! invalid packet queue({})", queueName);
                    }
                }
                connection.putBack(queueName, channel);
                return messages;

            } catch (AlreadyClosedException | IOException e) {
                if (maxTries == 0) {
                    maxTries = connection.getChannelCount(queueName) + MAX_TRIES;
                } else if (++tryCount == maxTries) {
                    log.error("! rabbit takeAll failed. queue({}, {})", tryCount, queueName, e);
                    return messages;
                }
            }
        }
    }

    /**
     * get one items out of the q
     */
    public static Packet take(String queueName) {
        Channel channel = connection.get(queueName);
        if (channel == null) {
            log.error("! channel failed queue({})", queueName);
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
            } else {
                log.error("! invalid packet queue({})", queueName);
            }
            connection.putBack(queueName, channel);

        } catch (AlreadyClosedException | IOException e) {
            log.error("! rabbit get failed. queue({})", queueName, e);
        }
        return null;
    }

    /**
     * on demand Listener/threaded/blocking take
     */
    public static void takeUntilEmpty(String queueName, TakeCallback take, ReachedEmptyCallback reachedEmptyCallback) {
        int tryCount = 0;
        int maxTries = 0;
        while (true) {
            Channel channel = connection.get(queueName);
            if (channel == null) {
                log.error("! channel failed queue({})", queueName);
                return;
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
                        log.error("! invalid packet queue({})", queueName);
                    }
                }

                connection.putBack(queueName, channel);
                reachedEmptyCallback.reached();
                return;
            } catch (AlreadyClosedException | IOException e) {
                if (maxTries == 0) {
                    maxTries = connection.getChannelCount(queueName) + MAX_TRIES;
                } else if (++tryCount == maxTries) {
                    log.error("! rabbit takeUntilEmpty failed. queue({}, {})", tryCount, queueName, e);
                    return;
                }
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
        int tryCount = 0;
        int maxTries = 0;
        while (true) {
            Channel channel = connection.get(queueName);
            if (channel == null) {
                log.error("! channel failed queue({})", queueName);
                return null;
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
                                    log.error("! invalid packet queue({})", queueName);
                                    return;
                                }

                                if (take.getItem(packet, takerId)) {
                                    channel.basicAck(envelope.getDeliveryTag(), false);
                                } else {
                                    channel.basicReject(envelope.getDeliveryTag(), true);
                                }
                            } catch (AlreadyClosedException | IOException e) {
                                log.error("! rabbit take ack failed. queue({}, {})", queueName, takerId, e);
                            }
                        }

                        @Override
                        public void handleCancel(String consumerTag) {
                            take.fail(queueName, takerId);
                        }

                        @Override
                        public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                            take.fail(queueName, takerId);
                        }
                    }
                );

                channels.put(tag, channel);
                return tag;
            } catch (AlreadyClosedException | IOException e) {
                if (maxTries == 0) {
                    maxTries = connection.getChannelCount(queueName) + MAX_TRIES;
                } else if (++tryCount == maxTries) {
                    log.error("! rabbit take failed. queue({}, {}, {})", tryCount, queueName, takerId, e);
                    return null;
                }
            }
        }
    }

    public static void cancelTake(String workerTag, String queueName) {
        int tryCount = 0;
        int maxTries = 0;
        while (true) {
            Channel channel = channels.get(workerTag);
            if (channel == null) {
                log.error("! channel failed queue({})", queueName);
                return;
            }

            try {
                channel.basicCancel(workerTag);
                return;
            } catch (AlreadyClosedException | IOException e) {
                if (maxTries == 0) {
                    maxTries = connection.getChannelCount(queueName) + MAX_TRIES;
                } else if (++tryCount == maxTries) {
                    log.error("! rabbit cancel take failed. queue({}, {}, {})", tryCount, queueName, workerTag, e);
                    return;
                }
            }
        }
    }

    public static void cancelTake(Map<String, String> workerTags) {
        workerTags.forEach(Queue::cancelTake);
    }

    /**
     * broadcast message to all
     */
    public static void emmit(String exchangeName, Packet packet) {
        emmit(exchangeName, 1, packet);
    }
    public static void emmit(String exchangeName, int workerId, Packet packet) {
        int tryCount = 0;
        int maxTries = 0;
        while (true) {
            Channel channel = connection.get(exchangeName);
            if (channel == null) {
                log.error("! channel failed exchange({})", exchangeName);
                return;
            }

            try {
                channel.exchangeDeclare(exchangeName, "fanout");
                channel.basicPublish(exchangeName, "", null, packet.getString().getBytes(StandardCharsets.UTF_8));
                connection.putBack(exchangeName, channel);
                return;
            } catch (AlreadyClosedException | IOException e) {
                if (maxTries == 0) {
                    maxTries = connection.getChannelCount(exchangeName) + MAX_TRIES;
                } else if (++tryCount == maxTries) {
                    log.error("! rabbit emmit failed. exchange({}, {}, {})", maxTries, exchangeName, workerId, e);
                    return;
                }
            }
        }
    }

    /**
     * get emitted (broadcasted to all) message
     */
    public static String receive(String exchangeName, TakeCallback take) {
        return receive(exchangeName, take, DEFAULT_TAKER_ID);
    }
    public static String receive(String exchangeName, TakeCallback take, int takerId) {
        int tryCount = 0;
        int maxTries = 0;
        while (true) {
            Channel channel = connection.get(exchangeName);
            if (channel == null) {
                log.error("! channel failed exchange({})", exchangeName);
                return null;
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
                            log.error("! invalid packet queue({})", queueName);
                            return;
                        }
                        take.getItem(packet, takerId);
                    }

                    @Override
                    public void handleCancel(String consumerTag) {
                        take.fail(queueName, takerId);
                    }

                    @Override
                    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                        take.fail(queueName, takerId);
                    }

                });

                channels.put(tag, channel);
                return tag;
            } catch (AlreadyClosedException | IOException e) {
                if (maxTries == 0) {
                    maxTries = connection.getChannelCount(exchangeName) + MAX_TRIES;
                } else if (++tryCount == maxTries) {
                    log.error("! rabbit receive failed. exchange({}, {}, {})", tryCount, exchangeName, takerId, e);
                    return null;
                }
            }
        }
    }


    public interface Message {

    }
}
