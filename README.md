# Description

This project defines a *Java* implementation of a plugin adapter for Axini's standalone SmartDoor application (SUT). It connects the Axini Modeling Platform (AMP) to the standalone SmartDoor SUT. It has been developed to serve as a reference implementation for Axini's Adapter Training.

This is an initial version of the implementation; it is still work in progress.

The organization and architecture of the Java adapter is strongly based on Axini's in-house Ruby plugin-adapter-api library, version 4.x.

The software is distributed under the MIT license, see LICENSE.txt.


# Building the application - Maven

The Java application has been organized as a Maven application (https://maven.apache.org). The Java source files of the application reside in the following directory: 
`./src/main/java/com/axini/smartdooradapter`.

Maven's `pom.xml` defines all external dependencies and plugins to build a single jar archive including all external jars.

## Installing Protobuf's `protoc` compiler

Most of the building of the adapter is taken care of by Maven, except for one task: installing [Protobuf](https://github.com/protocolbuffers/protobuf)'s `protoc` compiler. Given `.proto` descriptions, the `protoc` compiler generates Java source files to manipulate Protobuf messages. 

Maven's `pom.xml` uses the `protobuf-maven-plugin` plugin, which specifies the location of the `protoc` compiler:
```
<configuration>
  <protocExecutable>/usr/local/bin/protoc</protocExecutable>
</configuration>
```
If the `protoc` compiler is installed in a different location, you have to modify the line above in `pom.xml`. Precompiled `protoc` binaries can be retrieved from [Protobuf releases](https://github.com/protocolbuffers/protobuf/releases).

## Building executable jar *with* external dependencies

A single jar with all dependencies can be built with:
```
$ mvn compile assembly:single
```
This will generate the following jar archive:
```
./target/smartdoor-adapter-<version>-jar-with-dependencies.jar
```
Where `<version>` is the version as specified in `pom.xml`. It is possible to rename the jar archive, of course.

The adapter can now be started with:
```
$ java -jar smartdoor-adapter-<version>-jar-with-dependencies.jar <name> <url> <token>
```
The shell script `adapter` eases the starting of the adapter.

## Building executable jar without external libraries

It is also possible to build a jar archive with only the classes of the SmartDoor application:
```
$ mvn package
```
When executing the (small) generated jar, one now needs to specify the jar files of the external libraries.

## Cleaning up

After generating the Java classes and the jar file the repository can be cleaned up with (after copying the `.jar` file to safe place):
```
$ mvn clean
```
This will remove the ./target directory, containing the compiled classes and jar files.


# External libraries

Maven's pom.xml specifies the external dependencies of the SmartDoor adapter. When compiling the application with Maven, these dependencies are automatically downloaded from the Maven Central Repository: https://search.maven.org.

## Java-Websocket
https://github.com/TooTallNate/Java-WebSocket

## Google's Protobuf
https://github.com/protocolbuffers/protobuf

## Simple Logging Facade For Java (SLF4J)
https://www.slf4j.org/

The file `./main/resources/simplelogger.properties` contains the (formatting) settings of the logger. This file is automatically copied to the resulting `jar` file of the SmartDoor application.


# Some notes on the implementation

The source code of the adapter can be found in the subdirectory `src/main/java/com/axini/smartdooradapter/.`. The AMP related code is stored in the subdirectory `./generic` and can be used as-is for **any** Java plugin adapter. All SUT specific code (in this case for the SmartDoor SUT) is stored in `./smartdoor` and should be modified for any new SUT.

## Threads
The main thread of the adapter ensures that messages from AMP are received and handled. The `SmartdoorConnection` class (in `./smartdoor`) starts a separate thread which is used for the messages from the SmartDoor SUT over the WebSocket connection between the SUT and the adapter. 

The class `QThread` (in `./generic`) manages a Queue of items and a Thread. Items can be added to the Queue and the Thread processes items from the queue in a FIFO manner. The Queue can also be emptied. The plugin adapter (class `AdapterCore` in `./generic`) uses two QThreads for (i) handling messages from AMP and (ii) sending messages to AMP. This ensures that messages from AMP (stimuli) and the SUT (responses) are serviced immediately: any resulting message is added to a queue of pending messages which is processed by either one of the two QThreads.

Using a separate QThread for sending the responses to AMP ensures that only a single WebSocket message can be in transit to AMP. 

The QThread for the messages from AMP (Configuration, Ready, stimuli) is needed for a different reason. The processing of actual ProtoBuf messages from AMP may take some (considerable) time. For instance, after a Configuration message, the SUT has to be started and after a Reset message the SUT has to be reset to its initial state. And even the handling of a stimulus at the SUT may take some time. The WebSocket library is single threaded which means that as long as the BrokerConnection's `onMessage` method is being executed, the websocket library cannot handle any new WebSocket message from AMP, including heartbeat (ping) messages. Therefore, the AdapterCore uses a separate QThread to handle ProtoBuf messages from AMP. When a ProtoBuf message is received from AMP, the `onMessage` method calls the AdapterCore's `handleMessageFromAmp` method which only adds this message to the queue of pending messages. This ensures that the WebSocket thread is always ready to react on new WebSocket messages from AMP.

The plugin adapter and all its threads are set to run forever. No code is added to gracefully terminate the adapter and its threads. Consequently, when terminating the adapter with Ctrl-C, you may observe several Exceptions on the stderr. This is harmless, though.


# Current limitations

- Documentation is lacking. No javadoc comments for methods.
- The Java application is developed by a non-native Java programmer; the application may include Ruby-style constructs.
- The application (esp. the AdapterCore class) is not yet Thread safe.
- Error handling should be improved upon.
- Virtual stimuli to inject bad weather behavior could be added.
- Command line options to the adapter script are not implemented yet.
- (Unit) tests are missing. The adapter has been tested with AMP and the Smartdoor application, though. ;-)
