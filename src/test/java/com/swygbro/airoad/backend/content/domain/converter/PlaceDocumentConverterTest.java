package com.swygbro.airoad.backend.content.domain.converter;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.fixture.content.PlaceFixture;

import static org.assertj.core.api.Assertions.assertThat;

/** PlaceDocumentConverter 단위 테스트 */
@ActiveProfiles("test")
class PlaceDocumentConverterTest {

  private PlaceDocumentConverter converter;

  @BeforeEach
  void setUp() {
    // given: 테스트용 컨버터 초기화
    converter = new PlaceDocumentConverter();
  }

  @Nested
  class Place를_Document로_변환_시 {

    @Test
    void 모든_필드가_있을_때_완전한_Document로_변환할_수_있다() {
      // given: 모든 필드가 채워진 Place 데이터 준비
      Place place = PlaceFixture.createWithFullInfo();
      Place placeWithEntity =
          PlaceFixture.withFullEntity(
              1L, LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 10, 12, 0), place);

      // when: Place를 Document로 변환 요청
      Document document = converter.toDocument(placeWithEntity);

      // then: 모든 필드를 포함한 content 생성됨
      String content = document.getText();
      assertThat(content).contains("장소명: 서울역");
      assertThat(content).contains("주소: 서울특별시 용산구 한강대로 405");
      assertThat(content).contains("설명: 서울의 중심지에 위치한 역사적인 장소입니다. 다양한 문화 체험과 맛집이 많습니다.");
      assertThat(content).contains("운영 시간: 09:00-22:00");
      assertThat(content).contains("휴무일: 연중무휴");

      // then: 검색에 필요한 메타데이터 포함됨
      Map<String, Object> metadata = document.getMetadata();
      assertThat(metadata).containsEntry("placeId", 1L);
      assertThat(metadata).containsEntry("name", "서울역");
      assertThat(metadata).containsEntry("address", "서울특별시 용산구 한강대로 405");
      assertThat(metadata).containsEntry("latitude", 37.5547);
      assertThat(metadata).containsEntry("longitude", 126.9716);
      assertThat(metadata).containsEntry("isMustVisit", true);
      assertThat(metadata).containsEntry("placeScore", 5);
      assertThat(metadata).containsKey("contentHash");
      assertThat(metadata).containsKey("embeddedAt");
      assertThat(metadata).containsEntry("placeUpdatedAt", "2025-01-10T12:00");
    }

    @Test
    void 선택_필드가_없을_때도_Document로_변환할_수_있다() {
      // given: description이 null인 Place 데이터 준비
      Place place = PlaceFixture.createWithMinimalInfo();
      Place placeWithEntity =
          PlaceFixture.withFullEntity(
              2L, LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 5, 10, 0), place);

      // when: Place를 Document로 변환 요청
      Document document = converter.toDocument(placeWithEntity);

      // then: 필수 필드만 포함된 content 생성됨
      String content = document.getText();
      assertThat(content).contains("장소명: 강남역");
      assertThat(content).contains("주소: 서울특별시 강남구 강남대로 지하 396");
      assertThat(content).doesNotContain("설명:");

