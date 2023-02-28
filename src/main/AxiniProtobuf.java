// Copyright 2022 Axini B.V. https://www.axini.com, see: LICENSE.txt.

import java.util.*;

import com.google.protobuf.ByteString;

import PluginAdapter.Api.MessageOuterClass.*;
import PluginAdapter.Api.AnnouncementOuterClass.*;
import PluginAdapter.Api.ConfigurationOuterClass.*;
import PluginAdapter.Api.LabelOuterClass.*;
import PluginAdapter.Api.LabelOuterClass.Label.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// AxiniProtobuf: helper class to construct Protobuf messages.
// This class is not yet complete, but more than enough for SmartDoor adapter.
// The namegiving for the various create* methods is subject for change.
public class AxiniProtobuf {
    private static Logger logger = LoggerFactory.getLogger(AxiniProtobuf.class);

    public static final List<Label>
        EMPTY_LABEL_LIST = Collections.<Label>emptyList();

    public static final List<Label.Parameter>
        EMPTY_PARAM_LIST = Collections.<Label.Parameter>emptyList();

    public static final List<Configuration.Item>
        EMPTY_ITEM_LIST = Collections.<Configuration.Item>emptyList();

    // Current time in nano seconds since EPOCH.
    // TODO: try to also add nano seconds.
    public static long timestamp() {
        return System.currentTimeMillis() * 1000 * 1000;
    }

    // ----- Label

    // Create a stimulus Label with parameters.
    public static Label createStimulus(String name, String channel,
                                       List<Label.Parameter> params) {
        return createLabel(name, channel, LabelType.STIMULUS, params);
    }

    // Create a stimulus Label with *no* parameters.
    public static Label createStimulus(String name, String channel) {
        return createLabel(name, channel, LabelType.STIMULUS, EMPTY_PARAM_LIST);
    }

    // Create a response Label with parameters.
    public static Label createResponse(String name, String channel,
                                       List<Label.Parameter> params) {
        return createLabel(name, channel, LabelType.RESPONSE, params);
    }

    // Create a response Label with *no* parameters.
    public static Label createResponse(String name, String channel) {
        return createLabel(name, channel, LabelType.RESPONSE, EMPTY_PARAM_LIST);
    }

    // Create Protobuf Label.
    public static Label createLabel(String name, String channel, LabelType type,
                                    List<Label.Parameter> params) {
        Label.Builder builder = Label.newBuilder()
            .setType(type)
            .setLabel(name)
            .setChannel(channel);

        for (Label.Parameter param : safe_p(params))
            builder.addParameters(param);

        Label label = builder.build();
        return label;
    }

    // Create Protobuf Label on the basis of some other Label.
    public static Label createLabel(Label label, ByteString physicalLabel,
                                    long timestamp) {
        return label.toBuilder()
            .setPhysicalLabel(physicalLabel)
            .setTimestamp(timestamp)
            .build();
    }

    // Create Protobuf Label with correlation id on the basis of some other Label.
    public static Label createLabel(Label label, ByteString physicalLabel,
                                    long timestamp, long correlationId) {
        return createLabel(label, physicalLabel, timestamp)
            .toBuilder()
            .setCorrelationId(correlationId)
            .build();
    }

    // ----- Parameter and Value

    public static Label.Parameter createParameter(String name,
                                                  Label.Parameter.Value value) {
        return Label.Parameter.newBuilder()
            .setName(name)
            .setValue(value)
            .build();
    }

    // Perhaps we could use overloading here to only have a single createValue!
    // ... but this will not work for Date and Time.

    public static Label.Parameter.Value createStringValue(String value) {
        return Label.Parameter.Value.newBuilder()
            .setString(value)
            .build();
    }

    public static Label.Parameter.Value createIntValue(int value) {
        return Label.Parameter.Value.newBuilder()
            .setInteger(value)
            .build();
    }

    public static Label.Parameter.Value createDecimalValue(double value) {
        return Label.Parameter.Value.newBuilder()
            .setDecimal(value)
            .build();
    }

