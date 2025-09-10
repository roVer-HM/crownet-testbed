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

/**
 * Spring configuration class for setting up rate control related services.
 *
 * <p>This configuration defines beans for node estimation, message size estimation, and rate
 * adaptation services used in the beacon and message modules.
 */
@Configuration
public class Config {

  /**
   * Provides a shared node estimator service instance.
   * <p>Used by both the beacon and message modules for one-hop neighbor estimation.</p>
   */
  @Bean("oneHopNodeEstimator")
  public NodeEstimatorService nodeEstimatorService() {
    return new OneHopNodeEstimatorImpl();
  }

  /**
   * Message size estimator for the beacon module.
   * <p>Configured with a smoothing factor {@code alpha} and a fixed header size of 38 bytes.</p>
   */
  @Bean("beaconMessageSizeService")
  public MessageSizeService beaconMessageSizeService(
          @Value("${crownet.testbed.ms.alpha:0.1}") double alpha) {
    return new MessageSizeServiceImpl(alpha, 38);
  }

  /**
   * Rate adaptation service for the beacon module.
   * <p>Configured with the maximum available bandwidth (in bytes/sec) and a fixed header size of 38 bytes.</p>
   */
  @Bean("beaconRateAdaptionService")
  public RateAdaptionService beaconRateAdaptionService(
          @Value("${crownet.testbed.adhoc.beacon.bandwidth}") int maxBytesPerSec) {
    return new RateAdaptionServiceImpl(maxBytesPerSec, 38);
  }

  /**
   * Message size estimator for the message module.
   * <p>Configured with a smoothing factor {@code alpha} and a maximum payload size of 1400 bytes.</p>
   */
  @Bean("messageMessageSizeService")
  public MessageSizeService messageMessageSizeService(
          @Value("${crownet.testbed.ms.alpha:0.1}") double alpha) {
    return new MessageSizeServiceImpl(alpha, 1400.0);
  }

  /**
   * Rate adaptation service for the message module.
   * <p>Configured with the maximum available bandwidth (in bytes/sec) and a maximum payload size of 1400 bytes.</p>
   */
  @Bean("messageRateAdaptionService")
  public RateAdaptionService messageRateAdaptionService(
          @Value("${crownet.testbed.adhoc.message.bandwidth}") int maxBytesPerSec) {
    return new RateAdaptionServiceImpl(maxBytesPerSec, 1400);
  }
}