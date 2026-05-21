package com.example.courseregistration.repository;

import com.example.courseregistration.entity.User;
import com.example.courseregistration.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndPassword(String username, String password);

    List<User> findByRole(UserRole role);

    boolean existsByUsername(String username);
}
