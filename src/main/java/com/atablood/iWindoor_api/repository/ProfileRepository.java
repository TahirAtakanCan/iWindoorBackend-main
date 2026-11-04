package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Profile;
import com.atablood.iWindoor_api.entity.ProfileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    List<Profile> findBySeries_Id(Long seriesId);

    List<Profile> findBySeries_IdOrderByIdAsc(Long seriesId);

    List<Profile> findBySeries_IdAndTypeOrderByIdAsc(Long seriesId, ProfileType type);

    List<Profile> findByType(ProfileType type);

    List<Profile> findByCompanyIdOrderByIdAsc(Long companyId);

    List<Profile> findByCompanyIdAndTypeOrderByIdAsc(Long companyId, ProfileType type);

    List<Profile> findByCompanyIdIsNullOrderByIdAsc();

    boolean existsByCompanyIdAndCode(Long companyId, String code);
}
