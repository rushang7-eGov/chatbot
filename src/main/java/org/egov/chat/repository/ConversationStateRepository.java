package org.egov.chat.repository;

import com.fasterxml.jackson.databind.JsonNode;
import org.egov.chat.models.ConversationState;
import org.egov.chat.repository.querybuilder.ConversationStateQueryBuilder;
import org.egov.chat.repository.rowmapper.ConversationStateResultSetExtractor;
import org.egov.tracer.model.CustomException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.Objects;

@Repository
public class ConversationStateRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private ConversationStateResultSetExtractor conversationStateResultSetExtractor;

    private static final String insertNewConversationQuery = "INSERT INTO eg_chat_conversation_state " +
            "(conversation_id, user_id, active) VALUES (?, ?, ?)";

    private static final String updateConversationStateQuery = "UPDATE eg_chat_conversation_state SET active_node_id=? " +
            "question_details=? WHERE conversation_id=?";

    private static final String updateActiveStateForConversationQuery = "UPDATE eg_chat_conversation_state SET " +
            "active=FALSE WHERE conversation_id=?";

    private static final String selectActiveNodeIdForConversationStateQuery = "SELECT (active_node_id " +
            ") FROM eg_chat_conversation_state WHERE conversation_id=?";

    private static final String selectConversationStateForIdQuery = "SELECT * FROM eg_chat_conversation_state WHERE " +
            "conversation_id=?";

    private static final String selectConversationStateForUserIdQuery = "SELECT * FROM eg_chat_conversation_state WHERE " +
            "user_id=? AND active=TRUE";

    public int insertNewConversation(ConversationState conversationState) {
        return jdbcTemplate.update(insertNewConversationQuery,
                conversationState.getConversationId(),
                conversationState.getUserId(),
                conversationState.isActive());
    }

    public int updateConversationStateForId(String activeNodeId, JsonNode questionDetails, String conversationId) {
        return namedParameterJdbcTemplate.update(ConversationStateQueryBuilder.UPDATE_CONVERSATION_STATE_QUERY,
                ConversationStateQueryBuilder.getParametersForConversationStateUpdate(activeNodeId, questionDetails, conversationId));
//        return jdbcTemplate.update(updateConversationStateQuery, activeNodeId, questionDetails, conversationId);
    }

    public int markConversationInactive(String conversationId) {
        return jdbcTemplate.update(updateActiveStateForConversationQuery, conversationId);
    }

    public String getActiveNodeIdForConversation(String conversationId) {
        return  (jdbcTemplate.queryForObject(selectActiveNodeIdForConversationStateQuery, new Object[] { conversationId },
                String.class));
    }

    public ConversationState getConversationStateForUserId(String userId) {
        try {
            return jdbcTemplate.query(selectConversationStateForUserIdQuery, new Object[]{ userId },
                    conversationStateResultSetExtractor);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public ConversationState getConversationStateForId(String conversationId) {
        return jdbcTemplate.query(selectConversationStateForIdQuery, new Object[] { conversationId },
                conversationStateResultSetExtractor);
    }


}
