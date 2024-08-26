# Microservice-Oriented Database System (MODB)

MODB is a distributed, event-driven, microservice-oriented database management system. The first principled approach for designing stateful microservices that require advanced data management requirements.

MODB offers event-driven functionalities by design, which makes it easy for developers to build stateful microservices that run on the cloud, at the same time offering abstractions developers are more used to work with.

MODB runs application logic in seamlessly within the application tier, not requiring developers to implement complex and error-prone stored procedures in the database.

Developers can plug and play any microservice at any time, the MODB then adapt the event streams seamlessly as requested by the application. In the end, developers still experience the flexibility and dinamicity offered by microservice architectures, but with built-in system-level database support to alleviate several challenges usually found in the practice.

## Table of Contents
- [Getting Started](#getting-started)
    * [Prerequisites](#prerequisites)
    * [Compilation](#compilation)
    * [Configuration](#config)
- [MODB](#modb)
    * [Abstractions](#abstractions)
    * [Architecture](#architecture)
    * [APIs](#apis)
    * [Play Around](#play)
    * [Testing](#test)
- [Links](#links)

## <a name="getting-started"></a>Getting Started

### <a name="prerequisites"></a>Prerequisites

- Maven

  To assemble the dependencies and compile the project

  [How to install maven on Ubuntu](https://www.hostinger.com/tutorials/how-to-install-maven-on-ubuntu)

- Java Runtime Environment 21 

  Java Development Kit 21 if you intend to modify the source code

- Curl 

  If you want to play with the APIs, Curl allows to easily submit HTTP requests

### <a name="compilation"></a>Compilation

It is necessary to generate the dependencies required to compile the microservice.
This can be accomplished via running the following command in the root folder:

```
mvn clean install -DskipTests=true
```

Then you can just run the following command:
```
mvn clean package -DskipTests=true
```

## <a name="modb"></a>MODB

Differently from traditional server-based database systems, where users interact via a well-defined network protocol, in MODB, users solely write code and all the data management complexity is abstracted by the runtime.

## <a name="links"></a>Useful links

- [Packet Size ,Window Size and Socket Buffer In TCP](https://stackoverflow.com/a/37267929/7735153)
- [Throughput and TCP windows](http://packetbomb.com/understanding-throughput-and-tcp-windows/)
- [Tuning the windows size](https://docs.oracle.com/cd/E23507_01/Platform.20073/ATGInstallGuide/html/s0507tuningthetcpwindowsize01.html)