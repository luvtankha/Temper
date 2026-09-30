package dev.temper.cerebro.conversation.port;
import java.util.*;
import dev.temper.cerebro.conversation.domain.Participant;
public interface ParticipantRepository {List<Participant> findParticipants(UUID conversationId); void saveParticipant(Participant participant);}
