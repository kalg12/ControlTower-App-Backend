package com.controltower.app.kanban.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {

    List<BoardColumn> findByBoardId(UUID boardId);
}
