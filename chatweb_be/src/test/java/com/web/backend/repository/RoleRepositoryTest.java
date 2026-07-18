package com.web.backend.repository;

import com.web.backend.model.RoleEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class RoleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void testFindByName_Success() {
        RoleEntity role = new RoleEntity();
        role.setName("USER");
        entityManager.persistAndFlush(role);

        Optional<RoleEntity> found = roleRepository.findByName("USER");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("USER");
    }

    @Test
    void testFindByName_NotFound() {
        Optional<RoleEntity> found = roleRepository.findByName("UNKNOWN");
        assertThat(found).isNotPresent();
    }
}
