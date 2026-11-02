package com.atablood.iWindoor_api.config;

import com.atablood.iWindoor_api.entity.Glass;
import com.atablood.iWindoor_api.entity.Profile;
import com.atablood.iWindoor_api.entity.ProfileType; // Enum importu önemli
import com.atablood.iWindoor_api.repository.GlassRepository;
import com.atablood.iWindoor_api.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProfileRepository profileRepository;
    private final GlassRepository glassRepository;

    @Override
    public void run(String... args) throws Exception {
        // Profil tablosu boşsa doldur
        if (profileRepository.count() == 0) {
            System.out.println("--- Veritabanı Başlatılıyor: Varsayılan Profiller Ekleniyor ---");

            // Kasalar
            profileRepository.save(createProfile("60'lık Geniş Kasa", ProfileType.FRAME, 120.0));
            profileRepository.save(createProfile("70'lik Süper Kasa", ProfileType.FRAME, 150.0));

            // Kanatlar
            profileRepository.save(createProfile("60'lık Düz Kanat", ProfileType.SASH, 130.0));
            profileRepository.save(createProfile("70'lik Elit Kanat", ProfileType.SASH, 160.0));

            // *** KAYITLAR (MULLION) - Bunlar eksikti ***
            profileRepository.save(createProfile("60'lık Orta Kayıt", ProfileType.MULLION, 125.0));
            profileRepository.save(createProfile("70'lik Orta Kayıt", ProfileType.MULLION, 155.0));
        }

        // Cam tablosu boşsa doldur
        if (glassRepository.count() == 0) {
            System.out.println("--- Veritabanı Başlatılıyor: Varsayılan Camlar Ekleniyor ---");

            glassRepository.save(createGlass("4mm Tek Cam", 250.0));
            glassRepository.save(createGlass("4+12+4 Isıcam S", 450.0));
            glassRepository.save(createGlass("4+16+4 Konfor Cam", 600.0));
        }
    }

    private Profile createProfile(String name, ProfileType type, double price) {
        Profile p = new Profile();
        p.setName(name);
        p.setType(type);
        p.setPricePerMeter(BigDecimal.valueOf(price));
        p.setCurrency("TL");
        return p;
    }

    private Glass createGlass(String name, double price) {
        Glass g = new Glass();
        g.setName(name);
        g.setPricePerSquareMeter(BigDecimal.valueOf(price));
        g.setCurrency("TL");
        return g;
    }
}