package com.kfarms.controller;

import com.kfarms.dto.HatchDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.FishHatch;
import com.kfarms.mapper.HatchMapper;
import com.kfarms.service.HatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// TODO: Add role-based access control to this controller
@RestController
@RequestMapping("/api/hatch")
public class HatchController {
    private final HatchService service;
    public HatchController(HatchService service){
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HatchDto>> getById(@PathVariable Long id){
        FishHatch fishHatch = service.getById(id);
        if (id != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "FishHatch record fetched", HatchMapper.toDto(fishHatch))
            );
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HatchDto>>> getAll(){
        List<HatchDto> dtos = service.getAll().stream()
                .map(HatchMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "All FishHatch record fetched successfully", dtos)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HatchDto>> create(@RequestBody HatchDto dto){
        FishHatch fishHatch = service.save(HatchMapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "FishHatch record saved successfully", HatchMapper.toDto(fishHatch))
                );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "FishHatch record deleted successfully", null)
        );
    }
}
