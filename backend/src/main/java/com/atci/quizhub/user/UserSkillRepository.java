package com.atci.quizhub.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    @Query("select us.user from UserSkill us where us.stack.id = :stackId")
    List<User> findUsersByStackId(Long stackId);
}
