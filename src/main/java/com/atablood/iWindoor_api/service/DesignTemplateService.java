package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.design.DesignDocumentValidator;
import com.atablood.iWindoor_api.dto.template.DesignTemplateCreateRequest;
import com.atablood.iWindoor_api.dto.template.DesignTemplateDto;
import com.atablood.iWindoor_api.dto.template.DesignTemplateUpdateRequest;
import com.atablood.iWindoor_api.entity.DesignTemplate;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.exception.ConflictException;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.repository.DesignTemplateRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DesignTemplateService {

    public static final int MAX_TEMPLATES_PER_COMPANY = 200;
    private static final Set<String> CATEGORIES = Set.of("WINDOW", "DOOR", "SLIDING", "OTHER");

    private final DesignTemplateRepository repository;
    private final DesignDocumentValidator validator;
    private final AccessGuard guard;

    @Transactional(readOnly = true)
    public List<DesignTemplateDto> list(String category, User user) {
        Long companyId = guard.companyId(user);
        List<DesignTemplate> list = (category == null || category.isBlank())
                ? repository.findByCompanyIdOrderBySortOrderAscNameAsc(companyId)
                : repository.findByCompanyIdAndCategoryOrderBySortOrderAscNameAsc(companyId, normalizeCategory(category));
        return list.stream().map(this::toDto).toList();
    }

    @Transactional
    public DesignTemplateDto create(DesignTemplateCreateRequest req, User user) {
        Long companyId = guard.companyId(user);
        guard.checkCatalogWrite(companyId, user);
        if (repository.countByCompanyId(companyId) >= MAX_TEMPLATES_PER_COMPANY) {
            throw new BadRequestException("TEMPLATE_LIMIT",
                    "Firma başına en fazla " + MAX_TEMPLATES_PER_COMPANY + " şablon kaydedilebilir.");
        }
        String name = req.name().trim();
        String category = normalizeCategory(req.category());
        ensureNameFree(companyId, name, null);
        validator.validateOrThrow(req.design());

        DesignTemplate t = new DesignTemplate();
        t.setCompanyId(companyId);
        t.setName(name);
        t.setCategory(category);
        t.setDesign(req.design());
        t.setSortOrder(0);
        t.setCreatedBy(user.getId());
        return toDto(repository.save(t));
    }

    @Transactional
    public DesignTemplateDto update(Long id, DesignTemplateUpdateRequest req, User user) {
        DesignTemplate t = requireOwned(id, user);
        guard.checkCatalogWrite(t.getCompanyId(), user);

        if (req.name() != null) {
            String name = req.name().trim();
            if (name.isEmpty()) {
                throw new BadRequestException("VALIDATION_FAILED", "name boş olamaz.");
            }
            ensureNameFree(t.getCompanyId(), name, t.getId());
            t.setName(name);
        }
        if (req.category() != null) {
            t.setCategory(normalizeCategory(req.category()));
        }
        if (req.sortOrder() != null) {
            t.setSortOrder(req.sortOrder());
        }
        if (req.design() != null) {
            validator.validateOrThrow(req.design());
            t.setDesign(req.design());
        }
        return toDto(repository.save(t));
    }

    @Transactional
    public void delete(Long id, User user) {
        DesignTemplate t = requireOwned(id, user);
        guard.checkCatalogWrite(t.getCompanyId(), user);
        repository.delete(t);
    }

    private DesignTemplate requireOwned(Long id, User user) {
        DesignTemplate t = repository.findById(id).orElseThrow(() -> new NotFoundException("Şablon", id));
        if (!guard.isSystemAdmin(user) && !t.getCompanyId().equals(guard.companyId(user))) {
            throw new NotFoundException("Şablon", id);
        }
        return t;
    }

    private void ensureNameFree(Long companyId, String name, Long excludeId) {
        boolean taken = excludeId == null
                ? repository.existsByCompanyIdAndNameIgnoreCase(companyId, name)
                : repository.existsByCompanyIdAndNameIgnoreCaseAndIdNot(companyId, name, excludeId);
        if (taken) {
            throw new ConflictException("TEMPLATE_NAME_TAKEN", "Bu isimde bir şablon zaten var: " + name);
        }
    }

    private static String normalizeCategory(String category) {
        String c = category.trim().toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(c)) {
            throw new BadRequestException("VALIDATION_FAILED",
                    "category WINDOW, DOOR, SLIDING veya OTHER olmalı.");
        }
        return c;
    }

    private DesignTemplateDto toDto(DesignTemplate t) {
        return new DesignTemplateDto(t.getId(), t.getName(), t.getCategory(), t.getSortOrder(),
                t.getDesign(), t.getUpdatedAt());
    }
}
