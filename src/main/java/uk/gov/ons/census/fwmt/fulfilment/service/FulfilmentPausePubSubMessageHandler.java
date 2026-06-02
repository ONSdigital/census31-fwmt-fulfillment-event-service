package uk.gov.ons.census.fwmt.fulfilment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.pubsub.v1.PubsubMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.data.fulfillment.dto.PauseOutcome;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.fulfilment.lookup.ChannelLookup;

@Slf4j
@Component
@RequiredArgsConstructor
public class FulfilmentPausePubSubMessageHandler {

  private static final String RECEIVED_FULFILMENT = "RECEIVED_FULFILMENT";
  private static final String FAILED_CHANNEL_MATCH = "FAILED_CHANNEL_MATCH";

  private static final String ROUTING_KEY_ATTR = "routingKey";
  private static final String FULFILMENT_ROUTING_KEY = "event.fulfilment.request";
  private static final String TIMESTAMP_ATTR = "timestamp";

  private final FulfilmentService fulfilmentService;
  private final GatewayEventManager eventManager;
  private final ChannelLookup channelLookup;
  private final ObjectMapper jsonMapper;

  /**
   * @return true if this message was a fulfilment request and was processed (or failed processing);
   *     false if the message is for a different routing key and was ignored.
   */
  public boolean handle(PubsubMessage message) throws Exception {
    String routingKey = message.getAttributesOrDefault(ROUTING_KEY_ATTR, "");
    if (!FULFILMENT_ROUTING_KEY.equals(routingKey)) {
      return false;
    }

    String pausePayload = message.getData().toString(StandardCharsets.UTF_8);
    Instant receivedMessageTime = parseTimestamp(message);

    try {
      PauseOutcome pauseOutcome = jsonMapper.readValue(pausePayload, PauseOutcome.class);

      String channelSent = pauseOutcome.getEvent().getChannel();
      String channelId = channelLookup.getLookup(channelSent);

      String fulfilmentProductCode = "Fulfilment Product Code";
      String caseId = "Case ID";

      if (channelId != null) {
        eventManager.triggerEvent(
            pauseOutcome.getPayload().getFulfilmentRequest().getCaseId(),
            RECEIVED_FULFILMENT,
            caseId,
            pauseOutcome.getPayload().getFulfilmentRequest().getCaseId(),
            "Individual CaseId",
            pauseOutcome.getPayload().getFulfilmentRequest().getIndividualCaseId(),
            fulfilmentProductCode,
            pauseOutcome.getPayload().getFulfilmentRequest().getFulfilmentCode());
        fulfilmentService.processPauseCase(pauseOutcome, receivedMessageTime);
      } else {
        eventManager.triggerEvent(
            pauseOutcome.getPayload().getFulfilmentRequest().getCaseId(),
            FAILED_CHANNEL_MATCH,
            "Pause outcome",
            pauseOutcome.toString(),
            "Channel",
            channelSent,
            fulfilmentProductCode,
            pauseOutcome.getPayload().getFulfilmentRequest().getFulfilmentCode());
      }
      return true;
    } catch (JsonProcessingException e) {
      eventManager.triggerErrorEvent(
          this.getClass(), "Unable to convert message to json", pausePayload, e.getMessage());
      throw e;
    }
  }

  private Instant parseTimestamp(PubsubMessage message) {
    String ts = message.getAttributesOrDefault(TIMESTAMP_ATTR, "");
    try {
      return Instant.ofEpochMilli(Long.parseLong(ts));
    } catch (RuntimeException ex) {
      return Instant.now();
    }
  }
}

