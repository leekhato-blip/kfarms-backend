package com.kfarms.controller;

import com.kfarms.dto.EggDto;
import com.kfarms.dto.FeedDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Egg;
import com.kfarms.entity.Feed;
import com.kfarms.mapper.EggMapper;
import com.kfarms.mapper.FeedMapper;
import com.kfarms.service.FeedService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// TODO: Add role-based access control to this controller
@RestController
@RequestMapping("/api/Feeds")
public class FeedController {
    private final FeedService service;
    public FeedController(FeedService service){
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeedDto>> getById(@PathVariable Long id){
        Feed feed = service.getById(id);
        if(feed != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Feed detail fetched", FeedMapper.toDto(feed))
            );
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<FeedDto>>> getAll(){
        List<FeedDto> dtos = service.getAll().stream()
                .map(FeedMapper::toDto) // Convert each entity to DTO
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "All Feeds fetched", dtos)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeedDto>> create(@RequestBody FeedDto dto){
        Feed feed = service.save(FeedMapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "Feed saved successfully", FeedMapper.toDto(feed))
                );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "feed deleted successfully", null)
        );
    }

}
