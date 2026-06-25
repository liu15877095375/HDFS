package com.cgrs.driver.dao;

import com.cgrs.driver.model.Share;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ShareRepository extends JpaRepository<Share, Long> {

    // 通过分享链接查询
    Optional<Share> findByShareLink(String shareLink);

    // 查询用户的所有分享（按创建时间倒序）
    List<Share> findBySharerIdOrderByCreateTimeDesc(Long sharerId);

    // 删除用户的某个分享（验证所有权）
    @Transactional
    @Modifying
    @Query("DELETE FROM Share s WHERE s.shareLink = :shareLink AND s.sharerId = :sharerId")
    int deleteByShareLinkAndSharerId(String shareLink, Long sharerId);
}