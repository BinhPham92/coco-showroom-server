package com.cocoshowroom.server.email;

import com.cocoshowroom.server.config.AppProperties;
import com.cocoshowroom.server.order.OrderResponse;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * Sends the order confirmation email after the order transaction commits.
 *
 * <p>The method is annotated {@link Async} so it runs on a separate virtual thread
 * and does not block the HTTP response. Any SMTP failure is caught and logged —
 * a failed confirmation email must never roll back or delay the order itself.
 *
 * <p>The {@link TransactionalEventListener} phase {@code AFTER_COMMIT} guarantees
 * that the email is only sent once the order row is safely written to the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEmailService {

    private final JavaMailSender   mailSender;
    private final TemplateEngine   templateEngine;
    private final AppProperties    appProperties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        OrderResponse order = event.order();
        try {
            // Select template and locale by the order's preferred language
            boolean isEn = "en".equals(order.locale());
            String templateName = isEn ? "emails/order-confirmation-en" : "emails/order-confirmation";

            Context ctx = new Context(isEn ? Locale.ENGLISH : Locale.forLanguageTag("vi-VN"));
            ctx.setVariable("order", order);
            ctx.setVariable("orderId", shortId(order.id()));

            String html = templateEngine.process(templateName, ctx);

            // Build MIME message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());

            AppProperties.Mail mailProps = appProperties.getMail();
            helper.setFrom(new InternetAddress(
                    mailProps.getFromAddress(),
                    mailProps.getFromName(),
                    StandardCharsets.UTF_8.name()
            ));
            helper.setTo(order.contactEmail());
            String subject = isEn
                    ? "Order Confirmation #" + shortId(order.id()) + " – Coco Showroom"
                    : "Xác nhận đơn hàng #" + shortId(order.id()) + " – Coco Showroom";
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Order confirmation sent order_id={} to={}", order.id(), order.contactEmail());

        } catch (Exception e) {
            // Failure must never surface to the caller — the order is already saved.
            log.error("Failed to send order confirmation email order_id={}", order.id(), e);
        }
    }

    /** Returns the first 8 hex chars of a {@link UUID} in upper-case — a human-readable order reference. */
    static String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
