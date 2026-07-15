package com.css.challenge.fulfillment;

import com.css.challenge.client.Order;
import com.css.challenge.client.StorageType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class Storage {
    private final StorageType type;
    private final int capacity;
    private final Map<String, Order> orders = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Storage(StorageType type, int capacity) {
        this.type = type;
        this.capacity = capacity;
    }

    public StorageType getType() {
        return type;
    }

    void lock() {
        lock.lock();
    }

    void unlock() {
        lock.unlock();
    }

    boolean tryAdd(Order order) {
        if (orders.size() >= capacity) {
            return false;
        }
        order.transitionTo(type);
        orders.put(order.getId(), order);
        return true;
    }

    public Order remove(String orderId) {
        return orders.remove(orderId);
    }

    public Collection<Order> getOrders() {
        return new ArrayList<>(orders.values());
    }

}
