package org.inaetics.pubsub.api;

public class Constants {

    //scope & topic property which can be used on the service properties/filter
    public static final String TOPIC_KEY = "topic";
    public static final String SCOPE_KEY = "scope";

    //config property, which can be used to select a pubsub config in the topic properties.
    public static final String TOPIC_CONFIG_KEY = "config";

    //config for requested qos support (topic properties)
    public static final String TOPIC_QOS_KEY = "qos";
    public static final String TOPIC_SAMPLE_QOS_VALUE = "sample";
    public static final String TOPIC_CONTROL_QOS_VALUE = "control";

    //config for serializer setting (topic properties)
    public static final String TOPIC_SERIALIZER_KEY = "pubsub.serializer";
}
