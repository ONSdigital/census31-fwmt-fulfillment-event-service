package uk.gov.ons.census.fwmt.fulfilment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import uk.gov.ons.census.fwmt.fulfilment.data.GatewayCaseRecord;

import jakarta.persistence.LockModeType;

@Repository
public interface GatewayCaseRecordRepository extends JpaRepository<GatewayCaseRecord, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  GatewayCaseRecord findByCaseIdAndTypeAndExistsInFwmt(String caseId, int type, boolean exists);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  GatewayCaseRecord findByIndividualCaseIdAndTypeAndExistsInFwmt(String caseId, int type, boolean exists);

}
