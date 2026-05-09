package com.pvig.preenWebApi.comment;

import com.pvig.preenWebApi.patch.PatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {
    List<CommentEntity> findByPatch(PatchEntity patch);
    long countByPatchId(UUID patchId);
}
