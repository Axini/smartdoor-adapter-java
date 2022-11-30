# makefile for the Java implementation of the SmartDoor adapter

JAVAC = /usr/bin/javac
JAVA = /usr/bin/java

WEBSOCKET_JARS = ./jar/Java-WebSocket-1.5.2.jar
SLF4J_JARS = ./jar/slf4j-api-1.7.25.jar:./jar/slf4j-simple-1.7.25.jar
PROTOBUF_JARS = ./jar/protobuf-java-3.21.11.jar:./jar/pa-protobuf.jar
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
	AxiniProtobuf.class SmartDoorHandler.class SmartDoorConnection.class

adapter: $(TARGETS)
all: pa-protobuf.jar adapter

# ----- cleaning up

clean:
	rm -f *.class
	rm -f -r ./class
	rm -f VERSION.txt

very_clean: clean
	rm -f -r PluginAdapter

# ----- make ZIP for distribution

ZIP = /usr/bin/zip
ZIP_NAME = smartdoor-adapter-java.zip
ZIP_OPTIONS = -o -rp -q --exclude=*.git*

THIS_DATE=`date`
THIS_COMMIT=`git rev-parse --short HEAD`

VERSION.txt:
	@echo "Version of this smartdoor-java adapter: " > $@
	@echo "- created on: ${THIS_DATE}" >> $@
	@echo "- latest git revision: ${THIS_COMMIT}" >> $@

zip: very_clean pa-protobuf.jar adapter VERSION.txt
	pushd ..; $(ZIP) $(ZIP_OPTIONS) $(ZIP_NAME) smartdoor-java; popd
	rm -f VERSION.txt
