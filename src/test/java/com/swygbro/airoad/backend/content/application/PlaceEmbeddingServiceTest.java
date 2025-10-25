package com.swygbro.airoad.backend.content.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.content.domain.converter.PlaceDocumentConverter;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceVectorStoreRepository;
import com.swygbro.airoad.backend.fixture.content.PlaceFixture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.never;

/** PlaceEmbeddingService 단위 테스트 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PlaceEmbeddingServiceTest {

  @Mock private PlaceRepository placeRepository;

  @Mock private PlaceVectorStoreRepository vectorStoreRepository;

  @Mock private PlaceDocumentConverter documentConverter;

  @InjectMocks private PlaceEmbeddingService placeEmbeddingService;

  private Document mockDocument;
  private int batchSize;

  @BeforeEach
  void setUp() throws Exception {
    // given: 테스트용 Mock Document 초기화
    mockDocument = new Document("테스트 content");

    // given: PlaceEmbeddingService의 배치 크기 읽기
    var field = PlaceEmbeddingService.class.getDeclaredField("BATCH_SIZE");
    field.setAccessible(true);
    batchSize = field.getInt(null);
  }

  @Nested
  class 전체_Place_임베딩_시 {

    @Test
    void 모든_Place_임베딩을_요청하면_전체_데이터를_처리할_수_있다() {
      // given: 여러 Place 데이터 준비
      Place place1 =
          PlaceFixture.withFullEntity(
              1L,
              LocalDateTime.of(2025, 1, 1, 0, 0),
              LocalDateTime.of(2025, 1, 10, 10, 0),
              PlaceFixture.createWithFullInfo());
      Place place2 =
          PlaceFixture.withFullEntity(
              2L,
              LocalDateTime.of(2025, 1, 1, 0, 0),
              LocalDateTime.of(2025, 1, 11, 11, 0),
              PlaceFixture.createGangnam());
      Place place3 =
          PlaceFixture.withFullEntity(
              3L,
              LocalDateTime.of(2025, 1, 1, 0, 0),
              LocalDateTime.of(2025, 1, 12, 12, 0),
              PlaceFixture.createJejuAirport());

      Stream<Place> placeStream = Stream.of(place1, place2, place3);

      // given: Repository와 Converter 모킹
      given(placeRepository.streamAllBy()).willReturn(placeStream);
      given(documentConverter.toDocument(any(Place.class))).willReturn(mockDocument);

      // when: 전체 Place 임베딩 요청
      placeEmbeddingService.embedAllPlaces();

      // then: 모든 Place를 스트림으로 조회하고 임베딩 처리
      then(placeRepository).should(times(1)).streamAllBy();
      then(vectorStoreRepository).should(times(3)).deleteByPlaceId(any(Long.class));
      then(documentConverter).should(times(3)).toDocument(any(Place.class));
    }

    @Test
    void 대량의_Place가_있을_때_배치_단위로_나누어_처리할_수_있다() {
      // given: 배치 크기를 초과하는 데이터 생성 (배치 크기 + 1)
      int totalCount = batchSize + 1;
      List<Place> places = new ArrayList<>();
      for (int i = 1; i <= totalCount; i++) {
        Place place =
            PlaceFixture.withFullEntity(
                (long) i,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 10, 10, 0),
                PlaceFixture.create());
        places.add(place);
      }

      Stream<Place> placeStream = places.stream();

      // given: Repository와 Converter 모킹
      given(placeRepository.streamAllBy()).willReturn(placeStream);
      given(documentConverter.toDocument(any(Place.class))).willReturn(mockDocument);

      // when: 전체 Place 임베딩 요청
      placeEmbeddingService.embedAllPlaces();

      // then: 모든 Place를 임베딩 처리
      then(vectorStoreRepository).should(times(totalCount)).deleteByPlaceId(any(Long.class));
      then(documentConverter).should(times(totalCount)).toDocument(any(Place.class));

      // then: 배치 단위로 나누어 저장 (배치 크기만큼 1회 + 나머지 1개)
      then(vectorStoreRepository).should(times(2)).saveAll(anyList());
    }

    @Test
    void 처리할_Place가_없을_때_안전하게_종료할_수_있다() {
      // given: 빈 스트림 반환
      Stream<Place> emptyStream = Stream.empty();
      given(placeRepository.streamAllBy()).willReturn(emptyStream);

      // when: 전체 Place 임베딩 요청
      placeEmbeddingService.embedAllPlaces();

      // then: 조회는 수행되지만 처리할 데이터가 없으면 안전하게 종료
      then(placeRepository).should(times(1)).streamAllBy();
      then(vectorStoreRepository).should(never()).deleteByPlaceId(any(Long.class));
      then(documentConverter).should(never()).toDocument(any(Place.class));
      then(vectorStoreRepository).should(never()).saveAll(anyList());
    }

    @Test
    void 배치_크기_정확히_일치할_때_한_번만_저장할_수_있다() {
      // given: 배치 크기와 정확히 같은 개수의 Place 데이터 생성
      List<Place> places = new ArrayList<>();
      for (int i = 1; i <= batchSize; i++) {
        Place place =
            PlaceFixture.withFullEntity(
                (long) i,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 10, 10, 0),
                PlaceFixture.create());
        places.add(place);
      }

      Stream<Place> placeStream = places.stream();

      // given: Repository와 Converter 모킹
      given(placeRepository.streamAllBy()).willReturn(placeStream);
      given(documentConverter.toDocument(any(Place.class))).willReturn(mockDocument);

      // when: 전체 Place 임베딩 요청
      placeEmbeddingService.embedAllPlaces();

      // then: 모든 Place를 임베딩 처리
      then(vectorStoreRepository).should(times(batchSize)).deleteByPlaceId(any(Long.class));
      then(documentConverter).should(times(batchSize)).toDocument(any(Place.class));

      // then: 배치 크기와 정확히 일치하므로 한 번만 저장
      then(vectorStoreRepository).should(times(1)).saveAll(anyList());
    }
  }

  @Nested
  class 수정된_Place_임베딩_시 {

    @Test
    void 특정_시점_이후_수정된_Place만_임베딩할_수_있다() {
      // given: 특정 시점 기준 설정
      LocalDateTime since = LocalDateTime.of(2025, 1, 10, 0, 0);

      // given: 기준 시각 이후 수정된 Place 데이터 준비
      Place place1 =
          PlaceFixture.withFullEntity(
              1L,
              LocalDateTime.of(2025, 1, 1, 0, 0),
              LocalDateTime.of(2025, 1, 11, 10, 0),
              PlaceFixture.createWithFullInfo());
      Place place2 =
          PlaceFixture.withFullEntity(
              2L,
              LocalDateTime.of(2025, 1, 1, 0, 0),
              LocalDateTime.of(2025, 1, 12, 11, 0),
              PlaceFixture.createGangnam());

      Stream<Place> placeStream = Stream.of(place1, place2);

      // given: Repository와 Converter 모킹
      given(placeRepository.streamByUpdatedAtAfter(since)).willReturn(placeStream);
      given(documentConverter.toDocument(any(Place.class))).willReturn(mockDocument);

      // when: 특정 시점 이후 수정된 Place 임베딩 요청
      placeEmbeddingService.embedModifiedPlaces(since);

      // then: 기준 시점 이후 수정된 Place만 조회하여 임베딩 처리
      then(placeRepository).should(times(1)).streamByUpdatedAtAfter(eq(since));
      then(vectorStoreRepository).should(times(2)).deleteByPlaceId(any(Long.class));
      then(documentConverter).should(times(2)).toDocument(any(Place.class));
    }

    @Test
    void 대량의_수정된_Place를_배치로_처리할_수_있다() {
      // given: 특정 시점과 대량의 수정된 Place 준비
      LocalDateTime since = LocalDateTime.of(2025, 1, 1, 0, 0);

      // given: 배치 크기를 초과하는 데이터 생성 (배치 크기 * 1.2배)
      int totalCount = (int) (batchSize * 1.2);
      List<Place> places = new ArrayList<>();
      for (int i = 1; i <= totalCount; i++) {
        Place place =
            PlaceFixture.withFullEntity(
                (long) i,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 10, 10, 0),
                PlaceFixture.create());
        places.add(place);
      }

      Stream<Place> placeStream = places.stream();

      // given: Repository와 Converter 모킹
      given(placeRepository.streamByUpdatedAtAfter(since)).willReturn(placeStream);
      given(documentConverter.toDocument(any(Place.class))).willReturn(mockDocument);

      // when: 특정 시점 이후 수정된 Place 임베딩 요청
      placeEmbeddingService.embedModifiedPlaces(since);

      // then: 모든 수정된 Place를 임베딩 처리
      then(vectorStoreRepository).should(times(totalCount)).deleteByPlaceId(any(Long.class));
      then(documentConverter).should(times(totalCount)).toDocument(any(Place.class));
    }

    @Test
    void 수정된_Place가_없을_때_안전하게_종료할_수_있다() {
      // given: 특정 시점 기준 설정
      LocalDateTime since = LocalDateTime.of(2025, 1, 20, 0, 0);

      // given: 빈 스트림 반환 (수정된 Place가 없음)
      Stream<Place> emptyStream = Stream.empty();
      given(placeRepository.streamByUpdatedAtAfter(since)).willReturn(emptyStream);

      // when: 특정 시점 이후 수정된 Place 임베딩 요청
      placeEmbeddingService.embedModifiedPlaces(since);

      // then: 조회는 수행되지만 처리할 데이터가 없으면 안전하게 종료
      then(placeRepository).should(times(1)).streamByUpdatedAtAfter(eq(since));
      then(vectorStoreRepository).should(never()).deleteByPlaceId(any(Long.class));
      then(documentConverter).should(never()).toDocument(any(Place.class));
      then(vectorStoreRepository).should(never()).saveAll(anyList());
    }
  }

  @Nested
  class 단일_Place_임베딩_시 {

    @Test
    void 특정_Place_ID를_지정하면_해당_Place만_임베딩할_수_있다() {
      // given: 특정 Place ID와 데이터 준비
      Long placeId = 1L;

      // given: Place 데이터 준비
      Place place =
          PlaceFixture.withFullEntity(
              placeId,
              LocalDateTime.of(2025, 1, 1, 0, 0),
              LocalDateTime.of(2025, 1, 10, 10, 0),
              PlaceFixture.createWithFullInfo());

      // given: Repository와 Converter 모킹
      given(placeRepository.findById(placeId)).willReturn(Optional.of(place));
      given(documentConverter.toDocument(place)).willReturn(mockDocument);

      // when: 특정 Place ID로 임베딩 요청
      placeEmbeddingService.embedPlace(placeId);

      // then: 해당 Place를 조회하고 임베딩 처리 후 저장
      then(placeRepository).should(times(1)).findById(eq(placeId));
      then(vectorStoreRepository).should(times(1)).deleteByPlaceId(eq(placeId));
      then(documentConverter).should(times(1)).toDocument(eq(place));
      then(vectorStoreRepository).should(times(1)).save(eq(mockDocument));
    }

    @Test
    void 존재하지_않는_Place_ID로_요청하면_예외가_발생해야_한다() {
      // given: 존재하지 않는 Place ID 준비
      Long nonExistentPlaceId = 999L;

      // given: Repository가 빈 Optional 반환
      given(placeRepository.findById(nonExistentPlaceId)).willReturn(Optional.empty());

      // when & then: 존재하지 않는 Place ID로 임베딩 요청 시 예외 발생
      assertThatThrownBy(() -> placeEmbeddingService.embedPlace(nonExistentPlaceId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Place not found with ID: " + nonExistentPlaceId);

      // then: 조회는 수행되지만 Place를 찾을 수 없어 이후 처리는 진행되지 않음
      then(placeRepository).should(times(1)).findById(eq(nonExistentPlaceId));
      then(vectorStoreRepository).should(never()).deleteByPlaceId(any(Long.class));
      then(documentConverter).should(never()).toDocument(any(Place.class));
      then(vectorStoreRepository).should(never()).save(any(Document.class));
    }

    @Test
    void 임베딩할_때_기존_데이터를_삭제하고_새로_저장해야_한다() {
      // given: Place ID와 데이터 준비
      Long placeId = 5L;

      // given: Place 데이터 준비
      Place place =
          PlaceFixture.withFullEntity(
              placeId,
              LocalDateTime.of(2025, 1, 1, 0, 0),
              LocalDateTime.of(2025, 1, 8, 9, 0),
              PlaceFixture.createGangnam());

      // given: Repository와 Converter 모킹
      given(placeRepository.findById(placeId)).willReturn(Optional.of(place));
      given(documentConverter.toDocument(place)).willReturn(mockDocument);

      // when: 특정 Place ID로 임베딩 요청
      placeEmbeddingService.embedPlace(placeId);

      // then: 기존 임베딩 데이터 삭제 후 새로운 데이터 저장
      then(vectorStoreRepository).should(times(1)).deleteByPlaceId(eq(placeId));
      then(vectorStoreRepository).should(times(1)).save(eq(mockDocument));

      // then: Place를 Document로 변환
      then(documentConverter).should(times(1)).toDocument(eq(place));
    }

    @Test
    void 여러_Place를_순차적으로_임베딩할_수_있다() {
      // given: 여러 Place ID와 데이터 준비
      Long placeId1 = 1L;
      Long placeId2 = 2L;
      Long placeId3 = 3L;

      // given: Place 데이터 준비
      Place place1 =
          PlaceFixture.withFullEntity(
              placeId1,
              LocalDateTime.of(2025, 1, 1, 0, 0),
              LocalDateTime.of(2025, 1, 10, 10, 0),
              PlaceFixture.createWithFullInfo());
      Place place2 =
          PlaceFixture.withFullEntity(
              placeId2,
              LocalDateTime.of(2025, 1, 1, 0, 0),
              LocalDateTime.of(2025, 1, 11, 11, 0),
              PlaceFixture.createGangnam());
      Place place3 =
          PlaceFixture.withFullEntity(
              placeId3,
              LocalDateTime.of(2025, 1, 1, 0, 0),
              LocalDateTime.of(2025, 1, 12, 12, 0),
              PlaceFixture.createJejuAirport());

      // given: Repository와 Converter 모킹
      given(placeRepository.findById(placeId1)).willReturn(Optional.of(place1));
      given(placeRepository.findById(placeId2)).willReturn(Optional.of(place2));
      given(placeRepository.findById(placeId3)).willReturn(Optional.of(place3));
      given(documentConverter.toDocument(any(Place.class))).willReturn(mockDocument);

      // when: 각 Place ID를 순차적으로 임베딩 요청
      placeEmbeddingService.embedPlace(placeId1);
      placeEmbeddingService.embedPlace(placeId2);
      placeEmbeddingService.embedPlace(placeId3);

      // then: 각 Place를 순차적으로 조회하고 임베딩 처리
      then(placeRepository).should(times(1)).findById(eq(placeId1));
      then(placeRepository).should(times(1)).findById(eq(placeId2));
      then(placeRepository).should(times(1)).findById(eq(placeId3));

      // then: 각 Place에 대해 기존 데이터 삭제 후 새로 저장
      then(vectorStoreRepository).should(times(3)).deleteByPlaceId(any(Long.class));
      then(vectorStoreRepository).should(times(3)).save(any(Document.class));
    }
  }
}
