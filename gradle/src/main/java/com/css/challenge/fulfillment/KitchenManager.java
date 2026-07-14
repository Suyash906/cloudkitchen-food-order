package com.css.challenge.fulfillment;

import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import com.css.challenge.client.StorageType;

import java.util.Map;
import java.util.List;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KitchenManager  implements OrderFulfillmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KitchenManager.class);
    private final ReentrantLock kitchenLock = new ReentrantLock(true);

    private final Storage cooler = new Storage(StorageType.COOLER, 6);
    private final Storage heater = new Storage(StorageType.HEATER, 6);
    private final Storage shelf = new Storage(StorageType.SHELF, 12);

    private final Map<String, Storage> masterLookup = new ConcurrentHashMap<>();

    private final ActionLedger ledger = new ActionLedger();

    @Override
    public void placeOrder(Order order) {
        if (order == null || order.getId() == null || order.getTemp() == null) {
            throw new IllegalArgumentException("Order, order id, and temp must not be null");
        }
        kitchenLock.lock();
        try {
            if (masterLookup.containsKey(order.getId())) {
                LOGGER.info("[REJECT] Order {} already exists; ignoring duplicate placement.", order.getId());
                return;
            }

            StorageType idealStorage = StorageType.fromTemp(order.getTemp());

            // Rule 1: Try to place in its primary ideal storage location
            if (idealStorage == StorageType.COOLER && cooler.hasRoom()) {
                storeOrder(order, cooler); // Typo protection handled below
                logAction(order.getId(), Action.PLACE, cooler.getType().getActionTarget());
                return;
            } else if (idealStorage == StorageType.HEATER && heater.hasRoom()) {
                storeOrder(order, heater);
                logAction(order.getId(), Action.PLACE, heater.getType().getActionTarget());
                return;
            } else if (idealStorage == StorageType.SHELF && shelf.hasRoom()) {
                storeOrder(order, shelf);
                logAction(order.getId(), Action.PLACE, shelf.getType().getActionTarget());
                return;
            }

            // Rule 2: Ideal storage is full, attempt fallback to the overflow Shelf
            if (shelf.hasRoom()) {
                storeOrder(order, shelf);
                logAction(order.getId(), Action.PLACE, shelf.getType().getActionTarget());
                return;
            }

            // Rule 3: The Shelf is full! Attempt to free space by moving a misplaced item back home
            if (tryMovingAnOrderFromShelfToIdeal()) {
                // Space successfully created on the shelf! Place the new order here
                storeOrder(order, shelf);
                logAction(order.getId(), Action.PLACE, shelf.getType().getActionTarget());
                return;
            }

            // Rule 4: Evict an item using the "Fabulous Food" minimum freshness rule
            evictLowestFreshnessFromShelf();
            storeOrder(order, shelf);
            logAction(order.getId(), Action.PLACE, shelf.getType().getActionTarget());
        } finally {
            kitchenLock.unlock();
        }
    }

    @Override
    public void pickupOrder(String orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order id must not be null");
        }
        kitchenLock.lock();
        try {
            Storage targetStorage = masterLookup.get(orderId);
            if (targetStorage == null) {
                // Driver arrived for an order that isn't present (already discarded or never existed)
                return;
            }

            Order order = targetStorage.remove(orderId);
            masterLookup.remove(orderId);

            logAction(orderId, Action.PICKUP, targetStorage.getType().getActionTarget());
            LOGGER.info("[PICKUP] Driver successfully retrieved Order {}.", orderId);

        } finally {
            kitchenLock.unlock();
        }
    }

    private void storeOrder(Order order, Storage storage) {
        storage.add(order);
        masterLookup.put(order.getId(), storage);
    }

    private boolean tryMovingAnOrderFromShelfToIdeal() {
        for (Order shelfOrder : shelf.getOrders()) {
            StorageType ideal = StorageType.fromTemp(shelfOrder.getTemp());
            if (ideal == StorageType.COOLER && cooler.hasRoom()) {
                shelf.remove(shelfOrder.getId());
                storeOrder(shelfOrder, cooler);
                logAction(shelfOrder.getId(), Action.MOVE, cooler.getType().getActionTarget());
                return true;
            } else if (ideal == StorageType.HEATER && heater.hasRoom()) {
                shelf.remove(shelfOrder.getId());
                storeOrder(shelfOrder, heater);
                logAction(shelfOrder.getId(), Action.MOVE, heater.getType().getActionTarget());
                return true;
            }
        }
        return false;
    }

    private void evictLowestFreshnessFromShelf() {
        Order worstOrder = shelf.getOrders().stream()
                .min(Comparator.comparingDouble(Order::getRemainingFreshness))
                .orElse(null);
        if (worstOrder != null) {
            shelf.remove(worstOrder.getId());
            masterLookup.remove(worstOrder.getId());
            logAction(worstOrder.getId(), Action.DISCARD, shelf.getType().getActionTarget());

            // Required criteria text embedding
            LOGGER.info("[EVICTION] Discarded lowest quality item {} from shelf to prioritize fresh, fabulous food.", worstOrder.getId());

        }

    }

    private void logAction(String id, String actionType, String target) {
        ledger.record(id, actionType, target);
    }

    @Override
    public List<Action> getLedger() {
        return ledger.getActions();
    }
}
