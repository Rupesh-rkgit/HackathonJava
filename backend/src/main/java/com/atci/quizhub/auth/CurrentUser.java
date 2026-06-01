package com.atci.quizhub.auth;

import com.atci.quizhub.common.NotFoundException;
import com.atci.quizhub.user.User;
import com.atci.quizhub.user.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final UserRepository userRepository;
    public CurrentUser(UserRepository userRepository) { this.userRepository = userRepository; }

    public User get() {
        String enterpriseId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEnterpriseId(enterpriseId)
                .orElseThrow(() -> new NotFoundException("User not found: " + enterpriseId));
    }
}
