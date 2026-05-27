package com.cocoshowroom.server.email;

import com.cocoshowroom.server.config.AppProperties;
import com.cocoshowroom.server.order.OrderItemResponse;
import com.cocoshowroom.server.order.OrderResponse;
import com.cocoshowroom.server.order.OrderStatus;
import com.cocoshowroom.server.order.PaymentMethod;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrderEmailService}.
 *
 * <p>Calls {@code onOrderConfirmed()} directly — bypassing {@code @Async} and
 * {@code @TransactionalEventListener} so no Spring context is required.
 */
class OrderEmailServiceTest {

    private JavaMailSender     mailSender;
    private TemplateEngine     templateEngine;
    private OrderEmailService  service;

    @BeforeEach
    void setUp() {
        mailSender     = mock(JavaMailSender.class);
        templateEngine = mock(TemplateEngine.class);

        AppProperties props = new AppProperties();
        AppProperties.Mail mail = new AppProperties.Mail();
        mail.setFromAddress("no-reply@cocoshowroom.vn");
        mail.setFromName("Coco Showroom");
        props.setMail(mail);

        when(templateEngine.process(eq("emails/order-confirmation"), any(IContext.class)))
                .thenReturn("<html><body>Order VI</body></html>");
        when(templateEngine.process(eq("emails/order-confirmation-en"), any(IContext.class)))
                .thenReturn("<html><body>Order EN</body></html>");
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage((Session) null));

        service = new OrderEmailService(mailSender, templateEngine, props);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void onOrderConfirmed_invokesMailSenderSend() {
        service.onOrderConfirmed(new OrderConfirmedEvent(sampleOrder("customer@example.com")));

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void onOrderConfirmed_subjectContainsShortId() throws Exception {
        UUID orderId = UUID.fromString("abcdef12-1234-0000-0000-000000000000");
        OrderResponse order = orderWithId(orderId, "test@example.com", "vi");

        MimeMessage captured = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(captured);

        service.onOrderConfirmed(new OrderConfirmedEvent(order));

        assertThat(captured.getSubject()).contains("ABCDEF12");
    }

    @Test
    void onOrderConfirmed_subjectContainsStoreName() throws Exception {
        MimeMessage captured = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(captured);

        service.onOrderConfirmed(new OrderConfirmedEvent(sampleOrder("test@example.com")));

        assertThat(captured.getSubject()).contains("Coco Showroom");
    }

    // ── Template selection ────────────────────────────────────────────────────

    @Test
    void onOrderConfirmed_vietnameseLocale_usesVietnameseTemplate() {
        service.onOrderConfirmed(new OrderConfirmedEvent(sampleOrderWithLocale("vi")));

        verify(templateEngine).process(eq("emails/order-confirmation"), any(IContext.class));
        verify(templateEngine, never()).process(eq("emails/order-confirmation-en"), any(IContext.class));
    }

    @Test
    void onOrderConfirmed_englishLocale_usesEnglishTemplate() {
        service.onOrderConfirmed(new OrderConfirmedEvent(sampleOrderWithLocale("en")));

        verify(templateEngine).process(eq("emails/order-confirmation-en"), any(IContext.class));
        verify(templateEngine, never()).process(eq("emails/order-confirmation"), any(IContext.class));
    }

    // ── shortId helper ────────────────────────────────────────────────────────

    @Test
    void shortId_returnsFirst8CharsUpperCase() {
        UUID id = UUID.fromString("abcdef12-0000-0000-0000-000000000000");
        assertThat(OrderEmailService.shortId(id)).isEqualTo("ABCDEF12");
    }

    // ── Resilience ────────────────────────────────────────────────────────────

    @Test
    void onOrderConfirmed_templateEngineThrows_doesNotPropagateException() {
        when(templateEngine.process(anyString(), any(IContext.class))).thenThrow(new RuntimeException("template error"));

        // Must not throw — failure is logged, not re-thrown
        assertThatCode(() -> service.onOrderConfirmed(
                new OrderConfirmedEvent(sampleOrder("test@example.com"))
        )).doesNotThrowAnyException();
    }

    @Test
    void onOrderConfirmed_mailSenderThrows_doesNotPropagateException() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP error"));

        assertThatCode(() -> service.onOrderConfirmed(
                new OrderConfirmedEvent(sampleOrder("test@example.com"))
        )).doesNotThrowAnyException();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OrderResponse sampleOrder(String contactEmail) {
        return orderWithId(UUID.randomUUID(), contactEmail, "vi");
    }

    private OrderResponse sampleOrderWithLocale(String locale) {
        return orderWithId(UUID.randomUUID(), "test@example.com", locale);
    }

    private OrderResponse orderWithId(UUID id, String contactEmail, String locale) {
        return new OrderResponse(
                id,
                OrderStatus.PENDING,
                List.of(new OrderItemResponse(
                        "dongtrung-tuoi-premium",
                        "Đông trùng tươi cao cấp",
                        "Fresh Cordyceps Premium",
                        1,
                        2_500_000L
                )),
                "Nguyễn Văn A",
                "0901234567",
                "123 Đường ABC",
                "Quận 1",
                "TP.HCM",
                null,
                contactEmail,
                locale,
                PaymentMethod.COD,
                2_500_000L,
                Instant.now()
        );
    }
}
