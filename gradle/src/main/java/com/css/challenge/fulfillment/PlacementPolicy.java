package com.css.challenge.fulfillment;

import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import com.css.challenge.client.StorageType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlacementPolicy {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlacementPolicy.class);

    private final Storage cooler;
    private final Storage heater;
    private final Storage shelf;
    private final Map<String, Storage> masterLookup;
    private final ActionLedger ledger;
    private final List<Storage> lockOrder;

    public PlacementPolicy(
        Storage cooler, Storage heater, Storage shelf, Map<String, Storage> masterLookup, ActionLedger ledger) {
        this.cooler = cooler;
        this.heater = heater;
        this.shelf = shelf;
        this.masterLookup = masterLookup;
        this.ledger = ledger;
        this.lockOrder = List.of(cooler, heater, shelf);
    }

    private void lockAll() {
        for (Storage storage : lockOrder) {
            storage.lock();
        }
    }

    private void unlockAll() {
        for (int i = lockOrder.size() - 1; i >= 0; i--) {
            lockOrder.get(i).unlock();
        }
    }

    public void place(Order order) {
        lockAll();
        try {
            if (masterLookup.containsKey(order.getId())) {
                LOGGER.info("[REJECT] Order {} already exists; ignoring duplicate placement.", order.getId());
                return;
            }

            Storage ideal = storageFor(StorageType.fromTemp(order.getTemp()));

            // Rule 1: Try to place in its primary ideal storage location
            if (ideal.tryAdd(order)) {
                commit(order, ideal);
                return;
            }

            // Rule 2: Ideal storage is full, attempt fallback to the overflow Shelf
            if (shelf.tryAdd(order)) {
                commit(order, shelf);
                return;
            }

            // Rule 3: The Shelf is full! Attempt to free space by moving a misplaced item back home
            if (tryMovingAnOrderFromShelfToIdeal() && shelf.tryAdd(order)) {
                commit(order, shelf);
                return;
            }

            // Rule 4: Evict an item using the "Fabulous Food" minimum freshness rule
            evictLowestFreshnessFromShelf();
            shelf.tryAdd(order);
            commit(order, shelf);
        } finally {
            unlockAll();
        }
    }

    private void commit(Order order, Storage storage) {
        masterLookup.put(order.getId(), storage);
        ledger.record(order.getId(), Action.PLACE, storage.getType().getActionTarget());
    }

    private boolean tryMovingAnOrderFromShelfToIdeal() {
        for (Order shelfOrder : shelf.getOrders()) {
            StorageType idealType = StorageType.fromTemp(shelfOrder.getTemp());
            if (idealType != StorageType.COOLER && idealType != StorageType.HEATER) {
                continue;
            }
            Storage target = storageFor(idealType);
            if (target.tryAdd(shelfOrder)) {
                shelf.remove(shelfOrder.getId());
                masterLookup.put(shelfOrder.getId(), target);
                ledger.record(shelfOrder.getId(), Action.MOVE, target.getType().getActionTarget());
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
            ledger.record(worstOrder.getId(), Action.DISCARD, shelf.getType().getActionTarget());

            // Required criteria text embedding
            LOGGER.info(
                "[EVICTION] Discarded lowest quality item {} from shelf to prioritize fresh, fabulous food.",
                worstOrder.getId());
        }
    }
    private Storage storageFor(StorageType type) {
        switch (type) {
            case COOLER:
                return cooler;
            case HEATER:
                return heater;
            default:
                return shelf;
        }
    }
}
