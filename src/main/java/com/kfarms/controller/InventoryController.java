package com.kfarms.controller;

import com.kfarms.dto.InventoryRequestDto;
import com.kfarms.dto.InventoryResponseDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Inventory;
import com.kfarms.mapper.InventoryMapper;
import com.kfarms.service.InventoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;
    public InventoryController(InventoryService service){
        this.service = service;
    }

    // CREATE - add new inventory item
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> create(@RequestBody InventoryRequestDto dto){
        InventoryResponseDto saved = service.create(dto);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Inventory record saved successfully", saved)
        );
    }

    // READ - get all with filtering & pagination
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date
            ){
        Map<String, Object> response = service.getAll(page, size, itemName, category, date);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "All inventory fetched successfully", response)
        );
    }

    // READ - get inventory by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<InventoryResponseDto> getById(
            @PathVariable Long id,
            @RequestBody InventoryRequestDto request
    ){
        InventoryResponseDto dto = service.getById(id);
        if (dto != null) {
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Inventory record fetched successfully", dto)
            );
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Inventory with ID: " + id + " not found", null));
        }
    }

    // DELETE - delete an inventory item by ID
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Inventory record deleted successfully", null)
        );
    }

    // SUMMARY - dashboard, summary & analysis
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        Map<String, Object> summary = service.getSummary();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Inventory summary fetched")
        );
    }

}
