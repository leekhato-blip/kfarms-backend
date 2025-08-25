package com.kfarms.controller;

import com.kfarms.dto.FowlDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Fowl;
import com.kfarms.mapper.FowlMapper;
import com.kfarms.service.FowlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// TODO: Add role-based access control to this controller
@RestController
@RequestMapping("/api/Fowls")
public class FowlController {
    private final FowlService service;
    public FowlController(FowlService service){
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FowlDto>>> getAll(){
        List<FowlDto> dtos = service.getAll().stream()
                .map(FowlMapper::toDto) // Convert each Fowl to DTO
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "All fowls fetched", dtos)
        );
    }

    @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<FowlDto>> getById(@PathVariable Long id){
        Fowl fowl = service.getById(id);
        if(fowl != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Fowl fetched", FowlMapper.toDto(fowl))
            );
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FowlDto>> create(@RequestBody FowlDto dto){
        Fowl fowl = service.save(FowlMapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "Fowl saved successfully", FowlMapper.toDto(fowl))
                );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<List<FowlDto>>> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Fowl deleted successfully", null)
        );
    }
}
