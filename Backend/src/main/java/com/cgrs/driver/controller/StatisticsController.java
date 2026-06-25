package com.cgrs.driver.controller;

import com.cgrs.driver.dto.Result;
import com.cgrs.driver.dto.StatisticsResponse;
import com.cgrs.driver.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/operation")
    public Result<StatisticsResponse> getOperationStatistics() {
        StatisticsResponse response = statisticsService.getStatistics();
        return Result.success("获取统计数据成功", response);
    }
}