package com.atablood.iWindoor_api.security;

import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.Role;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.exception.ForbiddenException;
import org.springframework.stereotype.Component;

/** Şirket bazlı erişim kuralları tek yerde. */
@Component
public class AccessGuard {

    public Long companyId(User user) {
        if (user == null || user.getCompany() == null) {
            throw new ForbiddenException("Bir şirkete bağlı değilsiniz.");
        }
        return user.getCompany().getId();
    }

    public boolean isSystemAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    public void checkProject(Project project, User user) {
        if (isSystemAdmin(user)) return;
        Long mine = companyId(user);
        User owner = project.getCreatedBy();
        if (owner == null || owner.getCompany() == null || !mine.equals(owner.getCompany().getId())) {
            throw new ForbiddenException("Bu projeye erişim yetkiniz yok.");
        }
    }

    /** Katalog kaydı: şirketin kendi kaydıysa serbest; şablon (companyId null) sadece sistem yöneticisine açık. */
    public void checkCatalogWrite(Long itemCompanyId, User user) {
        if (isSystemAdmin(user)) return;
        if (itemCompanyId == null || !itemCompanyId.equals(companyId(user))) {
            throw new ForbiddenException("Bu katalog kaydını değiştirme yetkiniz yok.");
        }
    }

    public void checkCatalogRead(Long itemCompanyId, User user) {
        if (itemCompanyId == null || isSystemAdmin(user)) return;
        if (!itemCompanyId.equals(companyId(user))) {
            throw new ForbiddenException("Bu katalog kaydına erişim yetkiniz yok.");
        }
    }
}
