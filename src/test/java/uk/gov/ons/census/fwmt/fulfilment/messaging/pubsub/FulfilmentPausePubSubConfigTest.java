package uk.gov.ons.census.fwmt.fulfilment.messaging.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberOperations;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;

class FulfilmentPausePubSubConfigTest {

  @Test
  void externalFulfilmentSubscriberUsesRmTemplate() throws NoSuchMethodException {
    Method method = FulfilmentPausePubSubConfig.class.getDeclaredMethod(
      "fulfilmentPausePubSubInbound", MessageChannel.class, PubSubSubscriberOperations.class);

    Qualifier qualifier = method.getParameters()[1].getAnnotation(Qualifier.class);

    assertThat(qualifier).isNotNull();
    assertThat(qualifier.value()).isEqualTo("rmPubSubTemplate");
  }
}