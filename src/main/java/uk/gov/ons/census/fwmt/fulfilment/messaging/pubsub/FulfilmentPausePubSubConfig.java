package uk.gov.ons.census.fwmt.fulfilment.messaging.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.google.pubsub.v1.PubsubMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import uk.gov.ons.census.fwmt.fulfilment.service.FulfilmentPausePubSubMessageHandler;

@Configuration
@Slf4j
public class FulfilmentPausePubSubConfig {

  @Value("${app.messaging.pubsub.fulfilment-events-subscription:fulfilment-event-service-events}")
  private String fulfilmentEventsSubscription;

  @Bean(name = "fulfilmentPausePubSubInputChannel")
  public MessageChannel fulfilmentPausePubSubInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public PubSubInboundChannelAdapter fulfilmentPausePubSubInbound(
      @Qualifier("fulfilmentPausePubSubInputChannel") MessageChannel inputChannel,
      PubSubTemplate pubSubTemplate) {
    PubSubInboundChannelAdapter adapter =
        new PubSubInboundChannelAdapter(pubSubTemplate, fulfilmentEventsSubscription);
    adapter.setOutputChannel(inputChannel);
    adapter.setAckMode(AckMode.MANUAL);
    return adapter;
  }

  @Bean
  @ServiceActivator(inputChannel = "fulfilmentPausePubSubInputChannel")
  public MessageHandler fulfilmentPausePubSubHandler(FulfilmentPausePubSubMessageHandler handler) {
    return message -> {
      BasicAcknowledgeablePubsubMessage original =
          message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
      PubsubMessage pubsubMessage = original.getPubsubMessage();

      try {
        boolean handled = handler.handle(pubsubMessage);
        // Even if it wasn't a fulfilment message, ack it for this subscription.
        if (handled) {
          original.ack();
        } else {
          original.ack();
        }
      } catch (RuntimeException ex) {
        log.error("Failed to process fulfilment Pub/Sub message", ex);
        original.nack();
        throw ex;
      } catch (Exception ex) {
        log.error("Failed to process fulfilment Pub/Sub message", ex);
        original.nack();
      }
    };
  }
}

