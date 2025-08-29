package edu.hm.crownet.testbed.ratecontrol.config;

import edu.hm.crownet.testbed.ratecontrol.MessageSizeService;
import edu.hm.crownet.testbed.ratecontrol.NodeEstimatorService;
import edu.hm.crownet.testbed.ratecontrol.RateAdaptionService;
import edu.hm.crownet.testbed.ratecontrol.impl.MessageSizeServiceImpl;
import edu.hm.crownet.testbed.ratecontrol.impl.OneHopNodeEstimatorImpl;
import edu.hm.crownet.testbed.ratecontrol.impl.RateAdaptionServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

  // The beacon and message module share the same node estimator service instance.
  @Bean("oneHopNodeEstimator")
  public NodeEstimatorService nodeEstimatorService() {
    return new OneHopNodeEstimatorImpl();
  }

  // Message size and rate adaption service for the beacon module.
  @Bean("beaconMessageSizeService")
  public MessageSizeService beaconMessageSizeService(@Value("${crownet.testbed.ms.alpha:0.1}") double alpha) {
    return new MessageSizeServiceImpl(alpha, 38);
  }

  @Bean("beaconRateAdaptionService")
  public RateAdaptionService beaconRateAdaptionService(@Value("${crownet.testbed.adhoc.beacon.bandwidth}") int maxBytesPerSec) {
    return new RateAdaptionServiceImpl(maxBytesPerSec);
  }

  // Message size and rate adaption service for the message module.
  @Bean("messageMessageSizeService")
  public MessageSizeService messageMessageSizeService(@Value("${crownet.testbed.ms.alpha:0.1}") double alpha) {
    return new MessageSizeServiceImpl(alpha, 1400.0);
  }

  @Bean("messageRateAdaptionService")
  public RateAdaptionService messageRateAdaptionService(@Value("${crownet.testbed.adhoc.message.bandwidth}") int maxBytesPerSec) {
    return new RateAdaptionServiceImpl(maxBytesPerSec);
  }
}