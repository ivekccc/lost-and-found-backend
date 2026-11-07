package com.example.demo.audit.repository;

import com.example.demo.audit.model.AdminAuditEntry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminAuditEntryRepository extends JpaRepository<AdminAuditEntry, Long>, JpaSpecificationExecutor<AdminAuditEntry> {
}
