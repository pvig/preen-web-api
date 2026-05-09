package com.pvig.preenWebApi.patch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface PatchRepository extends JpaRepository<PatchEntity, UUID>, JpaSpecificationExecutor<PatchEntity> {
}
