package org.egov.chat.service.streams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.egov.chat.config.JsonPointerNameConstants;
import org.egov.chat.config.KafkaStreamsConfig;
import org.egov.chat.repository.MessageRepository;
import org.egov.chat.service.restendpoint.RestAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
public class CreateEndpointStream extends CreateStream {

    @Autowired
    private KafkaStreamsConfig kafkaStreamsConfig;

    @Autowired
    private RestAPI restAPI;

    @Autowired
    private MessageRepository messageRepository;

    private String continueMessage = "To continue chatting send \"Hi\"";

    public void createEndpointStream(JsonNode config, String inputTopic, String sendMessageTopic) {

        String streamName = config.get("name").asText() + "-answer";

        Properties streamConfiguration = kafkaStreamsConfig.getDefaultStreamConfiguration();
        streamConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, streamName);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, JsonNode> answerKStream = builder.stream(inputTopic, Consumed.with(Serdes.String(),
                kafkaStreamsConfig.getJsonSerde()));

        answerKStream.flatMapValues(chatNode -> {
            try {
                ObjectNode responseMessage = restAPI.makeRestEndpointCall(config, chatNode);

                ((ObjectNode) chatNode).set("response", responseMessage);

                String conversationId = chatNode.at(JsonPointerNameConstants.conversationId).asText();
                conversationStateRepository.markConversationInactive(conversationId);

                List<JsonNode> nodes = new ArrayList<>();
                nodes.add(chatNode);

                nodes.add(createContinueMessageNode(chatNode));

                return nodes;
            } catch (Exception e) {
                log.error(e.getMessage());
                return Collections.emptyList();
            }
        }).to(sendMessageTopic, Produced.with(Serdes.String(), kafkaStreamsConfig.getJsonSerde()));

        kafkaStreamsConfig.startStream(builder, streamConfiguration);

        log.info("Endpoint Stream started : " + streamName + ", from : " + inputTopic + ", to : " + sendMessageTopic);
    }

    private JsonNode createContinueMessageNode(JsonNode chatNode) {
        JsonNode continueMessageNode = chatNode.deepCopy();

        ObjectNode response = (ObjectNode) continueMessageNode.get("response");
        response.put("type", "text");
        response.put("text", continueMessage);

        return continueMessageNode;
    }

}
