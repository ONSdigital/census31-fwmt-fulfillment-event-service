package uk.gov.ons.census.fwmt.fulfilment.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.fwmt.common.data.fulfillment.dto.PauseFulfilmentRequest;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.fulfilment.data.GatewayCaseRecord;
import uk.gov.ons.census.fwmt.fulfilment.lookup.PauseRulesLookup;
import uk.gov.ons.census.fwmt.fulfilment.messaging.ActionInstructionPublisher;
import uk.gov.ons.census.fwmt.fulfilment.messaging.model.FulfilmentRequestEvent;
import uk.gov.ons.census.fwmt.fulfilment.messaging.model.FulfilmentRequestPayload;

@ExtendWith(MockitoExtension.class)
class FulfilmentServiceTest {

  private static final String CASE_ID = "bd6345af-d706-43d3-a13b-8c549e081a76";

  @Mock
  private PauseRulesLookup pauseRulesLookup;

  @Mock
  private GatewayCaseRecordService cacheService;

  @Mock
  private GatewayEventManager eventManager;

  @Mock
  private ActionInstructionPublisher messagePublisher;

  @InjectMocks
  private FulfilmentService fulfilmentService;

  @Test
  void reportsNoRecordFoundUsingTheEventType() {
    fulfilmentService.processPauseCase(request("P_OR_H1"), Instant.now(), "correlation-id");

    verify(eventManager).triggerEvent(CASE_ID, "NO_RECORD_FOUND", "Reason",
        "Could not find an existing record or case is not a household");
  }

  @Test
  void reportsUnrecognisedProductCodeUsingTheEventType() {
    GatewayCaseRecord household = new GatewayCaseRecord();
    household.caseId = CASE_ID;
    when(cacheService.getByIdAndTypeAndExists(CASE_ID, 10, true)).thenReturn(household);

    fulfilmentService.processPauseCase(request("UNKNOWN_PRODUCT"), Instant.now(), "correlation-id");

    verify(eventManager).triggerEvent(CASE_ID, "UNRECOGNISED_FULFILLMENT_CODE", "Reason",
        "Could not find a rule for the fulfilment request and product code.",
        "Product code", "UNKNOWN_PRODUCT");
  }

  private FulfilmentRequestEvent request(String fulfilmentCode) {
    PauseFulfilmentRequest fulfilmentRequest = new PauseFulfilmentRequest();
    fulfilmentRequest.setCaseId(CASE_ID);
    fulfilmentRequest.setFulfilmentCode(fulfilmentCode);
    FulfilmentRequestPayload payload = new FulfilmentRequestPayload();
    payload.setFulfilmentRequest(fulfilmentRequest);
    FulfilmentRequestEvent event = new FulfilmentRequestEvent();
    event.setPayload(payload);
    return event;
  }
}
