package com.agencyvoyage.infrastructure.messaging.kafka;

import com.agencyvoyage.application.port.out.AuditEntry;
import com.agencyvoyage.application.port.out.AuditEventType;
import com.agencyvoyage.application.port.out.AuditTrailRepository;
import com.agencyvoyage.domain.booking.GroupBookingId;
import com.agencyvoyage.domain.booking.GroupBookingStatus;
import com.agencyvoyage.domain.booking.ParticipantId;
import com.agencyvoyage.infrastructure.messaging.kafka.dto.GroupBookingFinalizedMessage;
import com.agencyvoyage.infrastructure.messaging.kafka.dto.ParticipantJoinedMessage;
import com.agencyvoyage.infrastructure.messaging.kafka.dto.ParticipantLeftMessage;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Builds a group booking's audit trail by persisting every event it ever raised. Its
 * own consumer group, independent of {@link NotificationKafkaListener} and the SSE
 * bridge, so a slow or failing consumer elsewhere never delays this from recording
 * history.
 *
 * <p>Only active when {@code agency-voyage.kafka.enabled} is true (the default); see
 * {@code InProcessGroupBookingEventPublisher} for the no-Kafka alternative.
 */
@Component
@ConditionalOnProperty(prefix = "agency-voyage.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditTrailKafkaListener {

    private final AuditTrailRepository auditTrailRepository;

    public AuditTrailKafkaListener(AuditTrailRepository auditTrailRepository) {
        this.auditTrailRepository = Objects.requireNonNull(auditTrailRepository, "auditTrailRepository must not be null");
    }

    @KafkaListener(topics = KafkaTopics.PARTICIPANT_JOINED, groupId = "agency-voyage-audit-trail")
    public void onParticipantJoined(ParticipantJoinedMessage message) {
        auditTrailRepository.append(new AuditEntry(
                GroupBookingId.of(message.bookingId()),
                AuditEventType.PARTICIPANT_JOINED,
                ParticipantId.of(message.participantId()),
                message.customerName(),
                message.participantCount(),
                message.pricePerSeat(),
                null,
                message.occurredAt()));
    }

    @KafkaListener(topics = KafkaTopics.PARTICIPANT_LEFT, groupId = "agency-voyage-audit-trail")
    public void onParticipantLeft(ParticipantLeftMessage message) {
        auditTrailRepository.append(new AuditEntry(
                GroupBookingId.of(message.bookingId()),
                AuditEventType.PARTICIPANT_LEFT,
                ParticipantId.of(message.participantId()),
                null,
                message.participantCount(),
                message.pricePerSeat(),
                null,
                message.occurredAt()));
    }

    @KafkaListener(topics = KafkaTopics.FINALIZED, groupId = "agency-voyage-audit-trail")
    public void onFinalized(GroupBookingFinalizedMessage message) {
        auditTrailRepository.append(new AuditEntry(
                GroupBookingId.of(message.bookingId()),
                AuditEventType.FINALIZED,
                null,
                null,
                message.participantCount(),
                message.finalPricePerSeat(),
                GroupBookingStatus.valueOf(message.status()),
                message.occurredAt()));
    }
}
