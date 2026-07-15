package com.css.challenge.fulfillment;

import com.css.challenge.client.Order;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;

class FreshnessIndex {
    private final TreeMap<Long, LinkedHashSet<Order>> byProjectedExpiry = new TreeMap<>();
    private final Map<String, Long> expiryByOrderId = new HashMap<>();

    void add(Order order, int decayMultiplier) {
        long projectedExpiryMillis = computeProjectedExpiry(order, decayMultiplier);
        byProjectedExpiry.computeIfAbsent(projectedExpiryMillis, k -> new LinkedHashSet<>()).add(order);
        expiryByOrderId.put(order.getId(), projectedExpiryMillis);
    }

    Order removeMin() {
        Map.Entry<Long, LinkedHashSet<Order>> entry = byProjectedExpiry.firstEntry();
        if (entry == null) {
            return null;
        }
        LinkedHashSet<Order> bucket = entry.getValue();
        Order order = bucket.iterator().next();
        bucket.remove(order);
        if (bucket.isEmpty()) {
            byProjectedExpiry.remove(entry.getKey());
        }
        expiryByOrderId.remove(order.getId());
        return order;
    }

    void remove(String orderId) {
        Long key = expiryByOrderId.remove(orderId);
        if (key == null) {
            return;
        }
        LinkedHashSet<Order> bucket = byProjectedExpiry.get(key);
        if (bucket != null) {
            bucket.removeIf(o -> o.getId().equals(orderId));
            if (bucket.isEmpty()) {
                byProjectedExpiry.remove(key);
            }
        }
    }

    private static long computeProjectedExpiry(Order order, int decayMultiplier) {
        double remainingFreshness = order.getRemainingFreshness();
        long msUntilExpiry = (long) (remainingFreshness / decayMultiplier * 1000);
        return System.currentTimeMillis() + msUntilExpiry;
    }

}
