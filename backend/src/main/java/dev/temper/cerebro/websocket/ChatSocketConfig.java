package dev.temper.cerebro.websocket;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration @EnableWebSocketMessageBroker
public class ChatSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final RoomChannelGuard guard;private final String[] origins;
    public ChatSocketConfig(RoomChannelGuard guard,@Value("${temper.websocket.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String[] origins) {this.guard=guard;this.origins=origins;}
    @Bean public ThreadPoolTaskScheduler brokerHeartbeatScheduler() {var s=new ThreadPoolTaskScheduler();s.setPoolSize(1);s.setThreadNamePrefix("stomp-heartbeat-");return s;}
    public void registerStompEndpoints(StompEndpointRegistry registry) {registry.setPreserveReceiveOrder(true);registry.addEndpoint("/ws").setAllowedOrigins(origins);}
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");registry.enableSimpleBroker("/topic","/queue").setHeartbeatValue(new long[]{10000,10000}).setTaskScheduler(brokerHeartbeatScheduler());registry.setPreservePublishOrder(true);
    }
    public void configureClientInboundChannel(ChannelRegistration registration) {registration.interceptors(guard);}
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {registration.setMessageSizeLimit(16384).setSendTimeLimit(10000).setSendBufferSizeLimit(65536);}
}
