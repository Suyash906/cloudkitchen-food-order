package com.css.challenge.fulfillment;

import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import java.util.List;

public interface OrderFulfillmentService {
    void placeOrder(Order order);
    void pickupOrder(String orderId);
    List<Action> getLedger();
}
