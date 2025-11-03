package com.swygbro.airoad.backend.tourdata.domain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** TourAPI 카테고리 분류 체계 (cat1, cat2, cat3) - 전체 카테고리 */
public class CategoryType {

  /** 대분류 (cat1) */
  @Getter
  @RequiredArgsConstructor
  public enum Cat1 {
    NATURE("A01", "자연"),
    CULTURE("A02", "인문(문화/예술/역사)"),
    LEISURE("A03", "레포츠"),
    SHOPPING("A04", "쇼핑"),
    FOOD("A05", "음식"),
    ACCOMMODATION("B02", "숙박"),
    TRAVEL_COURSE("C01", "여행코스");

    private final String code;
    private final String name;

    public static Cat1 fromCode(String code) {
      return Arrays.stream(values()).filter(cat -> cat.code.equals(code)).findFirst().orElse(null);
    }
  }

  /** 중분류 (cat2) - 전체 카테고리 */
  @Getter
  @RequiredArgsConstructor
  public enum Cat2 {
    // A01 자연
    NATURE_TOURISM("A0101", "A01", "자연관광지"),
    NATURE_RECREATION("A0102", "A01", "자연휴양림"),

    // A02 인문(문화/예술/역사)
    HISTORICAL_SITE("A0201", "A02", "역사관광지"),
    LEISURE_FACILITY("A0202", "A02", "휴양관광지"),
    EXPERIENCE("A0203", "A02", "체험관광지"),
    INDUSTRIAL("A0204", "A02", "산업관광지"),
    ARCHITECTURE("A0205", "A02", "건축/조형물"),
    CULTURE_FACILITY("A0206", "A02", "문화시설"),
    FESTIVAL("A0207", "A02", "축제"),
    PERFORMANCE("A0208", "A02", "공연/행사"),

    // A03 레포츠
    LEISURE_SPORTS("A0301", "A03", "레포츠소개"),
    LAND_SPORTS("A0302", "A03", "육상 레포츠"),
    WATER_SPORTS("A0303", "A03", "수상 레포츠"),
    AIR_SPORTS("A0304", "A03", "항공 레포츠"),
    COMPLEX_SPORTS("A0305", "A03", "복합 레포츠"),

    // A04 쇼핑
    SHOPPING_INTRO("A0401", "A04", "쇼핑"),
    DEPARTMENT_STORE("A0402", "A04", "백화점"),
    OUTLET("A0403", "A04", "아울렛"),
    MARKET("A0404", "A04", "전통시장"),
    SHOPPING_MALL("A0405", "A04", "상설시장"),
    DUTY_FREE("A0406", "A04", "면세점"),
    SHOPPING_CENTER("A0407", "A04", "대형마트"),
    FOLK_CRAFT("A0408", "A04", "특산물판매점"),
    SPECIALTY_STORE("A0409", "A04", "공예/공방"),

    // A05 음식
    FOOD_INTRO("A0501", "A05", "음식점"),
    KOREAN_FOOD("A0502", "A05", "한식"),
    WESTERN_FOOD("A0503", "A05", "서양식"),
    JAPANESE_FOOD("A0504", "A05", "일식"),
    CHINESE_FOOD("A0505", "A05", "중식"),
    ASIAN_FOOD("A0506", "A05", "아시아식"),
    CAFE("A0507", "A05", "제과/제빵/카페"),
    FOOD_ETC("A0508", "A05", "기타"),

    // B02 숙박
    ACCOMMODATION_INTRO("B0201", "B02", "숙박시설소개"),
    HOTEL("B0202", "B02", "관광호텔"),
    CONDO("B0203", "B02", "콘도미니엄"),
    YOUTH_HOSTEL("B0204", "B02", "유스호스텔"),
    PENSION("B0205", "B02", "펜션"),
    MOTEL("B0206", "B02", "민박"),
    GUESTHOUSE("B0207", "B02", "게스트하우스"),
    HOMESTAY("B0208", "B02", "홈스테이"),
    RESORT("B0209", "B02", "서비스드레지던스"),
    HANOK("B0210", "B02", "한옥"),

