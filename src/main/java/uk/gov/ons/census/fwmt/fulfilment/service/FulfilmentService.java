package uk.gov.ons.census.fwmt.fulfilment.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.common.action.PauseActionInstruction;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.fulfilment.data.GatewayCaseRecord;
import uk.gov.ons.census.fwmt.fulfilment.lookup.PauseRulesLookup;
import uk.gov.ons.census.fwmt.fulfilment.messaging.ActionInstructionPublisher;
import uk.gov.ons.census.fwmt.fulfilment.messaging.model.FulfilmentRequestEvent;

import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Slf4j
@Service
@Transactional
public class FulfilmentService {

  private static final String PAUSE_PROCESSED_AND_SENT = "PAUSE_PROCESSED_AND_SENT";

  private static final String NO_RECORD_FOUND = "NO_RECORD_FOUND";

  private static final String CASE_ALREADY_CANCELLED = "CASE_ALREADY_CANCELLED";

  private static final String UNRECOGNISED_FULFILLMENT_CODE = "UNRECOGNISED_FULFILLMENT_CODE";

  @Autowired
  private PauseRulesLookup pauseRulesLookup;

  @Autowired
  private GatewayCaseRecordService cacheService;

  @Autowired
  private GatewayEventManager eventManager;

  @Autowired
  private ActionInstructionPublisher messagePublisher;

  public void processPauseCase(FulfilmentRequestEvent pauseRequest, Instant messageReceivedTime,
      String correlationId) {
    GatewayCaseRecord indCache = null;
    String caseId;
    String individualCaseId = pauseRequest.getPayload().getFulfilmentRequest().getIndividualCaseId();
    String productCode = pauseRequest.getPayload().getFulfilmentRequest().getFulfilmentCode();

    final GatewayCaseRecord caseCache = cacheService.getByIdAndTypeAndExists(pauseRequest.getPayload().getFulfilmentRequest().getCaseId(),
        10, true);

    if (!Strings.isEmpty(individualCaseId)) {
        indCache = cacheService.getByIndividualCaseIdAndTypeAndExists(individualCaseId, 10, true);
    }

    if (caseCache == null && indCache == null) {
      caseId = pauseRequest.getPayload().getFulfilmentRequest().getCaseId();
      eventManager.triggerEvent(caseId, "Could not find an existing record or case is not a household", NO_RECORD_FOUND);
    } else if (caseCache == null) {
      caseId = indCache.individualCaseId;
      if ("CANCEL".equals(indCache.lastActionInstruction) || "CANCEL(HELD)".equals(indCache.lastActionInstruction)) {
        eventManager.triggerEvent(caseId, CASE_ALREADY_CANCELLED);
      } else {
        sendPause(pauseRequest, messageReceivedTime, caseId, productCode, correlationId);
      }
    } else {
      caseId = caseCache.caseId;
      if ("CANCEL".equals(caseCache.lastActionInstruction) || "CANCEL(HELD)".equals(caseCache.lastActionInstruction)) {
        caseId = pauseRequest.getPayload().getFulfilmentRequest().getCaseId();
        eventManager.triggerEvent(caseId, CASE_ALREADY_CANCELLED);
      } else {
        sendPause(pauseRequest, messageReceivedTime, caseId, productCode, correlationId);
      }
    }
  }

  private void sendPause(FulfilmentRequestEvent pauseRequest, Instant messageReceivedTime, String caseId,
      String productCode, String correlationId) {
    String pauseRule;
    pauseRule = pauseRulesLookup.getLookup(pauseRequest.getPayload().getFulfilmentRequest().getFulfilmentCode());
    if (pauseRule == null) {
      eventManager.triggerEvent(caseId, "Could not find a rule for the fulfilment request and product code.",
          UNRECOGNISED_FULFILLMENT_CODE, "Product code", productCode);
    } else if (caseId != null) {
      PauseActionInstruction pauseActionInstruction = buildPause(messageReceivedTime, caseId, pauseRule);
      messagePublisher.publish(pauseActionInstruction, correlationId);
      eventManager.triggerEvent(pauseRequest.getPayload().getFulfilmentRequest().getCaseId(), PAUSE_PROCESSED_AND_SENT);
    }
  }

  private PauseActionInstruction buildPause(Instant messageReceivedTime, String caseId, String pauseRule) {
    return PauseActionInstruction.builder()
        .caseId(caseId)
        .actionInstruction("PAUSE")
        .surveyName("CENSUS")
        .addressType("HH")
        .addressLevel("U")
        .pauseFrom(messageReceivedTime)
        .pauseCode(pauseRule)
        .build();
  }
}
