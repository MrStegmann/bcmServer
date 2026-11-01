package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import com.basketapp.bcmserver.domain.model.User;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock
    private SpringDataJpaUserRepository jpaRepository;

    private UserPersistenceMapper mapper;
    private UserRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new UserPersistenceMapperImpl();
        adapter = new UserRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    @DisplayName("Should map and persist domain User via JPA repository")
    void shouldSaveUser() {
        User user = User.create("adapter_user", "$2a$12$hashed");
        UserJpaEntity savedEntity = new UserJpaEntity(1L, "adapter_user", "$2a$12$hashed", "ACTIVE", Instant.now());

        when(jpaRepository.save(any(UserJpaEntity.class))).thenReturn(savedEntity);

        User result = adapter.save(user);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("adapter_user", result.getUsername());
        assertEquals(com.basketapp.bcmserver.domain.model.UserStatus.ACTIVE, result.getStatus());
        verify(jpaRepository).save(any(UserJpaEntity.class));
    }

    @Test
    @DisplayName("Should find user by ID and map to domain model")
    void shouldFindById() {
        UserJpaEntity entity = new UserJpaEntity(1L, "adapter_user", "$2a$12$hashed", "ACTIVE", Instant.now());
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));

        Optional<User> result = adapter.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("adapter_user", result.get().getUsername());
        assertEquals(com.basketapp.bcmserver.domain.model.UserStatus.ACTIVE, result.get().getStatus());
    }

    @Test
    @DisplayName("Should find user by username and map to domain model")
    void shouldFindByUsername() {
        UserJpaEntity entity = new UserJpaEntity(1L, "adapter_user", "$2a$12$hashed", Instant.now());
        when(jpaRepository.findByUsername("adapter_user")).thenReturn(Optional.of(entity));

        Optional<User> result = adapter.findByUsername("adapter_user");

        assertTrue(result.isPresent());
        assertEquals("adapter_user", result.get().getUsername());
    }

    @Test
    @DisplayName("Should delegate existsByUsername check")
    void shouldCheckExistsByUsername() {
        when(jpaRepository.existsByUsername("adapter_user")).thenReturn(true);

        assertTrue(adapter.existsByUsername("adapter_user"));
        verify(jpaRepository).existsByUsername("adapter_user");
    }

    @Test
    @DisplayName("Should delegate deleteById")
    void shouldDeleteById() {
        adapter.deleteById(1L);

        verify(jpaRepository).deleteById(1L);
    }
}