      // then: 메타데이터는 정상적으로 생성됨
      Map<String, Object> metadata = document.getMetadata();
      assertThat(metadata).containsEntry("placeId", 2L);
      assertThat(metadata).containsEntry("name", "강남역");
      assertThat(metadata).containsKey("contentHash");
    }

    @Test
    void 운영시간이_빈_값일_때_생략하고_변환할_수_있다() {
      // given: operatingHours가 빈 문자열인 Place 데이터 준비
      Place place =
          PlaceFixture.builder()
              .description("테스트 설명")
              .operatingHours("")
              .holidayInfo("매주 월요일")
              .build();
      Place placeWithEntity =
          PlaceFixture.withFullEntity(
              3L, LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 3, 9, 0), place);

      // when: Place를 Document로 변환 요청
      Document document = converter.toDocument(placeWithEntity);

      // then: 운영시간은 생략하고 content 생성됨
      String content = document.getText();
      assertThat(content).contains("장소명: 서울역");
      assertThat(content).contains("설명: 테스트 설명");
      assertThat(content).doesNotContain("운영 시간:");
      assertThat(content).contains("휴무일: 매주 월요일");

      // then: 메타데이터는 정상적으로 생성됨
      Map<String, Object> metadata = document.getMetadata();
      assertThat(metadata).containsEntry("placeId", 3L);
      assertThat(metadata).containsKey("contentHash");
    }

    @Test
    void 변환_시_검색에_필요한_메타데이터가_포함되어야_한다() {
      // given: Place 데이터 준비
      Place place = PlaceFixture.createWithFullInfo();
      Place placeWithEntity =
          PlaceFixture.withFullEntity(
              10L,
              LocalDateTime.of(2025, 1, 1, 0, 0),
              LocalDateTime.of(2025, 1, 15, 14, 30),
              place);

      // when: Place를 Document로 변환 요청
      Document document = converter.toDocument(placeWithEntity);

      // then: 검색과 필터링에 필요한 모든 메타데이터 포함됨
      Map<String, Object> metadata = document.getMetadata();
      assertThat(metadata)
          .containsKeys(
              "placeId",
              "name",
              "address",
              "latitude",
              "longitude",
              "isMustVisit",
              "placeScore",
              "contentHash",
              "embeddedAt",
              "placeUpdatedAt");

      // then: 위치 정보가 올바르게 매핑됨 (Point의 Y=latitude, X=longitude)
      assertThat(metadata.get("latitude")).isEqualTo(37.5547);
      assertThat(metadata.get("longitude")).isEqualTo(126.9716);
    }

    @Test
    void 같은_내용의_Place는_동일한_해시를_생성해야_한다() {
      // given: 동일한 내용의 Place 두 개 준비
      Place place1 = PlaceFixture.createGangnam();
      Place place1WithEntity =
          PlaceFixture.withFullEntity(
              100L,
              LocalDateTime.of(2025, 1, 1, 0, 0),
              LocalDateTime.of(2025, 1, 10, 10, 0),
              place1);

      Place place2 = PlaceFixture.createGangnam();
      Place place2WithEntity =
          PlaceFixture.withFullEntity(
              200L,
              LocalDateTime.of(2025, 1, 5, 0, 0),
              LocalDateTime.of(2025, 1, 20, 15, 0),
              place2);

      // when: 각각 Document로 변환 요청
      Document document1 = converter.toDocument(place1WithEntity);
      Document document2 = converter.toDocument(place2WithEntity);

      // then: 동일한 content는 동일한 contentHash 생성
      String hash1 = (String) document1.getMetadata().get("contentHash");
      String hash2 = (String) document2.getMetadata().get("contentHash");
      assertThat(hash1).isEqualTo(hash2);

      // then: SHA-256 해시 형식 (64자 HEX 문자열)
      assertThat(hash1).hasSize(64);
      assertThat(hash1).matches("^[0-9a-f]+$");
    }

    @Test
    void 다른_내용의_Place는_서로_다른_해시를_생성해야_한다() {
      // given: 서로 다른 Place 두 개 준비
      Place place1 = PlaceFixture.createGangnam();
      Place place1WithEntity =
          PlaceFixture.withFullEntity(
              1L, LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 10, 10, 0), place1);

      Place place2 = PlaceFixture.createJejuAirport();
      Place place2WithEntity =
          PlaceFixture.withFullEntity(
              2L, LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 10, 10, 0), place2);

      // when: 각각 Document로 변환 요청
      Document document1 = converter.toDocument(place1WithEntity);
      Document document2 = converter.toDocument(place2WithEntity);

      // then: 서로 다른 content는 서로 다른 contentHash 생성
      String hash1 = (String) document1.getMetadata().get("contentHash");
      String hash2 = (String) document2.getMetadata().get("contentHash");
      assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void 휴무일_정보가_없어도_변환할_수_있다() {
      // given: holidayInfo가 null인 Place 데이터 준비
      Place place =
          PlaceFixture.builder()
              .description("테스트 장소")
              .operatingHours("09:00-18:00")
              .holidayInfo(null)
              .build();
      Place placeWithEntity =
          PlaceFixture.withFullEntity(
              5L, LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 5, 10, 0), place);

      // when: Place를 Document로 변환 요청
      Document document = converter.toDocument(placeWithEntity);

      // then: 휴무일은 생략하고 content 생성됨
      String content = document.getText();
      assertThat(content).contains("설명: 테스트 장소");
      assertThat(content).contains("운영 시간: 09:00-18:00");
      assertThat(content).doesNotContain("휴무일:");
      assertThat(content.trim()).doesNotEndWith("\n");
    }

    @Test
    void 필수_필드만_있어도_Document로_변환할_수_있다() {
      // given: 필수 필드만 있는 Place 데이터 준비
      Place place = PlaceFixture.createWithMinimalInfo();
      Place placeWithEntity =
          PlaceFixture.withFullEntity(
              6L, LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 2, 8, 0), place);

      // when: Place를 Document로 변환 요청
      Document document = converter.toDocument(placeWithEntity);

      // then: 필수 필드만으로도 content 생성됨
      String content = document.getText();
      assertThat(content).contains("장소명: 강남역");
      assertThat(content).contains("주소: 서울특별시 강남구 강남대로 지하 396");
      assertThat(content).doesNotContain("설명:");
      assertThat(content).doesNotContain("운영 시간:");
      assertThat(content).doesNotContain("휴무일:");

      // then: 메타데이터는 정상적으로 생성됨
      Map<String, Object> metadata = document.getMetadata();
      assertThat(metadata).containsEntry("placeId", 6L);
      assertThat(metadata).containsEntry("isMustVisit", false);
      assertThat(metadata).containsEntry("placeScore", 1);
      assertThat(metadata).containsKey("contentHash");
    }
  }
}
