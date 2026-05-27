package uk.gov.ons.census.fwmt.fulfilment.messaging.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.common.messaging.MessagingProperties;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.fulfilment.messaging.RmFieldPausePublisher;

@Service
@ConditionalOnProperty(name = MessagingProperties.PROVIDER, havingValue = MessagingProperties.PROVIDER_RABBIT, matchIfMissing = true)
public class RabbitRmFieldPausePublisher implements RmFieldPausePublisher {

  private final RabbitTemplate rabbitTemplate;
  private final String rmFieldDestination;

  public RabbitRmFieldPausePublisher(
      @Qualifier("republishRabbitTemplate") RabbitTemplate rabbitTemplate,
      @Value("${app.messaging.destinations.rmField:RM.Field}") String rmFieldDestination) {
    this.rabbitTemplate = rabbitTemplate;
    this.rmFieldDestination = rmFieldDestination;
  }

  @Override
  public void pausePublish(FwmtActionInstruction pauseActionInstruction) {
    rabbitTemplate.convertAndSend(rmFieldDestination, pauseActionInstruction);
  }
}
