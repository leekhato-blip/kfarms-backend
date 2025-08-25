package com.kfarms.controller;

import com.kfarms.dto.HatchDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Hatch;
import com.kfarms.mapper.HatchMapper;
import com.kfarms.service.HatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// TODO: Add role-based access control to this controller
@RestController
@RequestMapping("/api/Hatch")
public class HatchController {
    private final HatchService service;
    public HatchController(HatchService service){
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HatchDto>> getById(@PathVariable Long id){
        Hatch hatch = service.getById(id);
        if (id != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Hatch record fetched", HatchMapper.toDto(hatch))
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
                new ApiResponse<>(true, "All Hatch record fetched successfully", dtos)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HatchDto>> create(@RequestBody HatchDto dto){
        Hatch hatch = service.save(HatchMapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "Hatch record saved successfully", HatchMapper.toDto(hatch))
                );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Hatch record deleted successfully", null)
        );
    }
}
