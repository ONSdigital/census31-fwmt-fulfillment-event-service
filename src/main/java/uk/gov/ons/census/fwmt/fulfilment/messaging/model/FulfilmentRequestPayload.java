package uk.gov.ons.census.fwmt.fulfilment.messaging.model;

import lombok.Data;
import uk.gov.ons.census.fwmt.common.data.fulfillment.dto.PauseFulfilmentRequest;

@Data
public class FulfilmentRequestPayload {

  private PauseFulfilmentRequest fulfilmentRequest;
}