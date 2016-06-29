#!/bin/bash
export PATH=$PATH:/home/vagrant/jdk/bin 
nohup java -jar demo.jar >/dev/null 2>&1 &