    // C01 여행코스
    TRAVEL_COURSE("C0112", "C01", "가족코스"),
    COUPLE_COURSE("C0113", "C01", "나홀로코스"),
    HEALING_COURSE("C0114", "C01", "힐링코스"),
    WALKING_COURSE("C0115", "C01", "도보코스"),
    ECOLOGY_COURSE("C0116", "C01", "캠핑코스"),
    BIKE_COURSE("C0117", "C01", "자전거코스");

    private final String code;
    private final String cat1Code;
    private final String name;

    private static final Map<String, Cat2> CODE_MAP = new HashMap<>();

    static {
      for (Cat2 cat2 : values()) {
        CODE_MAP.put(cat2.code, cat2);
      }
    }

    public static Cat2 fromCode(String code) {
      return CODE_MAP.get(code);
    }

    public static String getName(String code) {
      Cat2 cat2 = fromCode(code);
      return cat2 != null ? cat2.name : "기타";
    }

    /**
     * Cat2 코드로 PlaceThemeType 목록 조회
     *
     * @return 해당 카테고리의 테마 Set
     */
    public Set<PlaceThemeType> getThemes() {
      Set<PlaceThemeType> themes = new HashSet<>();

      switch (this) {
        case NATURE_TOURISM:
          themes.add(PlaceThemeType.FAMOUS_SPOT);
          themes.add(PlaceThemeType.HEALING);
          themes.add(PlaceThemeType.SNS_HOTSPOT);
          break;

        case NATURE_RECREATION:
          themes.add(PlaceThemeType.HEALING);
          themes.add(PlaceThemeType.EXPERIENCE_ACTIVITY);
          themes.add(PlaceThemeType.FAMOUS_SPOT);
          break;

        case HISTORICAL_SITE:
          themes.add(PlaceThemeType.FAMOUS_SPOT);
          themes.add(PlaceThemeType.CULTURE_ART);
          break;

        case LEISURE_FACILITY:
          themes.add(PlaceThemeType.HEALING);
          themes.add(PlaceThemeType.EXPERIENCE_ACTIVITY);
          themes.add(PlaceThemeType.FAMOUS_SPOT);
          break;

        case EXPERIENCE:
          themes.add(PlaceThemeType.EXPERIENCE_ACTIVITY);
          themes.add(PlaceThemeType.SNS_HOTSPOT);
          break;

        case INDUSTRIAL:
          themes.add(PlaceThemeType.FAMOUS_SPOT);
          themes.add(PlaceThemeType.EXPERIENCE_ACTIVITY);
          themes.add(PlaceThemeType.CULTURE_ART);
          break;

        case ARCHITECTURE:
          themes.add(PlaceThemeType.CULTURE_ART);
          themes.add(PlaceThemeType.SNS_HOTSPOT);
          themes.add(PlaceThemeType.FAMOUS_SPOT);
          break;

        case CULTURE_FACILITY:
          themes.add(PlaceThemeType.CULTURE_ART);
          themes.add(PlaceThemeType.FAMOUS_SPOT);
          break;

        case FESTIVAL:
          themes.add(PlaceThemeType.EXPERIENCE_ACTIVITY);
          themes.add(PlaceThemeType.SNS_HOTSPOT);
          themes.add(PlaceThemeType.CULTURE_ART);
          break;

        case PERFORMANCE:
          themes.add(PlaceThemeType.CULTURE_ART);
          themes.add(PlaceThemeType.SNS_HOTSPOT);
          break;

        case LEISURE_SPORTS:
        case LAND_SPORTS:
        case WATER_SPORTS:
        case AIR_SPORTS:
        case COMPLEX_SPORTS:
          themes.add(PlaceThemeType.EXPERIENCE_ACTIVITY);
          break;

        case SHOPPING_INTRO:
          themes.add(PlaceThemeType.SHOPPING);
          break;

        case DEPARTMENT_STORE:
        case OUTLET:
        case SHOPPING_MALL:
        case DUTY_FREE:
        case SHOPPING_CENTER:
          themes.add(PlaceThemeType.SHOPPING);
          break;

        case MARKET:
          themes.add(PlaceThemeType.SHOPPING);
          themes.add(PlaceThemeType.FAMOUS_SPOT);
          break;

        case FOLK_CRAFT:
          themes.add(PlaceThemeType.SHOPPING);
          themes.add(PlaceThemeType.CULTURE_ART);
          break;

        case SPECIALTY_STORE:
          themes.add(PlaceThemeType.SHOPPING);
          themes.add(PlaceThemeType.EXPERIENCE_ACTIVITY);
          themes.add(PlaceThemeType.CULTURE_ART);
          break;

        case FOOD_INTRO:
        case KOREAN_FOOD:
        case WESTERN_FOOD:
        case JAPANESE_FOOD:
        case CHINESE_FOOD:
        case ASIAN_FOOD:
        case FOOD_ETC:
          themes.add(PlaceThemeType.RESTAURANT);
          break;

        case CAFE:
          themes.add(PlaceThemeType.RESTAURANT);
          themes.add(PlaceThemeType.SNS_HOTSPOT);
          break;

        case ACCOMMODATION_INTRO:
        case HOTEL:
        case CONDO:
        case YOUTH_HOSTEL:
        case PENSION:
        case MOTEL:
        case GUESTHOUSE:
        case HOMESTAY:
        case RESORT:
          themes.add(PlaceThemeType.HEALING);
          break;

        case HANOK:
          themes.add(PlaceThemeType.HEALING);
          themes.add(PlaceThemeType.CULTURE_ART);
          themes.add(PlaceThemeType.SNS_HOTSPOT);
          break;

        case TRAVEL_COURSE:
          themes.add(PlaceThemeType.FAMOUS_SPOT);
          themes.add(PlaceThemeType.EXPERIENCE_ACTIVITY);
          themes.add(PlaceThemeType.HEALING);
          break;

        case COUPLE_COURSE:
          themes.add(PlaceThemeType.FAMOUS_SPOT);
          themes.add(PlaceThemeType.HEALING);
          themes.add(PlaceThemeType.SNS_HOTSPOT);
          break;

        case HEALING_COURSE:
          themes.add(PlaceThemeType.HEALING);
          break;

        case WALKING_COURSE:
        case ECOLOGY_COURSE:
          themes.add(PlaceThemeType.HEALING);
          themes.add(PlaceThemeType.EXPERIENCE_ACTIVITY);
          break;

        case BIKE_COURSE:
          themes.add(PlaceThemeType.FAMOUS_SPOT);
          themes.add(PlaceThemeType.HEALING);

          themes.add(PlaceThemeType.EXPERIENCE_ACTIVITY);
          break;

        default:
          break;
      }

      return themes;
    }
  }

