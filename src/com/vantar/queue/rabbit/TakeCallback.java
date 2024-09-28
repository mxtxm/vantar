package com.vantar.queue.rabbit;


import com.vantar.queue.common.Packet;


public interface TakeCallback {

    boolean getItem(Packet item, int takerId);

    void cancel(String queueName, int workerId);

    void shutDown(String queueName, int workerId);
}
