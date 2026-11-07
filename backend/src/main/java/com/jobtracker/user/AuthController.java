package com.jobtracker.user;

import com.jobtracker.user.AuthDtos.AuthResponse;
import com.jobtracker.user.AuthDtos.LoginRequest;
import com.jobtracker.user.AuthDtos.OubliRequest;
import com.jobtracker.user.AuthDtos.RegisterRequest;
import com.jobtracker.user.AuthDtos.ReinitialisationRequest;
import com.jobtracker.user.TentativesConnexion.TropDeTentativesException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ReinitialisationService reinitialisationService;

    public AuthController(AuthService authService, ReinitialisationService reinitialisationService) {
        this.authService = authService;
        this.reinitialisationService = reinitialisationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /** Toujours 204, que le compte existe ou non : on ne revele pas quels emails sont inscrits. */
    @PostMapping("/mot-de-passe-oublie")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void motDePasseOublie(@Valid @RequestBody OubliRequest req) {
        reinitialisationService.demander(req.email());
    }

    @PostMapping("/reinitialiser")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reinitialiser(@Valid @RequestBody ReinitialisationRequest req) {
        reinitialisationService.reinitialiser(req.jeton(), req.nouveau());
    }

    @ExceptionHandler(TropDeTentativesException.class)
    public ProblemDetail tropDeTentatives(TropDeTentativesException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail badCredentials(BadCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }
}