  /** 소분류 (cat3) - 전체 카테고리 */
  @Getter
  @RequiredArgsConstructor
  public enum Cat3 {
    // A0101 자연 > 자연관광지
    NATIONAL_PARK("A01010100", "A0101", "국립공원"),
    PROVINCIAL_PARK("A01010200", "A0101", "도립공원"),
    COUNTY_PARK("A01010300", "A0101", "군립공원"),
    VALLEY("A01010400", "A0101", "계곡"),
    BEACH("A01010500", "A0101", "해변"),
    RIVER("A01010600", "A0101", "하천"),
    SWAMP("A01010700", "A0101", "약수터"),
    FALLS("A01010800", "A0101", "폭포"),
    POND("A01010900", "A0101", "약수/온천"),
    CAVE("A01011000", "A0101", "동굴"),
    ROCK("A01011100", "A0101", "암석/기암괴석"),
    MOUNTAIN_PEAK("A01011200", "A0101", "산"),
    ISLAND("A01011300", "A0101", "섬"),
    COASTAL_TRAIL("A01011400", "A0101", "해안절경"),
    NATURAL_FOREST("A01011500", "A0101", "수목원"),
    NATURAL_LAKE("A01011600", "A0101", "호수"),
    PORT("A01011700", "A0101", "항구/포구"),
    LIGHTHOUSE("A01011800", "A0101", "등대"),
    OBSERVATORY("A01011900", "A0101", "전망대"),
    WATER_RECREATION("A01012000", "A0101", "수련시설"),
    NATURAL_MONUMENT("A01012100", "A0101", "자연생태관광지"),
    FARM("A01012200", "A0101", "농/산/어촌 체험"),

