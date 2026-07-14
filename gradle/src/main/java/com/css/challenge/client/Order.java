package com.css.challenge.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/** Order is a json-friendly representation of an order. */
public class Order {
  private final String id; // order id
  private final String name; // food name
  private final String temp; // ideal temperature
  private final int price; // price in dollars
  private final int freshness; // freshness in seconds
  private double remainingFreshness;
  private long lastUpdateTimestamp;
  private StorageType currentStorage;

  public Order(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("temp") String temp,
      @JsonProperty("price") int price,
      @JsonProperty("freshness") int freshness) {
    this.id = id;
    this.name = name;
    this.temp = temp;
    this.price = price;
    this.freshness = freshness;
    this.remainingFreshness = freshness;
    this.lastUpdateTimestamp = System.currentTimeMillis();
  }

  static List<Order> parse(String json) throws JsonProcessingException {
    return new ObjectMapper().readValue(json, new TypeReference<List<Order>>() {});
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getTemp() {
    return temp;
  }

  public int getPrice() {
    return price;
  }

  public int getFreshness() {
    return freshness;
  }

  public synchronized void transitionTo(StorageType newStorage){
    this.updateFreshness();
    this.currentStorage = newStorage;
  }

  public synchronized double getRemainingFreshness() {
    updateFreshness();
    return this.remainingFreshness;
  }

  private void updateFreshness() {
      long now = System.currentTimeMillis();
      if (this.currentStorage != null && this.remainingFreshness > 0) {
          double elapsedTime = (now - this.lastUpdateTimestamp) / 1000.0;
          double decay = elapsedTime * this.currentStorage.getDecayMultiplier();
          this.remainingFreshness = Math.max(0, this.remainingFreshness - decay);
      }
      this.lastUpdateTimestamp = now;
  }

  @Override
  public String toString() {
    return "{id: "
        + id
        + ", name: "
        + name
        + ", temp: "
        + temp
        + ", price: $"
        + price
        + ", freshness:"
        + freshness
        + "}";
  }
}
