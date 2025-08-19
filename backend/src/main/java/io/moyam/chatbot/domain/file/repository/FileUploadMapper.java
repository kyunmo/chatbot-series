package io.moyam.chatbot.domain.file.repository;

import io.moyam.chatbot.domain.file.model.FileUpload;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface FileUploadMapper {
    void insert(FileUpload fileUpload);
    Optional<FileUpload> findById(@Param("id") Long id);
    List<FileUpload> findByUserId(@Param("userId") Long userId);
    void update(FileUpload fileUpload);
    void deleteById(@Param("id") Long id);
}
