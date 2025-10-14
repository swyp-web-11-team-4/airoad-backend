package com.swygbro.airoad.backend.example.infrastructure;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.common.config.JpaConfig;
import com.swygbro.airoad.backend.example.domain.entity.Example;

import static org.assertj.core.api.Assertions.assertThat;

/** ExampleRepository의 테스트 클래스입니다. */
@DataJpaTest
@Import(JpaConfig.class)
@ActiveProfiles("test")
class ExampleRepositoryTest {

  @Autowired private ExampleRepository exampleRepository;
  @Autowired private EntityManager entityManager;

  @Nested
  @DisplayName("기본 CRUD 테스트")
  class BasicCrud {

    @Test
    @DisplayName("Example 엔티티 저장 및 조회 테스트")
    void saveAndFind() {
      // given
      String name = "test-example";
      Example newExample = new Example(name);

      // when
      Example savedExample = exampleRepository.saveAndFlush(newExample);
      Example foundExample = exampleRepository.findById(savedExample.getId()).orElse(null);

      // then
      assertThat(savedExample.getId()).isNotNull();
      assertThat(foundExample).isNotNull();
      assertThat(foundExample.getId()).isEqualTo(savedExample.getId());
      assertThat(foundExample.getName()).isEqualTo(name);
    }
  }

  @Nested
  @DisplayName("Hard Delete 테스트")
  class HardDeleteTest {
    @Test
    @DisplayName("Example 엔티티를 삭제하면 데이터베이스에서 완전히 제거된다.")
    void deleteExampleHard() {
      // given
      Example example = new Example("test-example-to-delete");
      exampleRepository.saveAndFlush(example);
      Long exampleId = example.getId();

      // when
      exampleRepository.delete(example);
      entityManager.flush(); // 변경사항을 DB에 즉시 반영
      entityManager.clear(); // 영속성 컨텍스트 초기화

      // then
      assertThat(exampleRepository.findById(exampleId)).isEmpty();
    }
  }
}
