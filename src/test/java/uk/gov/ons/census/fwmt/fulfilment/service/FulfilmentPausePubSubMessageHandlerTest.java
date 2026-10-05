package uk.gov.ons.census.fwmt.fulfilment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.fulfilment.lookup.ChannelLookup;

@ExtendWith(MockitoExtension.class)
class FulfilmentPausePubSubMessageHandlerTest {

  @Mock
  private FulfilmentService fulfilmentService;

  @Mock
  private GatewayEventManager eventManager;

  private FulfilmentPausePubSubMessageHandler handler;
  private String eventJson;

  @BeforeEach
  void setUp() throws Exception {
    ChannelLookup channels = new ChannelLookup();
    channels.add("CC", "CC");
    channels.add("AD", "AD");
    handler = new FulfilmentPausePubSubMessageHandler(
        fulfilmentService, eventManager, channels, new ObjectMapper().findAndRegisterModules());
    eventJson = Files.readString(Path.of("src/test/resources/fixtures/fulfilment-request-event.json"));
  }

  @Test
  void acceptsEventDictionaryEventAndUsesHeaderInstant() throws Exception {
    handler.handle(message(eventJson));

    verify(fulfilmentService).processPauseCase(any(), eq(java.time.Instant.parse("2026-09-21T10:15:30Z")), eq("2d91fec8-4f3a-4ce0-ad4f-165815bd5ec4"));
  }

  @Test
  void rejectsMessagesWithUnexpectedTopic() {
    String invalidJson = eventJson.replace("event_fulfilment-request", "events");

    assertThatThrownBy(() -> handler.handle(message(invalidJson)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unexpected fulfilment event topic");
  }

  @Test
  void rejectsUnsupportedChannelsWithoutAcknowledging() {
    String invalidJson = eventJson.replace("\"channel\": \"CC\"", "\"channel\": \"FIELD\"");

    assertThatThrownBy(() -> handler.handle(message(invalidJson)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported fulfilment event channel");
  }

  private PubsubMessage message(String json) {
    return PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(json)).build();
  }
}