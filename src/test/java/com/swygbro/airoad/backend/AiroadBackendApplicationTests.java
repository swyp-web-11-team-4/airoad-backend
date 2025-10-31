package com.swygbro.airoad.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.common.config.TestVectorStoreConfig;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestVectorStoreConfig.class)
class AiroadBackendApplicationTests {

  @Test
  void contextLoads() {}
}
