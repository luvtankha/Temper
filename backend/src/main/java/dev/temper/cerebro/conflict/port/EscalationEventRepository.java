package dev.temper.cerebro.conflict.port;
import java.util.*;
import dev.temper.cerebro.conflict.domain.EscalationEvent;
public interface EscalationEventRepository {List<EscalationEvent> findEscalationEvents(UUID conversationId); void saveEscalationEvent(EscalationEvent event);}
