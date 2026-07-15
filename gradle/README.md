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
When an order can't be placed on it's ideal storage, and the shelf is also full then we discard the order with the lowest remaining freshness. We are trying to preserve the highest quality food.
```

### Implementation Details
- FreshnessIndex class has been added for this. It uses TreeMap under the hood to arrange the order based on the decay rate 
- Use of TreeMap allow us to achieve O(log n) time complexity.