    // A0102 자연 > 자연휴양림
    RECREATIONAL_FOREST("A01020100", "A0102", "자연휴양림"),
    NATIONAL_FOREST("A01020200", "A0102", "수목원"),
    ECO_PARK("A01020300", "A0102", "생태공원"),
    ARBORETUM("A01020400", "A0102", "수목원"),

    // A0201 인문 > 역사관광지
    PALACE("A02010100", "A0201", "고궁"),
    CASTLE("A02010200", "A0201", "성"),
    HISTORIC_SITE("A02010300", "A0201", "사적지"),
    TOMB("A02010400", "A0201", "고분/왕릉"),
    STONE_MONUMENT("A02010500", "A0201", "석탑/석비"),
    TEMPLE("A02010600", "A0201", "사찰"),
    SHRINE("A02010700", "A0201", "종교성지"),
    HISTORIC_GATE("A02010800", "A0201", "문"),
    OLD_HOUSE("A02010900", "A0201", "고택"),
    FORTIFICATION("A02011000", "A0201", "생가"),
    TRADITIONAL_VILLAGE("A02011100", "A0201", "민속마을"),
    BATTLEFIELD("A02011200", "A0201", "유적지/사적지"),
    STATUE("A02011300", "A0201", "기념탑/기념비/전승비"),
    HISTORIC_BUILDING("A02011400", "A0201", "전통건축물"),
    LIGHTHOUSE_HISTORIC("A02011500", "A0201", "동상"),

    // A0202 인문 > 휴양관광지
    RESORT_COMPLEX("A02020100", "A0202", "관광단지"),
    AMUSEMENT_PARK("A02020200", "A0202", "관광지"),
    THEME_PARK("A02020300", "A0202", "유원지"),
    SPA("A02020400", "A0202", "관광휴게시설"),
    HOT_SPRING("A02020500", "A0202", "휴양림"),
    PARK("A02020600", "A0202", "공원"),
    BEACH_RESORT("A02020700", "A0202", "유람선/리프트"),

    // A0203 인문 > 체험관광지
    EXPERIENCE_SITE("A02030100", "A0203", "농/산/어촌체험"),
    FOLK_VILLAGE("A02030200", "A0203", "전통체험"),
    TRADITIONAL_MARKET("A02030300", "A0203", "산사체험"),
    HANDS_ON_MUSEUM("A02030400", "A0203", "이색체험"),
    FARM_EXPERIENCE("A02030500", "A0203", "관광공장"),
    WORKSHOP("A02030600", "A0203", "이색거리"),
    CRAFT_EXPERIENCE("A02030700", "A0203", "체험관광지"),

    // A0204 인문 > 산업관광지
    INDUSTRIAL_TOUR("A02040100", "A0204", "산업단지"),
    FACTORY_TOUR("A02040400", "A0204", "공장/산업시설"),
    OBSERVATORY_INDUSTRY("A02040600", "A0204", "전망대"),
    MINE("A02040800", "A0204", "광산"),
    POWERPLANT("A02041000", "A0204", "발전소"),

    // A0205 인문 > 건축/조형물
    TOWER("A02050100", "A0205", "다리/대교"),
    BRIDGE("A02050200", "A0205", "타워"),
    BUILDING("A02050300", "A0205", "건물"),
    SCULPTURE("A02050400", "A0205", "기념탑/기념비"),
    FOUNTAIN("A02050500", "A0205", "분수"),

