package uk.gov.ons.census.fwmt.fulfilment.messaging.pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.time.Instant;
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
import uk.gov.ons.census.fwmt.fulfilment.messaging.model.ActionInstructionEvent;
import uk.gov.ons.census.fwmt.fulfilment.messaging.model.ActionInstructionHeader;

@Slf4j
@Service
@RequiredArgsConstructor
public class PubSubActionInstructionPublisher implements ActionInstructionPublisher {

  private final PubSubTemplate pubSubTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.messaging.destinations.actionInstruction:event_fieldwork_action-instruction}")
  private String actionInstructionTopic;

  @Value("${app.messaging.publish-timeout-millis:5000}")
  private long publishTimeoutMillis;

  @Override
  public void publish(PauseActionInstruction pauseActionInstruction, String correlationId) {
    String eventId = UUID.randomUUID().toString();
    ActionInstructionEvent event = ActionInstructionEvent.builder()
        .header(ActionInstructionHeader.builder()
            .eventId(eventId)
            .eventType("FIELDWORK_ACTION_INSTRUCTION")
            .schemaVersion("1.0")
            .occurredAt(Instant.now())
            .correlationId(correlationId)
            .build())
        .payload(pauseActionInstruction)
        .build();

    final String body;
    try {
      body = objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize action instruction event " + eventId, exception);
    }

    CompletableFuture<String> publishFuture = pubSubTemplate.publish(
        actionInstructionTopic,
        PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(body)).build());
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
}