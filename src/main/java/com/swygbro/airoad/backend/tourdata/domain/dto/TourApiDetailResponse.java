package com.swygbro.airoad.backend.tourdata.domain.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;

/** TourAPI detailCommon2 응답 DTO */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourApiDetailResponse {

  private Response response;

  @Getter
  @NoArgsConstructor
  public static class Response {
    private Header header;
    private Body body;
  }

  @Getter
  @NoArgsConstructor
  public static class Header {
    @JsonProperty("resultCode")
    private String resultCode;

    @JsonProperty("resultMsg")
    private String resultMsg;
  }

  @Getter
  @NoArgsConstructor
  public static class Body {
    private Items items;

    @JsonProperty("numOfRows")
    private Integer numOfRows;

    @JsonProperty("pageNo")
    private Integer pageNo;

    @JsonProperty("totalCount")
    private Integer totalCount;
  }

  @Getter
  @NoArgsConstructor
  public static class Items {
    private List<Item> item;
  }

  @Getter
  @NoArgsConstructor
  public static class Item {
    private Long contentid;
    private Integer contenttypeid;
    private String title;
    private String createdtime;
    private String modifiedtime;
    private String tel;
    private String telname;
    private String homepage;
    private String firstimage;
    private String firstimage2;

    @JsonProperty("cpyrhtDivCd")
    private String cpyrhtDivCd;

    private Integer areacode;
    private Integer sigungucode;

    @JsonProperty("lDongRegnCd")
    private String lDongRegnCd;

    @JsonProperty("lDongSignguCd")
    private String lDongSignguCd;

    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;
    private String cat1;
    private String cat2;
    private String cat3;
    private String addr1;
    private String addr2;
    private String zipcode;
    private Double mapx;
    private Double mapy;
    private Integer mlevel;
    private String overview;
  }
}
