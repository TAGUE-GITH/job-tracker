package com.jobtracker.user;

import com.jobtracker.user.AuthDtos.AuthResponse;
import com.jobtracker.user.AuthDtos.LoginRequest;
import com.jobtracker.user.AuthDtos.RegisterRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    /** Les comptes de demonstration ont une adresse dans ce domaine fictif. */
    public static final String DOMAINE_DEMO = "@demo.taguora";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long expirationHours;
    private final TentativesConnexion tentatives;
    private final Administrateurs administrateurs;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
            @Value("${app.jwt.expiration-hours}") long expirationHours, TentativesConnexion tentatives,
            Administrateurs administrateurs) {
        this.administrateurs = administrateurs;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.expirationHours = expirationHours;
        this.tentatives = tentatives;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Un compte existe deja avec cet email");
        }
        User user = new User(req.nom().trim(), email, passwordEncoder.encode(req.password()));
        user.accepterConditions();
        return toResponse(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        tentatives.verifier(email);
        User user = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .orElse(null);
        if (user == null) {
            tentatives.echec(email);
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }
        tentatives.succes(email);
        return toResponse(user);
    }

    /** Jeton de connexion pour un utilisateur (inscription, connexion, compte de demonstration). */
    public AuthResponse toResponse(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("ver", user.getVersionJeton())
                .issuedAt(now)
                .expiresAt(now.plus(expirationHours, ChronoUnit.HOURS))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(token, user.getId(), user.getNom(), user.getEmail(),
                user.getEmail().endsWith(DOMAINE_DEMO), administrateurs.estAdmin(user));
    }
}
