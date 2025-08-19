package io.moyam.chatbot.domain.file.repository;

import io.moyam.chatbot.domain.file.model.FileLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface FileLinkMapper {
    void insert(FileLink fileLink);
    Optional<FileLink> findById(@Param("id") Long id);
    Optional<FileLink> findByLinkToken(@Param("linkToken") String linkToken);
    List<FileLink> findByFileId(@Param("fileId") Long fileId);
    void update(FileLink fileLink);
    void deleteById(@Param("id") Long id);
}
