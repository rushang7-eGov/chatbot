package org.egov.chat.streams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.kstream.Produced;
import org.egov.chat.service.AnswerExtractor;
import org.egov.chat.service.AnswerStore;
import org.egov.chat.config.graph.TopicNameGetter;
import org.egov.chat.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
@Slf4j
public class CreateBranchStream extends CreateStream {

    @Autowired
    private Validator validator;
    @Autowired
    private AnswerExtractor answerExtractor;
    @Autowired
    private AnswerStore answerStore;
    @Autowired
    private TopicNameGetter topicNameGetter;

    public void createEvaluateAnswerStreamForConfig(JsonNode config, String answerInputTopic, String questionTopic) {

        String streamName = config.get("name").asText() + "-answer";

        List<String> branchNames = getBranchNames(config);
        List<Predicate<String, JsonNode>> predicates = makePredicatesForBranches(branchNames, config);
        predicates.add(0, (s, chatNode) -> ! validator.isValid(config, chatNode));                  //First check invalid

        Properties streamConfiguration = (Properties) defaultStreamConfiguration.clone();
        streamConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, streamName);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, JsonNode> answerKStream = builder.stream(answerInputTopic, Consumed.with(Serdes.String(), jsonSerde));
        KStream<String, JsonNode>[] kStreamBranches = answerKStream.branch(predicates.toArray(new Predicate[predicates.size()]));

        kStreamBranches[0].mapValues(value -> value).to(questionTopic, Produced.with(Serdes.String(), jsonSerde));

        for(int i = 1; i < kStreamBranches.length; i++) {
            String targetNode = config.get(branchNames.get(i - 1)).asText();
            String targetTopicName = topicNameGetter.getQuestionTopicNameForNode(targetNode);
            kStreamBranches[i].mapValues(chatNode -> {
                chatNode = answerExtractor.extractAnswer(config, chatNode);
                answerStore.saveAnswer(config, chatNode);
                return chatNode;
            }).to(targetTopicName, Produced.with(Serdes.String(), jsonSerde));
        }

        startStream(builder, streamConfiguration);

        log.info("Branch Stream started : " + streamName + ", from : " + answerInputTopic);
    }

    private List<String> getBranchNames(JsonNode config) {
        List<String> branchNames = new ArrayList<>();
        ArrayNode arrayNode = (ArrayNode) config.get("values") ;
        for(JsonNode jsonNode : arrayNode) {
            branchNames.add(jsonNode.asText());
        }
        return branchNames;
    }

    private List<Predicate<String, JsonNode>> makePredicatesForBranches(List<String> branchNames, JsonNode config) {
        List<Predicate<String, JsonNode>> predicates = new ArrayList<>();
        for(String branchName : branchNames) {
            Predicate<String, JsonNode> predicate = (s, chatNode) -> {
                chatNode = answerExtractor.extractAnswer(config, chatNode);
                String answer = chatNode.get("answer").asText();
                if(answer.equalsIgnoreCase(branchName)) {
                    return true;
                }
                return false;
            };
            predicates.add(predicate);
        }
        return predicates;
    }

}
