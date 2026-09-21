package uk.gov.ons.census.fwmt.fulfilment.messaging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.census.fwmt.common.action.PauseActionInstruction;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionInstructionEvent {

  private ActionInstructionHeader header;
  private PauseActionInstruction payload;
}