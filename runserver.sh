#!/bin/sh
java -classpath lib/*:bin/.:conf/  edu.upenn.cis.cis455.webserver.HttpServer 8080 /home/cis455/workspace /home/cis455/workspace/HW1/conf/web.xml &
sleep 2
curl http://localhost:8080/control
curl http://localhost:8080/shutdown
