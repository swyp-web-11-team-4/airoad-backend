package com.swygbro.airoad.backend.fixture.common;

import com.swygbro.airoad.backend.trip.domain.embeddable.TravelSegment;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

public class TravelSegmentFixture {

  public static TravelSegment create() {
    return TravelSegment.builder().travelTime(30).transportation(Transportation.PUBLIC_TRANSIT).build();
  }

  public static TravelSegment createWalking() {
    return TravelSegment.builder().travelTime(10).transportation(Transportation.WALKING).build();
  }

  public static TravelSegment createByCar() {
    return TravelSegment.builder().travelTime(45).transportation(Transportation.CAR).build();
  }

  public static TravelSegment createNoTransport() {
    return TravelSegment.builder().travelTime(0).transportation(Transportation.NONE).build();
  }

  public static TravelSegment.TravelSegmentBuilder builder() {
    return TravelSegment.builder().travelTime(20).transportation(Transportation.PUBLIC_TRANSIT);
  }
}
