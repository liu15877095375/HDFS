package com.cgrs.driver.controller;

import com.cgrs.driver.dto.AiAnalysisResponse;
import com.cgrs.driver.dto.Result;
import com.cgrs.driver.service.AiAnalysisService;
import com.cgrs.driver.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {

    @Autowired
    private AiAnalysisService aiAnalysisService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/analyze")
    public Result<AiAnalysisResponse> analyze(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "token", required = false) String tokenHeader,
            @RequestParam Long fileId,
            @RequestParam String prompt) {

        try {
            String token = authorization;
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            } else if (tokenHeader != null) {
                token = tokenHeader;
            }

            Long userId = jwtUtil.getUserIdFromToken(token);

            // 执行AI分析
            AiAnalysisResponse resp = aiAnalysisService.analyze(userId, fileId, prompt);

            // 按照你的Result格式返回
            return Result.success("AI分析成功", resp);
        } catch (Exception e) {
            // 错误返回
            return Result.error(500, e.getMessage());
        }
    }
}