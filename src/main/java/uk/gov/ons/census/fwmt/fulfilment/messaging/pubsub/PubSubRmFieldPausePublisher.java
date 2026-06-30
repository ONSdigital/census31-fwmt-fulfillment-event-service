package uk.gov.ons.census.fwmt.fulfilment.messaging.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PubsubMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.common.messaging.FieldWorkerInstructionJsonCodec;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.fulfilment.messaging.RmFieldPausePublisher;

@Service
@RequiredArgsConstructor
public class PubSubRmFieldPausePublisher implements RmFieldPausePublisher {

  private final PubSubTemplate pubSubTemplate;
  private final FieldWorkerInstructionJsonCodec codec;

  @Value("${app.messaging.destinations.rmField:RM.Field}")
  private String rmFieldTopic;

  @Override
  public void pausePublish(FwmtActionInstruction pauseActionInstruction) {
    PubsubMessage message = codec.toPubsubMessage(pauseActionInstruction, true);
    pubSubTemplate.publish(rmFieldTopic, message);
  }
}
