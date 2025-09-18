package com.kfarms.controller;

import com.kfarms.dto.SalesResponseDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Sales;
import com.kfarms.mapper.SalesMapper;
import com.kfarms.service.SalesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// TODO: Add role-based access control to this controller
@RestController
@RequestMapping("/api/Sales")
public class SalesController {
    private final SalesService service;
    public SalesController(SalesService service){
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesResponseDto>> getById(@PathVariable Long id){
        Sales sales = service.getById(id);
        if(id != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Sales record fetched successfully", SalesMapper.toDto(sales))
            );
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SalesResponseDto>>> getAll(){
        List<SalesResponseDto> dtos = service.getAll().stream()
                .map(SalesMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "All sales record fetched successfully", dtos)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SalesResponseDto>> create(@RequestBody SalesResponseDto dto){
        Sales sales = service.create(SalesMapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "Sales record saved successfully", SalesMapper.toDto(sales))
                );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Sales record deleted successfully", null)
        );
    }
}
