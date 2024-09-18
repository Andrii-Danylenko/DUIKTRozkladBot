package org.rozkladbot.entities;

import jakarta.persistence.*;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "roles")
public class UserRole {
    @Override
    public String toString() {
        return "UserRole{" +
               "id=" + id +
               ", role=" + role +
               '}';
    }

    public long getId() {
        return id;
    }

    public enum Roles {
        USER,
        ADMIN;
        private static Roles getUserRoleFromString(String role) {
            try {
                return Roles.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                return USER;
            }
        }
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "roleName", unique = true, nullable = false)
    private Roles role;
    @OneToMany(mappedBy = "role", cascade = CascadeType.REMOVE)
    private List<User> users;
    public UserRole() {

    }
    public UserRole(String role) {
        this.role = Roles.valueOf(role);
    }
    public Roles getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = Roles.valueOf(role);
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
