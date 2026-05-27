package uk.gov.ons.census.fwmt.fulfilment.messaging.pubsub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.common.messaging.MessagingProperties;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.fulfilment.messaging.RmFieldPausePublisher;

@Service
@ConditionalOnProperty(name = MessagingProperties.PROVIDER, havingValue = MessagingProperties.PROVIDER_PUBSUB)
public class PubSubRmFieldPausePublisher implements RmFieldPausePublisher {

  @Override
  public void pausePublish(FwmtActionInstruction pauseActionInstruction) {
    throw new UnsupportedOperationException(
        "Pub/Sub RM.Field pause publish is not implemented (Stage 2). Set app.messaging.provider=rabbit.");
  }
}
