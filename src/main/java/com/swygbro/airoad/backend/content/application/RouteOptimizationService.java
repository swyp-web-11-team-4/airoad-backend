package com.swygbro.airoad.backend.content.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteOptimizationService implements RouteOptimizationUseCase {

  private static final int MORNING_PLACE_COUNT = 1;
  private static final int AFTERNOON_PLACE_COUNT = 1;
  private static final int EVENING_PLACE_COUNT = 1;

  private static final int TOTAL_PLACE_SLOTS =
      MORNING_PLACE_COUNT + AFTERNOON_PLACE_COUNT + EVENING_PLACE_COUNT;
  private static final int TOTAL_RESTAURANT_SLOTS = 2;

  // 앵커 반경
  private static final double MAX_RADIUS_KM = 15.0;

  // 최소 이격 거리 (너무 다닥다닥 붙는 것 방지)
  private static final double MIN_DISTANCE_KM = 3.0;

  private final DistanceCalculationUseCase distanceCalculationUseCase;

  @Override
  public List<Document> optimizeRoute(List<Document> places, List<Document> restaurants) {
    if (places == null || places.isEmpty()) {
      log.warn("경로를 생성할 관광지 후보가 없습니다.");
      return new ArrayList<>();
    }

    // 1. 전체 후보군 복사
    List<Document> allPlaces = new ArrayList<>(places);
    List<Document> allRestaurants = new ArrayList<>(restaurants != null ? restaurants : List.of());

    // 2. 앵커 선정
    Document anchor =
        allPlaces.stream()
            .max(Comparator.comparingDouble(Document::getScore))
            .orElseThrow(() -> new IllegalStateException("앵커를 선정할 수 없습니다."));

    log.info("여행 중심 앵커 선정: {} (Score: {})", anchor.getMetadata().get("name"), anchor.getScore());

    // 3. 반경 필터링 (15km 이내)
    List<Document> filteredPlaces = filterByRadius(anchor, allPlaces, MAX_RADIUS_KM);
    List<Document> filteredRestaurants = filterByRadius(anchor, allRestaurants, MAX_RADIUS_KM);

    log.info(
        "반경 {}km 필터링 결과: 관광지 {}→{}개, 식당 {}→{}개",
        MAX_RADIUS_KM,
        allPlaces.size(),
        filteredPlaces.size(),
        allRestaurants.size(),
        filteredRestaurants.size());

    // 4. 상위 후보 선별
    List<Document> selectedPlaces = selectTopCandidates(filteredPlaces, TOTAL_PLACE_SLOTS + 2);
    List<Document> selectedRestaurants =
        selectTopCandidates(filteredRestaurants, TOTAL_RESTAURANT_SLOTS + 2);

    // 5. Smart Greedy 경로 생성
    List<Document> route = buildSmartGreedyRoute(selectedPlaces, selectedRestaurants);

    double greedyDistance = calculateTotalDistance(route);
    log.debug("Greedy 경로 생성 완료 ({}km)", String.format("%.2f", greedyDistance));

    // 6. 최적화 (2-Opt)
    route = optimizeWithSwap(route);

    double optimizedDistance = calculateTotalDistance(route);
    log.info(
        "최종 경로 최적화 완료: {}km → {}km",
        String.format("%.2f", greedyDistance),
        String.format("%.2f", optimizedDistance));

    return route;
  }

  private List<Document> filterByRadius(
      Document anchor, List<Document> candidates, double radiusKm) {
    return candidates.stream()
        .filter(
            target -> {
              if (target.equals(anchor)) return true;
              return calculateDistance(anchor, target) <= radiusKm;
            })
        .collect(Collectors.toList());
  }

  private List<Document> selectTopCandidates(List<Document> documents, int topN) {
    if (documents == null || documents.isEmpty()) return new ArrayList<>();
    return documents.stream()
        .sorted(Comparator.comparingDouble(Document::getScore).reversed())
        .limit(topN)
        .collect(Collectors.toList());
  }

  private List<Document> buildSmartGreedyRoute(List<Document> places, List<Document> restaurants) {
    List<Document> route = new ArrayList<>();
    List<Document> remainingPlaces = new ArrayList<>(places);
    List<Document> remainingRestaurants = new ArrayList<>(restaurants);

    // 앵커 확보
    Document anchor = null;
    if (!remainingPlaces.isEmpty()) {
      anchor =
          remainingPlaces.stream()
              .max(Comparator.comparingDouble(Document::getScore))
              .orElseThrow();
      route.add(anchor);
      remainingPlaces.remove(anchor);
    }

    Document currentLocation = anchor;

    // 슬롯 채우기 패턴
    // 2. 점심
    if (!remainingRestaurants.isEmpty()) {
      Document next = findNextSmartSpot(currentLocation, remainingRestaurants);
      route.add(next);
      remainingRestaurants.remove(next);
      currentLocation = next;
    }

    // 3. 오후 관광
    int afternoonAdded = 0;
    while (afternoonAdded < AFTERNOON_PLACE_COUNT && !remainingPlaces.isEmpty()) {
      Document next = findNextSmartSpot(currentLocation, remainingPlaces);
      route.add(next);
      remainingPlaces.remove(next);
      currentLocation = next;
      afternoonAdded++;
    }

    // 4. 저녁
    if (!remainingRestaurants.isEmpty()) {
      Document next = findNextSmartSpot(currentLocation, remainingRestaurants);
      route.add(next);
      remainingRestaurants.remove(next);
      currentLocation = next;
    }

    // 5. 야경
    int eveningAdded = 0;
    while (eveningAdded < EVENING_PLACE_COUNT && !remainingPlaces.isEmpty()) {
      Document next = findNextSmartSpot(currentLocation, remainingPlaces);
      route.add(next);
      remainingPlaces.remove(next);
      currentLocation = next;
      eveningAdded++;
    }

    return route;
  }

  private Document findNextSmartSpot(Document current, List<Document> candidates) {
    if (current == null) {
      return candidates.stream().max(Comparator.comparingDouble(Document::getScore)).orElseThrow();
    }

    // 1순위 후보군: 적정 거리(3km ~ 15km) 내에 있는 장소들
    List<Document> optimalCandidates =
        candidates.stream()
            .filter(
                doc -> {
                  double dist = calculateDistance(current, doc);
                  return dist >= MIN_DISTANCE_KM && dist <= MAX_RADIUS_KM;
                })
            .toList();

    if (!optimalCandidates.isEmpty()) {
      // 적정 거리 후보 중에서는 이동 시간을 줄이기 위해 '가장 가까운' 곳 선택
      return optimalCandidates.stream()
          .min(Comparator.comparingDouble(doc -> calculateDistance(current, doc)))
          .orElseThrow();
    }

    // 2순위: 적정 거리 후보가 없으면 (다 너무 가깝거나, 다 너무 멀거나)
    // -> 무조건 '가장 가까운' 곳을 선택하여 대형 점프(23km)를 방지함
    return candidates.stream()
        .min(Comparator.comparingDouble(doc -> calculateDistance(current, doc)))
        .orElseThrow();
  }

  private List<Document> optimizeWithSwap(List<Document> route) {
    if (route.size() < 5) return route;
    List<Document> optimized = new ArrayList<>(route);
    double currentDistance = calculateTotalDistance(optimized);

    // 식당 Swap (1 <-> 3)
    if (route.size() > 3 && isRestaurant(route.get(1)) && isRestaurant(route.get(3))) {
      List<Document> swapped = new ArrayList<>(optimized);
      swapped.set(1, optimized.get(3));
      swapped.set(3, optimized.get(1));
      if (calculateTotalDistance(swapped) < currentDistance) {
        optimized = swapped;
        currentDistance = calculateTotalDistance(swapped);
      }
    }

    // 관광지 Swap (2 <-> 4)
    if (route.size() > 4 && !isRestaurant(route.get(2)) && !isRestaurant(route.get(4))) {
      List<Document> swapped = new ArrayList<>(optimized);
      swapped.set(2, optimized.get(4));
      swapped.set(4, optimized.get(2));
      if (calculateTotalDistance(swapped) < currentDistance) {
        optimized = swapped;
      }
    }
    return optimized;
  }

  private double calculateDistance(Document from, Document to) {
    Double lat1 = (Double) from.getMetadata().get("latitude");
    Double lon1 = (Double) from.getMetadata().get("longitude");
    Double lat2 = (Double) to.getMetadata().get("latitude");
    Double lon2 = (Double) to.getMetadata().get("longitude");
    return distanceCalculationUseCase
        .calculateDistance(lat1, lon1, lat2, lon2)
        .straightlineDistanceKm();
  }

  private double calculateTotalDistance(List<Document> route) {
    double total = 0.0;
    for (int i = 0; i < route.size() - 1; i++) {
      total += calculateDistance(route.get(i), route.get(i + 1));
    }
    return total;
  }

  private boolean isRestaurant(Document doc) {
    @SuppressWarnings("unchecked")
    List<String> themes = (List<String>) doc.getMetadata().get("themes");
    return themes != null && themes.contains("RESTAURANT");
  }
}
