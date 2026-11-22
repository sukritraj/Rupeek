package com.rupeek.hotel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Walks the whole guest journey through the real HTTP layer. */
@SpringBootTest(properties = "hotel.seed.enabled=false")
@AutoConfigureMockMvc
class HotelApiIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    private JsonNode call(org.springframework.test.web.servlet.RequestBuilder req, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(req).andExpect(status().is(expectedStatus)).andReturn();
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? null : json.readTree(body);
    }

    @Test
    void onboardSearchBookPayCancel() throws Exception {
        LocalDate in = LocalDate.now().plusDays(30);
        LocalDate out = in.plusDays(2);

        JsonNode owner = call(post("/api/owners").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"IT Hotels","email":"it@example.com","properties":[
                  {"name":"IT Inn","city":"Jaipur","locality":"C-Scheme","starRating":4,"amenities":["wifi"],
                   "roomTypes":[{"name":"Std","maxGuests":2,"totalRooms":1,"basePrice":4000}]}]}
                """), 201);
        String propertyId = owner.get("properties").get(0).get("id").asText();
        String roomTypeId = owner.get("properties").get(0).get("roomTypes").get(0).get("id").asText();

        String searchUrl = "/api/properties/search?city=jaipur&guests=2&minStars=4&checkIn=" + in + "&checkOut=" + out;
        assertThat(call(get(searchUrl), 200)).hasSize(1);

        String bookBody = """
                {"propertyId":"%s","roomTypeId":"%s","checkIn":"%s","checkOut":"%s","guests":2,
                 "guestName":"Ria","guestEmail":"ria@example.com"}""".formatted(propertyId, roomTypeId, in, out);
        JsonNode booking = call(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(bookBody), 201);
        String bookingId = booking.get("id").asText();
        assertThat(booking.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(booking.get("totalPrice").decimalValue()).isEqualByComparingTo("8000");

        // the only room is held -> gone from search, and a second booking is a 409
        assertThat(call(get(searchUrl), 200)).isEmpty();
        call(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(bookBody), 409);

        JsonNode declined = call(post("/api/bookings/" + bookingId + "/payments")
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"UPI\",\"details\":{\"vpa\":\"xx@fail\"}}"), 200);
        assertThat(declined.get("paymentStatus").asText()).isEqualTo("FAILED");
        assertThat(declined.get("bookingStatus").asText()).isEqualTo("PENDING_PAYMENT");

        JsonNode paid = call(post("/api/bookings/" + bookingId + "/payments")
                .header("Idempotency-Key", "k-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"UPI\",\"details\":{\"vpa\":\"ria@okbank\"}}"), 200);
        assertThat(paid.get("bookingStatus").asText()).isEqualTo("CONFIRMED");
        JsonNode replay = call(post("/api/bookings/" + bookingId + "/payments")
                .header("Idempotency-Key", "k-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"UPI\",\"details\":{\"vpa\":\"ria@okbank\"}}"), 200);
        assertThat(replay.get("paymentId").asText()).isEqualTo(paid.get("paymentId").asText());

        JsonNode cancelled = call(post("/api/bookings/" + bookingId + "/cancel"), 200);
        assertThat(cancelled.get("booking").get("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.get("refund").decimalValue()).isEqualByComparingTo("8000");
        call(post("/api/bookings/" + bookingId + "/cancel"), 409);

        assertThat(call(get(searchUrl), 200)).hasSize(1);
    }

    @Test
    void badInputGetsAReadableError() throws Exception {
        JsonNode error = call(post("/api/owners").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"email\":\"nope\",\"properties\":[]}"), 400);
        assertThat(error.get("message").asText()).contains("email").contains("name");

        call(get("/api/bookings/does-not-exist"), 404);
        call(get("/api/no-such-route"), 404);
        call(get("/api/bookings/x/cancel"), 405);
        call(get("/api/properties/search?city=Goa&checkIn=2020-01-01&checkOut=2020-01-03"), 400);
        call(get("/api/properties/search?city=Goa&checkIn=garbage&checkOut=2030-01-03"), 400);
    }
}
