package com.kfarms.controller;

import com.kfarms.dto.DuckDto;
import com.kfarms.dto.LayerDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Duck;
import com.kfarms.mapper.DuckMapper;
import com.kfarms.service.DuckService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// TODO: Add role-based access control to this controller
@RestController
@RequestMapping("/api/ducks")
public class DuckController {
    private final DuckService service;

    // Constructor injection of the service
    public DuckController(DuckService service){
        this.service = service;
    }

    // Get /api/ducks - Returns a list of all ducks as DTOs
    @GetMapping
    public ResponseEntity<ApiResponse<List<DuckDto>>> getAll(){
        List<DuckDto> dtos = service.getAll().stream()
                .map(DuckMapper::toDto) // Convert each entity to DTO
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "All Ducks fetched", dtos)
        );
    }

    // Get /api/ducks{id} - Returns a single Duck by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DuckDto>> getById(@PathVariable Long id){
        Duck duck = service.getById(id);
        if(duck != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Duck fetched", DuckMapper.toDto(duck))
            );
        }else{
            return ResponseEntity.notFound().build();
        }

    }

    // POST /api/ducks - Create a new Duck
    @PostMapping
    public ResponseEntity<ApiResponse<DuckDto>>  create(@RequestBody DuckDto dto) {
        System.out.println("Dto Received" + dto);
        Duck duck = service.save(DuckMapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "Duck saved successfully", DuckMapper.toDto(duck))
                );
    }

    // DELETE /api/duck/{id} - Delete a duck
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Duck deleted successfully", null)
        );
    }

}

