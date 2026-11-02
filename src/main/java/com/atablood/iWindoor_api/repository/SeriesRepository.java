package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeriesRepository extends JpaRepository<Series, Long> {
    // Spring bizim yerimize şu metodun SQL'ini hazırlar:
    // SELECT * FROM series WHERE name = ?
    boolean existsByName(String name);
}
