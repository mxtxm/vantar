package com.vantar.queue;


public interface TakeCallback {

    boolean getItem(Packet item, int takerId);

    void fail(String queueName, int workerId);
}
