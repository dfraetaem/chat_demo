package src.softproject.chatdemo.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import src.softproject.chatdemo.entity.ChatMessage;
import src.softproject.chatdemo.service.ChatService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/chat")
public class chatController {
    private final ChatClient chatClient;
    private final ChatService chatService; // 注入 ChatService
    private static final String DEFAULT_PROMPT = "你是一个聊天助手，请根据用户问题，进行简短的回答！";

    public chatController(ChatClient.Builder chatClientBuilder,ChatService chatService) {
        this.chatService = chatService;
        this.chatClient = chatClientBuilder
                .defaultSystem(DEFAULT_PROMPT)  // 设置默认系统提示
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(new InMemoryChatMemory()) // 上下文记忆
                )
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()  // 日志记录
                )
                .defaultOptions(  // 模型参数
                        OpenAiChatOptions.builder().topP(0.7).build()
                )
                .build();
    }

    @GetMapping("/simple0")
    public String simpleChat(@RequestParam String message) {
        return chatClient.prompt(message).call().content();
    }

    @GetMapping("/simple1")
    public String simpleChat(@RequestParam String message, @RequestParam String chatId) {
        return chatClient.prompt(message)
                .advisors(a -> a
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
                ).call().content();
    }

    @GetMapping("/stream")
    public Flux<String> streamChat(@RequestParam String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return chatClient.prompt(message).stream().content();
    }

    @GetMapping(value = "/stream/response", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content()
                .map(content -> ServerSentEvent.<String>builder().data(content).build());
    }


    /**
     * 流式响应并保存到数据库
     * @param message 用户消息
     * @param userId 用户ID（从请求参数或 Token 中获取）
     */
    @GetMapping(value = "/stream/sql", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(
            @RequestParam String message,
            @RequestParam String userId
    ) {
        // 1. 初始化会话并保存用户消息
        String chatId = chatService.initOrUpdateSession(userId, 3600);
        chatService.saveMessages(List.of(
                new ChatMessage(chatId, "user", message, LocalDateTime.now())
        ));
        // 2. 获取历史上下文
        List<Message> history = chatService.getRecentMessages(chatId, 10);
        // 3. 调用 OpenAI 获取流式响应
        Flux<String> contentFlux = chatClient.prompt()
                .messages(history)
                .user(message)
                .stream()
                .content();
        // 4. 共享流以便多次订阅
        Flux<String> sharedFlux = contentFlux.share();
        // 5. 异步保存完整回复到数据库（流结束后触发）
        sharedFlux.collectList()
                .flatMap(contentList -> {
                    String fullContent = String.join("", contentList);
                    return chatService.saveAssistantMessage(chatId, fullContent)
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .subscribe();
        // 6. 实时返回流式响应
        return sharedFlux.map(content -> ServerSentEvent.builder(content).build());
    }
}
