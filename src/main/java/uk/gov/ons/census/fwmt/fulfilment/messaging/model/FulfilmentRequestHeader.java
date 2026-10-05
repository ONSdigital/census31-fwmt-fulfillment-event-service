package uk.gov.ons.census.fwmt.fulfilment.messaging.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class FulfilmentRequestHeader {

  private String topic;
  private String messageType;
  private String source;
  private String channel;
  private String version;
  private String dateTime;
  private UUID messageId;
  private UUID correlationId;

  public OffsetDateTime dateTimeAsOffsetDateTime() {
    return OffsetDateTime.parse(dateTime);
  }
}