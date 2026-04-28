package com.bae4.drivedrop.repository;

import com.bae4.drivedrop.entity.ShareTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShareTaskRepository extends JpaRepository<ShareTask, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM ShareTask t WHERE t.id = :id")
    Optional<ShareTask> findWithLockById(String id);
}
