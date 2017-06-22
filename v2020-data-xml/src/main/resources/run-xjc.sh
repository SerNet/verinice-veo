#!/bin/sh
echo "Using JAVA_HOME: $JAVA_HOME"
$JAVA_HOME/bin/xjc -wsdl -d ../java/ sync-service.wsdl
