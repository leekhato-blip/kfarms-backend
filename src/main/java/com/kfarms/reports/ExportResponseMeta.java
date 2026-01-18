package com.kfarms.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
public class ExportResponseMeta {
    private String filename;
    private String contentType;
}