    // A0206 인문 > 문화시설
    MUSEUM("A02060100", "A0206", "박물관"),
    MEMORIAL_HALL("A02060200", "A0206", "기념관"),
    EXHIBITION_HALL("A02060300", "A0206", "전시관"),
    CONVENTION_CENTER("A02060400", "A0206", "컨벤션센터"),
    THEATER("A02060500", "A0206", "공연장"),
    LIBRARY("A02060600", "A0206", "도서관"),
    ART_GALLERY("A02060700", "A0206", "미술관/화랑"),
    BROADCAST_STATION("A02060800", "A0206", "방송국"),
    CULTURAL_CENTER("A02060900", "A0206", "문화원"),
    ARTS_CENTER("A02061000", "A0206", "문화/예술회관"),
    MOVIE_THEATER("A02061100", "A0206", "영화관"),
    SCIENCE_CENTER("A02061200", "A0206", "어린이회관"),
    AQUARIUM("A02061300", "A0206", "수족관"),
    ZOO("A02061400", "A0206", "동물원"),
    BOTANICAL_GARDEN("A02061500", "A0206", "식물원"),
    ART_VILLAGE("A02061600", "A0206", "공예/공방"),

    // A0207 인문 > 축제
    CULTURE_FESTIVAL("A02070100", "A0207", "문화관광축제"),
    GENERAL_FESTIVAL("A02070200", "A0207", "일반축제"),

    // A0208 인문 > 공연/행사
    CULTURAL_EVENT("A02080100", "A0208", "전통공연"),
    PERFORMANCE("A02080200", "A0208", "연극"),
    CONCERT("A02080300", "A0208", "뮤지컬"),
    DANCE("A02080400", "A0208", "오페라"),
    MUSICAL("A02080500", "A0208", "전시회"),
    MOVIE_EVENT("A02080600", "A0208", "박람회"),
    EXHIBITION("A02080700", "A0208", "컨벤션"),
    SPORTS_EVENT("A02080800", "A0208", "무용"),
    OTHER_EVENT("A02080900", "A0208", "클래식음악회"),
    PARADE("A02081000", "A0208", "대중콘서트"),
    EXPERIENCE_EVENT("A02081100", "A0208", "영화"),
    CELEBRATION("A02081200", "A0208", "스포츠경기"),
    MARKET_EVENT("A02081300", "A0208", "기타행사"),

    // A0301 레포츠 > 레포츠소개
    LEISURE_INTRO("A03010100", "A0301", "레포츠소개"),

    // A0302 레포츠 > 육상 레포츠
    CAMPING("A03020100", "A0302", "서바이벌게임장"),
    AUTO_CAMPING("A03020200", "A0302", "카트경기장"),
    GLAMPING("A03020300", "A0302", "골프장"),
    CARAVAN("A03020400", "A0302", "경마장"),
    HORSEBACK_RIDING("A03020500", "A0302", "경륜장"),
    GOLF("A03020600", "A0302", "카지노"),
    MOUNTAIN_CLIMBING("A03020700", "A0302", "승마장"),
    ROCK_CLIMBING("A03020800", "A0302", "사격장"),
    BUNGEE_JUMP("A03020900", "A0302", "야영장"),
    ATV("A03021000", "A0302", "암벽등반"),
    BICYCLE("A03021100", "A0302", "수렵장"),
    MOTOR_SPORTS("A03021200", "A0302", "사륜오토바이"),
    SHOOTING("A03021300", "A0302", "MTB"),
    PAINTBALL("A03021400", "A0302", "오프로드"),
    CART("A03021500", "A0302", "번지점프"),
    RACING("A03021600", "A0302", "스키/눈썰매장"),
    SLEDDING("A03021700", "A0302", "스케이트장"),
    INLINE_SKATING("A03021800", "A0302", "썰매장"),
    SKATE_PARK("A03021900", "A0302", "수렵장"),

