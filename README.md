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


# Current limitations

- Documentation is lacking. No javadoc comments for methods.
- The Java application is developed by a non-native Java programmer; the application may include Ruby-style constructs.
- The application (esp. the AdapterCore class) is not yet Thread safe.
- Error handling should be improved upon.
- Virtual stimuli to inject bad weather behavior could be added.
- Command line options to the adapter script are not implemented yet.
- (Unit) tests are missing. The adapter has been tested with AMP and the Smartdoor application, though. ;-)
