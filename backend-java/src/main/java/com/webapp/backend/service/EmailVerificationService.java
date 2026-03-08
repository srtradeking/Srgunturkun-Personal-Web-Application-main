package com.webapp.backend.service;

import com.webapp.backend.model.EmailVerificationToken;
import com.webapp.backend.model.User;
import com.webapp.backend.model.UserProfile;
import com.webapp.backend.repository.EmailVerificationTokenRepository;
import com.webapp.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    @Value("${EMAIL_FROM_ADDRESS}")
    private String fromAddress;

    @Value("${FRONTEND_PUBLIC_URL}")
    private String frontendPublicUrl;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(UserProfile profile) {
        try {
            EmailVerificationToken token = createToken(profile);
            String verifyUrl = frontendPublicUrl + "/verify-email?token=" + token.getToken();

            String subject = "Verify your email address";
            String body = "Hello " + profile.getUsername() + ",\n\n" +
                    "Please verify your email address by clicking the link below:\n" +
                    verifyUrl + "\n\n" +
                    "If you did not create an account, you can safely ignore this email.\n\n" +
                    "--\n" +
                    "Best Regards,\n" +
                    "SR Gunturkun\n" +
                    "noreply.yourdomain@gmail.com";

            sendEmail(profile.getEmail(), subject, body);
            log.info("Verification email sent to {}", profile.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to {}", profile.getEmail(), e);
        }
    }

    public boolean verifyToken(String tokenValue) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(tokenValue);
        if (tokenOpt.isEmpty()) {
            log.warn("Email verification failed: token not found");
            return false;
        }

        EmailVerificationToken token = tokenOpt.get();

        if (Boolean.TRUE.equals(token.getUsed())) {
            log.warn("Email verification failed: token already used");
            return false;
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Email verification failed: token expired");
            return false;
        }

        UserProfile profile = token.getUserProfile();
        if (profile == null) {
            log.warn("Email verification failed: token has no associated profile");
            return false;
        }

        profile.setIsVerified(true);
        token.setUsed(true);

        tokenRepository.save(token);
        log.info("Email verified for userProfileId={}", profile.getId());
        return true;
    }

    public void resendVerification(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("Resend verification requested for non-existent email={}", email);
            return; // do not leak existence
        }

        User user = userOpt.get();
        UserProfile profile = user.getUserProfile();
        if (profile == null) {
            log.warn("Resend verification: user has no profile for email={}", email);
            return;
        }

        if (Boolean.TRUE.equals(profile.getIsVerified())) {
            log.info("Resend verification requested for already verified email={}", email);
            return;
        }

        sendVerificationEmail(profile);
    }

    private EmailVerificationToken createToken(UserProfile profile) {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .userProfile(profile)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        return tokenRepository.save(token);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom(fromAddress);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error sending verification email via SMTP", e);
        }
    }
}
