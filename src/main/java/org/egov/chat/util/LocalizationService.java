package org.egov.chat.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@PropertySource("classpath:xternal.properties")
@Service
public class LocalizationService {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TemplateMessageService templateMessageService;

    @Value("${localization.service.host}")
    private String localizationHost;
    @Value("${localization.service.search.path}")
    private String localizationSearchPath;
    @Value("${state.level.tenant.id}")
    private String stateLevelTenantId;
    @Value("#{'${supported.locales}'.split(',')}")
    private List<String> supportedLocales;

    private Map<String, Map<String,String>>  codeToMessageMapping;
    private Map<String, Map<String,String>>  messageToCodeMapping;

    @PostConstruct
    public void init() {
        codeToMessageMapping = new HashMap<>();
        messageToCodeMapping = new HashMap<>();

        for(String locale : supportedLocales) {
            UriComponentsBuilder uriComponents = UriComponentsBuilder.fromUriString(localizationHost + localizationSearchPath);
            uriComponents.queryParam("locale", locale);
            uriComponents.queryParam("tenantId", stateLevelTenantId);

            ObjectNode localizationData = restTemplate.postForObject(uriComponents.buildAndExpand().toUriString(),
                    objectMapper.createObjectNode(), ObjectNode.class);

            ArrayNode localizationMessages = (ArrayNode) localizationData.get("messages");

            initializeMaps(localizationMessages, locale);
        }
    }

    private void initializeMaps(ArrayNode localizationMessages, String locale) {
        Map<String, String> codeToMessageMappingForLocale = new HashMap<>();
        Map<String, String> messageToCodeMappingForLocale = new HashMap<>();

        for(JsonNode localizationMessage : localizationMessages) {
            String code = localizationMessage.get("code").asText();
            String message = localizationMessage.get("message").asText();

            codeToMessageMappingForLocale.put(code, message);
            messageToCodeMappingForLocale.put(message, code);
        }

        codeToMessageMapping.put(locale, codeToMessageMappingForLocale);
        messageToCodeMapping.put(locale, messageToCodeMappingForLocale);
    }

    public Map<String, String> fetchLocalizationData(String tenantId, String locale) {
        UriComponentsBuilder uriComponents = UriComponentsBuilder.fromUriString(localizationHost + localizationSearchPath);
        uriComponents.queryParam("locale", locale);
        uriComponents.queryParam("tenantId", tenantId);

        ObjectNode localizationData = restTemplate.postForObject(uriComponents.buildAndExpand().toUriString(),
                objectMapper.createObjectNode(), ObjectNode.class);

        ArrayNode localizationMessages = (ArrayNode) localizationData.get("messages");

        Map<String, String> codeToMessageMapping = new HashMap<>();
        for(JsonNode localizationMessage : localizationMessages) {
            String code = localizationMessage.get("code").asText();
            String message = localizationMessage.get("message").asText();

            codeToMessageMapping.put(code, message);
        }

        return codeToMessageMapping;
    }

    public List<String> getMessagesForCodes(ArrayNode localizationCodes, String locale) {
        List<String> values = new ArrayList<>();
        String tenantId = "";
        Map<String, String> codeToMessageMapping = null;
        for(JsonNode code : localizationCodes) {
            if(code.has("templateId"))
                values.add(templateMessageService.getMessageForTemplate(code, locale));
            else if(code.has("value"))
                values.add(code.get("value").asText());
            else {
                log.debug("Fetching Localization for : " + code.toString());
                String newTenantId = code.get("tenantId") != null ? code.get("tenantId").asText() : stateLevelTenantId;
                if (!newTenantId.equalsIgnoreCase(tenantId)) {
                    tenantId = newTenantId;
                    codeToMessageMapping = fetchLocalizationData(tenantId, locale);
                }
                values.add(codeToMessageMapping.get(code.get("code").asText()));
            }
        }
        log.debug("Localized values : " + values.toString());
        return values;
    }

    public String getMessageForCode(JsonNode localizationCode, String locale) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(localizationCode);
        return getMessagesForCodes(arrayNode, locale).get(0);
    }

    public String getMessageForCode(String code) {
        return getMessageForCode(code, "en_IN");
    }

    public String getMessageForCode(String code, String locale) {
        return codeToMessageMapping.get(locale).get(code);
    }

    @Deprecated
    public String getCodeForMessage(String message) {
        return getCodeForMessage(message, "en_IN");
    }

    @Deprecated
    public String getCodeForMessage(String message, String locale) {
        return messageToCodeMapping.get(locale).get(message);
    }
}
