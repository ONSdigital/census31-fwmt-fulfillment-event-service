package uk.gov.ons.census.fwmt.fulfilment.messaging.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionInstructionHeader {

  private String eventId;
  private String eventType;
  private String schemaVersion;
  private Instant occurredAt;
  private String correlationId;
}