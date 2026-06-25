package com.cgrs.driver.service;

import com.cgrs.driver.dto.AiAnalysisResponse;

public interface AiAnalysisService {
    AiAnalysisResponse analyze(Long userId, Long fileId, String prompt) throws Exception;
}