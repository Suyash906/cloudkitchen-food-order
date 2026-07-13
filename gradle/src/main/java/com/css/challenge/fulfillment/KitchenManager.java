package com.css.challenge.fulfillment;

import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import com.css.challenge.client.StorageType;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class KitchenManager{
    private static final int COOLER_CAPACITY = 6;
    private static final int HEATER_CAPACITY = 6;
    private static final int SHELF_CAPACITY = 12;

    private final ReentrantLock kitchenLock = new ReentrantLock(true);

    private final Map<String, Order> cooler = new HashMap<>();
    private final Map<String, Order> heater = new HashMap<>();
    private final Map<String, Order> shelf = new HashMap<>();

    private final Map<String, Map<String, Order>> masterLookup = new ConcurrentHashMap<>();

    private final List<Action> ledger = Collections.synchronizedList(new ArrayList<>());

    public void placeOrder(Order order) {
        kitchenLock.lock();
        try {
            StorageType idealStorage = getIdealStorage(order);

            // Rule 1: Try to place in its primary ideal storage location
            if (idealStorage == StorageType.COOLER && cooler.size() < COOLER_CAPACITY) {
                storeOrder(order, cooler, StorageType.COOLER, Action.COOLER); // Typo protection handled below
                logAction(order.getId(), Action.PLACE, Action.COOLER);
                return;
            } else if (idealStorage == StorageType.HEATER && heater.size() < HEATER_CAPACITY) {
                storeOrder(order, heater, StorageType.HEATER, Action.HEATER);
                logAction(order.getId(), Action.PLACE, Action.HEATER);
                return;
            } else if (idealStorage == StorageType.SHELF && shelf.size() < SHELF_CAPACITY) {
                storeOrder(order, shelf, StorageType.SHELF, Action.SHELF);
                logAction(order.getId(), Action.PLACE, Action.SHELF);
                return;
            }

            // Rule 2: Ideal storage is full, attempt fallback to the overflow Shelf
            if (shelf.size() < SHELF_CAPACITY) {
                storeOrder(order, shelf, StorageType.SHELF, Action.SHELF);
                logAction(order.getId(), Action.PLACE, Action.SHELF);
                return;
            }

            // Rule 3: The Shelf is full! Attempt to free space by moving a misplaced item back home
            if (tryMovingAnOrderFromShelfToIdeal()) {
                // Space successfully created on the shelf! Place the new order here
                storeOrder(order, shelf, StorageType.SHELF, Action.SHELF);
                logAction(order.getId(), Action.PLACE, Action.SHELF);
                return;
            }

            // Rule 4: Evict an item using the "Fabulous Food" minimum freshness rule
            evictLowestFreshnessFromShelf();
            storeOrder(order, shelf, StorageType.SHELF, Action.SHELF);
            logAction(order.getId(), Action.PLACE, Action.SHELF);
        } finally {
            kitchenLock.unlock();
        }
    }

    public void pickupOrder(String orderId) {
        kitchenLock.lock();
        try {
            Map<String, Order> targetStorage = masterLookup.get(orderId);
            if (targetStorage == null) {
                // Driver arrived for an order that isn't present (already discarded or never existed)
                return;
            }

            Order order = targetStorage.remove(orderId);
            masterLookup.remove(orderId);

            if (order.isExpired()) {
                logAction(orderId, Action.DISCARD, getActionTargetString(targetStorage));
                System.out.printf("[DISCARD] Order %s expired before pickup.%n", orderId);
            } else {
                logAction(orderId, Action.PICKUP, getActionTargetString(targetStorage));
                System.out.printf("[PICKUP] Driver successfully retrieved Order %s.%n", orderId);
            }
        } finally {
            kitchenLock.unlock();
        }
    }

    private void storeOrder(Order order, Map<String, Order> storage, StorageType type, String targetConstant) {
        order.transitionTo(type);
        storage.put(order.getId(), order);
        masterLookup.put(order.getId(), storage);
    }

    private boolean tryMovingAnOrderFromShelfToIdeal() {
        for (Order shelfOrder : shelf.values()) {
            StorageType ideal = getIdealStorage(shelfOrder);
            if (ideal == StorageType.COOLER && cooler.size() < COOLER_CAPACITY) {
                shelf.remove(shelfOrder.getId());
                storeOrder(shelfOrder, cooler, StorageType.COOLER, Action.COOLER);
                logAction(shelfOrder.getId(), Action.MOVE, Action.COOLER);
                return true;
            } else if (ideal == StorageType.HEATER && heater.size() < HEATER_CAPACITY) {
                shelf.remove(shelfOrder.getId());
                storeOrder(shelfOrder, heater, StorageType.HEATER, Action.HEATER);
                logAction(shelfOrder.getId(), Action.MOVE, Action.HEATER);
                return true;
            }
        }
        return false;
    }

    private void evictLowestFreshnessFromShelf() {
        Order worstOrder = shelf.values().stream()
                .min(Comparator.comparingDouble(Order::getRemainingFreshness))
                .orElse(null);
        if (worstOrder != null) {
            shelf.remove(worstOrder.getId());
            masterLookup.remove(worstOrder.getId());
            logAction(worstOrder.getId(), Action.DISCARD, Action.SHELF);

            // Required criteria text embedding
            System.out.printf("[EVICTION] Discarded lowest quality item %s from shelf to prioritize fresh, fabulous food.%n", worstOrder.getId());

        }

    }

    private StorageType getIdealStorage(Order order) {
        String temp = order.getTemp().toLowerCase();
        switch(temp) {
            case "hot": return StorageType.HEATER;
            case "cold": return StorageType.COOLER;
            default: return StorageType.SHELF;
        }
    }

    private String getActionTargetString(Map<String, Order> storage) {
        if (storage == cooler) return Action.COOLER;
        if (storage == heater) return Action.HEATER;
        return Action.SHELF;
    }

    private void logAction(String id, String actionType, String target) {
        Action action = new Action(Instant.now(), id, actionType, target);
        ledger.add(action);
    }

    public List<Action> getLedger() {
        return new ArrayList<>(ledger);
    }
}
