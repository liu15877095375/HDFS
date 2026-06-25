package com.cgrs.driver.dto;

import lombok.Data;

@Data
public class AiAnalysisResponse {
    private String fileName;
    private String prompt;
    private String summary;
}