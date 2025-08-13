package com.acme.im.common.websocket.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * WebSocket数据传输对象
 * 提供通用的数据传输功能
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketDTO<T> {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息子类型
     */
    private String subType;

    /**
     * 消息时间
     */
    private LocalDateTime timestamp;

    /**
     * 发送者ID
     */
    private String senderId;

    /**
     * 接收者ID
     */
    private String receiverId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 消息数据
     */
    private T data;

    /**
     * 消息元数据
     */
    private Map<String, Object> metadata;

    /**
     * 消息标签
     */
    private String[] tags;

    /**
     * 消息优先级
     */
    private Integer priority;

    /**
     * 消息状态
     */
    private String status;

    /**
     * 消息版本
     */
    private String version;

    /**
     * 是否压缩
     */
    private Boolean compressed;

    /**
     * 是否加密
     */
    private Boolean encrypted;

    /**
     * 签名
     */
    private String signature;

    /**
     * 创建文本消息
     */
    public static WebSocketDTO<String> createTextMessage(String senderId, String receiverId, String content) {
        return WebSocketDTO.<String>builder()
                .messageId(generateMessageId())
                .messageType("TEXT")
                .subType("MESSAGE")
                .timestamp(LocalDateTime.now())
                .senderId(senderId)
                .receiverId(receiverId)
                .data(content)
                .metadata(new HashMap<>())
                .tags(new String[]{"text", "message"})
                .priority(5)
                .status("SENT")
                .version("1.0")
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建文件消息
     */
    public static WebSocketDTO<FileMessage> createFileMessage(String senderId, String receiverId, String fileName, String fileUrl, Long fileSize) {
        FileMessage fileData = FileMessage.builder()
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileSize(fileSize)
                .fileType(getFileType(fileName))
                .timestamp(LocalDateTime.now())
                .build();

        return WebSocketDTO.<FileMessage>builder()
                .messageId(generateMessageId())
                .messageType("FILE")
                .subType("UPLOAD")
                .timestamp(LocalDateTime.now())
                .senderId(senderId)
                .receiverId(receiverId)
                .data(fileData)
                .metadata(new HashMap<>())
                .tags(new String[]{"file", "upload"})
                .priority(5)
                .status("SENT")
                .version("1.0")
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建图片消息
     */
    public static WebSocketDTO<ImageMessage> createImageMessage(String senderId, String receiverId, String imageUrl, String thumbnailUrl, Integer width, Integer height) {
        ImageMessage imageData = ImageMessage.builder()
                .imageUrl(imageUrl)
                .thumbnailUrl(thumbnailUrl)
                .width(width)
                .height(height)
                .timestamp(LocalDateTime.now())
                .build();

        return WebSocketDTO.<ImageMessage>builder()
                .messageId(generateMessageId())
                .messageType("IMAGE")
                .subType("SEND")
                .timestamp(LocalDateTime.now())
                .senderId(senderId)
                .receiverId(receiverId)
                .data(imageData)
                .metadata(new HashMap<>())
                .tags(new String[]{"image", "media"})
                .priority(5)
                .status("SENT")
                .version("1.0")
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建语音消息
     */
    public static WebSocketDTO<VoiceMessage> createVoiceMessage(String senderId, String receiverId, String voiceUrl, Long duration) {
        VoiceMessage voiceData = VoiceMessage.builder()
                .voiceUrl(voiceUrl)
                .duration(duration)
                .timestamp(LocalDateTime.now())
                .build();

        return WebSocketDTO.<VoiceMessage>builder()
                .messageId(generateMessageId())
                .messageType("VOICE")
                .subType("RECORD")
                .timestamp(LocalDateTime.now())
                .senderId(senderId)
                .receiverId(receiverId)
                .data(voiceData)
                .metadata(new HashMap<>())
                .tags(new String[]{"voice", "media"})
                .priority(5)
                .status("SENT")
                .version("1.0")
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建视频消息
     */
    public static WebSocketDTO<VideoMessage> createVideoMessage(String senderId, String receiverId, String videoUrl, String thumbnailUrl, Long duration, Integer width, Integer height) {
        VideoMessage videoData = VideoMessage.builder()
                .videoUrl(videoUrl)
                .thumbnailUrl(thumbnailUrl)
                .duration(duration)
                .width(width)
                .height(height)
                .timestamp(LocalDateTime.now())
                .build();

        return WebSocketDTO.<VideoMessage>builder()
                .messageId(generateMessageId())
                .messageType("VIDEO")
                .subType("RECORD")
                .timestamp(LocalDateTime.now())
                .senderId(senderId)
                .receiverId(receiverId)
                .data(videoData)
                .metadata(new HashMap<>())
                .tags(new String[]{"video", "media"})
                .priority(5)
                .status("SENT")
                .version("1.0")
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建位置消息
     */
    public static WebSocketDTO<LocationMessage> createLocationMessage(String senderId, String receiverId, Double latitude, Double longitude, String address) {
        LocationMessage locationData = LocationMessage.builder()
                .latitude(latitude)
                .longitude(longitude)
                .address(address)
                .timestamp(LocalDateTime.now())
                .build();

        return WebSocketDTO.<LocationMessage>builder()
                .messageId(generateMessageId())
                .messageType("LOCATION")
                .subType("SHARE")
                .timestamp(LocalDateTime.now())
                .senderId(senderId)
                .receiverId(receiverId)
                .data(locationData)
                .metadata(new HashMap<>())
                .tags(new String[]{"location", "share"})
                .priority(5)
                .status("SENT")
                .version("1.0")
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建联系人消息
     */
    public static WebSocketDTO<ContactMessage> createContactMessage(String senderId, String receiverId, String contactId, String contactName, String contactAvatar) {
        ContactMessage contactData = ContactMessage.builder()
                .contactId(contactId)
                .contactName(contactName)
                .contactAvatar(contactAvatar)
                .timestamp(LocalDateTime.now())
                .build();

        return WebSocketDTO.<ContactMessage>builder()
                .messageId(generateMessageId())
                .messageType("CONTACT")
                .subType("SHARE")
                .timestamp(LocalDateTime.now())
                .senderId(senderId)
                .receiverId(receiverId)
                .data(contactData)
                .metadata(new HashMap<>())
                .tags(new String[]{"contact", "share"})
                .priority(5)
                .status("SENT")
                .version("1.0")
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建系统通知
     */
    public static WebSocketDTO<SystemNotification> createSystemNotification(String receiverId, String title, String content, String level) {
        SystemNotification notificationData = SystemNotification.builder()
                .title(title)
                .content(content)
                .level(level)
                .timestamp(LocalDateTime.now())
                .build();

        return WebSocketDTO.<SystemNotification>builder()
                .messageId(generateMessageId())
                .messageType("SYSTEM")
                .subType("NOTIFICATION")
                .timestamp(LocalDateTime.now())
                .senderId("SYSTEM")
                .receiverId(receiverId)
                .data(notificationData)
                .metadata(new HashMap<>())
                .tags(new String[]{"system", "notification"})
                .priority(8)
                .status("SENT")
                .version("1.0")
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 添加元数据
     */
    public WebSocketDTO<T> addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        return this;
    }

    /**
     * 添加标签
     */
    public WebSocketDTO<T> addTag(String tag) {
        if (tags == null) {
            tags = new String[0];
        }
        String[] newTags = new String[tags.length + 1];
        System.arraycopy(tags, 0, newTags, 0, tags.length);
        newTags[tags.length] = tag;
        this.tags = newTags;
        return this;
    }

    /**
     * 设置高优先级
     */
    public WebSocketDTO<T> setHighPriority() {
        this.priority = 8;
        return this;
    }

    /**
     * 设置紧急优先级
     */
    public WebSocketDTO<T> setUrgentPriority() {
        this.priority = 10;
        return this;
    }

    /**
     * 启用压缩
     */
    public WebSocketDTO<T> enableCompression() {
        this.compressed = true;
        return this;
    }

    /**
     * 启用加密
     */
    public WebSocketDTO<T> enableEncryption() {
        this.encrypted = true;
        return this;
    }

    /**
     * 设置状态
     */
    public WebSocketDTO<T> setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * 生成消息ID
     */
    private static String generateMessageId() {
        return "MSG_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 获取文件类型
     */
    private static String getFileType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unknown";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "unknown";
    }

    /**
     * 文件消息数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileMessage {
        private String fileName;
        private String fileUrl;
        private Long fileSize;
        private String fileType;
        private String fileHash;
        private LocalDateTime timestamp;
        private String description;
        private Map<String, Object> attributes;
    }

    /**
     * 图片消息数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageMessage {
        private String imageUrl;
        private String thumbnailUrl;
        private Integer width;
        private Integer height;
        private Long fileSize;
        private String format;
        private LocalDateTime timestamp;
        private String description;
        private Map<String, Object> attributes;
    }

    /**
     * 语音消息数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoiceMessage {
        private String voiceUrl;
        private Long duration;
        private Long fileSize;
        private String format;
        private LocalDateTime timestamp;
        private String description;
        private Map<String, Object> attributes;
    }

    /**
     * 视频消息数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VideoMessage {
        private String videoUrl;
        private String thumbnailUrl;
        private Long duration;
        private Integer width;
        private Integer height;
        private Long fileSize;
        private String format;
        private LocalDateTime timestamp;
        private String description;
        private Map<String, Object> attributes;
    }

    /**
     * 位置消息数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationMessage {
        private Double latitude;
        private Double longitude;
        private String address;
        private String city;
        private String country;
        private LocalDateTime timestamp;
        private String description;
        private Map<String, Object> attributes;
    }

    /**
     * 联系人消息数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContactMessage {
        private String contactId;
        private String contactName;
        private String contactAvatar;
        private String contactPhone;
        private String contactEmail;
        private LocalDateTime timestamp;
        private String description;
        private Map<String, Object> attributes;
    }

    /**
     * 系统通知数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemNotification {
        private String title;
        private String content;
        private String level; // INFO, WARNING, ERROR, CRITICAL
        private String category;
        private String action;
        private String actionUrl;
        private LocalDateTime timestamp;
        private String description;
        private Map<String, Object> attributes;
    }
} 