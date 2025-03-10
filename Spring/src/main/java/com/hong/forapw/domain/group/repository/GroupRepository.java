package com.hong.forapw.domain.group.repository;

import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.group.model.query.GroupIdAndLikeCount;
import com.hong.forapw.domain.post.model.query.CommentIdAndLikeCount;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.region.constant.Province;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g WHERE g.province = :province AND g.district = :district " +
            "AND (:userId IS NULL OR g.id NOT IN " +
            "(SELECT g.id FROM GroupUser gu JOIN gu.group g JOIN gu.user u WHERE u.id = :userId AND gu.groupRole != :role))")
    Page<Group> findByProvinceAndDistrictWithoutMyGroup(@Param("province") Province province,
                                                        @Param("district") District district,
                                                        @Param("userId") Long userId,
                                                        @Param("role") GroupRole role,
                                                        Pageable pageable);

    @Query("SELECT g FROM Group g WHERE g.province = :province " +
            "AND (:userId IS NULL OR g.id NOT IN " +
            "(SELECT g.id FROM GroupUser gu JOIN gu.group g JOIN gu.user u " +
            "WHERE u.id = :userId AND gu.groupRole != :role))")
    Page<Group> findByProvinceWithoutMyGroup(@Param("province") Province province,
                                             @Param("userId") Long userId,
                                             @Param("role") GroupRole role,
                                             Pageable pageable);

    @Query("SELECT g FROM Group g WHERE (:userId IS NULL OR g.id NOT IN " +
            "(SELECT g.id FROM GroupUser gu JOIN gu.group g JOIN gu.user u WHERE u.id = :userId))")
    Page<Group> findAllWithoutMyGroup(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT * FROM groups_tb WHERE MATCH(name) AGAINST(:name IN BOOLEAN MODE)",
            countQuery = "SELECT COUNT(*) FROM groups_tb WHERE MATCH(name) AGAINST(:name IN BOOLEAN MODE)",
            nativeQuery = true)
    Page<Group> findByNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT new com.hong.forapw.domain.group.model.query.GroupIdAndLikeCount(g.id, COUNT(fg.id)) " +
            "FROM Group g " +
            "LEFT JOIN FavoriteGroup fg on g.id = fg.group.id " +
            "WHERE g.id IN (:missingIds) " +
            "GROUP BY g.id ")
    List<GroupIdAndLikeCount> findLikeCountsByIds(List<Long> missingIds);

    @Query("SELECT COUNT(fg) FROM FavoriteGroup fg WHERE fg.group.id = :groupId")
    Long countLikesByGroupId(@Param("groupId") Long groupId);

    boolean existsByName(String name);

    @Query("SELECT COUNT(g) > 0 FROM Group g WHERE g.id != :id AND g.name = :name")
    boolean existsByNameExcludingId(@Param("name") String name, @Param("id") Long id);

    @Modifying
    @Query("UPDATE Group g SET g.participantNum = g.participantNum - 1 WHERE g.id = :id AND g.participantNum > 0")
    void decrementParticipantNum(@Param("id") Long id);
}
