package com.css.challenge;

import com.css.challenge.client.Order;
import com.css.challenge.fulfillment.OrderFulfillmentService;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderArrivalSimulator {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderArrivalSimulator.class);
    private static final int SCHEDULER_THREAD_POOL_SIZE = 4;
    private static final long TERMINATION_BUFFER_SECONDS = 5;

    private final OrderFulfillmentService kitchenManager;
    private final Duration rate;
    private final Duration min;
    private final Duration max;

    public OrderArrivalSimulator(OrderFulfillmentService kitchenManager, Duration rate, Duration min, Duration max) {
        this.kitchenManager = kitchenManager;
        this.rate = rate;
        this.min = min;
        this.max = max;
    }

    public void simulate(List<Order> orders) throws InterruptedException {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SCHEDULER_THREAD_POOL_SIZE);

        long currentOrderDelayMillis = 0;
        long minPickupMillis = min.toMillis();
        long maxPickupMillis = max.toMillis();

        for (Order order : orders) {
            scheduler.schedule(() -> placeOrder(order), currentOrderDelayMillis, TimeUnit.MILLISECONDS);

            long driverDelayMillis = ThreadLocalRandom.current().nextLong(minPickupMillis, maxPickupMillis + 1);
            long totalDelayToPickup = currentOrderDelayMillis + driverDelayMillis;
            scheduler.schedule(() -> pickupOrder(order), totalDelayToPickup, TimeUnit.MILLISECONDS);

            currentOrderDelayMillis += rate.toMillis();
        }

        scheduler.shutdown();

        long maxWaitTimeSeconds = (currentOrderDelayMillis + maxPickupMillis) / 1000 + TERMINATION_BUFFER_SECONDS;
        LOGGER.info("All tasks dispatched. Awaiting final driver pickups (Max wait: {}s)...", maxWaitTimeSeconds);

        if (!scheduler.awaitTermination(maxWaitTimeSeconds, TimeUnit.SECONDS)) {
            LOGGER.warn("Simulation reached timeout before all drivers completed their runs.");
        }
    }

    private void placeOrder(Order order) {
        try {
            LOGGER.info("[ARRIVAL] New Order prepared: {} ({})", order.getId(), order.getName());
            kitchenManager.placeOrder(order);
        } catch (RuntimeException e) {
            LOGGER.error("[ARRIVAL] Failed to place order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    private void pickupOrder(Order order) {
        try {
            LOGGER.info("[DRIVER] Driver arriving to collect Order: {}", order.getId());
            kitchenManager.pickupOrder(order.getId());
        } catch (RuntimeException e) {
            LOGGER.error("[DRIVER] Failed to pickup order {}: {}", order.getId(), e.getMessage(), e);
        }
    }
}
