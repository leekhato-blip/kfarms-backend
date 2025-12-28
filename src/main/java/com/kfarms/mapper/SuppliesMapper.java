package com.kfarms.mapper;

import com.kfarms.dto.SuppliesRequestDto;
import com.kfarms.dto.SuppliesResponseDto;
import com.kfarms.entity.Supplies;
import com.kfarms.entity.SupplyCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class SuppliesMapper {
    public static SuppliesResponseDto toResponseDto(Supplies entity){
        SuppliesResponseDto dto = new SuppliesResponseDto();
        dto.setId(entity.getId());
        dto.setItemName(entity.getItemName());
        dto.setCategory(entity.getCategory() != null ? entity.getCategory().name() : null);
        dto.setSupplierName(entity.getSupplierName());
        dto.setQuantity(entity.getQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setTotalPrice(entity.getTotalPrice());
        dto.setDate(entity.getSupplyDate());
        dto.setNote(entity.getNote());
        

        // audits
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static Supplies toEntity(SuppliesRequestDto dto){
        Supplies entity = new Supplies();
        entity.setItemName(dto.getItemName());
        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(dto.getUnitPrice());

        // set Category (TYPE)
        if (dto.getCategory() != null) {
            entity.setCategory(SupplyCategory.valueOf(dto.getCategory().toUpperCase()));
        }

        // auto calculate total price (quantity × unitPrice)
        if (dto.getQuantity() != null && dto.getUnitPrice() != null) {
            BigDecimal totalPrice = dto.getUnitPrice()
                    .multiply(BigDecimal.valueOf(dto.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            entity.setTotalPrice(totalPrice);
        } else {
            entity.setTotalPrice(BigDecimal.ZERO);
        }
        entity.setNote(dto.getNote());

        entity.setSupplierName(dto.getSupplierName());
        entity.setSupplyDate(dto.getSupplyDate() != null ? dto.getSupplyDate() : LocalDate.now());
        return entity;
    }
}
