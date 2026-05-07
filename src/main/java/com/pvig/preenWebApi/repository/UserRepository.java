package com.pvig.preenWebApi.repository;

import com.pvig.preenWebApi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // JWT subject is the user's email; this method provides a semantic alias used by patch/comment services
    @Query("SELECT u FROM User u WHERE u.email = :sub")
    Optional<User> findBySub(@Param("sub") String sub);
}
