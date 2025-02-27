package com.example.minipro2_post.service;

import com.example.minipro2_post.entity.PostEntity;
import com.example.minipro2_post.entity.TagEntity;
import com.example.minipro2_post.repository.PostRepository;
import com.example.minipro2_post.repository.PostTagRepository;
import com.example.minipro2_post.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 검색기능
 * 1. 이메일 검색
 * 2. 내용 검색
 * 3. 태그 검색
 */
@Service
public class SearchService {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private PostTagRepository postTagRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    // 이메일 검색
    public List<PostEntity> searchByEmail(String email) {
        try {
            // email 검색을 위한 API 호출
            Mono<Long> webClient = webClientBuilder.baseUrl("http://localhost:8083").build()
                    .post()
                    .uri("/user/checkemail")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"email\":\"" + email + "\"}")
                    .retrieve()
                    .bodyToMono(Long.class);

            Long uid = webClient.block(); // API 응답이 없거나 오류 발생 시 예외 발생

            return postRepository.findByUid(uid);
        } catch (Exception e) {
            throw new RuntimeException("해당 이메일을 가진 사용자가 없습니다.");
        }
    }

    // 내용 검색
    public List<PostEntity> search(String findString) {
        List<PostEntity> contentSerach = postRepository.findByContentContaining(findString);

        for (PostEntity postEntity : contentSerach) {
            System.out.println(postEntity);
        }

        return contentSerach;
    }

    // 태그 검색
    public List<PostEntity> searchByTag(String tagName) {
        // tagName을 기준으로 태그 찾기
        Optional<TagEntity> tagEntity = tagRepository.findByTagName(tagName);

        // 태그가 존재하면 해당 태그와 연결된 게시글 리스트 반환
        return tagEntity.map(tag -> postTagRepository.findByTag(tag)
                .stream()
                .map(postTag -> postTag.getPost())
                .collect(Collectors.toList())
        ).orElse(Collections.emptyList()); // 태그가 없으면 빈 리스트 반환
    }


}
