# FinSim Terminal

A console application for testing and interacting with FinSim microservices.

## Overview

FinSim Terminal is a command-line tool designed for testing and experimenting with the FinSim microservices. It has a menu interface to perform various operations and tests against the services.

## Building

To build the application, run from the finsim root directory:

```bash
mvn clean compile -pl experiments/finsim-term
```

## Running

### Direct Java execution

From the finsim-term directory:

```bash
mvn compile
java -cp target/classes finsim.term.FinSimTerminal
```
