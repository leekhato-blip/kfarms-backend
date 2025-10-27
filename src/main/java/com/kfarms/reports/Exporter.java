package com.kfarms.reports;

import java.io.InputStream;
import java.time.LocalDate;

public interface Exporter {
    InputStream exportSales(LocalDate start, LocalDate end) throws Exception;
    InputStream exportEggProduction(LocalDate start, LocalDate end) throws Exception;
    InputStream exportFeedUsage(LocalDate start, LocalDate end) throws Exception;
    InputStream exportInventory(LocalDate start, LocalDate end) throws Exception;
    InputStream exportLivestock(LocalDate start, LocalDate end) throws Exception;
    InputStream exportFishPond(LocalDate start, LocalDate end) throws Exception;
}
