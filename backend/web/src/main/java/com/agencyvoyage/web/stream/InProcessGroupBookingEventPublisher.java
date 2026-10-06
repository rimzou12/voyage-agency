package com.agencyvoyage.web.stream;

import com.agencyvoyage.application.port.out.AuditEntry;
import com.agencyvoyage.application.port.out.AuditEventType;
import com.agencyvoyage.application.port.out.AuditTrailRepository;
import com.agencyvoyage.application.port.out.GroupBookingEventPublisher;
import com.agencyvoyage.application.port.out.event.GroupBookingFinalizedEvent;
import com.agencyvoyage.application.port.out.event.ParticipantJoinedEvent;
import com.agencyvoyage.application.port.out.event.ParticipantLeftEvent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Handles group-booking domain events directly in-process instead of round-tripping
 * them through Kafka - the audit trail, the notification log and the SSE broadcast all
 * happen synchronously on the calling thread. Used in place of
 * {@link com.agencyvoyage.infrastructure.messaging.kafka.KafkaGroupBookingEventPublisher}
 * in environments with no Kafka broker (e.g. the Render deployment), trading the
 * decoupling a real broker gives for zero extra infrastructure.
 */
@Component
@ConditionalOnProperty(prefix = "agency-voyage.kafka", name = "enabled", havingValue = "false")
public class InProcessGroupBookingEventPublisher implements GroupBookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InProcessGroupBookingEventPublisher.class);

    private final AuditTrailRepository auditTrailRepository;
    private final GroupBookingEventBroadcaster broadcaster;

    public InProcessGroupBookingEventPublisher(
            AuditTrailRepository auditTrailRepository, GroupBookingEventBroadcaster broadcaster) {
        this.auditTrailRepository = Objects.requireNonNull(auditTrailRepository, "auditTrailRepository must not be null");
        this.broadcaster = Objects.requireNonNull(broadcaster, "broadcaster must not be null");
    }

    @Override
    public void publishParticipantJoined(ParticipantJoinedEvent event) {
        log.info(
                "Group booking {} now has {} participant(s) at {} per seat ({} just joined)",
                event.bookingId(),
                event.participantCount(),
                event.pricePerSeat(),
                event.customerName());
        auditTrailRepository.append(new AuditEntry(
                event.bookingId(),
                AuditEventType.PARTICIPANT_JOINED,
                event.participantId(),
                event.customerName(),
                event.participantCount(),
                event.pricePerSeat(),
                null,
                event.occurredAt()));
        broadcaster.broadcast(event.bookingId().toString(), "participant-joined");
    }

    @Override
    public void publishParticipantLeft(ParticipantLeftEvent event) {
        log.info(
                "Group booking {} now has {} participant(s) at {} per seat (someone just left)",
                event.bookingId(),
                event.participantCount(),
                event.pricePerSeat());
        auditTrailRepository.append(new AuditEntry(
                event.bookingId(),
                AuditEventType.PARTICIPANT_LEFT,
                event.participantId(),
                null,
                event.participantCount(),
                event.pricePerSeat(),
                null,
                event.occurredAt()));
        broadcaster.broadcast(event.bookingId().toString(), "participant-left");
    }

    @Override
    public void publishFinalized(GroupBookingFinalizedEvent event) {
        log.info(
                "Group booking {} finalized as {} with {} participant(s) at {} per seat",
                event.bookingId(),
                event.status(),
                event.participantCount(),
                event.finalPricePerSeat());
        auditTrailRepository.append(new AuditEntry(
                event.bookingId(),
                AuditEventType.FINALIZED,
                null,
                null,
                event.participantCount(),
                event.finalPricePerSeat(),
                event.status(),
                event.occurredAt()));
        broadcaster.broadcast(event.bookingId().toString(), "finalized");
    }
}
