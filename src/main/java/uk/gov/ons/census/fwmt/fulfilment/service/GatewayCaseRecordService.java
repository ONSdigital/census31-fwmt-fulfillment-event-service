package uk.gov.ons.census.fwmt.fulfilment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.fulfilment.data.GatewayCaseRecord;
import uk.gov.ons.census.fwmt.fulfilment.repository.GatewayCaseRecordRepository;

/**
 * This class is bare-bones because it's a simple connector between the rest of the code and the caching implementation
 * Please don't subvert this class by touching the GatewayCaseRecordRepository
 * If we ever change from a database to redis, this class will form the breaking point
 */

@Slf4j
@Service
public class GatewayCaseRecordService {
  public final GatewayCaseRecordRepository repository;

  public GatewayCaseRecordService(GatewayCaseRecordRepository repository) {
    this.repository = repository;
  }

  public GatewayCaseRecord getByIdAndTypeAndExists(String caseId, int type, boolean exists) {
    return repository.findByCaseIdAndTypeAndExistsInFwmt(caseId, type, exists); }

  public GatewayCaseRecord getByIndividualCaseIdAndTypeAndExists(String indCaseId, int type, boolean exists) {
    return repository.findByIndividualCaseIdAndTypeAndExistsInFwmt(indCaseId, type, exists); }
}
