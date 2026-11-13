package com.jobtracker.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import com.jobtracker.user.UserRepository;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

    private final SecretKey secretKey;
    private final List<String> corsOrigins;

    public SecurityConfig(@Value("${app.jwt.secret}") String secret,
            @Value("${app.cors.origins}") List<String> corsOrigins) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret doit faire au moins 32 caracteres");
        }
        this.secretKey = new SecretKeySpec(bytes, "HmacSHA256");
        this.corsOrigins = corsOrigins.stream()
                .map(SecurityConfig::origine)
                .filter(o -> !o.isEmpty())
                .toList();
        LOG.info("Origines CORS autorisees : {}", this.corsOrigins.isEmpty() ? "aucune" : this.corsOrigins);
    }

    /**
     * Ramene une valeur de CORS_ORIGINS a une origine "https://hote[:port]" :
     * Render fournit un nom d'hote sans "https://", et une URL copiee depuis le navigateur
     * peut contenir un chemin (/login) ou un "/" final, qui ne correspondent jamais a l'en-tete Origin.
     * Les jokers sont acceptes, par exemple https://*.vercel.app.
     */
    public static String origine(String valeur) {
        String o = valeur.trim();
        if (o.isEmpty()) {
            return o;
        }
        if (!o.contains("://")) {
            o = "https://" + o;
        }
        int debutHote = o.indexOf("://") + 3;
        int finHote = o.indexOf('/', debutHote);
        return finHote < 0 ? o : o.substring(0, finHote);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/contact").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/public/orvyna").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
                .build();
    }

    /** Utile seulement quand le frontend est servi depuis un autre domaine (ex. Render). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(corsOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Sans origine configuree (dev avec le proxy Vite, Docker avec nginx), pas de regle CORS :
        // sinon Spring refuserait (403) les requetes qui portent un en-tete Origin.
        if (!corsOrigins.isEmpty()) {
            source.registerCorsConfiguration("/api/**", config);
        }
        return source;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(UserRepository userRepository) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        // Expiration (validateurs par defaut) + version du jeton (sessions revoquees)
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), new ValidateurVersionJeton(userRepository)));
        return decoder;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
