package com.atci.quizhub.mcq;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface McqRepository extends JpaRepository<Mcq, Long> {
    Page<Mcq> findByCreatorId(Long creatorId, Pageable pageable);
}
