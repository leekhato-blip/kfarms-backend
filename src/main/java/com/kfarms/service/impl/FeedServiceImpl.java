package com.kfarms.service.impl;

import com.kfarms.entity.Feed;
import com.kfarms.repository.FeedRepository;
import com.kfarms.service.FeedService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedServiceImpl implements FeedService {
    private final FeedRepository repo;
    public FeedServiceImpl(FeedRepository repo) { this.repo = repo; }
    public List<Feed> getAll() { return repo.findAll(); }
    public Feed getById(Long id) { return repo.findById(id).orElse(null); }
    public Feed save(Feed feed) {
        return repo.save(feed);
    }
    public void delete(Long id) { repo.deleteById(id); }
}
