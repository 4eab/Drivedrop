package com.bae4.drivedrop.repository;

import com.bae4.drivedrop.entity.AccessCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccessCodeRepository extends JpaRepository<AccessCode, String> {
    boolean existsByCode(String codeStr);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AccessCode c SET c.assigned = true, c.assignedAt = CURRENT_TIMESTAMP " +
            "WHERE c.code = :key " +
            "AND c.assigned = false " +
            "AND c.task.id = :taskId " +
            "AND c.task.shareMode = 'DISTRIBUTED'")
    int markAsAssigned(@Param("taskId") String taskId, @Param("key") String key);

    @Query("SELECT c FROM AccessCode c JOIN FETCH c.task WHERE c.code = :key AND c.task.id = :taskId")
    Optional<AccessCode> findByCodeAndTaskId(@Param("key") String key, @Param("taskId") String taskId);

    boolean existsByTaskIdAndUsedFalse(String id);
}
