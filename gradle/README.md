# README

Author: `Suyash Srivastava`

## How to run

The `Dockerfile` defines a self-contained Java/Gradle reference environment.
Build and run the program using [Docker](https://docs.docker.com/get-started/get-docker/):
```
$ docker build -t challenge .
$ docker run --rm -it challenge --auth=<token>
```
Feel free to modify the `Dockerfile` as you see fit.

If java `25` or later is installed locally, run the program directly for convenience:
```
$ ./gradlew run --args="--auth=<token>"
```

## Discard criteria

### Logic
```
When an order can't be placed in its ideal storage, and the shelf is also full, we first try to free a shelf slot by relocating a misplaced item back to its own ideal storage. If that isn't possible, we discard the shelf order with the lowest remaining freshness, to preserve the highest quality food.
```

### Implementation Details
```
FreshnessIndex uses a TreeMap keyed by each order's projected expiry time (when its remaining freshness will hit zero, given its current storage's decay rate), giving O(log n) lookup of the least-fresh order instead of a linear scan.
```
