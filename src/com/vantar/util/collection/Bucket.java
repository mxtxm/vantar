package com.vantar.util.collection;

import com.vantar.util.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Bucket<T> {

    private static final Logger log = LoggerFactory.getLogger(Bucket.class);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private final BlockingQueue<T> data;
    private final int maxSize;
    private volatile String json;


    public Bucket(int maxSize) {
        this.maxSize = maxSize;
        data = new LinkedBlockingQueue<>(maxSize);
    }

    public void add(T value) {
        writeLock.lock();
        try {
            if (data.size() == maxSize) {
                data.take();
            }
            data.add(value);
            json = null;
        } catch (InterruptedException e) {
            log.error("! bucket({})", value, e);
        } finally{
            writeLock.unlock();
        }
    }

    public String getAsJson() {
        readLock.lock();
        try {
            if (json == null) {
                json = Json.toJson(data);
            }
            return json;
        } finally{
            readLock.unlock();
        }
    }

    public BlockingQueue<T> get() {
        readLock.lock();
        try {
            return data;
        } finally{
            readLock.unlock();
        }
    }
}
