package org.egov.chat.service.valuefetch;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public interface ExternalValueFetcher {

    public List<String> getValues(ObjectNode params);

    public String getCodeForValue(ObjectNode params, String value);

    public String createExternalLinkForParams(ObjectNode params);

}
