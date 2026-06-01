package com.atci.quizhub.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findByStackId(Long stackId);
}
