package uk.gov.ons.census.fwmt.fulfilment.messaging;

import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;

/**
 * Port for republishing pause instructions to the RM Field lane.
 */
public interface RmFieldPausePublisher {

  void pausePublish(FwmtActionInstruction pauseActionInstruction);
}
