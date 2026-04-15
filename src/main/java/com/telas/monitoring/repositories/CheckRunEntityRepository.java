package com.telas.monitoring.repositories;

import com.telas.monitoring.entities.CheckRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CheckRunEntityRepository extends JpaRepository<CheckRunEntity, UUID> {}
