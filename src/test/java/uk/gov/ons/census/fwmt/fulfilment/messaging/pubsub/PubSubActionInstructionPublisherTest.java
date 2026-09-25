package uk.gov.ons.census.fwmt.fulfilment.messaging.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PubsubMessage;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.fwmt.common.action.PauseActionInstruction;

@ExtendWith(MockitoExtension.class)
class PubSubActionInstructionPublisherTest {

  private static final String TOPIC = "event_fieldwork_action-instruction_internal";
  private static final String CASE_ID = "case-123";
  private static final String CORRELATION_ID = "correlation-123";
  private static final Instant PAUSE_FROM = Instant.parse("2026-09-21T10:15:30Z");

  @Mock
  private PubSubTemplate pubSubTemplate;

  private PubSubActionInstructionPublisher publisher;
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @BeforeEach
  void setUp() {
    publisher = new PubSubActionInstructionPublisher(pubSubTemplate, objectMapper);
    ReflectionTestUtils.setField(publisher, "actionInstructionInternalTopic", TOPIC);
    ReflectionTestUtils.setField(publisher, "publishTimeoutMillis", 5000L);
  }

  @Test
  void publishesFlatPausePayloadWithRmAdapterAttributes() throws Exception {
    when(pubSubTemplate.publish(eq(TOPIC), org.mockito.ArgumentMatchers.any(PubsubMessage.class)))
        .thenReturn(CompletableFuture.completedFuture("message-123"));

    publisher.publish(pauseInstruction(), CORRELATION_ID);

    ArgumentCaptor<PubsubMessage> messageCaptor = ArgumentCaptor.forClass(PubsubMessage.class);
    verify(pubSubTemplate).publish(eq(TOPIC), messageCaptor.capture());

    PubsubMessage message = messageCaptor.getValue();
    JsonNode payload = objectMapper.readTree(message.getData().toStringUtf8());

    assertThat(payload.get("actionInstruction").asText()).isEqualTo("PAUSE");
    assertThat(payload.get("surveyName").asText()).isEqualTo("CENSUS");
    assertThat(payload.get("caseId").asText()).isEqualTo(CASE_ID);
    assertThat(payload.get("pauseCode").asText()).isEqualTo("P_OR_H1");
    assertThat(payload.has("header")).isFalse();
    assertThat(payload.has("payload")).isFalse();

    assertThat(message.getAttributesOrDefault("eventId", "")).isNotBlank();
    assertThat(message.getAttributesOrDefault("correlationId", "")).isEqualTo(CORRELATION_ID);
    assertThat(message.getAttributesOrDefault("caseId", "")).isEqualTo(CASE_ID);
    assertThat(message.getAttributesOrDefault("eventType", "")).isEqualTo("FIELDWORK_ACTION_INSTRUCTION");
    assertThat(message.getAttributesOrDefault("schemaVersion", "")).isEqualTo("1.0");
    assertThat(message.getAttributesOrDefault("occurredAt", "")).isEqualTo(PAUSE_FROM.toString());
  }

  private PauseActionInstruction pauseInstruction() {
    return PauseActionInstruction.builder()
        .actionInstruction("PAUSE")
        .surveyName("CENSUS")
        .caseId(CASE_ID)
        .addressType("HH")
        .addressLevel("U")
        .pauseCode("P_OR_H1")
        .pauseFrom(PAUSE_FROM)
        .build();
  }
}
