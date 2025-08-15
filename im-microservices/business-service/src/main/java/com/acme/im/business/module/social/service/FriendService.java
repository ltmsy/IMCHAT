package com.acme.im.business.module.social.service;

import com.acme.im.business.module.social.entity.FriendRequest;
import com.acme.im.business.module.social.entity.Friendship;

import java.util.List;

/**
 * 好友管理服务接口
 */
public interface FriendService {
    
    // ================================
    // 好友申请管理
    // ================================
    
    /**
     * 发送好友申请
     * 
     * @param fromUserId 发起用户ID
     * @param toUserId 目标用户ID
     * @param message 申请消息
     * @param source 申请来源
     * @param sourceInfo 来源信息
     * @return 好友申请记录
     */
    FriendRequest sendFriendRequest(Long fromUserId, Long toUserId, String message, Integer source, String sourceInfo);
    
    /**
     * 处理好友申请
     * 
     * @param requestId 申请ID
     * @param toUserId 目标用户ID
     * @param approved 是否同意
     * @param message 回复消息
     * @return 处理结果
     */
    boolean processFriendRequest(Long requestId, Long toUserId, boolean approved, String message);
    
    /**
     * 获取收到的好友申请列表
     * 
     * @param userId 用户ID
     * @return 好友申请列表
     */
    List<FriendRequest> getReceivedFriendRequests(Long userId);
    
    /**
     * 获取发出的好友申请列表
     * 
     * @param userId 用户ID
     * @return 好友申请列表
     */
    List<FriendRequest> getSentFriendRequests(Long userId);
    
    /**
     * 取消好友申请
     * 
     * @param requestId 申请ID
     * @param fromUserId 发起用户ID
     * @return 取消结果
     */
    boolean cancelFriendRequest(Long requestId, Long fromUserId);
    
    // ================================
    // 好友关系管理
    // ================================
    
    /**
     * 获取好友列表
     * 
     * @param userId 用户ID
     * @return 好友列表
     */
    List<Friendship> getFriendList(Long userId);
    
    /**
     * 获取指定分组的好友列表
     * 
     * @param userId 用户ID
     * @param groupName 分组名称
     * @return 好友列表
     */
    List<Friendship> getFriendsByGroup(Long userId, String groupName);
    
    /**
     * 删除好友
     * 
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 删除结果
     */
    boolean deleteFriend(Long userId, Long friendId);
    
    /**
     * 更新好友备注
     * 
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param remark 备注
     * @return 更新结果
     */
    boolean updateFriendRemark(Long userId, Long friendId, String remark);
    
    /**
     * 更新好友分组
     * 
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param groupName 分组名称
     * @return 更新结果
     */
    boolean updateFriendGroup(Long userId, Long friendId, String groupName);
    
    /**
     * 设置/取消星标好友
     * 
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param isStarred 是否星标
     * @return 设置结果
     */
    boolean setFriendStarred(Long userId, Long friendId, boolean isStarred);
    
    /**
     * 设置/取消置顶好友
     * 
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param isTop 是否置顶
     * @return 设置结果
     */
    boolean setFriendTop(Long userId, Long friendId, boolean isTop);
    
    /**
     * 设置/取消免打扰
     * 
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param muteNotifications 是否免打扰
     * @return 设置结果
     */
    boolean setFriendMuteNotifications(Long userId, Long friendId, boolean muteNotifications);
    
    // ================================
    // 好友分组管理
    // ================================
    
    /**
     * 创建好友分组
     * 
     * @param userId 用户ID
     * @param groupName 分组名称
     * @return 创建结果
     */
    boolean createFriendGroup(Long userId, String groupName);
    
    /**
     * 删除好友分组
     * 
     * @param userId 用户ID
     * @param groupName 分组名称
     * @return 删除结果
     */
    boolean deleteFriendGroup(Long userId, String groupName);
    
    /**
     * 重命名好友分组
     * 
     * @param userId 用户ID
     * @param oldGroupName 旧分组名称
     * @param newGroupName 新分组名称
     * @return 重命名结果
     */
    boolean renameFriendGroup(Long userId, String oldGroupName, String newGroupName);
    
    /**
     * 获取用户的好友分组列表
     * 
     * @param userId 用户ID
     * @return 分组名称列表
     */
    List<String> getFriendGroups(Long userId);
    
    // ================================
    // 好友状态检查
    // ================================
    
    /**
     * 检查是否为好友
     * 
     * @param userId 用户ID
     * @param targetUserId 目标用户ID
     * @return 是否为好友
     */
    boolean isFriend(Long userId, Long targetUserId);
    
    /**
     * 检查是否有待处理的好友申请
     * 
     * @param fromUserId 发起用户ID
     * @param toUserId 目标用户ID
     * @return 是否有待处理申请
     */
    boolean hasPendingFriendRequest(Long fromUserId, Long toUserId);
    
    /**
     * 获取好友数量
     * 
     * @param userId 用户ID
     * @return 好友数量
     */
    long getFriendCount(Long userId);
    
    /**
     * 获取指定分组的好友数量
     * 
     * @param userId 用户ID
     * @param groupName 分组名称
     * @return 好友数量
     */
    long getFriendCountByGroup(Long userId, String groupName);
} 