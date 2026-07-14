package com.css.challenge.fulfillment;

import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import com.css.challenge.client.StorageType;

import java.util.Comparator;
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

    public PlacementPolicy(
        Storage cooler, Storage heater, Storage shelf, Map<String, Storage> masterLookup, ActionLedger ledger) {
        this.cooler = cooler;
        this.heater = heater;
        this.shelf = shelf;
        this.masterLookup = masterLookup;
        this.ledger = ledger;
    }

    public void place(Order order) {
        if (masterLookup.containsKey(order.getId())) {
            LOGGER.info("[REJECT] Order {} already exists; ignoring duplicate placement.", order.getId());
            return;
        }

        StorageType idealStorage = StorageType.fromTemp(order.getTemp());

        // Rule 1: Try to place in its primary ideal storage location
        if (idealStorage == StorageType.COOLER && cooler.hasRoom()) {
            storeOrder(order, cooler);
            ledger.record(order.getId(), Action.PLACE, cooler.getType().getActionTarget());
            return;
        } else if (idealStorage == StorageType.HEATER && heater.hasRoom()) {
            storeOrder(order, heater);
            ledger.record(order.getId(), Action.PLACE, heater.getType().getActionTarget());
            return;
        } else if (idealStorage == StorageType.SHELF && shelf.hasRoom()) {
            storeOrder(order, shelf);
            ledger.record(order.getId(), Action.PLACE, shelf.getType().getActionTarget());
            return;
        }

        // Rule 2: Ideal storage is full, attempt fallback to the overflow Shelf
        if (shelf.hasRoom()) {
            storeOrder(order, shelf);
            ledger.record(order.getId(), Action.PLACE, shelf.getType().getActionTarget());
            return;
        }

        // Rule 3: The Shelf is full! Attempt to free space by moving a misplaced item back home
        if (tryMovingAnOrderFromShelfToIdeal()) {
            storeOrder(order, shelf);
            ledger.record(order.getId(), Action.PLACE, shelf.getType().getActionTarget());
            return;
        }

        // Rule 4: Evict an item using the "Fabulous Food" minimum freshness rule
        evictLowestFreshnessFromShelf();
        storeOrder(order, shelf);
        ledger.record(order.getId(), Action.PLACE, shelf.getType().getActionTarget());
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
                ledger.record(shelfOrder.getId(), Action.MOVE, cooler.getType().getActionTarget());
                return true;
            } else if (ideal == StorageType.HEATER && heater.hasRoom()) {
                shelf.remove(shelfOrder.getId());
                storeOrder(shelfOrder, heater);
                ledger.record(shelfOrder.getId(), Action.MOVE, heater.getType().getActionTarget());
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
}
