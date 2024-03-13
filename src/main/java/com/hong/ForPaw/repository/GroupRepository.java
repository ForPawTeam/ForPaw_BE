package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Group.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    Page<Group> findByRegion(String region, Pageable pageable);

    boolean existsByName(String name);

    boolean existsById(Long id);

    @Modifying
    @Query("UPDATE Group g SET g.likeNum = g.likeNum + 1 WHERE g.id = :groupId")
    void incrementLikeNumById(@Param("groupId") Long groupId);

    @Modifying
    @Query("UPDATE Group g SET g.likeNum = g.likeNum - 1 WHERE g.id = :groupId AND g.likeNum > 0")
    void decrementLikeNumById(@Param("groupId") Long groupId);

    Page<Group> findByNameContaining(@Param("name") String name, Pageable pageable);
}
