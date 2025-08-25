package com.kfarms.entity;

public enum SourceType {
    FARM_BIRTH, // born on the farm → startingAgeInWeeks = 0
    SUPPLIER // purchased from supplier → startingAgeInWeeks may be 1, 2, ...
}
