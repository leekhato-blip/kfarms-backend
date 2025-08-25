package com.kfarms.service;

import com.kfarms.entity.Feed;

import java.util.List;

public interface FeedService {
    List<Feed> getAll();
    Feed getById(Long id);
    Feed save(Feed feed);
    void delete(Long id);
}

