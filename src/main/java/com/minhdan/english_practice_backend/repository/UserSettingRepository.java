package com.minhdan.english_practice_backend.repository;

import com.minhdan.english_practice_backend.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
}
