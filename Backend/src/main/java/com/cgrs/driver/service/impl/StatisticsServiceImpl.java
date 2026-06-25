package com.cgrs.driver.service.impl;

import com.cgrs.driver.dao.OperationLogRepository;
import com.cgrs.driver.dto.StatisticsResponse;
import com.cgrs.driver.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final OperationLogRepository operationLogRepository;

    private static final Map<String, String> OPERATION_TYPE_MAP = new HashMap<>();
    static {
        OPERATION_TYPE_MAP.put("upload", "上传");
        OPERATION_TYPE_MAP.put("download", "下载");
        OPERATION_TYPE_MAP.put("delete", "删除");
        OPERATION_TYPE_MAP.put("move", "移动");
        OPERATION_TYPE_MAP.put("share", "分享");
        OPERATION_TYPE_MAP.put("create", "创建");
        OPERATION_TYPE_MAP.put("update", "更新");
    }

    @Override
    public StatisticsResponse getStatistics() {
        StatisticsResponse response = new StatisticsResponse();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();

        response.setTotalOperations(operationLogRepository.count());
        response.setTodayOperations(operationLogRepository.countToday(startOfToday, endOfToday));
        
        response.setUploadCount(operationLogRepository.countByOperationType("upload"));
        response.setDownloadCount(operationLogRepository.countByOperationType("download"));
        response.setDeleteCount(operationLogRepository.countByOperationType("delete"));
        response.setMoveCount(operationLogRepository.countByOperationType("move"));
        response.setShareCount(operationLogRepository.countByOperationType("share"));
        
        response.setDailyTrend(getDailyTrend());
        response.setTypeDistribution(getTypeDistribution());
        response.setTopActiveUsers(getTopActiveUsers());
        
        return response;
    }

    private List<StatisticsResponse.OperationTrend> getDailyTrend() {
        List<StatisticsResponse.OperationTrend> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            Long count = operationLogRepository.countByDate(startOfDay, endOfDay);
            trend.add(new StatisticsResponse.OperationTrend(dateStr, count));
        }
        
        return trend;
    }

    private List<StatisticsResponse.OperationTypeCount> getTypeDistribution() {
        List<StatisticsResponse.OperationTypeCount> distribution = new ArrayList<>();
        Long total = operationLogRepository.count();
        
        for (Map.Entry<String, String> entry : OPERATION_TYPE_MAP.entrySet()) {
            String type = entry.getKey();
            String typeName = entry.getValue();
            Long count = operationLogRepository.countByOperationType(type);
            Double percentage = total > 0 ? (count * 100.0 / total) : 0.0;
            distribution.add(new StatisticsResponse.OperationTypeCount(type, typeName, count, Math.round(percentage * 100.0) / 100.0));
        }
        
        return distribution;
    }

    private List<StatisticsResponse.UserActivity> getTopActiveUsers() {
        List<Object[]> results = operationLogRepository.findTopActiveUsers();
        List<StatisticsResponse.UserActivity> users = new ArrayList<>();
        
        for (Object[] row : results) {
            Long userId = (Long) row[0];
            String username = (String) row[1];
            Long count = (Long) row[2];
            users.add(new StatisticsResponse.UserActivity(userId, username, count));
        }
        
        return users;
    }
}