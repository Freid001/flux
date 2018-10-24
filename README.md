# Flux

## About
* A command line interface which retrieves cloud watch metrics and pushes them into Kafka.
* The purpose of this project was to gain some experience working with Kafka. 

### Requirements
* [Docker](https://www.docker.com/) 
* [Leiningen](https://leiningen.org/)

### Installation
```bash
// run app
docker-compose up

// stop app
docker-compose down
```

## Usage
```bash

// fetch and push metrics into kafka
lein run fetch --metric=METRIC \
               --namespace=NAMESPACE \
               --statistic=STATISTIC \ 
               --start-time=TIME \
               --end-time=TIME \
               --period SECONDS
               
// poll kafka topic
lein run poll
```