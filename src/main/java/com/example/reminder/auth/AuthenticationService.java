package com.example.reminder.auth;

import com.example.reminder.email.EmailService;
import com.example.reminder.exception.UserAlreadyExistsException;
import com.example.reminder.repository.RoleRepository;
import com.example.reminder.repository.TokenRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.request.RegistrationRequest;
import com.example.reminder.security.JwtService;
import com.example.reminder.user.Token;
import com.example.reminder.user.User;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static com.example.reminder.email.EmailTemplateName.ACTIVATE_ACCOUNT;

@Service
public class AuthenticationService {
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;
    @Value("${activation.url}")
    private String activationUrl;

    public void register(RegistrationRequest request) throws MessagingException {
            var userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));
            if (userRepository.existsByEmail(request.getEmail()))
                throw new UserAlreadyExistsException("Email already exists");

            var user = User.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .accountLocked(false)
                    .enabled(false)
                    .roles(List.of(userRole))
                    .build();
            userRepository.save(user);
            sendVerificationEmail(user);
    }

    private void sendVerificationEmail(User user) throws MessagingException {
        var token = generateAndSaveToken(user);
        emailService.sendEmail(
                user.getEmail(),
                user.getUsername(),
                ACTIVATE_ACCOUNT,
                activationUrl,
                token,
                "Account Activation"
        );
    }

    private String generateAndSaveToken(User user) {
        String generatedToken = generateToken(6);
        var token = new Token();
        token.setToken(generatedToken);
        token.setUser(user);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(token);
        return generatedToken;
    }

    private String generateToken(int length) {
        String characters = "0123456789";
        StringBuilder token = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            var randomIndex = random.nextInt(characters.length());
            token.append(characters.charAt(randomIndex));
        }
        return token.toString();
    }

    public AuthenticationResponse login(@Valid AuthenticationRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var claims = new HashMap<String, Object>();
        var user = (User) auth.getPrincipal();
        claims.put("email", user.getEmail());
        var jwtToken = jwtService.generateToken(claims, (User) auth.getPrincipal());
        return AuthenticationResponse
                .builder()
                .token(jwtToken)
                .build();

    }

    public void activateAccount(String token) throws MessagingException {
        var savedToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("Invalid token"));

        if (savedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            sendVerificationEmail(savedToken.getUser());
            throw new IllegalStateException("Expired token");
        }

        var user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
    }
}
