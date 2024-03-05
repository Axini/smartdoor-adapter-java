PROTOC   = /usr/local/bin/protoc
JAVA_OUT = --java_out=./..

protobuf_java:
	$(PROTOC) $(JAVA_OUT) announcement.proto
	$(PROTOC) $(JAVA_OUT) configuration.proto
	$(PROTOC) $(JAVA_OUT) label.proto
	$(PROTOC) $(JAVA_OUT) message.proto
