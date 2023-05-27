package com.vantar.queue;


public interface TakeCallback {

    boolean getItem(Packet item, int takerId);

    void cancel(String queueName, int workerId);

    void shutDown(String queueName, int workerId);
}
