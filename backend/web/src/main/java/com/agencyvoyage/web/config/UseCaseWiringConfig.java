package com.agencyvoyage.web.config;

import com.agencyvoyage.application.port.in.AddHotelReviewUseCase;
import com.agencyvoyage.application.port.in.AddHotelUseCase;
import com.agencyvoyage.application.port.in.ConfirmHotelReservationUseCase;
import com.agencyvoyage.application.port.in.CreateGroupBookingUseCase;
import com.agencyvoyage.application.port.in.CreateTripUseCase;
import com.agencyvoyage.application.port.in.DeleteHotelReviewUseCase;
import com.agencyvoyage.application.port.in.DeleteHotelUseCase;
import com.agencyvoyage.application.port.in.DeleteTripUseCase;
import com.agencyvoyage.application.port.in.FinalizeGroupBookingUseCase;
import com.agencyvoyage.application.port.in.GetAuditTrailUseCase;
import com.agencyvoyage.application.port.in.GetConversationUseCase;
import com.agencyvoyage.application.port.in.GetGroupBookingUseCase;
import com.agencyvoyage.application.port.in.GetTripUseCase;
import com.agencyvoyage.application.port.in.JoinGroupBookingUseCase;
import com.agencyvoyage.application.port.in.JoinWaitlistUseCase;
import com.agencyvoyage.application.port.in.LeaveGroupBookingUseCase;
import com.agencyvoyage.application.port.in.LeaveWaitlistUseCase;
import com.agencyvoyage.application.port.in.ListContactMessagesUseCase;
import com.agencyvoyage.application.port.in.ListHotelReviewsUseCase;
import com.agencyvoyage.application.port.in.ListHotelsForTripUseCase;
import com.agencyvoyage.application.port.in.ListTripsUseCase;
import com.agencyvoyage.application.port.in.LoginUseCase;
import com.agencyvoyage.application.port.in.RegisterUserUseCase;
import com.agencyvoyage.application.port.in.ReplyToConversationUseCase;
import com.agencyvoyage.application.port.in.RequestHotelReservationUseCase;
import com.agencyvoyage.application.port.in.SendContactMessageUseCase;
import com.agencyvoyage.application.port.in.UpdateHotelReviewUseCase;
import com.agencyvoyage.application.port.in.UpdateHotelUseCase;
import com.agencyvoyage.application.port.in.UpdateTripUseCase;
import com.agencyvoyage.application.port.out.AuditTrailRepository;
import com.agencyvoyage.application.port.out.ContactMessageRepository;
import com.agencyvoyage.application.port.out.EmailSender;
import com.agencyvoyage.application.port.out.GroupBookingEventPublisher;
import com.agencyvoyage.application.port.out.GroupBookingRepository;
import com.agencyvoyage.application.port.out.HotelRepository;
import com.agencyvoyage.application.port.out.HotelReviewRepository;
import com.agencyvoyage.application.port.out.PasswordHasher;
import com.agencyvoyage.application.port.out.TokenIssuer;
import com.agencyvoyage.application.port.out.TripRepository;
import com.agencyvoyage.application.port.out.UserRepository;
import com.agencyvoyage.application.service.AddHotelReviewService;
import com.agencyvoyage.application.service.AddHotelService;
import com.agencyvoyage.application.service.ConfirmHotelReservationService;
import com.agencyvoyage.application.service.CreateGroupBookingService;
import com.agencyvoyage.application.service.CreateTripService;
import com.agencyvoyage.application.service.DeleteHotelReviewService;
import com.agencyvoyage.application.service.DeleteHotelService;
import com.agencyvoyage.application.service.DeleteTripService;
import com.agencyvoyage.application.service.FinalizeGroupBookingService;
import com.agencyvoyage.application.service.GetAuditTrailService;
import com.agencyvoyage.application.service.GetConversationService;
import com.agencyvoyage.application.service.GetGroupBookingService;
import com.agencyvoyage.application.service.JoinGroupBookingService;
import com.agencyvoyage.application.service.JoinWaitlistService;
import com.agencyvoyage.application.service.LeaveGroupBookingService;
import com.agencyvoyage.application.service.LeaveWaitlistService;
import com.agencyvoyage.application.service.ListContactMessagesService;
import com.agencyvoyage.application.service.ListHotelReviewsService;
import com.agencyvoyage.application.service.ListHotelsForTripService;
import com.agencyvoyage.application.service.LoginService;
import com.agencyvoyage.application.service.RegisterUserService;
import com.agencyvoyage.application.service.ReplyToConversationService;
import com.agencyvoyage.application.service.RequestHotelReservationService;
import com.agencyvoyage.application.service.SendContactMessageService;
import com.agencyvoyage.application.service.TripQueryService;
import com.agencyvoyage.application.service.UpdateHotelReviewService;
import com.agencyvoyage.application.service.UpdateHotelService;
import com.agencyvoyage.application.service.UpdateTripService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free application layer by hand: use-case services are plain
 * classes with no Spring annotations of their own, constructed here with the
 * infrastructure adapters (themselves regular {@code @Component} beans) injected as
 * their out-ports. Keeps the application module free of any framework dependency.
 */
