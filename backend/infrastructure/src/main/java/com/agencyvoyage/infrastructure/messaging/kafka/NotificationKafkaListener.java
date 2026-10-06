package com.agencyvoyage.infrastructure.messaging.kafka;

import com.agencyvoyage.infrastructure.messaging.kafka.dto.GroupBookingFinalizedMessage;
import com.agencyvoyage.infrastructure.messaging.kafka.dto.ParticipantJoinedMessage;
import com.agencyvoyage.infrastructure.messaging.kafka.dto.ParticipantLeftMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Stands in for a downstream notification service in this single-deployable MVP: in a
 * real system this consumer would live in its own service, reading the same topics to
 * email/SMS customers. Kept here, logging only, to demonstrate the event flow end to
 * end without building a second deployable for this pass.
 *
 * <p>Only active when {@code agency-voyage.kafka.enabled} is true (the default); see
 * {@code InProcessGroupBookingEventPublisher} for the no-Kafka alternative.
 */
@Component
@ConditionalOnProperty(prefix = "agency-voyage.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaListener.class);

    @KafkaListener(topics = KafkaTopics.PARTICIPANT_JOINED, groupId = "agency-voyage-notifications")
    public void onParticipantJoined(ParticipantJoinedMessage message) {
        log.info(
                "Group booking {} now has {} participant(s) at {} per seat ({} just joined)",
                message.bookingId(),
                message.participantCount(),
                message.pricePerSeat(),
                message.customerName());
    }

    @KafkaListener(topics = KafkaTopics.PARTICIPANT_LEFT, groupId = "agency-voyage-notifications")
    public void onParticipantLeft(ParticipantLeftMessage message) {
        log.info(
                "Group booking {} now has {} participant(s) at {} per seat (someone just left)",
                message.bookingId(),
                message.participantCount(),
                message.pricePerSeat());
    }

    @KafkaListener(topics = KafkaTopics.FINALIZED, groupId = "agency-voyage-notifications")
    public void onFinalized(GroupBookingFinalizedMessage message) {
        log.info(
                "Group booking {} finalized as {} with {} participant(s) at {} per seat",
                message.bookingId(),
                message.status(),
                message.participantCount(),
                message.finalPricePerSeat());
    }
}
