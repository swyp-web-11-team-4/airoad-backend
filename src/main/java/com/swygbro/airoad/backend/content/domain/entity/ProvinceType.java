package com.swygbro.airoad.backend.content.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대한민국 광역자치단체(시/도) 공식 명칭
 *
 * <p>DB 필터링 및 검색에 사용되는 공식 행정구역 명칭을 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ProvinceType {
  SEOUL("서울특별시"),
  BUSAN("부산광역시"),
  DAEGU("대구광역시"),
  INCHEON("인천광역시"),
  GWANGJU("광주광역시"),
  DAEJEON("대전광역시"),
  ULSAN("울산광역시"),
  SEJONG("세종특별자치시"),
  GYEONGGI("경기도"),
  GANGWON("강원특별자치도"),
  CHUNGBUK("충청북도"),
  CHUNGNAM("충청남도"),
  JEONBUK("전북특별자치도"),
  JEONNAM("전라남도"),
  GYEONGBUK("경상북도"),
  GYEONGNAM("경상남도"),
  JEJU("제주특별자치도");

  private final String officialName;
}
