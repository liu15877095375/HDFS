package com.cgrs.driver.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ai_analysis_log")
public class AiAnalysisLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;
    private Long userId;
    private Long fileId;
    @Column(length = 2000)
    private String prompt;
    @Column(length = 10000)
    private String summary;
    @Column(length = 50)
    private String status;
    private LocalDateTime createTime;
}
