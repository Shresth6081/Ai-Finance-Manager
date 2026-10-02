package com.financemanager.controller;

import com.financemanager.dto.AgentActionProposal;
import com.financemanager.dto.AgentAlertDTO;
import com.financemanager.dto.AgentChatRequest;
import com.financemanager.dto.AgentStreamEvent;
import com.financemanager.model.AgentAlert;
import com.financemanager.model.User;
import com.financemanager.repository.AgentAlertRepository;
import com.financemanager.repository.UserRepository;
import com.financemanager.service.agent.AgentOrchestratorService;
import com.financemanager.service.agent.FinancialToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentOrchestratorService orchestratorService;
    private final FinancialToolService toolService;
    private final AgentAlertRepository alertRepository;
    private final UserRepository userRepository;

    /**
     * Real-time SSE Stream Endpoint for ReAct Agentic Reasoning.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAgentChat(
            @RequestParam("prompt") String prompt,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);
        SseEmitter emitter = new SseEmitter(180_000L); // 3-minute timeout

        emitter.onCompletion(() -> log.info("SSE Stream completed for user {}", user.getUsername()));
        emitter.onTimeout(() -> {
            log.warn("SSE Stream timed out for user {}", user.getUsername());
            emitter.complete();
        });
        emitter.onError((e) -> log.error("SSE Stream error for user {}", user.getUsername(), e));

        CompletableFuture.runAsync(() -> {
            try {
                orchestratorService.executeReActLoop(user, prompt, Collections.emptyList(), event -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(event.getType())
                                .data(event));
                    } catch (IOException e) {
                        log.error("Failed to send SSE event to user {}", user.getUsername(), e);
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                log.error("Agent execution failed for user {}", user.getUsername(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(AgentStreamEvent.error(e.getMessage())));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * Standard Non-Streaming JSON Chat Endpoint.
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chatWithAgent(
            @RequestBody AgentChatRequest request,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);
        StringBuilder fullResponse = new StringBuilder();

        orchestratorService.executeReActLoop(user, request.getPrompt(), request.getHistory(), event -> {
            if ("done".equals(event.getType())) {
                fullResponse.append(event.getMessage());
            }
        });

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "response", fullResponse.toString()
        ));
    }

    /**
     * Execute an approved Action Proposal.
     */
    @PostMapping("/execute-action")
    public ResponseEntity<Map<String, Object>> executeAction(
            @RequestBody AgentActionProposal proposal,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);
        try {
            Map<String, Object> result = toolService.executeActionProposal(user.getId(), proposal);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to execute action proposal", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get active proactive alerts generated by the hunter agent.
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<AgentAlertDTO>> getActiveAlerts(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        List<AgentAlert> alerts = alertRepository.findActiveAlertsByUserId(user.getId());

        List<AgentAlertDTO> dtos = alerts.stream()
                .map(a -> AgentAlertDTO.builder()
                        .id(a.getId())
                        .alertType(a.getAlertType())
                        .title(a.getTitle())
                        .message(a.getMessage())
                        .suggestedAction(a.getSuggestedAction())
                        .isDismissed(a.isDismissed())
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Dismiss a proactive alert.
     */
    @PutMapping("/alerts/{id}/dismiss")
    public ResponseEntity<Void> dismissAlert(
            @PathVariable("id") Long id,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);
        AgentAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));

        if (!alert.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        alert.setDismissed(true);
        alertRepository.save(alert);
        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            // Fallback from SecurityContextHolder if parameter injection is null
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetails ud) {
                return userRepository.findByUsername(ud.getUsername())
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + ud.getUsername()));
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userDetails.getUsername()));
    }
}
