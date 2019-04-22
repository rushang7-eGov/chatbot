package org.egov.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.egov.chat.config.graph.TopicNameGetter;
import org.egov.chat.repository.ConversationStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InputSegregator {

    private String rootQuestionTopic = "root-question";

    @Autowired
    private ConversationStateRepository conversationStateRepository;
    @Autowired
    private TopicNameGetter topicNameGetter;
    @Autowired
    private KafkaTemplate<String, JsonNode> kafkaTemplate;

    public void segregateAnswer(ConsumerRecord<String, JsonNode> consumerRecord) {
        JsonNode chatNode = consumerRecord.value();
        String conversationId = chatNode.get("conversationId").asText();

        String activeNodeId = conversationStateRepository.getActiveNodeIdForConversation(conversationId);

        String topic = getOutputTopcName(activeNodeId);

        kafkaTemplate.send(topic, consumerRecord.key(), chatNode);
    }

    private String getOutputTopcName(String activeNodeId) {
        String topic;
        if(activeNodeId == null)
            topic = rootQuestionTopic;
        else
            topic = topicNameGetter.getAnswerInputTopicNameForNode(activeNodeId);
        return topic;
    }

}
