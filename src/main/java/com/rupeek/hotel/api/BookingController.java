package com.rupeek.hotel.api;

import com.rupeek.hotel.api.dto.BookingResponse;
import com.rupeek.hotel.api.dto.CancellationResponse;
import com.rupeek.hotel.api.dto.CreateBookingRequest;
import com.rupeek.hotel.api.dto.PayRequest;
import com.rupeek.hotel.api.dto.PaymentResponse;
import com.rupeek.hotel.booking.Booking;
import com.rupeek.hotel.booking.BookingService;
import com.rupeek.hotel.cancellation.CancellationService;
import com.rupeek.hotel.payment.Payment;
import com.rupeek.hotel.payment.PaymentCommand;
import com.rupeek.hotel.payment.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final CancellationService cancellationService;

    public BookingController(BookingService bookingService, PaymentService paymentService,
                             CancellationService cancellationService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.cancellationService = cancellationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@Valid @RequestBody CreateBookingRequest request) {
        return BookingResponse.from(bookingService.book(request.toCommand()));
    }

    @GetMapping("/{bookingId}")
    public BookingResponse get(@PathVariable String bookingId) {
        return BookingResponse.from(bookingService.get(bookingId));
    }

    @PostMapping("/{bookingId}/payments")
    public PaymentResponse pay(@PathVariable String bookingId,
                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                               @Valid @RequestBody PayRequest request) {
        Payment payment = paymentService.pay(
                new PaymentCommand(bookingId, request.method(), request.details(), idempotencyKey));
        Booking booking = bookingService.get(bookingId);
        return PaymentResponse.from(payment, booking);
    }

    @PostMapping("/{bookingId}/cancel")
    public CancellationResponse cancel(@PathVariable String bookingId) {
        return CancellationResponse.from(cancellationService.cancel(bookingId));
    }
}
