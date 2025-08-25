package com.kfarms.controller;

import com.kfarms.dto.FishPondDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.FishPond;
import com.kfarms.mapper.FishPondMapper;
import com.kfarms.service.FishPondService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// TODO: Add role-based access control to this controller
@RestController
@RequestMapping("/api/fishponds")
public class FishPondController {
    private final FishPondService service;
    public FishPondController(FishPondService service){
        this.service = service;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("FishPond Test Successful");
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FishPondDto>> getById(@PathVariable Long id){
        FishPond fishPond = service.getById(id);
        if(fishPond != null){
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Fishpond fetched", FishPondMapper.toDto(fishPond))
            );
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FishPondDto>>> getAll(){
        List<FishPondDto> dtos = service.getAll().stream()
                .map(FishPondMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "All FishPonds fetched", dtos)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FishPondDto>> create(@RequestBody FishPondDto dto){
        FishPond fishPond = service.save(FishPondMapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(true, "FishPond saved successfully", FishPondMapper.toDto(fishPond))
                );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FishPondDto>> update(@PathVariable Long id, @RequestBody FishPondDto dto){
        FishPond existing = service.getById(id);
        if(existing == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "FishPond not found", null));
        }

        dto.setId(id);
        FishPond updated = service.save(FishPondMapper.toEntity(dto));
        return ResponseEntity.ok(new ApiResponse<>(true, "FishPond updated successfully", FishPondMapper.toDto(updated)));
    }

    // TODO: Prevent deletion if FishPond has associated Hatches
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "FishPond deleted successfully", null)
        );
    }
}