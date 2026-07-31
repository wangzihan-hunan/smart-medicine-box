package com.example.medicinebox.service;

import com.example.medicinebox.constant.FeedbackStatus;
import com.example.medicinebox.dto.response.FeedbackListResponse;
import com.example.medicinebox.dto.response.FeedbackResponse;
import com.example.medicinebox.entity.Feedback;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.FeedbackRepository;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 意见反馈服务类
 * 核心功能：用户提交反馈、我的反馈列表查询、反馈详情、反馈删除、管理员反馈管理（回复/状态更新）等
 */
@Service
public class FeedbackService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(UserRepository userRepository, FeedbackRepository feedbackRepository) {
        super(userRepository);
        this.feedbackRepository = feedbackRepository;
    }

    /**
     * 提交意见反馈
     *
     * @param type 反馈类型
     * @param title 反馈标题
     * @param content 反馈内容
     * @param contact 联系方式
     * @return 反馈响应对象
     */
    @Transactional
    public FeedbackResponse submitFeedback(String type, String title, String content, String contact) {
        User user = getCurrentUser();
        log.debug("用户提交反馈, 用户ID: {}, 类型: {}, 标题: {}", user.getId(), type, title);

        Feedback feedback = new Feedback();
        feedback.setUserId(user.getId());
        feedback.setType(type);
        feedback.setTitle(title);
        feedback.setContent(content);
        feedback.setContact(contact);
        feedback.setStatus(FeedbackStatus.PENDING);

        feedback = feedbackRepository.save(feedback);
        log.info("反馈提交成功, ID: {}, 用户ID: {}", feedback.getId(), user.getId());

        return FeedbackResponse.fromEntity(feedback);
    }

    /**
     * 分页查询当前用户的反馈列表
     * 支持按状态筛选
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param status 反馈状态筛选（可选）
     * @return 分页反馈列表响应
     */
    @Transactional(readOnly = true)
    public FeedbackListResponse getMyFeedbacks(int page, int size, String status) {
        User user = getCurrentUser();
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        log.debug("查询我的反馈列表, 用户ID: {}, 状态: {}", user.getId(), status);

        Pageable pageable = PageRequest.of(validatedPage, validatedSize);
        Page<Feedback> feedbackPage;

        // 按状态筛选或查询全部
        if (status != null && !status.isEmpty()) {
            feedbackPage = feedbackRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), FeedbackStatus.valueOf(status), pageable);
        } else {
            feedbackPage = feedbackRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        }

        List<FeedbackResponse> list = feedbackPage.getContent().stream()
                .map(FeedbackResponse::fromEntity)
                .collect(Collectors.toList());

        return FeedbackListResponse.of(feedbackPage.getTotalElements(), feedbackPage.getTotalPages(), list);
    }

    /**
     * 查询反馈详情
     * 普通用户只能查看自己的反馈，管理员可查看所有反馈
     *
     * @param id 反馈ID
     * @return 反馈响应对象
     * @throws ResourceNotFoundException 反馈不存在或无权限时抛出
     */
    public FeedbackResponse getFeedbackDetail(Integer id) {
        User user = getCurrentUser();
        log.debug("查询反馈详情, ID: {}, 用户ID: {}", id, user.getId());

        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("反馈不存在"));

        // 权限校验：只能查看自己的反馈，管理员除外
        if (!feedback.getUserId().equals(user.getId()) && !user.isAdmin()) {
            throw new ResourceNotFoundException("反馈不存在");
        }

        return FeedbackResponse.fromEntity(feedback);
    }

    /**
     * 删除反馈
     * 普通用户只能删除自己的反馈，管理员可删除所有反馈
     *
     * @param id 反馈ID
     * @throws ResourceNotFoundException 反馈不存在或无权限时抛出
     */
    @Transactional
    public void deleteFeedback(Integer id) {
        User user = getCurrentUser();
        log.debug("删除反馈, ID: {}, 用户ID: {}", id, user.getId());

        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("反馈不存在"));

        // 权限校验：只能删除自己的反馈，管理员除外
        if (!feedback.getUserId().equals(user.getId()) && !user.isAdmin()) {
            throw new ResourceNotFoundException("反馈不存在");
        }

        feedbackRepository.delete(feedback);
        log.info("反馈删除成功, ID: {}", id);
    }

    /**
     * 管理员分页查询所有反馈
     * 支持同时按状态和类型组合筛选，pageSize 上限 100
     *
     * <p>使用 JPA Specification 动态组合筛选条件，status 和 type 可同时生效。
     *
     * @param page 页码
     * @param size 每页大小
     * @param status 状态筛选（可选）
     * @param type 类型筛选（可选）
     * @return 分页反馈列表响应
     */
    @Transactional(readOnly = true)
    public FeedbackListResponse getAllFeedbacks(int page, int size, String status, String type) {
        log.debug("管理员查询所有反馈, 状态: {}, 类型: {}", status, type);

        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Feedback> spec = buildAdminQuerySpec(status, type);
        Page<Feedback> feedbackPage = feedbackRepository.findAll(spec, pageable);

        List<FeedbackResponse> list = feedbackPage.getContent().stream()
                .map(FeedbackResponse::fromEntity)
                .collect(Collectors.toList());

        return FeedbackListResponse.of(feedbackPage.getTotalElements(), feedbackPage.getTotalPages(), list);
    }

    /**
     * 构建管理员查询的 JPA Specification
     * 动态组合 status 和 type 筛选条件，两者可同时生效
     */
    private Specification<Feedback> buildAdminQuerySpec(String status, String type) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (type != null && !type.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 管理员回复反馈
     * 设置回复内容、回复状态和回复时间
     *
     * @param id 反馈ID
     * @param reply 回复内容
     * @return 更新后的反馈响应对象
     * @throws ResourceNotFoundException 反馈不存在时抛出
     */
    @Transactional
    public FeedbackResponse replyFeedback(Integer id, String reply) {
        log.debug("管理员回复反馈, ID: {}", id);

        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("反馈不存在"));

        feedback.reply(reply);
        feedback = feedbackRepository.save(feedback);

        log.info("反馈回复成功, ID: {}", id);
        return FeedbackResponse.fromEntity(feedback);
    }

    /**
     * 更新反馈状态
     * 若状态设为已回复且未设置回复时间，则自动设置回复时间
     *
     * @param id 反馈ID
     * @param status 新状态
     * @return 更新后的反馈响应对象
     * @throws ResourceNotFoundException 反馈不存在时抛出
     */
    @Transactional
    public FeedbackResponse updateFeedbackStatus(Integer id, String status) {
        log.debug("更新反馈状态, ID: {}, 状态: {}", id, status);

        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("反馈不存在"));

        FeedbackStatus feedbackStatus = FeedbackStatus.valueOf(status);
        feedback.advanceStatus(feedbackStatus);
        feedback = feedbackRepository.save(feedback);

        log.info("反馈状态更新成功, ID: {}, 状态: {}", id, status);
        return FeedbackResponse.fromEntity(feedback);
    }

    /**
     * 获取待处理反馈数量
     *
     * @return 待处理反馈数
     */
    public long getPendingCount() {
        return feedbackRepository.countByStatus(FeedbackStatus.PENDING);
    }
}
