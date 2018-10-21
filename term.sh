#!/bin/sh

CP=target/classes:target/test-classes
CP=${CP}:~/.m2/repository/org/jline/jline/3.9.0/jline-3.9.0.jar

java -classpath $CP mylib.tools.misc.TerminalTest
