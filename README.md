# Description

This project defines a Java implementation of a plugin adapter for Axini's standalone SmartDoor SUT. It connects the Axini Modeling Platform (AMP) to the standalone SmartDoor SUT. It has been developed to serve as reference implementation for the Axini Adapter Training.

This is an initial version of the implementation; it is still work in progress.

The organization and architecture of the Java adapter is strongly based on Axini's Ruby plugin-adapter-api version 4.x.


# External libraries

The ./jar directory contains several .jar files from external parties, which are used by the plugin adapter.

## Java-Websocket
https://github.com/TooTallNate/Java-WebSocket

./jar/Java-WebSocket-1.5.2.jar

## Protocol Buffers
https://developers.google.com/protocol-buffers

./jar/protobuf-java-3.21.11.jar

The directory ./proto contains the Protobuf .proto files defining the Protobuf messages of Axini's 'Plugin Adapter Protocol'. 

## Simple Logging Facade For Java (SLF4J)
https://www.slf4j.org/

./jar/slf4j-api-1.7.25.jar
./jar/slf4j-simple-1.7.25.jar

The file 'simplelogger.properties' contains the (formatting) settings of the logger.


# Compilation

The distribution contains a makefile with two targets: 

* pa_protobuf.jar. To rebuild `pa_protobuf.jar`, Google's `protoc` compiler should be installed. This target calls the binary `protoc` compiler to generate Java support files for the Protobuf messages and compiles these Java into pa_protobuf.jar, which is placed into ./jar. 

* adapter. After generating pa_protobuf.jar, the 'adapter' target can be used to compile all .java files into .class files.

After building the makefile, the script ./adapter can be used to start the adapter.

This Java adapter has been built succesfully on macOS 11.7.1 (Big Sur) using: Java openjdk 17.0.2 and Protobuf version 3.21.11.


# Current limitations

- Documentation is lacking. No javadoc comments for methods.
- The Java application is developed by a non-native Java programmer; the application may include Ruby-style constructs.
- The application (esp. the AdapterCore class) is not yet Thread safe.
- Error handling should be improved upon.
- Virtual stimuli to inject bad weather behavior have to be added.
- Command line options to the adapter script are not implemented yet.
- (Unit) tests are missing.
