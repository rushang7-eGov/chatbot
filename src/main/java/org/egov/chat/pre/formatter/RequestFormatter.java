package org.egov.chat.pre.formatter;

import com.fasterxml.jackson.databind.JsonNode;

public interface RequestFormatter {

    public String getStreamName();

    public boolean isValid(JsonNode inputRequest);

    public JsonNode getTransformedRequest(JsonNode inputRequest);

    public void startRequestFormatterStream(String inputTopic, String outputTopic, String errorTopic);

}