    public static Label.Parameter.Value createBooleanValue(boolean value) {
        return Label.Parameter.Value.newBuilder()
            .setBoolean(value)
            .build();
    }

    // FIXME: or should we use some Date representation here?
    public static Label.Parameter.Value createDateValue(long value) {
        return Label.Parameter.Value.newBuilder()
            .setDate(value)
            .build();
    }

    // FIXME: or should we use some Date representation here?
    public static Label.Parameter.Value createTimeValue(long value) {
        return Label.Parameter.Value.newBuilder()
            .setTime(value)
            .build();
    }

    // TODO: add
    // - createArrayValue
    // - createStructValue
    // - createHashValue
    // But these are not needed for the SmartDoor exercise.

    // ------ Configuration

    public static Configuration.Item createItem(String key, String description,
                                                int value) {
        return Configuration.Item.newBuilder()
            .setKey(key)
            .setDescription(description)
            .setInteger(value)
            .build();
   }

    public static Configuration.Item createItem(String key, String description,
                                                String value) {
        return Configuration.Item.newBuilder()
            .setKey(key)
            .setDescription(description)
            .setString(value)
            .build();
   }

    public static Configuration.Item createItem(String key, String description,
                                                float value) {
        return Configuration.Item.newBuilder()
            .setKey(key)
            .setDescription(description)
            .setFloat(value)
            .build();
   }

    public static Configuration.Item createItem(String key, String description,
                                                boolean value) {
        return Configuration.Item.newBuilder()
            .setKey(key)
            .setDescription(description)
            .setBoolean(value)
            .build();
   }

    public static Configuration createConfiguration(List<Configuration.Item> items) {
        Configuration.Builder builder = Configuration.newBuilder();

        for (Configuration.Item item : safe_i(items))
            builder.addItems(item);

        return builder.build();
    }

    public static String getStringFromConfig(Configuration config, String key) {
        List<Configuration.Item> items = config.getItemsList();
        for (Configuration.Item item : items)
            if (item.getKey().equals(key) && item.hasString())
                return item.getString();

        // TODO: we should proably throw an Exception here.
        return "ERROR: key '" + key + "' is not key of String value";
    }

    public static long getIntegerFromConfig(Configuration config, String key) {
        List<Configuration.Item> items = config.getItemsList();
        for (Configuration.Item item : items)
            if (item.getKey().equals(key) && item.hasInteger())
                return item.getInteger();

        // TODO: we should probably throw an Exception here.
        return 0;
    }

    // ------ Message

    // Create Protobuf Message: Label.
    public static Message createMsgLabel(Label label) {
        return Message.newBuilder()
            .setLabel(label)
            .build();
    }

    // Create Protobuf Message: Ready.
    public static Message createMsgReady() {
        Message.Ready ready = Message.Ready.newBuilder().build();
        return Message.newBuilder()
            .setReady(ready)
            .build();
    }

    // Create Protobuf Message: Error.
    public static Message createMsgError(String message) {
        Message.Error error = Message.Error.newBuilder()
            .setMessage(message)
            .build();
        return Message.newBuilder()
            .setError(error)
            .build();
    }

    // Create Protobuf Message: Announcement.
    public static Message createMsgAnnouncement(String name,
            List<Label> supportedLabels, Configuration configuration) {

        Announcement.Builder builder = Announcement.newBuilder()
            .setName(name)
            .setConfiguration(configuration);

        for (Label label : safe_l(supportedLabels))
            builder.addLabels(label);

        Announcement announcement = builder.build();

        return Message.newBuilder()
            .setAnnouncement(announcement)
            .build();
    }

    // ----- private methods

    // We cannot overload the methods "safe_l" and "safe_p" to a single "safe"
    // due to the limitations of Java generics.

    private static List<Label> safe_l(List<Label> list) {
        return list == null ? EMPTY_LABEL_LIST : list;
    }

    private static List<Label.Parameter> safe_p(List<Label.Parameter> list) {
        return list == null ? EMPTY_PARAM_LIST : list;
    }

    private static List<Configuration.Item> safe_i(List<Configuration.Item> list) {
        return list == null ? EMPTY_ITEM_LIST : list;
    }
}
