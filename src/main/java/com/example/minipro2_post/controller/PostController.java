package com.example.minipro2_post.controller;

import com.example.minipro2_post.dto.PostDto;
import com.example.minipro2_post.service.PostService;
import com.fasterxml.jackson.annotation.JsonBackReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/post")
public class PostController {
    // DI
    @Autowired
    private PostService postService;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private Environment env;


    // 게시글 작성
    @PostMapping("/create")
    @JsonBackReference
    public ResponseEntity<PostDto> createPost(@RequestBody PostDto postDto,
                                                 @RequestHeader("X-Auth-User") String email) {
        String release_ip = env.getProperty("app.user_ip");
        Mono<Long> webClient = webClientBuilder.baseUrl(release_ip).build()
                .post()
                .uri("/user/checkemail")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"" + email + "\"}")
                .retrieve()
                .bodyToMono(Long.class);
        Long result = webClient.block(); // 동기 처리
//        System.out.println("응답 받은 Long 값: " + result);

        return ResponseEntity.ok(postService.createPost(postDto,result));
    }

    // 게시글 수정
    @PutMapping("/modify/{pid}")
    public ResponseEntity<PostDto> modifyPost(@PathVariable Long pid, @RequestBody PostDto postDto,
                                                 @RequestHeader("X-Auth-User") String email) {
        String release_ip = env.getProperty("app.user_ip");
        Mono<Long> webClient = webClientBuilder.baseUrl(release_ip).build()
                .post()
                .uri("/user/checkemail")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"" + email + "\"}")
                .retrieve()
                .bodyToMono(Long.class);
        Long result = webClient.block();

        return postService.modifyPost(pid, postDto, result)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 게시글 삭제
    @DeleteMapping("/delete/{pid}")
    public ResponseEntity<String> deletePost(@PathVariable Long pid, @RequestHeader("X-Auth-User") String email) {
        String release_ip = env.getProperty("app.user_ip");
        Mono<Long> webClient = webClientBuilder.baseUrl(release_ip).build()
                .post()
                .uri("/user/checkemail")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"" + email + "\"}")
                .retrieve()
                .bodyToMono(Long.class);
        Long result = webClient.block();

        if (postService.deletePost(pid, result)) {
            return ResponseEntity.ok(pid + "번 게시글 삭제 완료");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("삭제 권한이 없습니다.");
        }
    }

    // 특정 사용자의 모든 게시글 및 관련 데이터 삭제 (회원탈퇴용)
    @DeleteMapping("/deleteByUser/{uid}")
    public ResponseEntity<String> deletePostsByUser(@PathVariable Long uid) {
        try {
            int deletedCount = postService.deleteAllByUser(uid);
            return ResponseEntity.ok(uid + "번 사용자의 게시글 " + deletedCount + "개와 관련 데이터가 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("사용자 데이터 삭제 중 오류 발생: " + e.getMessage());
        }
    }

    // 좋아요 토글
    @PutMapping("/toggleLike/{pid}")
    public ResponseEntity<String> toggleLike(@PathVariable Long pid, @RequestHeader("X-Auth-User") String email) {
        String release_ip = env.getProperty("app.user_ip");
        Mono<Long> webClient = webClientBuilder.baseUrl(release_ip).build()
                .post()
                .uri("/user/checkemail")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"" + email + "\"}")
                .retrieve()
                .bodyToMono(Long.class);
        Long uid = webClient.block();

        return postService.toggleLike(pid, uid)
                .map(post -> ResponseEntity.ok(post.getYouLike() + ": 좋아요 상태 변경"))
                .orElse(ResponseEntity.notFound().build());
    }
}