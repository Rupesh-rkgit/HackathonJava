package com.atci.quizhub.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TechStackRepository extends JpaRepository<TechStack, Long> {
    Optional<TechStack> findByName(String name);
}
