package dev.temper.cerebro.chat.api;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import dev.temper.cerebro.chat.domain.Message;
import dev.temper.cerebro.conversation.service.ConversationService;

@RestController
@RequestMapping("/api/v1/conversations/{id}/messages")
public class MessageController {
    private final ConversationService service;
    public MessageController(ConversationService service) {this.service=service;}
    public record SendRequest(@NotBlank @Size(max=80) String speakerId,@NotBlank @Size(max=2000) String text) {}
    @GetMapping public List<Message> list(@PathVariable UUID id) {return service.messages(id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Message send(@PathVariable UUID id,@Valid @RequestBody SendRequest request) {return service.send(id,request.speakerId(),request.text().trim());}
}
