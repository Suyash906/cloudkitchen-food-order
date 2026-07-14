package com.css.challenge.fulfillment;

import com.css.challenge.client.Order;
import com.css.challenge.client.StorageType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Storage {
    private final StorageType type;
    private final int capacity;
    private final Map<String, Order> orders = new HashMap<>();

    public Storage(StorageType type, int capacity) {
        this.type = type;
        this.capacity = capacity;
    }

    public StorageType getType() {
        return type;
    }

    public boolean hasRoom() {
        return orders.size() < capacity;
    }

    public void add(Order order) {
        order.transitionTo(type);
        orders.put(order.getId(), order);
    }

    public Order remove(String orderId) {
        return orders.remove(orderId);
    }

    public Collection<Order> getOrders() {
        return new ArrayList<>(orders.values());
    }

}
