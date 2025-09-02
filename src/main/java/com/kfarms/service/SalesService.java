package com.kfarms.service;

import com.kfarms.entity.Sales;

import java.util.List;

public interface SalesService {
    List<Sales> getAll();
    Sales getById(Long id);
    Sales save(Sales sales);
    void delete(Long id);
}
