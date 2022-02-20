# makefile for the Java implementation of the SmartDoor adapter

JAVAC = /usr/bin/javac
JAVA = /usr/bin/java

WEBSOCKET_JARS = ./jar/Java-WebSocket-1.5.2.jar
SLF4J_JARS = ./jar/slf4j-api-1.7.25.jar:./jar/slf4j-simple-1.7.25.jar
PROTOBUF_JARS = ./jar/protobuf-java-3.19.1.jar:./jar/pa-protobuf.jar
CLASS_PATH = ".:$(WEBSOCKET_JARS):$(SLF4J_JARS):$(PROTOBUF_JARS)"

# The following CLASS_PATH is also correct, but less explicit:
# CLASS_PATH = ".:./jar/*"

%.class: %.java
	${JAVAC} -cp $(CLASS_PATH) $<

default:
	@echo "makefile has no default target"

PA_SRC = ./PluginAdapter/Api
PA_DEST = ./class
PA_COMPILE = $(JAVAC) -d $(PA_DEST) -s $(PA_SRC) -cp $(CLASS_PATH)

# Generate java files on basis of .proto files.
protobuf_java:
	cd proto; make $@; cd ..

# Compile generated .java files into .class files.
classes: protobuf_java
	mkdir -p class
	$(PA_COMPILE) $(PA_SRC)/AnnouncementOuterClass.java
	$(PA_COMPILE) $(PA_SRC)/AnnouncementsOuterClass.java
	$(PA_COMPILE) $(PA_SRC)/ConfigurationOuterClass.java
	$(PA_COMPILE) $(PA_SRC)/LabelOuterClass.java
	$(PA_COMPILE) $(PA_SRC)/MessageOuterClass.java

# Combine all .class files into a .jar file.
pa-protobuf.jar: classes
	cd class; jar cf ../jar/pa-protobuf.jar PluginAdapter; cd ..

TARGETS = Adapter.class AdapterCore.class BrokerConnection.class Handler.class \
	ProtobufAxini.class SmartDoorHandler.class SmartDoorConnection.class

adapter: $(TARGETS)

clean:
	rm -f *.class
	rm -f -r ./class

very_clean: clean
	rm -f -r PluginAdapter

ZIP = /usr/bin/zip
ZIP_OPTIONS = -o -rp --exclude=*.git*

zip: very_clean
	cd ..; $(ZIP) $(ZIP_OPTIONS) smartdoor-java.zip smartdoor-java; cd smartdoor-java
