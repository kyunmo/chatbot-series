package io.moyam.chatbot.domain.user.repository;

import io.moyam.chatbot.domain.user.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    void insert(User user);
    Optional<User> findById(@Param("id") Long id);
    Optional<User> findByEmail(@Param("email") String email);
    List<User> findAll();
    void update(User user);
    void deleteById(@Param("id") Long id);
}
