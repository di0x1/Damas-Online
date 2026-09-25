package br.com.damasonline.webSoket;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final JogoWebSocketHandler jogoWebSocketHandler;

    public WebSocketConfig(JogoWebSocketHandler jogoWebSocketHandler) {
        this.jogoWebSocketHandler = jogoWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(jogoWebSocketHandler, "/ws")
                .setAllowedOriginPatterns("*");
    }
}
