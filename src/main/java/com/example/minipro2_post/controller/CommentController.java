package com.example.minipro2_post.controller;

import com.example.minipro2_post.dto.CommentDto;
import com.example.minipro2_post.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 댓글 관련 컨틀로러 진행
 * 테이블 : pid,uid,comment
 * 1. 댓글 달기
 *  ㄴ pid를 확인하여 문제없는 확인
 * 2. 댓글 수정
 * ㄴ pid를 확인하여 문제없는 확인
 * 3. 댓글 삭제
 * ㄴ pid를 확인하여 문제없는 확인
 */


@RestController
@RequestMapping("/cmt")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private Environment env;



    // cmt 페이지 확인
    @GetMapping
    public ResponseEntity<String> mainPage() {
        return ResponseEntity.ok("Hello World");
    }
    // 댓글 생성 진행
    @PostMapping("create/{pid}")
    public ResponseEntity<String> createComment(
            @PathVariable Long pid,
            @RequestHeader("X-Auth-User") String email,
            @RequestBody CommentDto commentDto) {
        String release_ip = env.getProperty("app.user_ip");

        // User 쪽에 uid 요청(gid도 요청 가능)
        Mono<Long> webClient = webClientBuilder.baseUrl(release_ip).build()
                .post()
                .uri("/user/checkemail")
                .contentType(MediaType.APPLICATION_JSON) // json 형태로 전달
                .bodyValue("{\"email\":\"" + email + "\"}") // 요러케도 가능 .bodyValue(Map.of("email", email))
                .retrieve()
                .bodyToMono(Long.class);
        Long uid = webClient.block(); // 동기 처리

//        List<Long> gid = new ArrayList<>();
        // GID 확인
        Mono<List<Long>> webClient_2 = webClientBuilder.baseUrl(release_ip).build()
                .get()
                .uri(uriBuilder -> uriBuilder.path("/user/groupCheck")
                        .queryParam("email", email).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Object>>() {}) // List<Object>로 먼저 받아서
                .map(list -> list.stream()
                        .map(obj -> ((Number) obj).longValue()) // Object → Number → Long 변환
                        .toList());

        List<Long> gid = webClient_2.block();
        commentService.addComment(commentDto, pid,uid,gid);
        return ResponseEntity.ok("댓글 저장완료");
    }
    // 댓글 수정 진행
    @PutMapping("modify/{cid}")
    public ResponseEntity<String> modifyComment(
            @PathVariable Long cid,
            @RequestHeader("X-Auth-User") String email,
            @RequestBody CommentDto commentDto) {
        String release_ip = env.getProperty("app.user_ip");

        Mono<Long> webClient = webClientBuilder.baseUrl(release_ip).build()
                .post()
                .uri("/user/checkemail")
                .contentType(MediaType.APPLICATION_JSON) // json 형태로 전달
                .bodyValue("{\"email\":\"" + email + "\"}") // 요러케도 가능 .bodyValue(Map.of("email", email))
                .retrieve()
                .bodyToMono(Long.class);
        Long uid = webClient.block();

        commentService.modifyComment(commentDto,cid,uid);
        return ResponseEntity.ok("수정완료");
    }
    // 댓글 삭제 진행
    @DeleteMapping("delete/{cid}")
    public ResponseEntity<String> deleteComment(@PathVariable Long cid,
                                                @RequestHeader("X-Auth-User") String email) {
        String release_ip = env.getProperty("app.user_ip");
        Mono<Long> webClient = webClientBuilder.baseUrl(release_ip).build()
                .post()
                .uri("/user/checkemail")
                .contentType(MediaType.APPLICATION_JSON) // json 형태로 전달
                .bodyValue("{\"email\":\"" + email + "\"}") // 요러케도 가능 .bodyValue(Map.of("email", email))
                .retrieve()
                .bodyToMono(Long.class);
        Long uid = webClient.block();

        commentService.deleteComment(cid,uid);
        return ResponseEntity.ok("댓글 삭제완료");
    }

    // 댓글 조회 진행
    @GetMapping("/getAllComments")
    public ResponseEntity<List<CommentDto>> getAllComments() {
        List<CommentDto> comments = commentService.getAllComments();
        return ResponseEntity.ok(comments);
    }
    // 특정 pid로 댓글 조회 진행
    @GetMapping("/getPidComments/{pid}")
    public ResponseEntity<?> getPidComments(@PathVariable Long pid) {
        try {
            List<CommentDto> comments = commentService.getPidComments(pid);
            if (comments.isEmpty()) {
                return ResponseEntity.ok("해당 게시글에 댓글이 없습니다.");
            }
            return ResponseEntity.ok(comments);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 게시글이 존재하지 않습니다.");
        }
    }
}
