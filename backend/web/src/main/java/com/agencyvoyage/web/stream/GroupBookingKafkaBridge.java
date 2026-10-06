package com.agencyvoyage.web.stream;

import com.agencyvoyage.infrastructure.messaging.kafka.KafkaTopics;
import com.agencyvoyage.infrastructure.messaging.kafka.dto.GroupBookingFinalizedMessage;
import com.agencyvoyage.infrastructure.messaging.kafka.dto.ParticipantJoinedMessage;
import com.agencyvoyage.infrastructure.messaging.kafka.dto.ParticipantLeftMessage;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Bridges the Kafka topics to {@link GroupBookingEventBroadcaster}, which fans them out
 * to any browser tab watching that booking over SSE. A separate consumer group from
 * {@code NotificationKafkaListener} - each subscriber (this bridge, the notification
 * log) gets every message independently, which is exactly what Kafka consumer groups
 * are for.
 *
 * <p>Only active when {@code agency-voyage.kafka.enabled} is true (the default); see
 * {@link InProcessGroupBookingEventPublisher} for the no-Kafka alternative, which
 * broadcasts directly instead of via this bridge.
 */
@Component
@ConditionalOnProperty(prefix = "agency-voyage.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GroupBookingKafkaBridge {

    private final GroupBookingEventBroadcaster broadcaster;

    public GroupBookingKafkaBridge(GroupBookingEventBroadcaster broadcaster) {
        this.broadcaster = Objects.requireNonNull(broadcaster, "broadcaster must not be null");
    }

    @KafkaListener(topics = KafkaTopics.PARTICIPANT_JOINED, groupId = "agency-voyage-sse-bridge")
    public void onParticipantJoined(ParticipantJoinedMessage message) {
        broadcaster.broadcast(message.bookingId(), "participant-joined");
    }

    @KafkaListener(topics = KafkaTopics.PARTICIPANT_LEFT, groupId = "agency-voyage-sse-bridge")
    public void onParticipantLeft(ParticipantLeftMessage message) {
        broadcaster.broadcast(message.bookingId(), "participant-left");
    }

    @KafkaListener(topics = KafkaTopics.FINALIZED, groupId = "agency-voyage-sse-bridge")
    public void onFinalized(GroupBookingFinalizedMessage message) {
        broadcaster.broadcast(message.bookingId(), "finalized");
    }
}
