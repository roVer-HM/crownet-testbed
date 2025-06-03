package edu.hm.crownet.testbed.ratecontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageSizeServiceImplTest {

  private MessageSizeServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new MessageSizeServiceImpl();
  }

  @Test
  void shouldReturnZeroInitially() {
    // when
    double initial = service.getAverageMessageSize();

    // then
    assertEquals(0.0, initial, "Initial average message size should be zero");
  }

  @Test
  void shouldUpdateAverageCorrectlyOnFirstInput() {
    // when
    service.registerMessageSize(100);

    // then
    assertEquals(10.0, service.getAverageMessageSize(), 0.0001);
  }

  @Test
  void shouldSmoothAverageWithMultipleInputs() {
    // given
    service.registerMessageSize(100);
    service.registerMessageSize(200);

    // when
    double result = service.getAverageMessageSize();

    // then
    assertEquals(29.0, result, 0.0001);
  }

  @Test
  void shouldDecreaseAverageIfNextInputIsSmaller() {
    // given
    service.registerMessageSize(100);
    service.registerMessageSize(50);

    // when
    double result = service.getAverageMessageSize();

    // then
    assertEquals(14.0, result, 0.0001);
  }

  @Test
  void shouldRemainStableWithSameInput() {
    // given
    service.registerMessageSize(100);
    service.registerMessageSize(100);
    service.registerMessageSize(100);

    // when
    double result = service.getAverageMessageSize();

    // then
    assertEquals(27.1, result, 0.0001);
  }
}
