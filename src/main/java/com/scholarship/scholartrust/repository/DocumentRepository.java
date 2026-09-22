package com.scholarship.scholartrust.repository;

import com.scholarship.scholartrust.entity.Application;
import com.scholarship.scholartrust.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByApplication(Application application);
}
