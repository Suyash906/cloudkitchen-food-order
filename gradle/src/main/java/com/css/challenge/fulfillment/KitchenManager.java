package com.css.challenge.fulfillment;

import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import com.css.challenge.client.StorageType;

import java.util.Map;
import java.util.List;
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

    private final PlacementPolicy placementPolicy = new PlacementPolicy(cooler, heater, shelf, masterLookup, ledger);

    @Override
    public void placeOrder(Order order) {
        if (order == null || order.getId() == null || order.getTemp() == null) {
            throw new IllegalArgumentException("Order, order id, and temp must not be null");
        }
        kitchenLock.lock();
        try {
            placementPolicy.place(order);
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

            targetStorage.remove(orderId);
            masterLookup.remove(orderId);

            ledger.record(orderId, Action.PICKUP, targetStorage.getType().getActionTarget());
            LOGGER.info("[PICKUP] Driver successfully retrieved Order {}.", orderId);
        } finally {
            kitchenLock.unlock();
        }
    }
    @Override
    public List<Action> getLedger() {
        return ledger.getActions();
    }
}
