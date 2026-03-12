package com.kfarms.platform.service;

import com.kfarms.platform.dto.PlatformUserListItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlatformUserService {

    Page<PlatformUserListItemDto> searchUsers(String search, Pageable pageable);

    void setPlatformAdmin(Long userId, boolean value);

    void setUserEnabled(Long userId, boolean value); // remove if AppUser doesn't have enabled
}
