package com.auditvault.auditvault.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DomainEntityTest {

    @Test
    void role_builderCreatesValidObject() {
        Role role = Role.builder()
            .id(1L)
            .name("ADMIN")
            .build();

        assertEquals(1L, role.getId());
        assertEquals("ADMIN", role.getName());
    }

    @Test
    void role_settersAndGetters() {
        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        assertEquals(1L, role.getId());
        assertEquals("USER", role.getName());
    }

    @Test
    void role_allArgsConstructor() {
        Role role = new Role(1L, "ADMIN");

        assertEquals(1L, role.getId());
        assertEquals("ADMIN", role.getName());
    }

    @Test
    void role_noArgsConstructor() {
        Role role = new Role();

        assertNull(role.getId());
        assertNull(role.getName());
    }

    @Test
    void userEntity_builderCreatesValidObject() {
        Role role = Role.builder().id(1L).name("USER").build();
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        UserEntity user = UserEntity.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("encoded-password")
            .enabled(true)
            .roles(roles)
            .build();

        assertEquals(1L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("encoded-password", user.getPassword());
        assertTrue(user.isEnabled());
        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().contains(role));
    }

    @Test
    void userEntity_settersAndGetters() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encoded-password");
        user.setEnabled(true);

        Role role = Role.builder().id(1L).name("USER").build();
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        assertEquals(1L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("encoded-password", user.getPassword());
        assertTrue(user.isEnabled());
        assertEquals(1, user.getRoles().size());
    }

    @Test
    void userEntity_allArgsConstructor() {
        Role role = Role.builder().id(1L).name("USER").build();
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        UserEntity user = new UserEntity(1L, "testuser", "test@example.com", "encoded-password", true, roles);

        assertEquals(1L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("encoded-password", user.getPassword());
        assertTrue(user.isEnabled());
        assertEquals(1, user.getRoles().size());
    }

    @Test
    void userEntity_noArgsConstructor() {
        UserEntity user = new UserEntity();

        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getEmail());
        assertNull(user.getPassword());
        assertTrue(user.isEnabled()); // default is true
        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().isEmpty());
    }

    @Test
    void userEntity_defaultEnabledIsTrue() {
        UserEntity user = UserEntity.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password")
            .build();

        assertTrue(user.isEnabled());
    }

    @Test
    void userEntity_defaultRolesIsEmptySet() {
        UserEntity user = UserEntity.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password")
            .build();

        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().isEmpty());
    }

    @Test
    void userEntity_withMultipleRoles() {
        Role userRole = Role.builder().id(1L).name("USER").build();
        Role adminRole = Role.builder().id(2L).name("ADMIN").build();
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(adminRole);

        UserEntity user = UserEntity.builder()
            .username("adminuser")
            .email("admin@example.com")
            .password("password")
            .roles(roles)
            .build();

        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().contains(userRole));
        assertTrue(user.getRoles().contains(adminRole));
    }

    @Test
    void userEntity_canSetEnabledToFalse() {
        UserEntity user = UserEntity.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password")
            .enabled(false)
            .build();

        assertFalse(user.isEnabled());
    }
}
