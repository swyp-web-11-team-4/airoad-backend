package com.swygbro.airoad.backend.content.domain.converter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.content.domain.entity.Place;

import lombok.extern.slf4j.Slf4j;

/**
 * Place 엔티티를 Spring AI Document로 변환하는 컨버터
 *
 * <p>RAG 시스템에서 사용할 수 있도록 Place의 텍스트 정보를 Document의 content로 변환하고, 메타데이터를 추가합니다.
 */
@Slf4j
@Component
public class PlaceDocumentConverter {

  private static final String METADATA_PLACE_ID = "placeId";
  private static final String METADATA_NAME = "name";
  private static final String METADATA_ADDRESS = "address";
  private static final String METADATA_LATITUDE = "latitude";
  private static final String METADATA_LONGITUDE = "longitude";
  private static final String METADATA_IS_MUST_VISIT = "isMustVisit";
  private static final String METADATA_PLACE_SCORE = "placeScore";
  private static final String METADATA_CONTENT_HASH = "contentHash";
  private static final String METADATA_EMBEDDED_AT = "embeddedAt";
  private static final String METADATA_PLACE_UPDATED_AT = "placeUpdatedAt";

  /**
   * Place 엔티티를 Document로 변환
   *
   * @param place 변환할 Place 엔티티
   * @return Spring AI Document 객체
   */
  public Document toDocument(Place place) {
    String content = buildContent(place);
    Map<String, Object> metadata = buildMetadata(place, content);
    return new Document(content, metadata);
  }

  /**
   * Document의 content 구성
   *
   * <p>모든 텍스트 필드를 포함하여 임베딩 품질을 높입니다: - 장소명 - 주소 - 상세 설명 - 운영 시간 - 휴무일 정보
   */
  private String buildContent(Place place) {
    StringBuilder content = new StringBuilder();

    content.append("장소명: ").append(place.getLocation().getName()).append("\n");
    content.append("주소: ").append(place.getLocation().getAddress()).append("\n");

    if (place.getDescription() != null && !place.getDescription().isBlank()) {
      content.append("설명: ").append(place.getDescription()).append("\n");
    }

    if (place.getOperatingHours() != null && !place.getOperatingHours().isBlank()) {
      content.append("운영 시간: ").append(place.getOperatingHours()).append("\n");
    }

    if (place.getHolidayInfo() != null && !place.getHolidayInfo().isBlank()) {
      content.append("휴무일: ").append(place.getHolidayInfo());
    }

    return content.toString().trim();
  }

  /**
   * Document의 metadata 구성
   *
   * <p>벡터 검색 후 필터링 및 정렬에 사용할 메타데이터를 구성합니다.
   *
   * @param place 변환할 Place 엔티티
   * @param content 이미 생성된 content 문자열 (중복 생성 방지)
   * @return metadata Map
   */
  private Map<String, Object> buildMetadata(Place place, String content) {
    String contentHash = calculateContentHash(content);

    return Map.of(
        METADATA_PLACE_ID,
        place.getId(),
        METADATA_NAME,
        place.getLocation().getName(),
        METADATA_ADDRESS,
        place.getLocation().getAddress(),
        METADATA_LATITUDE,
        place.getLocation().getPoint().getY(),
        METADATA_LONGITUDE,
        place.getLocation().getPoint().getX(),
        METADATA_IS_MUST_VISIT,
        place.getIsMustVisit(),
        METADATA_PLACE_SCORE,
        place.getPlaceScore(),
        METADATA_CONTENT_HASH,
        contentHash,
        METADATA_EMBEDDED_AT,
        LocalDateTime.now(ZoneOffset.UTC).toString(),
        METADATA_PLACE_UPDATED_AT,
        place.getUpdatedAt().toString());
  }

  /**
   * Content의 SHA-256 해시값을 계산
   *
   * <p>동일한 content는 동일한 hash를 생성하여 임베딩 재생성 여부를 판단하는 데 사용합니다.
   *
   * @param content 해시를 계산할 content 문자열
   * @return SHA-256 해시값 (Hex 문자열)
   */
  private String calculateContentHash(String content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));

      // byte[] to hex string
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      log.error("SHA-256 algorithm not found", e);
      throw new RuntimeException("Failed to calculate content hash", e);
    }
  }
}
