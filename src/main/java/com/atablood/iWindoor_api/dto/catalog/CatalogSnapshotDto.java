package com.atablood.iWindoor_api.dto.catalog;

import java.time.Instant;
import java.util.List;

/** İstemcinin çevrimdışı çizim için yerelde tuttuğu katalog paketi. */
public record CatalogSnapshotDto(Instant generatedAt, List<SeriesDto> series, List<ProfileDto> profiles,
                                 List<GlassDto> glasses, List<AccessoryDto> accessories) {
}
