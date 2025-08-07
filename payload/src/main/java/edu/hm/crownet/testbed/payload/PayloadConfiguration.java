package edu.hm.crownet.testbed.payload;

import edu.hm.crownet.testbed.ratecontrol.MessageSizeService;
import edu.hm.crownet.testbed.ratecontrol.MessageSizeServiceImpl;
import edu.hm.crownet.testbed.ratecontrol.NodeEstimatorService;
import edu.hm.crownet.testbed.ratecontrol.OneHopNodeEstimatorImpl;
import edu.hm.crownet.testbed.ratecontrol.RateAdaptionService;
import edu.hm.crownet.testbed.ratecontrol.RateAdaptionServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for payload-specific rate adaptation services.
 * Provides separate instances of rate adaptation services for the payload module
 * to avoid conflicts with the beacon module.
 */
@Configuration
public class PayloadConfiguration {

  @Bean("payloadRateAdaptionService")
  public RateAdaptionService payloadRateAdaptionService() {
    return new RateAdaptionServiceImpl();
  }

  @Bean("payloadMessageSizeService")
  public MessageSizeService payloadMessageSizeService() {
    return new MessageSizeServiceImpl();
  }

  @Bean("payloadNodeEstimatorService")
  public NodeEstimatorService payloadNodeEstimatorService() {
    return new OneHopNodeEstimatorImpl();
  }
} 