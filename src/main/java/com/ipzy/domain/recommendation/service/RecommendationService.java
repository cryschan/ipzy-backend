package com.ipzy.domain.recommendation.service;

import com.ipzy._global.common.enums.ClothingCategory;
import com.ipzy.domain.quiz.entity.QuizSession;
import com.ipzy.domain.quiz.repository.QuizSessionRepository;
import com.ipzy.domain.recommendation.client.AiRecommendationClient;
import com.ipzy.domain.recommendation.dto.request.RecommendationRequest;
import com.ipzy.domain.recommendation.dto.response.OutfitRecommendationDto;
import com.ipzy.domain.recommendation.dto.response.RecommendationResponse;
import com.ipzy.domain.recommendation.dto.response.RecommendedItemDto;
import com.ipzy.domain.recommendation.entity.Recommendation;
import com.ipzy.domain.recommendation.entity.RecommendationItem;
import com.ipzy.domain.recommendation.exception.RecommendationException;
import com.ipzy.domain.recommendation.repository.RecommendationRepository;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 추천 서비스
 * - Python AI 서비스와 통신하여 코디 추천 생성
 */
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class RecommendationService {

    private final QuizSessionRepository quizSessionRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserService userService;
    private final AiRecommendationClient aiClient;

    /**
     * 퀴즈 세션 기반 추천 생성
     *
     * @param sessionId     완료된 퀴즈 세션 ID
     * @param currentUserId 현재 로그인한 사용자 ID (비로그인 시 null)
     * @return 생성된 추천 목록
     */
    @Transactional
    public List<Recommendation> generateRecommendation(Long sessionId, Long currentUserId) {

        log.info("추천 생성 시작: sessionId={}, currentUserId={}", sessionId, currentUserId);

        // 1. 세션 조회 및 검증
        QuizSession session = findCompletedSession(sessionId);

        // 2. 사용자 조회 (Security에서 이미 인증됨)
        User currentUser = userService.findActiveUser(currentUserId);

        // 3. 익명 세션이면 현재 사용자에게 연결
        if (session.isAnonymous()) {
            session.assignUser(currentUser);
            log.info("익명 세션을 사용자에게 연결: sessionId={}, userId={}", sessionId, currentUserId);
        } else {
            // 4. 다른 사용자의 세션이면 거부
            validateSessionOwnership(session, currentUserId);
        }

        // 5. 중복 생성 방지
        if (recommendationRepository.existsBySessionId(sessionId)) {
            throw RecommendationException.recommendationAlreadyExists(sessionId);
        }

        // 6. AI 추천 요청 (퀴즈 답변만 전송)
        RecommendationRequest request = RecommendationRequest.of(session);
        RecommendationResponse response = aiClient.requestRecommendation(request);

        // 7. Entity 변환
        List<Recommendation> recommendations = convertToEntities(session, response);

        // 8. 저장
        List<Recommendation> savedRecommendations = recommendationRepository.saveAll(recommendations);

        log.info("추천 생성 완료: sessionId={}, 추천 수={}", sessionId, savedRecommendations.size());
        return savedRecommendations;
    }

    /**
     * 세션별 추천 조회
     *
     * @param sessionId     퀴즈 세션 ID
     * @param currentUserId 현재 로그인한 사용자 ID (비로그인 시 null)
     */
    public List<Recommendation> getRecommendationsBySession(Long sessionId, Long currentUserId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> RecommendationException.sessionNotFound(sessionId));

        validateSessionAccess(session, currentUserId);

        return recommendationRepository.findBySessionIdWithItems(sessionId);
    }

    /**
     * 사용자별 추천 조회
     */
    public List<Recommendation> getRecommendationsByUser(Long userId) {
        return recommendationRepository.findByUserIdWithItems(userId);
    }

    /**
     * Python API 요청 DTO 미리보기 (테스트용)
     * - 실제 Python API 호출 없이 요청 데이터만 확인
     *
     * @param sessionId     완료된 퀴즈 세션 ID
     * @param currentUserId 현재 로그인한 사용자 ID (비로그인 시 null)
     */
    public RecommendationRequest previewRequest(Long sessionId, Long currentUserId) {
        QuizSession session = findCompletedSession(sessionId);
        validateSessionAccess(session, currentUserId);
        return RecommendationRequest.of(session);
    }

    /**
     * AI 서비스 연결 테스트
     *
     * @param msg 테스트 메시지
     * @return 응답 메시지
     */
    public String testConnection(String msg) {
        return aiClient.testConnection(msg);
    }

    /**
     * 완료된 세션 조회 및 검증
     */
    private QuizSession findCompletedSession(Long sessionId) {

        QuizSession session = quizSessionRepository.findByIdWithAnswers(sessionId)
                .orElseThrow(() -> RecommendationException.sessionNotFound(sessionId));

        if (!session.getCompleted()) {
            throw RecommendationException.sessionNotCompleted(sessionId);
        }

        return session;
    }

    /**
     * 세션 접근 권한 검증 (조회용)
     * - 익명 세션(user=null): 누구나 접근 가능
     * - 로그인 세션: 세션 소유자만 접근 가능
     *
     * @param session       검증 대상 세션
     * @param currentUserId 현재 로그인한 사용자 ID (비로그인 시 null)
     */
    private void validateSessionAccess(QuizSession session, Long currentUserId) {
        User sessionOwner = session.getUser();

        // 익명 세션은 누구나 접근 가능
        if (sessionOwner == null) {
            return;
        }

        // 로그인 세션은 소유자만 접근 가능
        if (!sessionOwner.getId().equals(currentUserId)) {
            log.warn("세션 접근 권한 없음: sessionId={}, ownerId={}, currentUserId={}",
                    session.getId(), sessionOwner.getId(), currentUserId);
            throw RecommendationException.accessDenied(session.getId());
        }
    }

    /**
     * 세션 소유권 검증 (추천 생성용)
     * - 다른 사용자의 세션이면 접근 거부
     *
     * @param session       검증 대상 세션
     * @param currentUserId 현재 로그인한 사용자 ID
     */
    private void validateSessionOwnership(QuizSession session, Long currentUserId) {
        User sessionOwner = session.getUser();

        if (!sessionOwner.getId().equals(currentUserId)) {
            log.warn("세션 소유권 없음: sessionId={}, ownerId={}, currentUserId={}",
                    session.getId(), sessionOwner.getId(), currentUserId);
            throw RecommendationException.accessDenied(session.getId());
        }
    }

    /**
     * AI 응답을 Entity 목록으로 변환
     * - Python 응답 데이터를 그대로 스냅샷으로 변환 (저장은 호출부에서 처리)
     */
    private List<Recommendation> convertToEntities(QuizSession session, RecommendationResponse response) {

        // NPE 방어: AI 응답 검증
        List<OutfitRecommendationDto> outfits = response.recommendedOutfits();
        if (outfits == null || outfits.isEmpty()) {
            log.error("AI 응답에 추천 결과 없음: sessionId={}", session.getId());
            throw RecommendationException.emptyRecommendation();
        }

        User user = session.getUser(); // null일 수 있음 (익명 사용자)

        List<Recommendation> recommendations = new ArrayList<>();

        for (OutfitRecommendationDto outfitDto : outfits) {
            Recommendation recommendation = outfitDto.toEntity(session, user);
            addItemsToRecommendation(recommendation, outfitDto.items());
            recommendations.add(recommendation);
        }

        return recommendations;
    }

    /**
     * 추천에 아이템 목록 추가
     */
    private void addItemsToRecommendation(Recommendation recommendation, List<RecommendedItemDto> itemDtos) {
        int itemOrder = 1;

        for (RecommendedItemDto itemDto : itemDtos) {
            if (itemDto.productId() == null) {
                log.warn("productId 누락, 아이템 스킵");
                continue;
            }

            ClothingCategory category = parseCategory(itemDto.category());
            RecommendationItem item = itemDto.toEntity(itemOrder++, category);
            recommendation.addItem(item);
        }
    }

    /**
     * 카테고리 문자열을 Enum으로 변환
     */
    private ClothingCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            log.warn("카테고리 없음, UNKNOWN 처리");
            return ClothingCategory.UNKNOWN;
        }

        try {
            return ClothingCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 카테고리: {}, UNKNOWN 처리", category);
            return ClothingCategory.UNKNOWN;
        }
    }
}
