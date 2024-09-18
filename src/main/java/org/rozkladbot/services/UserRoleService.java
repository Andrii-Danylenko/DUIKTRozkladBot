package org.rozkladbot.services;

import org.rozkladbot.entities.UserRole;
import org.rozkladbot.repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRoleService {
    private final UserRoleRepository userRoleRepository;
    @Autowired
    public UserRoleService(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }


    public boolean create(UserRole userRole) {
        System.out.println(userRoleRepository.existsByRoleName(userRole.getRole().name()));
        if (!userRoleRepository.existsByRoleName(userRole.getRole().name())) {
            userRoleRepository.save(userRole);
            return true;
        }
        return false;
    }

    public UserRole findByRoleName(String roleName) {
        return userRoleRepository.getUserRoleByRoleName(roleName);
    }
}
