package com.financemanager.repository;

import com.financemanager.model.AgentAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentAlertRepository extends JpaRepository<AgentAlert, Long> {

    @Query("SELECT a FROM AgentAlert a WHERE a.user.id = :userId AND a.isDismissed = false ORDER BY a.createdAt DESC")
    List<AgentAlert> findActiveAlertsByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM AgentAlert a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    List<AgentAlert> findAllByUserId(@Param("userId") Long userId);
}
