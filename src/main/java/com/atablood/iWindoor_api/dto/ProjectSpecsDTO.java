package com.atablood.iWindoor_api.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectSpecsDTO {
    private String projectName;
    private int totalWindowCount;
    private double totalAreaM2; // Toplam Pencere Alanı

    // Kullanılan Malzemeler
    private List<String> usedProfileTypes = new ArrayList<>(); // Örn: 60'lık Seri, 70'lik Seri
    private List<String> usedGlassTypes = new ArrayList<>();   // Örn: Isıcam S, Konfor Cam

    private double totalProfileLengthM; // Toplam Profil Metrajı (Yaklaşık)
}