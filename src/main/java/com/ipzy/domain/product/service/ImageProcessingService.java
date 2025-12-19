package com.ipzy.domain.product.service;

import com.ipzy.domain.product.dto.RemoveBackgroundRequest;
import com.ipzy.domain.product.dto.RemoveBackgroundResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * 이미지 처리 서비스 (파이썬 AI 서버 연동)
 */
@Slf4j
@Service
public class ImageProcessingService {

    private final RestClient pythonAiRestClient;

    public ImageProcessingService(@Qualifier("pythonAiRestClient") RestClient pythonAiRestClient) {
        this.pythonAiRestClient = pythonAiRestClient;
    }

    private static final int CHUNK_SIZE = 15; // 배치당 처리할 이미지 개수
    private static final String REMOVE_BACKGROUND_ENDPOINT = "/api/image/remove-background/batch";

    /**
     * 여러 이미지의 누끼를 배치로 제거
     *
     * @param imageUrls 원본 이미지 URL 리스트
     * @return 원본 URL -> 누끼 제거된 URL 매핑
     */
    public Map<String, String> removeBackgroundBatch(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            log.warn("이미지 URL 리스트가 비어있습니다");
            return Collections.emptyMap();
        }

        log.info("배치 누끼 제거 시작: 총 {}개 이미지 (청크 크기: {})", imageUrls.size(), CHUNK_SIZE);

        Map<String, String> resultMap = new HashMap<>();
        List<List<String>> chunks = partitionList(imageUrls, CHUNK_SIZE);

        for (int i = 0; i < chunks.size(); i++) {
            List<String> chunk = chunks.get(i);
            log.debug("청크 {}/{} 처리 중: {}개 이미지", i + 1, chunks.size(), chunk.size());

            try {
                Map<String, String> chunkResult = processChunk(chunk);
                resultMap.putAll(chunkResult);

                log.debug("청크 {}/{} 처리 완료: {}개 성공", i + 1, chunks.size(), chunkResult.size());

                // 청크 간 간격 (API 부하 방지)
                if (i < chunks.size() - 1) {
                    Thread.sleep(500);
                }

            } catch (InterruptedException e) {
                log.error("청크 처리 대기 중 인터럽트 발생", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("청크 {}/{} 처리 실패: {}", i + 1, chunks.size(), e.getMessage(), e);
                // 실패한 청크는 건너뛰고 계속 진행
            }
        }

        log.info("배치 누끼 제거 완료: 총 {}개 성공 / {}개 요청", resultMap.size(), imageUrls.size());
        return resultMap;
    }

    /**
     * 하나의 청크(최대 CHUNK_SIZE개)를 파이썬 서버로 전송하여 처리
     *
     * @param imageUrls 처리할 이미지 URL 리스트
     * @return 원본 URL -> 누끼 제거된 URL 매핑 (성공한 것만)
     */
    private Map<String, String> processChunk(List<String> imageUrls) {
        RemoveBackgroundRequest request = RemoveBackgroundRequest.builder()
                .imageUrls(imageUrls)
                .build();

        try {
            RemoveBackgroundResponse response = pythonAiRestClient.post()
                    .uri(REMOVE_BACKGROUND_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RemoveBackgroundResponse.class);

            if (response == null || response.getResults() == null) {
                log.warn("파이썬 서버 응답이 비어있습니다");
                return Collections.emptyMap();
            }

            // URL 기반으로 요청과 응답 매칭 (original_url 활용)
            Map<String, String> resultMap = new HashMap<>();

            for (RemoveBackgroundResponse.ImageResult result : response.getResults()) {
                if (result.getOriginalUrl() == null) {
                    log.warn("응답에 original_url이 없습니다: {}", result);
                    continue;
                }

                if (result.isSuccess() && result.getRemovedBackgroundUrl() != null) {
                    resultMap.put(result.getOriginalUrl(), result.getRemovedBackgroundUrl());
                } else {
                    log.warn("이미지 처리 실패: url={}, error={}",
                             result.getOriginalUrl(), result.getError());
                }
            }

            return resultMap;

        } catch (Exception e) {
            log.error("파이썬 서버 호출 실패: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 리스트를 지정된 크기의 청크로 분할
     */
    private <T> List<List<T>> partitionList(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, list.size());
            chunks.add(list.subList(i, end));
        }
        return chunks;
    }
}
