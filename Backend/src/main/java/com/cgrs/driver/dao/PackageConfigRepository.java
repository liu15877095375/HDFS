package com.cgrs.driver.dao;

import com.cgrs.driver.model.PackageConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PackageConfigRepository extends JpaRepository<PackageConfig, Long> {
    Optional<PackageConfig> findByRole(Integer role);
    boolean existsByRole(Integer role);
}