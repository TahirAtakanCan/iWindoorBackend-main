package com.atablood.iWindoor_api.repository;
import com.atablood.iWindoor_api.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}