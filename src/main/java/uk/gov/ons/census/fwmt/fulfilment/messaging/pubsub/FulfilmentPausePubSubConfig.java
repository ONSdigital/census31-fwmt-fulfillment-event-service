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
import org.springframework.core.retry.RetryPolicy;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import uk.gov.ons.census.fwmt.fulfilment.service.FulfilmentPausePubSubMessageHandler;

@Configuration
@Slf4j
public class FulfilmentPausePubSubConfig {

  @Value("${app.messaging.pubsub.fulfilment-request-subscription:fulfilment-event-service-fulfilment-request}")
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
    adapter.setAckMode(AckMode.AUTO);
    return adapter;
  }

  @Bean
  public RequestHandlerRetryAdvice fulfilmentRetryAdvice(
      @Value("${app.messaging.local-attempts:3}") int localAttempts) {
    RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();
    advice.setRetryPolicy(RetryPolicy.withMaxRetries(Math.max(0, localAttempts - 1L)));
    advice.setRecoveryCallback((attributes, failure) -> {
      log.error("Fulfilment message exhausted {} local attempts", localAttempts, failure);
      return null;
    });
    return advice;
  }

  @Bean
  @ServiceActivator(inputChannel = "fulfilmentPausePubSubInputChannel", adviceChain = "fulfilmentRetryAdvice")
  public MessageHandler fulfilmentPausePubSubHandler(FulfilmentPausePubSubMessageHandler handler) {
    return message -> {
      try {
        BasicAcknowledgeablePubsubMessage original = message.getHeaders()
            .get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
        if (original == null) {
          throw new IllegalStateException("Missing original Pub/Sub message header");
        }
        PubsubMessage pubsubMessage = original.getPubsubMessage();
        handler.handle(pubsubMessage);
      } catch (Exception exception) {
        log.error("Failed to process fulfilment Pub/Sub message", exception);
        throw new MessageHandlingException(message, "Failed to process fulfilment Pub/Sub message", exception);
      }
    };
  }
}

