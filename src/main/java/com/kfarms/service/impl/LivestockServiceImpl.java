package com.kfarms.service.impl;

import com.kfarms.dto.LivestockRequest;
import com.kfarms.dto.LivestockResponse;
import com.kfarms.entity.Livestock;
import com.kfarms.entity.LivestockType;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.LivestockMapper;
import com.kfarms.repository.LivestockRepository;
import com.kfarms.service.LivestockService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LivestockServiceImpl implements LivestockService {
    private final LivestockRepository repo;

    // CREATE - save Livestock
    @Override
    public  LivestockResponse create(LivestockRequest request, String createBy){
        Livestock entity = LivestockMapper.toEntity(request);
        entity.setCreatedBy(createBy);
        repo.save(entity);
        return LivestockMapper.toResponse(entity);
    }

    // READ - get all Livestock (Pagination and Filtering)
    @Override
    public Map<String, Object> getAll(int page, int size, String batchName, String type, LocalDate arrivalDate){
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        // Convert type string to enum(if provided)
        LivestockType typeEnum = null;
        if(type != null && !type.isBlank()){
            try{
                typeEnum = LivestockType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException ex){
                throw new IllegalArgumentException(
                        "Invalid livestock type: '" + type + "' . Allowed values: " + Arrays.toString(LivestockType.values())
                );
            }
        }
//        final String batchNameFinal = batchName;
        final LivestockType typeEnumFinal = typeEnum;
//        final LocalDate arrivalDateFinal = arrivalDate;


        Specification<Livestock> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (batchName != null && !batchName.isBlank()) {
                // use lower on expression and lowercase the param for case-insensitive search
                predicates.add(cb.like(cb.lower(root.get("batchName")), "%" + batchName.toLowerCase() + "%"));
            }
            if (typeEnumFinal != null) {
                predicates.add(cb.equal(root.get("type"), typeEnumFinal));
            }
            if (arrivalDate != null) {
                predicates.add(cb.equal(root.get("arrivalDate"), arrivalDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Livestock> livestockPage = repo.findAll(spec, pageable);

        List<LivestockResponse> items = livestockPage.getContent()
                .stream()
                .map(LivestockMapper::toResponse)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("page", livestockPage.getNumber());
        result.put("size", livestockPage.getSize());
        result.put("totalItems", livestockPage.getTotalElements());
        result.put("totalPages", livestockPage.getTotalPages());
        result.put("hasNext", livestockPage.hasNext());
        result.put("hasPrevious", livestockPage.hasPrevious());

        return result;

    }

    // READ - get Livestock by ID
    @Override
    public LivestockResponse getById(Long id){
        Livestock entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));
        return LivestockMapper.toResponse(entity);
    }

    // UPDATE - update existing Livestock
    @Override
    public LivestockResponse update(Long id,  LivestockRequest request, String updatedBy){
        Livestock entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));

        // Update fields from request
        entity.setBatchName(request.getBatchName());
        entity.setQuantity(request.getQuantity());
        entity.setType(request.getType());
        entity.setArrivalDate(request.getArrivalDate());
        entity.setSourceType(request.getSourceType());
        entity.setStartingAgeInWeeks(request.getStartingAgeInWeeks() != null ? request.getStartingAgeInWeeks() : entity.getStartingAgeInWeeks());
        entity.setMortality(request.getMortality() != null ? request.getMortality() : entity.getMortality());
        entity.setNotes(request.getNotes());
        entity.setUpdatedBy(updatedBy);

        repo.save(entity);
        return LivestockMapper.toResponse(entity);
    }

    // DELETE - delete livestock by ID
    @Override
    public void delete(Long id){
        if(!repo.existsById(id)){
            throw new ResourceNotFoundException("Livestock", "id", id);
        }
        repo.deleteById(id);
    }

    // SEARCH
    @Override
    public List<LivestockResponse> search(String batchName, String type, LocalDate arrivalDate){
        List<Livestock> list = repo.findAll(); // start with all
        if(batchName != null && !batchName.isEmpty()){
            list = list.stream()
                    .filter(l -> l.getBatchName().toLowerCase().contains(batchName.toLowerCase()))
                    .toList();
        }
        if(type != null && !type.isEmpty()){
            list = list.stream()
                    .filter(l -> l.getType().name().equalsIgnoreCase(type))
                    .toList();
        }
        if(arrivalDate != null){
            list = list.stream()
                    .filter(l -> arrivalDate.equals(l.getArrivalDate()))
                    .toList();
        }
        return list.stream().map(LivestockMapper::toResponse).toList();
    }

    // SUMMARY
    @Override
    public Map<String, Object> getSummary(){
        List<Livestock> all = repo.findAll();
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalLivestock", all.size());
        summary.put("totalMortality", all.stream().mapToInt(l -> l.getMortality() != null ? l.getMortality() : 0).sum());

        // count by type
        Map<String, Long> countByType = all.stream()
                .collect(Collectors.groupingBy(l -> l.getType().name(), Collectors.counting()));
        summary.put("countByType", countByType);

        return summary;
    }

}
