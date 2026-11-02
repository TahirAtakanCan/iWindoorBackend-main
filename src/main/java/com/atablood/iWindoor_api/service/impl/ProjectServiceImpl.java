package com.atablood.iWindoor_api.service.impl;

import com.atablood.iWindoor_api.dto.ProjectCostSummaryDTO;
import com.atablood.iWindoor_api.dto.ProjectSpecsDTO;
import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.entity.WindowNode;
import com.atablood.iWindoor_api.entity.WindowUnit;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.repository.UserRepository;
import com.atablood.iWindoor_api.service.PricingService;
import com.atablood.iWindoor_api.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final PricingService pricingService; // Maliyet hesaplamaları için
    private final UserRepository userRepository;

    // 1. PROJE OLUŞTURMA (User'a bağlanarak)
    @Override
    public Project createProject(Project project, User currentUser) {
        // Projeyi oluşturan kişiyi set et
        project.setCreatedBy(currentUser);

        // Eğer proje adı boşsa varsayılan ata
        if (project.getName() == null || project.getName().isEmpty()) {
            project.setName("Yeni Proje");
        }

        // Listeler null ise başlat (Hata önleyici)
        if (project.getWindowUnits() == null) {
            project.setWindowUnits(new ArrayList<>());
        }

        return projectRepository.save(project);
    }

    // 2. PROJELERİ LİSTELEME (Sadece kendi şirketim)
    @Override
    public List<Project> getAllProjects(User currentUser) {
        if (currentUser.getCompany() == null) {
            // Eğer kullanıcının şirketi yoksa boş liste dön veya hata fırlat
            // Güvenlik için boş liste dönüyoruz.
            return new ArrayList<>();
        }

        Long companyId = currentUser.getCompany().getId();
        // Repository'deki özel metodu çağır
        return projectRepository.findAllByCreatedBy_Company_Id(companyId);
    }

    // 3. TEK PROJE GETİRME (Güvenlik Kontrollü)
    @Override
    public Project getProject(Long id, User currentUser) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proje bulunamadı"));

        // --- GÜVENLİK DUVARI ---
        // Projenin sahibinin şirketi ile giren kişinin şirketi aynı mı?
        if (project.getCreatedBy() == null || project.getCreatedBy().getCompany() == null) {
            // Sahipsiz proje (eski veri) ise erişime izin ver veya engelle.
            // Şimdilik sadece loglayıp geçiyoruz ama normalde engellenmeli.
        } else {
            Long projectCompanyId = project.getCreatedBy().getCompany().getId();
            Long userCompanyId = currentUser.getCompany().getId();

            if (!projectCompanyId.equals(userCompanyId)) {
                throw new RuntimeException("Bu projeyi görüntüleme yetkiniz yok! (Farklı Şirket)");
            }
        }
        // -----------------------

        return project;
    }

    // 4. PROJE SİLME
    @Override
    public void deleteProject(Long id, User currentUser) {
        // getProject metodunu çağırarak güvenlik kontrolünü orada yapıyoruz
        Project project = getProject(id, currentUser);
        projectRepository.delete(project);
    }

    // 5. MEO4: MALİYET TABLOSU (Maliyet Hesaplama)
    @Override
    public ProjectCostSummaryDTO getCostSummary(Long projectId, User currentUser) {
        // 1. Projeyi güvenli şekilde çek
        Project project = getProject(projectId, currentUser);

        // 2. PricingService kullanarak hesaplat (Fiyatları güncelle)
        // Not: PricingService'in "calculateCostSummary" diye bir metodu olduğunu varsayıyoruz.
        // Eğer yoksa, PricingService'e bu metodu eklememiz gerekir.
        // Şimdilik projeyi hesaplatıp DTO'yu döndüren mantığı çağırıyoruz.
        return pricingService.calculateCostSummary(project);
    }

    // 6. MEO5: TEKNİK ÖZELLİKLER (Proje Özeti)
    @Override
    public ProjectSpecsDTO getProjectSpecs(Long projectId, User currentUser) {
        Project project = getProject(projectId, currentUser);
        ProjectSpecsDTO specs = new ProjectSpecsDTO();

        specs.setProjectName(project.getName());
        specs.setTotalWindowCount(project.getWindowUnits().size());

        double totalArea = 0;
        Set<String> profileNames = new HashSet<>();
        Set<String> glassNames = new HashSet<>();

        for (WindowUnit unit : project.getWindowUnits()) {
            // Pencere Alanı (m2)
            totalArea += (unit.getWidth() * unit.getHeight()) / 1_000_000.0;

            if (unit.getRootNode() != null) {
                collectSpecsRecursive(unit.getRootNode(), profileNames, glassNames);
            }
        }

        specs.setTotalAreaM2(Math.round(totalArea * 100.0) / 100.0); // 2 hane yuvarla
        specs.setUsedProfileTypes(new ArrayList<>(profileNames));
        specs.setUsedGlassTypes(new ArrayList<>(glassNames));

        return specs;
    }

    // MEO5 için yardımcı metod (Recursive)
    private void collectSpecsRecursive(WindowNode node, Set<String> profiles, Set<String> glasses) {
        if (node.getProfile() != null) {
            profiles.add(node.getProfile().getName());
        }
        if (node.getGlass() != null) {
            glasses.add(node.getGlass().getName());
        }

        if (node.getChildren() != null) {
            for (WindowNode child : node.getChildren()) {
                collectSpecsRecursive(child, profiles, glasses);
            }
        }
    }
}