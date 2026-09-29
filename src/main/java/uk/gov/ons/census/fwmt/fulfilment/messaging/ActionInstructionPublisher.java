package uk.gov.ons.census.fwmt.fulfilment.messaging;

import uk.gov.ons.census.fwmt.common.dto.fwmt.PauseActionInstruction;

public interface ActionInstructionPublisher {

  void publish(PauseActionInstruction pauseActionInstruction, String correlationId);
}