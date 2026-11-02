package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.WindowNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WindowNodeRepository extends JpaRepository<WindowNode, Long> {
    // Gerekirse özel sorgular buraya eklenecek
}
