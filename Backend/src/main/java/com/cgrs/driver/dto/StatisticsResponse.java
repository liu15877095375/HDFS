package com.cgrs.driver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {
    
    private Long totalOperations;
    
    private Long todayOperations;
    
    private Long uploadCount;
    
    private Long downloadCount;
    
    private Long deleteCount;
    
    private Long moveCount;
    
    private Long shareCount;
    
    private List<OperationTrend> dailyTrend;
    
    private List<OperationTypeCount> typeDistribution;
    
    private List<UserActivity> topActiveUsers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationTrend {
        private String date;
        private Long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationTypeCount {
        private String type;
        private String typeName;
        private Long count;
        private Double percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivity {
        private Long userId;
        private String username;
        private Long operationCount;
    }
}