    // A0303 레포츠 > 수상 레포츠
    FISHING("A03030100", "A0303", "윈드서핑/제트스키"),
    SURFING("A03030200", "A0303", "카약/카누"),
    DIVING("A03030300", "A0303", "요트"),
    WATER_SKI("A03030400", "A0303", "스노쿨링/스킨스쿠버다이빙"),
    RAFTING("A03030500", "A0303", "민물낚시"),
    YACHT("A03030600", "A0303", "바다낚시"),
    KAYAK("A03030700", "A0303", "수영"),
    CANOEING("A03030800", "A0303", "래프팅"),

    // A0304 레포츠 > 항공 레포츠
    PARAGLIDING("A03040100", "A0304", "스카이다이빙"),
    SKYDIVING("A03040200", "A0304", "초경량비행"),
    HANG_GLIDING("A03040300", "A0304", "헹글라이딩/패러글라이딩"),
    BALLOON("A03040400", "A0304", "열기구"),

    // A0305 레포츠 > 복합 레포츠
    SPORTS_COMPLEX("A03050100", "A0305", "복합 레포츠"),

    // A0401 쇼핑 > 쇼핑
    SHOPPING_DISTRICT("A04010100", "A0401", "쇼핑"),

    // A0402 쇼핑 > 백화점
    DEPT_STORE("A04020100", "A0402", "백화점"),

    // A0403 쇼핑 > 아울렛
    OUTLET_MALL("A04030100", "A0403", "아울렛"),

    // A0404 쇼핑 > 전통시장
    TRADITIONAL_MKT("A04040100", "A0404", "전통시장"),

    // A0405 쇼핑 > 상설시장
    PERMANENT_MARKET("A04050100", "A0405", "상설시장"),
    FLEA_MARKET("A04050200", "A0405", "5일장"),

    // A0406 쇼핑 > 면세점
    DUTY_FREE_SHOP("A04060100", "A0406", "면세점"),

    // A0407 쇼핑 > 대형마트
    SUPER_MARKET("A04070100", "A0407", "대형마트"),
    WHOLESALE_MARKET("A04070200", "A0407", "도매시장"),

    // A0502 음식 > 한식
    KOREAN_MAIN("A05020100", "A0502", "한정식"),
    KOREAN_BIBIMBAP("A05020200", "A0502", "면요리"),
    KOREAN_NOODLE("A05020300", "A0502", "죽"),
    KOREAN_PORRIDGE("A05020400", "A0502", "육류"),
    KOREAN_MEAT("A05020500", "A0502", "해물"),
    KOREAN_SEAFOOD("A05020600", "A0502", "구이"),
    KOREAN_GRILLED("A05020700", "A0502", "닭요리"),
    KOREAN_CHICKEN("A05020800", "A0502", "찌개"),
    KOREAN_STEW("A05020900", "A0502", "백반/한정식"),
    KOREAN_TABLE("A05021000", "A0502", "순대"),

    // A0503 음식 > 서양식
    WESTERN_MAIN("A05030100", "A0503", "스테이크"),
    STEAK("A05030200", "A0503", "패밀리레스토랑"),
    FAMILY_RESTAURANT("A05030300", "A0503", "이탈리안"),
    ITALIAN("A05030400", "A0503", "프랑스식"),
    FRENCH("A05030500", "A0503", "패스트푸드"),
    FAST_FOOD("A05030600", "A0503", "브런치"),

    // A0504 음식 > 일식
    JAPANESE_MAIN("A05040100", "A0504", "일식"),
    SUSHI("A05040200", "A0504", "초밥/롤"),
    UDON("A05040300", "A0504", "우동/소바"),
    RAMEN("A05040400", "A0504", "돈까스/덮밥"),
    SASHIMI("A05040500", "A0504", "일본식라면"),

    // A0505 음식 > 중식
    CHINESE_MAIN("A05050100", "A0505", "중식"),
    CHINESE_NOODLE("A05050200", "A0505", "중화요리"),
    CHINESE_DIM_SUM("A05050300", "A0505", "샤브샤브"),

