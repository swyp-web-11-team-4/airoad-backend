package com.swygbro.airoad.backend.content.domain.converter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PlaceDocumentConverter {

  public Map<String, Object> buildMetadataFromEvent(
      Long placeId, String name, String address, List<String> themes) {
    Map<String, Object> metadata = new HashMap<>();

    metadata.put("placeId", placeId);
    metadata.put("name", name);
    metadata.put("address", address);
    metadata.put("themes", themes);
    metadata.put("embeddedAt", LocalDateTime.now(ZoneOffset.UTC).toString());

    String[] addressParts = address.split(" ");
    if (addressParts.length >= 1) {
      metadata.put("province", addressParts[0]);
    }
    if (addressParts.length >= 2) {
      metadata.put("district", addressParts[1]);
    }

    return metadata;
  }
}