@Configuration
public class UseCaseWiringConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public TripQueryService tripQueryService(TripRepository tripRepository) {
        return new TripQueryService(tripRepository);
    }

    @Bean
    public ListTripsUseCase listTripsUseCase(TripQueryService tripQueryService) {
        return tripQueryService;
    }

    @Bean
    public GetTripUseCase getTripUseCase(TripQueryService tripQueryService) {
        return tripQueryService;
    }

    @Bean
    public CreateGroupBookingUseCase createGroupBookingUseCase(
            TripRepository tripRepository,
            GroupBookingRepository groupBookingRepository,
            GroupBookingEventPublisher eventPublisher,
            Clock clock) {
        return new CreateGroupBookingService(tripRepository, groupBookingRepository, eventPublisher, clock);
    }

    @Bean
    public JoinGroupBookingUseCase joinGroupBookingUseCase(
            GroupBookingRepository groupBookingRepository,
            GroupBookingEventPublisher eventPublisher,
            EmailSender emailSender,
            Clock clock) {
        return new JoinGroupBookingService(groupBookingRepository, eventPublisher, emailSender, clock);
    }

    @Bean
    public LeaveGroupBookingUseCase leaveGroupBookingUseCase(
            GroupBookingRepository groupBookingRepository, GroupBookingEventPublisher eventPublisher, Clock clock) {
        return new LeaveGroupBookingService(groupBookingRepository, eventPublisher, clock);
    }

    @Bean
    public GetGroupBookingUseCase getGroupBookingUseCase(GroupBookingRepository groupBookingRepository) {
        return new GetGroupBookingService(groupBookingRepository);
    }

    @Bean
    public GetAuditTrailUseCase getAuditTrailUseCase(AuditTrailRepository auditTrailRepository) {
        return new GetAuditTrailService(auditTrailRepository);
    }

    @Bean
    public JoinWaitlistUseCase joinWaitlistUseCase(GroupBookingRepository groupBookingRepository, Clock clock) {
        return new JoinWaitlistService(groupBookingRepository, clock);
    }

    @Bean
    public LeaveWaitlistUseCase leaveWaitlistUseCase(GroupBookingRepository groupBookingRepository) {
        return new LeaveWaitlistService(groupBookingRepository);
    }

    @Bean
    public FinalizeGroupBookingUseCase finalizeGroupBookingUseCase(
            GroupBookingRepository groupBookingRepository, GroupBookingEventPublisher eventPublisher, Clock clock) {
        return new FinalizeGroupBookingService(groupBookingRepository, eventPublisher, clock);
    }

    @Bean
    public RegisterUserUseCase registerUserUseCase(
            UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        return new RegisterUserService(userRepository, passwordHasher, tokenIssuer);
    }

    @Bean
    public LoginUseCase loginUseCase(
            UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        return new LoginService(userRepository, passwordHasher, tokenIssuer);
    }

    @Bean
    public RequestHotelReservationUseCase requestHotelReservationUseCase(
            GroupBookingRepository groupBookingRepository,
            UserRepository userRepository,
            EmailSender emailSender,
            Clock clock) {
        return new RequestHotelReservationService(groupBookingRepository, userRepository, emailSender, clock);
    }

    @Bean
    public ConfirmHotelReservationUseCase confirmHotelReservationUseCase(
            GroupBookingRepository groupBookingRepository,
            UserRepository userRepository,
            EmailSender emailSender,
            Clock clock) {
        return new ConfirmHotelReservationService(groupBookingRepository, userRepository, emailSender, clock);
    }

    @Bean
    public SendContactMessageUseCase sendContactMessageUseCase(
            ContactMessageRepository contactMessageRepository, Clock clock) {
        return new SendContactMessageService(contactMessageRepository, clock);
    }

    @Bean
    public ListContactMessagesUseCase listContactMessagesUseCase(ContactMessageRepository contactMessageRepository) {
        return new ListContactMessagesService(contactMessageRepository);
    }

    @Bean
    public ReplyToConversationUseCase replyToConversationUseCase(
            ContactMessageRepository contactMessageRepository, Clock clock) {
        return new ReplyToConversationService(contactMessageRepository, clock);
    }

    @Bean
    public GetConversationUseCase getConversationUseCase(ContactMessageRepository contactMessageRepository) {
        return new GetConversationService(contactMessageRepository);
    }

    @Bean
    public AddHotelUseCase addHotelUseCase(HotelRepository hotelRepository) {
        return new AddHotelService(hotelRepository);
    }

    @Bean
    public ListHotelsForTripUseCase listHotelsForTripUseCase(HotelRepository hotelRepository) {
        return new ListHotelsForTripService(hotelRepository);
    }

    @Bean
    public UpdateHotelUseCase updateHotelUseCase(HotelRepository hotelRepository) {
        return new UpdateHotelService(hotelRepository);
    }

    @Bean
    public CreateTripUseCase createTripUseCase(TripRepository tripRepository) {
        return new CreateTripService(tripRepository);
    }

    @Bean
    public UpdateTripUseCase updateTripUseCase(TripRepository tripRepository) {
        return new UpdateTripService(tripRepository);
    }

    @Bean
    public DeleteTripUseCase deleteTripUseCase(TripRepository tripRepository) {
        return new DeleteTripService(tripRepository);
    }

    @Bean
    public DeleteHotelUseCase deleteHotelUseCase(HotelRepository hotelRepository) {
        return new DeleteHotelService(hotelRepository);
    }

    @Bean
    public AddHotelReviewUseCase addHotelReviewUseCase(HotelReviewRepository hotelReviewRepository, Clock clock) {
        return new AddHotelReviewService(hotelReviewRepository, clock);
    }

    @Bean
    public UpdateHotelReviewUseCase updateHotelReviewUseCase(
            HotelReviewRepository hotelReviewRepository, Clock clock) {
        return new UpdateHotelReviewService(hotelReviewRepository, clock);
    }

    @Bean
    public DeleteHotelReviewUseCase deleteHotelReviewUseCase(HotelReviewRepository hotelReviewRepository) {
        return new DeleteHotelReviewService(hotelReviewRepository);
    }

    @Bean
    public ListHotelReviewsUseCase listHotelReviewsUseCase(HotelReviewRepository hotelReviewRepository) {
        return new ListHotelReviewsService(hotelReviewRepository);
    }
}