    // A0506 음식 > 아시아식
    ASIAN_MAIN("A05060100", "A0506", "아시아음식"),
    THAI("A05060200", "A0506", "인도음식"),
    VIETNAMESE("A05060300", "A0506", "태국음식"),
    INDIAN("A05060400", "A0506", "베트남음식"),

    // A0507 음식 > 제과/제빵/카페
    BAKERY("A05070100", "A0507", "제과점"),
    DESSERT_CAFE("A05070200", "A0507", "카페"),
    COFFEE_SHOP("A05070300", "A0507", "차/전통찻집"),
    TEA_HOUSE("A05070400", "A0507", "주스"),
    BRUNCH_CAFE("A05070500", "A0507", "베이커리"),

    // A0508 음식 > 기타
    FOOD_BAR("A05080100", "A0508", "술집"),
    BUFFET("A05080200", "A0508", "퓨전음식"),
    FUSION("A05080300", "A0508", "뷔페"),

    // B0201 숙박 > 숙박시설소개
    ACCOMMODATION_INTRO_SUB("B02010100", "B0201", "숙박시설소개"),

    // B0202 숙박 > 관광호텔
    TOURIST_HOTEL("B02020100", "B0202", "관광호텔"),
    BUSINESS_HOTEL("B02020200", "B0202", "수상관광호텔"),
    FAMILY_HOTEL("B02020300", "B0202", "전통호텔"),
    HANOK_HOTEL("B02020400", "B0202", "가족호텔"),
    RESORT_HOTEL("B02020500", "B0202", "호스텔"),
    BENIKEA("B02020600", "B0202", "베니키아"),
    GOODSTAY("B02020700", "B0202", "굿스테이"),

    // B0203 숙박 > 콘도미니엄
    CONDO_MAIN("B02030100", "B0203", "콘도미니엄"),

    // B0204 숙박 > 유스호스텔
    YOUTH_HOSTEL_MAIN("B02040100", "B0204", "유스호스텔"),

    // B0205 숙박 > 펜션
    PENSION_MAIN("B02050100", "B0205", "펜션"),

    // B0206 숙박 > 민박
    MOTEL_MAIN("B02060100", "B0206", "민박"),

    // B0207 숙박 > 게스트하우스
    GUESTHOUSE_MAIN("B02070100", "B0207", "게스트하우스"),

    // B0208 숙박 > 홈스테이
    HOMESTAY_MAIN("B02080100", "B0208", "홈스테이"),

    // B0209 숙박 > 서비스드레지던스
    SERVICED_RESIDENCE("B02090100", "B0209", "서비스드레지던스"),

    // B0210 숙박 > 한옥
    HANOK_MAIN("B02100100", "B0210", "한옥");

    private final String code;
    private final String cat2Code;
    private final String name;

    private static final Map<String, Cat3> CODE_MAP = new HashMap<>();

    static {
      for (Cat3 cat3 : values()) {
        CODE_MAP.put(cat3.code, cat3);
      }
    }

    public static Cat3 fromCode(String code) {
      return CODE_MAP.get(code);
    }

    public static String getName(String code) {
      Cat3 cat3 = fromCode(code);
      return cat3 != null ? cat3.name : "기타";
    }
  }

  /**
   * 전체 카테고리 경로 조회
   *
   * @param cat1Code cat1 코드
   * @param cat2Code cat2 코드
   * @param cat3Code cat3 코드
   * @return "자연 > 자연관광지 > 국립공원" 형식
   */
  public static String getFullCategoryPath(String cat1Code, String cat2Code, String cat3Code) {
    StringBuilder path = new StringBuilder();

    Cat1 cat1 = Cat1.fromCode(cat1Code);
    if (cat1 != null) {
      path.append(cat1.getName());
    }

    Cat2 cat2 = Cat2.fromCode(cat2Code);
    if (cat2 != null) {
      path.append(" > ").append(cat2.getName());
    }

    Cat3 cat3 = Cat3.fromCode(cat3Code);
    if (cat3 != null) {
      path.append(" > ").append(cat3.getName());
    }

    return path.length() > 0 ? path.toString() : "미분류";
  }
}
