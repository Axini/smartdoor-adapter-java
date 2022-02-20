// Copyright 2022 Axini B.V. https://www.axini.com, see: LICENSE.txt

import java.util.*;

import com.google.protobuf.ByteString;

import PluginAdapter.Api.LabelOuterClass.*;
import PluginAdapter.Api.LabelOuterClass.Label.*;
import PluginAdapter.Api.ConfigurationOuterClass.*;

// Domain specific adapter component. The class Handler contains all the
// specific adapter methods which need to be implemented for a specific SUT.
//
// When a response is received from the SUT, the adapter_core.send_response
// method should be called.
//
// Subclasses must provide all methods defined in this abstract superclass.
//
// TODO: the methods start, stop, reset and stimulate could fail; we should
// add exception declarations to these methods.
public abstract class Handler {
    protected AdapterCore   adapterCore;
    protected Configuration configuration;

    public Handler() {
        adapterCore = null;
        configuration = defaultConfiguration();
    }

    public void registerAdapterCore(AdapterCore adapterCore) {
        this.adapterCore = adapterCore;
    }

    // Set the configuration of this Handler.
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    // The configuration for the plugin adapter.
    public Configuration getConfiguration() {
        return configuration;
    }

    // The default configuration for this plugin adapter.
    public abstract Configuration defaultConfiguration();

    // Prepare to start testing.
    public abstract void start();

    // Stop testing.
    public abstract void stop();

    // Prepare for the next test case.
    public abstract void reset();

    // Stimulate the System Under Test and return the physical label.
    public abstract ByteString stimulate(Label stimulus);

    // The labels supported by the plugin adapter.
    public abstract List<Label> getSupportedLabels();
}
