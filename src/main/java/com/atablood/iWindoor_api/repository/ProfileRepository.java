package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Profile;
import com.atablood.iWindoor_api.entity.ProfileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    // Stok koduna göre profil bulmak için
    Profile findByCode(String code);

    // Belirli bir seriye ait profilleri listelemek için (Örn: Eko 60 serisinin tüm profilleri)
    List<Profile> findBySeriesId(Long seriesId);

    List<Profile> findByType(ProfileType type); // Enum tipine göre ara
}
