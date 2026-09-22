package uk.gov.ons.census.fwmt.fulfilment.messaging.pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.common.action.PauseActionInstruction;
import uk.gov.ons.census.fwmt.fulfilment.messaging.ActionInstructionPublisher;

@Slf4j
@Service
@RequiredArgsConstructor
public class PubSubActionInstructionPublisher implements ActionInstructionPublisher {

  private final PubSubTemplate pubSubTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.messaging.destinations.actionInstructionInternal:event_fieldwork_action-instruction_internal}")
  private String actionInstructionInternalTopic;

  @Value("${app.messaging.publish-timeout-millis:5000}")
  private long publishTimeoutMillis;

  @Override
  public void publish(PauseActionInstruction pauseActionInstruction, String correlationId) {
    String eventId = UUID.randomUUID().toString();

    final String body;
    try {
      body = objectMapper.writeValueAsString(pauseActionInstruction);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize action instruction " + eventId, exception);
    }

    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("eventId", eventId);
    attributes.put("correlationId", correlationId == null ? "" : correlationId);
    attributes.put("caseId", pauseActionInstruction.getCaseId());
    attributes.put("eventType", "FIELDWORK_ACTION_INSTRUCTION");
    attributes.put("schemaVersion", "1.0");
    attributes.put("occurredAt", occurredAt(pauseActionInstruction));

    CompletableFuture<String> publishFuture = pubSubTemplate.publish(
      actionInstructionInternalTopic,
        PubsubMessage.newBuilder()
            .setData(ByteString.copyFromUtf8(body))
            .putAllAttributes(attributes)
            .build());
    try {
      String messageId = publishFuture.get(publishTimeoutMillis, TimeUnit.MILLISECONDS);
      log.info("Published action instruction eventId={} correlationId={} messageId={}",
          eventId, correlationId, messageId);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while publishing action instruction " + eventId, exception);
    } catch (TimeoutException | java.util.concurrent.ExecutionException exception) {
      throw new IllegalStateException("Unable to publish action instruction " + eventId, exception);
    }
  }

  private String occurredAt(PauseActionInstruction pauseActionInstruction) {
    Instant pauseFrom = pauseActionInstruction.getPauseFrom();
    return pauseFrom == null ? Instant.now().toString() : pauseFrom.toString();
  }
}