package com.dbtraining.reconx.sse;

import com.dbtraining.reconx.dto.TradeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fan-out hub for GET /api/v1/trades/stream — keeps live Dashboard feeds in sync.
 */
@Component
public class TradeStreamHub {

    private static final Logger log = LoggerFactory.getLogger(TradeStreamHub.class);
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void publish(TradeResponse trade) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("message").data(trade));
            } catch (IOException ex) {
                emitters.remove(emitter);
                try { emitter.complete(); } catch (Exception ignored) { /* already dead */ }
                log.debug("Dropped dead SSE client: {}", ex.toString());
            }
        }
    }
}
