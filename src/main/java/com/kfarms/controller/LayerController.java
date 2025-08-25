package com.kfarms.controller;

import com.kfarms.dto.LayerDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.Layer;
import com.kfarms.mapper.LayerMapper;
import com.kfarms.service.LayerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// TODO: Add role-based access control to this controller
@RestController
@RequestMapping("/api/layers")
public class LayerController {
    private final LayerService service;
    public LayerController(LayerService service){ this.service = service;}

    @GetMapping
    public ResponseEntity<ApiResponse<List<LayerDto>>> getAll(){
        List<LayerDto> dtos = service.getAll().stream()
                .map(LayerMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "All layers fetched", dtos)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LayerDto>> getById(@PathVariable Long id){
        Layer layer = service.getById(id);
        if (layer != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Layer fetched", LayerMapper.toDto(layer))
            );
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LayerDto>> create(@RequestBody LayerDto dto){
        Layer layer = service.save(LayerMapper.toEntity(dto));
        System.out.println("Data saved successfully" + layer);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "Layer saved successfully", LayerMapper.toDto(layer))
                );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LayerDto>> update(@PathVariable Long id, @RequestBody LayerDto dto){
        Layer existing = service.getById(id);
        if(existing == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Layer not found", null));
        }
        dto.setId(id);
        Layer updated = service.save(LayerMapper.toEntity(dto));
        return ResponseEntity.ok(new ApiResponse<>(true, "Layer Updated successfully", LayerMapper.toDto(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        Layer layer = service.getById(id);
        if(layer == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Layer not found", null));
        }

        if(!layer.getEggs().isEmpty()){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(false, "cannot delete Layer because it has associated Eggs", null));
        }

        service.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Layer deleted successfully", null));
    }
}
