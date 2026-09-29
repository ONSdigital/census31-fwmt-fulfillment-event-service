package uk.gov.ons.census.fwmt.fulfilment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.pubsub.v1.PubsubMessage;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.data.fulfillment.dto.PauseFulfilmentRequest;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.fulfilment.lookup.ChannelLookup;
import uk.gov.ons.census.fwmt.fulfilment.messaging.model.FulfilmentRequestEvent;
import uk.gov.ons.census.fwmt.fulfilment.messaging.model.FulfilmentRequestHeader;

@Slf4j
@Component
@RequiredArgsConstructor
public class FulfilmentPausePubSubMessageHandler {

  private static final String RECEIVED_FULFILMENT = "RECEIVED_FULFILMENT";
  private static final String FAILED_CHANNEL_MATCH = "FAILED_CHANNEL_MATCH";

  private static final String EXPECTED_TOPIC = "event_fulfilment-request";
  private static final String EXPECTED_MESSAGE_TYPE = "FULFILMENT_REQUEST";

  private final FulfilmentService fulfilmentService;
  private final GatewayEventManager eventManager;
  private final ChannelLookup channelLookup;
  private final ObjectMapper jsonMapper;

  public void handle(PubsubMessage message) throws Exception {
    String payload = message.getData().toString(StandardCharsets.UTF_8);
    FulfilmentRequestEvent event = jsonMapper.readValue(payload, FulfilmentRequestEvent.class);
    validate(event);

    FulfilmentRequestHeader header = event.getHeader();
    PauseFulfilmentRequest request = event.getPayload().getFulfilmentRequest();
    OffsetDateTime receivedMessageTime = header.dateTimeAsOffsetDateTime();
    String channelSent = header.getChannel();
    String channelId = channelLookup.getLookup(channelSent);

    String fulfilmentProductCode = "Fulfilment Product Code";
    String caseId = "Case ID";

    if (channelId != null) {
      eventManager.triggerEvent(
          request.getCaseId(), RECEIVED_FULFILMENT, caseId, request.getCaseId(),
          "Individual CaseId", request.getIndividualCaseId(), fulfilmentProductCode,
          request.getFulfilmentCode());
      fulfilmentService.processPauseCase(event, receivedMessageTime.toInstant(),
          header.getCorrelationId() == null ? null : header.getCorrelationId().toString());
    } else {
      eventManager.triggerEvent(
          request.getCaseId(), FAILED_CHANNEL_MATCH, "Pause outcome", event.toString(),
          "Channel", channelSent, fulfilmentProductCode, request.getFulfilmentCode());
      throw new IllegalArgumentException("Unsupported fulfilment event channel: " + channelSent);
    }
  }

  private void validate(FulfilmentRequestEvent event) {
    if (event == null || event.getHeader() == null || event.getPayload() == null
        || event.getPayload().getFulfilmentRequest() == null) {
      throw new IllegalArgumentException("Fulfilment request event must contain header and payload.fulfilmentRequest");
    }
    FulfilmentRequestHeader header = event.getHeader();
    if (!EXPECTED_TOPIC.equals(header.getTopic())) {
      throw new IllegalArgumentException("Unexpected fulfilment event topic: " + header.getTopic());
    }
    if (!EXPECTED_MESSAGE_TYPE.equals(header.getMessageType())) {
      throw new IllegalArgumentException("Unexpected fulfilment event message type: " + header.getMessageType());
    }
    if (header.getDateTime() == null || header.getMessageId() == null) {
      throw new IllegalArgumentException("Fulfilment event requires dateTime and messageId");
    }
  }
}

