package com.css.challenge;

import com.css.challenge.client.Action;
import com.css.challenge.client.Client;
import com.css.challenge.client.Order;
import com.css.challenge.client.Problem;
import com.css.challenge.fulfillment.KitchenManager;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import com.css.challenge.fulfillment.OrderFulfillmentService;
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

        OrderFulfillmentService kitchenManager = new KitchenManager();
        new OrderArrivalSimulator(kitchenManager, rate, min, max).simulate(orders);

        List<Action> actions = kitchenManager.getLedger();
        LOGGER.info("Simulation complete. Submitting ledger with {} captured actions to server...", actions.size());

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
