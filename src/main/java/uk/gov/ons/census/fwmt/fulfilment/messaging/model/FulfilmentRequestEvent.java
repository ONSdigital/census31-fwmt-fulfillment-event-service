package uk.gov.ons.census.fwmt.fulfilment.messaging.model;

import lombok.Data;

@Data
public class FulfilmentRequestEvent {

  private FulfilmentRequestHeader header;
  private FulfilmentRequestPayload payload;
}