# Simple services that polls data from alpha ess cloud and publishes them to an mqtt broker

Implemented is currently only the support for basic user authentication for MQTT Broker.

It uses a auto discoverable format for famous [Home Assistant](https://www.home-assistant.io/). For this compatibility, I created a separated project: [HAMQTT](https://bitbucket.org/vdwals/hamqtt/src/main/).

## How to use
The project can either be run as Java code using maven or be build to a jar before.
Additionally, a Dockerfile and a docker-compose configuration are provided where build instructions and environment variables are explained.
I prefere running it as a Docker container parallel to Home Assistant.

If I have time, I'll convert it to an Home Assistant Addon, too.