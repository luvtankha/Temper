package dev.temper.cerebro.emotion.port;
import java.util.*;
import dev.temper.cerebro.emotion.domain.EmotionSnapshot;
public interface EmotionSnapshotRepository {List<EmotionSnapshot> findEmotionSnapshots(UUID conversationId); void saveEmotionSnapshot(EmotionSnapshot snapshot);}
