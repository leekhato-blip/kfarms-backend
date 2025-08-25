package com.kfarms.controller;


import com.kfarms.dto.SuppliesDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Supplies;
import com.kfarms.mapper.SuppliesMapper;
import com.kfarms.service.SuppliesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// TODO: Add role-based access control to this controller
@RestController
@RequestMapping("/api/Supplies")
public class SuppliesController {
    private final SuppliesService service;
    public SuppliesController(SuppliesService service){
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SuppliesDto>> getById(@PathVariable Long id){
        Supplies supplies = service.getById(id);
        if (supplies != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Supply record fetched successfully", SuppliesMapper.toDto(supplies))
            );
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SuppliesDto>>> getAll(){
        List<SuppliesDto> dtos = service.getAll().stream()
                .map(SuppliesMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "All supply records fetched successfully", dtos)
        );
    }


    @PostMapping
    public ResponseEntity<ApiResponse<SuppliesDto>> create(@RequestBody SuppliesDto dto){
        Supplies supplies = service.save(SuppliesMapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "Supply record saved successfully", SuppliesMapper.toDto(supplies))
                );
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Supply record deleted successfully", null)
        );
    }
}
