package com.jobtracker.config;

import com.jobtracker.user.UserRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Refuse un jeton emis avant un changement de mot de passe (ou d'un compte supprime) : le claim "ver"
 * doit correspondre a la version actuelle du compte. C'est ce qui permet de deconnecter toutes les sessions.
 */
public class ValidateurVersionJeton implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error SESSION_EXPIREE =
            new OAuth2Error("invalid_token", "Session expiree : reconnecte-toi", null);

    private final UserRepository userRepository;

    public ValidateurVersionJeton(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object uid = jwt.getClaim("uid");
        Object ver = jwt.getClaim("ver");
        if (!(uid instanceof Number id)) {
            return OAuth2TokenValidatorResult.failure(SESSION_EXPIREE);
        }
        // Jetons emis avant l'ajout du claim : version 0
        int version = ver instanceof Number n ? n.intValue() : 0;
        boolean valide = userRepository.findById(id.longValue())
                .map(u -> u.getVersionJeton() == version)
                .orElse(false);
        return valide ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(SESSION_EXPIREE);
    }
}
