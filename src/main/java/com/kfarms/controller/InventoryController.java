package com.kfarms.controller;

import com.kfarms.dto.InventoryDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Inventory;
import com.kfarms.mapper.InventoryMapper;
import com.kfarms.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// TODO: Add role-based access control to this controller
@RestController
@RequestMapping("/api/Inventory")
public class InventoryController {
    private final InventoryService service;
    public InventoryController(InventoryService service){
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryDto> getById(@PathVariable Long id){
        Inventory inventory = service.getById(id);
        if (id != null){
            return ResponseEntity.ok(InventoryMapper.toDto(inventory));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryDto>>> getAll(){
        List<InventoryDto> dtos = service.getAll().stream()
                .map(InventoryMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "All inventory fetched successfully", dtos)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryDto>> create(@RequestBody InventoryDto dto){
        Inventory inventory = service.save(InventoryMapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "Invtentory saved successfully", InventoryMapper.toDto(inventory))
                );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Inventory record deleted successfully", null)
        );
    }

}
