package com.css.challenge;

import com.css.challenge.client.Action;
import com.css.challenge.client.Client;
import com.css.challenge.client.Order;
import com.css.challenge.client.Problem;
import com.css.challenge.fulfillment.KitchenManager;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "challenge", showDefaultValues = true)
public class Main implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  static {
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT: %5$s %n");
  }

  @Option(names = "--endpoint", description = "Problem server endpoint")
  String endpoint = "https://api.cloudkitchens.com";

  @Option(names = "--auth", description = "Authentication token (required)")
  String auth = "";

  @Option(names = "--name", description = "Problem name. Leave blank (optional)")
  String name = "";

  @Option(names = "--seed", description = "Problem seed (random if zero)")
  long seed = 0;

  @Option(names = "--rate", description = "Inverse order rate")
  Duration rate = Duration.ofMillis(500);

  @Option(names = "--min", description = "Minimum pickup time")
  Duration min = Duration.ofSeconds(4);

  @Option(names = "--max", description = "Maximum pickup time")
  Duration max = Duration.ofSeconds(8);

  @Override
  public void run() {
    try {
      Client client = new Client(endpoint, auth);
      Problem problem = client.newProblem(name, seed);

      List<Order> orders = problem.getOrders();
      LOGGER.info("Starting simulation for {} incoming orders...", orders.size());

      // ------ Execution harness logic goes here using rate, min and max ----

      KitchenManager kitchenManager = new KitchenManager();

      ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

      long currentOrderDelayMillis = 0;
      long minPickupMillis = min.toMillis();
      long maxPickupMillis = max.toMillis();

      for (Order order : orders) {

        scheduler.schedule(() -> {
          LOGGER.info("[ARRIVAL] New Order prepared: {} ({})", order.getId(), order.getName());
          kitchenManager.placeOrder(order);
        }, currentOrderDelayMillis, TimeUnit.MILLISECONDS);


        long driverDelayMillis = ThreadLocalRandom.current().nextLong(minPickupMillis, maxPickupMillis + 1);
        long totalDelayToPickup = currentOrderDelayMillis + driverDelayMillis;

        scheduler.schedule(() -> {
          LOGGER.info("[DRIVER] Driver arriving to collect Order: {}", order.getId());
          kitchenManager.pickupOrder(order.getId());
        }, totalDelayToPickup, TimeUnit.MILLISECONDS);

        currentOrderDelayMillis += rate.toMillis();

      }


      scheduler.shutdown();


      long maxWaitTimeSeconds = (currentOrderDelayMillis + maxPickupMillis) / 1000 + 5;
      LOGGER.info("All tasks dispatched. Awaiting final driver pickups (Max wait: {}s)...", maxWaitTimeSeconds);

      if (!scheduler.awaitTermination(maxWaitTimeSeconds, TimeUnit.SECONDS)) {
        LOGGER.warn("Simulation reached timeout before all drivers completed their runs.");
      }

      // Harvesting the complete, thread-safe sequence of actions captured by our engine
      List<Action> actions = kitchenManager.getLedger();
      LOGGER.info("Simulation complete. Submitting ledger with {} captured actions to server...", actions.size());

      // ----------------------------------------------------------------------

      String result = client.solveProblem(problem.getTestId(), rate, min, max, actions);
      LOGGER.info("Result: {}", result);

    } catch (IOException | InterruptedException e) {
      LOGGER.error("Execution failed: {}", e.getMessage());
      Thread.currentThread().interrupt();
    }
  }

  public static void main(String[] args) {
    new CommandLine(new Main()).execute(args);
  }
}
