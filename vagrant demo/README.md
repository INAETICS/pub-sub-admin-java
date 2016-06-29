# Inaetics PubSub Demo

This is an example of the publish/subscribe system.

## Launching

1. Install Docker and Vagrant
2. Download the Oracle JDK and place it in the vagrant demo folder. Name the JDK folder 'jdk'.
2. Run docker-run-etcd-zookeeper-kafka.sh
3. In a new terminal, run 'vagrant up'
4. Wait for Vagrant to finish. After that visit the demo page at 10.10.10.11:8080/demo/

## The demo

On the demo page there are buttons to start and stop publishers and subscribers on a topic. When you start a publisher it will publish a random integer (0-10) every 2 seconds. The publish button can be used to publish an integer yourself.

The subscribers will show the messages they received in the values field. This will be updated every 5 seconds.
