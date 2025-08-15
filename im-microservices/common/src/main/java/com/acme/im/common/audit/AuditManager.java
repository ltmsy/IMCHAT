package com.acme.im.common.audit;

import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 审计管理器
 * 支持操作审计、日志记录、审计追踪等功能
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class AuditManager {

    @Autowired
    private AsyncEventPublisher eventPublisher;

    // 审计日志缓存
    private final ConcurrentHashMap<String, AuditLog> auditLogCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 审计日志
     */
    @Data
    public static class AuditLog {
        private String id;
        private String userId;
        private String username;
        private String sessionId;
        private String clientIp;
        private String userAgent;
        private String action;
        private String resource;
        private String resourceId;
        private String operation;
        private String result;
        private String details;
        private LocalDateTime timestamp;
        private long duration;
        private String[] tags;
        private Map<String, Object> metadata;
    }

    /**
     * 审计事件
     */
    @Data
    public static class AuditEvent {
        private String eventType;
        private String userId;
        private String action;
        private String resource;
        private String result;
        private LocalDateTime timestamp;
        private Map<String, Object> context;
    }

    /**
     * 审计配置
     */
    @Data
    public static class AuditConfig {
        private boolean enabled;
        private String level; // ALL, IMPORTANT, CRITICAL
        private boolean includeMetadata;
        private boolean includeStacktrace;
        private String[] excludedActions;
        private String[] excludedResources;
        private long retentionDays;
        private boolean enableCompression;
        private boolean enableEncryption;
    }

    /**
     * 初始化审计管理器
     */
    public void initialize() {
        try {
            // 启动清理任务
            startCleanupTask();
            
            log.info("审计管理器初始化成功");
        } catch (Exception e) {
            log.error("审计管理器初始化失败", e);
        }
    }

    /**
     * 记录审计日志
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param action 操作
     * @param resource 资源
     * @param result 结果
     * @return 是否记录成功
     */
    public boolean recordAuditLog(String userId, String username, String action, String resource, String result) {
        return recordAuditLog(userId, username, null, null, null, action, resource, null, result, null, null);
    }

    /**
     * 记录审计日志（完整版）
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param sessionId 会话ID
     * @param clientIp 客户端IP
     * @param userAgent 用户代理
     * @param action 操作
     * @param resource 资源
     * @param resourceId 资源ID
     * @param result 结果
     * @param details 详细信息
     * @param tags 标签
     * @return 是否记录成功
     */
    public boolean recordAuditLog(String userId, String username, String sessionId, String clientIp, String userAgent,
                                 String action, String resource, String resourceId, String result, String details, String[] tags) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setId(generateAuditLogId());
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setSessionId(sessionId);
            auditLog.setClientIp(clientIp);
            auditLog.setUserAgent(userAgent);
            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setResourceId(resourceId);
            auditLog.setResult(result);
            auditLog.setDetails(details);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setTags(tags != null ? tags : new String[0]);
            auditLog.setMetadata(new ConcurrentHashMap<>());

            // 缓存审计日志
            auditLogCache.put(auditLog.getId(), auditLog);
            
            // 发布审计事件
            publishAuditEvent("AUDIT_LOG_RECORDED", userId, action, resource, result);
            
            log.debug("审计日志记录成功: id={}, userId={}, action={}, resource={}", 
                    auditLog.getId(), userId, action, resource);
            
            return true;
        } catch (Exception e) {
            log.error("审计日志记录失败: userId={}, action={}, resource={}", userId, action, resource, e);
            return false;
        }
    }

    /**
     * 记录登录审计
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param clientIp 客户端IP
     * @param userAgent 用户代理
     * @param success 是否成功
     * @param details 详细信息
     * @return 是否记录成功
     */
    public boolean recordLoginAudit(String userId, String username, String clientIp, String userAgent, boolean success, String details) {
        String action = "LOGIN";
        String resource = "AUTHENTICATION";
        String result = success ? "SUCCESS" : "FAILED";
        String[] tags = {"security", "authentication"};
        
        return recordAuditLog(userId, username, null, clientIp, userAgent, action, resource, null, result, details, tags);
    }

    /**
     * 记录登出审计
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param sessionId 会话ID
     * @param clientIp 客户端IP
     * @return 是否记录成功
     */
    public boolean recordLogoutAudit(String userId, String username, String sessionId, String clientIp) {
        String action = "LOGOUT";
        String resource = "AUTHENTICATION";
        String result = "SUCCESS";
        String[] tags = {"security", "authentication"};
        
        return recordAuditLog(userId, username, sessionId, clientIp, null, action, resource, null, result, null, tags);
    }

    /**
     * 记录权限检查审计
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param resource 资源
     * @param action 操作
     * @param granted 是否授权
     * @param details 详细信息
     * @return 是否记录成功
     */
    public boolean recordPermissionAudit(String userId, String username, String resource, String action, boolean granted, String details) {
        String auditAction = "PERMISSION_CHECK";
        String result = granted ? "GRANTED" : "DENIED";
        String[] tags = {"security", "permission"};
        
        return recordAuditLog(userId, username, null, null, null, auditAction, resource, action, result, details, tags);
    }

    /**
     * 记录数据操作审计
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param operation 操作类型
     * @param resource 资源
     * @param resourceId 资源ID
     * @param success 是否成功
     * @param details 详细信息
     * @return 是否记录成功
     */
    public boolean recordDataOperationAudit(String userId, String username, String operation, String resource, String resourceId, boolean success, String details) {
        String result = success ? "SUCCESS" : "FAILED";
        String[] tags = {"data", operation.toLowerCase()};
        
        return recordAuditLog(userId, username, null, null, null, operation, resource, resourceId, result, details, tags);
    }

    /**
     * 记录系统操作审计
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param action 操作
     * @param resource 资源
     * @param success 是否成功
     * @param details 详细信息
     * @return 是否记录成功
     */
    public boolean recordSystemOperationAudit(String userId, String username, String action, String resource, boolean success, String details) {
        String result = success ? "SUCCESS" : "FAILED";
        String[] tags = {"system", "operation"};
        
        return recordAuditLog(userId, username, null, null, null, action, resource, null, result, details, tags);
    }

    /**
     * 查询审计日志
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param action 操作
     * @param resource 资源
     * @return 审计日志列表
     */
    public java.util.List<AuditLog> queryAuditLogs(String userId, LocalDateTime startTime, LocalDateTime endTime, String action, String resource) {
        try {
            return auditLogCache.values().stream()
                    .filter(log -> userId == null || userId.equals(log.getUserId()))
                    .filter(log -> startTime == null || !log.getTimestamp().isBefore(startTime))
                    .filter(log -> endTime == null || !log.getTimestamp().isAfter(endTime))
                    .filter(log -> action == null || action.equals(log.getAction()))
                    .filter(log -> resource == null || resource.equals(log.getResource()))
                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("查询审计日志失败: userId={}, startTime={}, endTime={}, action={}, resource={}", 
                    userId, startTime, endTime, action, resource, e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 获取审计统计信息
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息
     */
    public Map<String, Object> getAuditStatistics(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            java.util.List<AuditLog> logs = queryAuditLogs(userId, startTime, endTime, null, null);
            
            Map<String, Object> statistics = new ConcurrentHashMap<>();
            statistics.put("totalLogs", logs.size());
            statistics.put("uniqueUsers", logs.stream().map(AuditLog::getUserId).distinct().count());
            statistics.put("uniqueActions", logs.stream().map(AuditLog::getAction).distinct().count());
            statistics.put("uniqueResources", logs.stream().map(AuditLog::getResource).distinct().count());
            
            // 按结果统计
            Map<String, Long> resultStats = logs.stream()
                    .collect(java.util.stream.Collectors.groupingBy(AuditLog::getResult, java.util.stream.Collectors.counting()));
            statistics.put("resultStats", resultStats);
            
            // 按操作统计
            Map<String, Long> actionStats = logs.stream()
                    .collect(java.util.stream.Collectors.groupingBy(AuditLog::getAction, java.util.stream.Collectors.counting()));
            statistics.put("actionStats", actionStats);
            
            // 按资源统计
            Map<String, Long> resourceStats = logs.stream()
                    .collect(java.util.stream.Collectors.groupingBy(AuditLog::getResource, java.util.stream.Collectors.counting()));
            statistics.put("resourceStats", resourceStats);
            
            return statistics;
        } catch (Exception e) {
            log.error("获取审计统计信息失败: userId={}, startTime={}, endTime={}", userId, startTime, endTime, e);
            return new ConcurrentHashMap<>();
        }
    }

    /**
     * 导出审计日志
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param format 导出格式（JSON, CSV, XML）
     * @return 导出内容
     */
    public String exportAuditLogs(String userId, LocalDateTime startTime, LocalDateTime endTime, String format) {
        try {
            java.util.List<AuditLog> logs = queryAuditLogs(userId, startTime, endTime, null, null);
            
            switch (format.toUpperCase()) {
                case "JSON":
                    return exportToJson(logs);
                case "CSV":
                    return exportToCsv(logs);
                case "XML":
                    return exportToXml(logs);
                default:
                    log.warn("不支持的导出格式: {}", format);
                    return "";
            }
        } catch (Exception e) {
            log.error("导出审计日志失败: userId={}, startTime={}, endTime={}, format={}", 
                    userId, startTime, endTime, format, e);
            return "";
        }
    }

    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        // 每天清理过期的审计日志
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredAuditLogs();
            } catch (Exception e) {
                log.error("清理过期审计日志任务执行异常", e);
            }
        }, 24, 24, TimeUnit.HOURS);
    }

    /**
     * 清理过期的审计日志
     */
    private void cleanupExpiredAuditLogs() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(90); // 保留90天
            
            auditLogCache.entrySet().removeIf(entry -> {
                AuditLog auditLog = entry.getValue();
                boolean expired = auditLog.getTimestamp().isBefore(cutoffTime);
                if (expired) {
                    log.debug("清理过期审计日志: id={}, timestamp={}", auditLog.getId(), auditLog.getTimestamp());
                }
                return expired;
            });
            
            log.info("过期审计日志清理完成，剩余数量: {}", auditLogCache.size());
        } catch (Exception e) {
            log.error("清理过期审计日志异常", e);
        }
    }

    /**
     * 生成审计日志ID
     */
    private String generateAuditLogId() {
        return "AUDIT_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 发布审计事件
     */
    private void publishAuditEvent(String eventType, String userId, String action, String resource, String result) {
        try {
            AuditEvent event = new AuditEvent();
            event.setEventType(eventType);
            event.setUserId(userId);
            event.setAction(action);
            event.setResource(resource);
            event.setResult(result);
            event.setTimestamp(LocalDateTime.now());
            event.setContext(new ConcurrentHashMap<>());

            String subject = "im.audit.event";
            eventPublisher.publishToJetStream(subject, event);
            log.debug("审计事件发布: eventType={}, userId={}, action={}", eventType, userId, action);
        } catch (Exception e) {
            log.error("发布审计事件异常: eventType={}, userId={}, action={}", eventType, userId, action, e);
        }
    }

    /**
     * 导出为JSON格式
     */
    private String exportToJson(java.util.List<AuditLog> logs) {
        try {
            // 这里可以使用Gson进行序列化
            // 暂时返回简单的字符串表示
            StringBuilder json = new StringBuilder();
            json.append("[\n");
            
            for (int i = 0; i < logs.size(); i++) {
                AuditLog log = logs.get(i);
                json.append("  {\n");
                json.append("    \"id\": \"").append(log.getId()).append("\",\n");
                json.append("    \"userId\": \"").append(log.getUserId()).append("\",\n");
                json.append("    \"username\": \"").append(log.getUsername()).append("\",\n");
                json.append("    \"action\": \"").append(log.getAction()).append("\",\n");
                json.append("    \"resource\": \"").append(log.getResource()).append("\",\n");
                json.append("    \"result\": \"").append(log.getResult()).append("\",\n");
                json.append("    \"timestamp\": \"").append(log.getTimestamp()).append("\"\n");
                json.append("  }");
                
                if (i < logs.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            
            json.append("]\n");
            return json.toString();
        } catch (Exception e) {
            log.error("导出JSON格式失败", e);
            return "[]";
        }
    }

    /**
     * 导出为CSV格式
     */
    private String exportToCsv(java.util.List<AuditLog> logs) {
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("ID,UserID,Username,Action,Resource,Result,Timestamp\n");
            
            for (AuditLog log : logs) {
                csv.append(log.getId()).append(",");
                csv.append(log.getUserId()).append(",");
                csv.append(log.getUsername()).append(",");
                csv.append(log.getAction()).append(",");
                csv.append(log.getResource()).append(",");
                csv.append(log.getResult()).append(",");
                csv.append(log.getTimestamp()).append("\n");
            }
            
            return csv.toString();
        } catch (Exception e) {
            log.error("导出CSV格式失败", e);
            return "";
        }
    }

    /**
     * 导出为XML格式
     */
    private String exportToXml(java.util.List<AuditLog> logs) {
        try {
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<auditLogs>\n");
            
            for (AuditLog log : logs) {
                xml.append("  <auditLog>\n");
                xml.append("    <id>").append(log.getId()).append("</id>\n");
                xml.append("    <userId>").append(log.getUserId()).append("</userId>\n");
                xml.append("    <username>").append(log.getUsername()).append("</username>\n");
                xml.append("    <action>").append(log.getAction()).append("</action>\n");
                xml.append("    <resource>").append(log.getResource()).append("</resource>\n");
                xml.append("    <result>").append(log.getResult()).append("</result>\n");
                xml.append("    <timestamp>").append(log.getTimestamp()).append("</timestamp>\n");
                xml.append("  </auditLog>\n");
            }
            
            xml.append("</auditLogs>\n");
            return xml.toString();
        } catch (Exception e) {
            log.error("导出XML格式失败", e);
            return "";
        }
    }

    /**
     * 获取审计统计信息
     */
    public String getAuditStatistics() {
        return String.format("AuditStatistics{totalLogs=%d, uniqueUsers=%d, uniqueActions=%d, uniqueResources=%d}", 
                auditLogCache.size(),
                auditLogCache.values().stream().map(AuditLog::getUserId).distinct().count(),
                auditLogCache.values().stream().map(AuditLog::getAction).distinct().count(),
                auditLogCache.values().stream().map(AuditLog::getResource).distinct().count());
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            log.info("审计管理器资源已释放");
        } catch (Exception e) {
            log.error("审计管理器资源释放异常", e);
        }
    }
} 