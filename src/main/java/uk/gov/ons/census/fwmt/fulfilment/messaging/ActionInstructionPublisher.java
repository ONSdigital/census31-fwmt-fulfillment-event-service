package uk.gov.ons.census.fwmt.fulfilment.messaging;

import java.time.Instant;
import uk.gov.ons.census.fwmt.common.dto.rm.SuperInstruction;

public interface ActionInstructionPublisher {

  /** Publishes an instruction with a required event occurrence time. */
  void publish(SuperInstruction instruction, Instant occurredAt, String correlationId);
}