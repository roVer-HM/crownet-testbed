package edu.hm.crownet.testbed.beacon;

import edu.hm.crownet.testbed.ratecontrol.MessageSizeService;
import edu.hm.crownet.testbed.ratecontrol.MessageSizeServiceImpl;
import edu.hm.crownet.testbed.ratecontrol.NodeEstimatorService;
import edu.hm.crownet.testbed.ratecontrol.OneHopNodeEstimatorImpl;
import edu.hm.crownet.testbed.ratecontrol.RateAdaptionService;
import edu.hm.crownet.testbed.ratecontrol.RateAdaptionServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for beacon-specific rate adaptation services.
 * Provides separate instances of rate adaptation services for the beacon module
 * to maintain consistency with the payload module structure.
 */
@Configuration
public class BeaconConfiguration {

  @Bean("beaconRateAdaptionService")
  public RateAdaptionService beaconRateAdaptionService() {
    return new RateAdaptionServiceImpl();
  }

  @Bean("beaconMessageSizeService")
  public MessageSizeService beaconMessageSizeService() {
    return new MessageSizeServiceImpl();
  }

  @Bean("beaconNodeEstimatorService")
  public NodeEstimatorService beaconNodeEstimatorService() {
    return new OneHopNodeEstimatorImpl();
  }
} 