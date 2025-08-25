package com.kfarms.controller;

import com.kfarms.dto.EggDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Egg;
import com.kfarms.entity.Egg;
import com.kfarms.mapper.EggMapper;
import com.kfarms.mapper.EggMapper;
import com.kfarms.service.EggService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

// TODO: Add role-based access control to this controller
@RestController
@RequestMapping("/api/eggs")
public class EggController {
    private final EggService service;

    // Constructor injection of the service
    public EggController(EggService service) {
        this.service = service;
    }

    // Get /api/eggs?layerId=&date=
    @GetMapping
    public ResponseEntity<ApiResponse<List<EggDto>>> getAll(
            @RequestParam(required = false) Long layerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date
    ) {
        List<Egg> eggs = service.getFilteredEggs(layerId, date);

        List<EggDto> dtos = eggs.stream()
                .map(EggMapper::toDto) // Convert each entity to DTO
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Eggs fetched", dtos)
        );
    }

    // Get /api/eggs{id} - Returns a single Egg by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EggDto>> getById(@PathVariable Long id){
        Egg egg = service.getById(id);
        if (egg == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Egg not found", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Egg fetched", EggMapper.toDto(egg)));
    }

    // POST /api/eggs - Create a new Egg
    @PostMapping
    public ResponseEntity<ApiResponse<EggDto>> create(@RequestBody EggDto dto){
        Egg saved = service.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "Egg saved successfully", EggMapper.toDto(saved))
                );
    }

    //  TODO: Restrict to ROLE_ADMIN or ROLE_MANAGER only
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EggDto>> update(@PathVariable Long id, @RequestBody EggDto dto){
        Egg existing = service.getById(id);
        if(existing == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Egg not found", null));
        }
        dto.setId(id); // Ensure ID is consistent
        Egg updated = service.save(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Egg updated successfully", EggMapper.toDto(updated)));
    }

    // TODO: Only allow ROLE_ADMIN to delete eggs
    // DELETE /api/eggs/{id} - Delete an egg
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Egg deleted successfully", null)
        );
    }

}