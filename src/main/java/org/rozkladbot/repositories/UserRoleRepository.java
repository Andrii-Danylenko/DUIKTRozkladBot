package org.rozkladbot.repositories;

import org.rozkladbot.entities.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    @Query(value = "select count(role_name) > 0 from roles where role_name = ?1", nativeQuery = true)
    boolean existsByRoleName(String roleName);

    @Query(value = "select * from roles role where role_name = ?1", nativeQuery = true)
    UserRole getUserRoleByRoleName(String roleName);
}
