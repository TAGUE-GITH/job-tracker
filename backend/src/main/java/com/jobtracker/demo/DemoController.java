package com.jobtracker.demo;

import com.jobtracker.user.AuthDtos.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    /** Public (sous /api/auth) : cree un compte de demonstration rempli et renvoie son jeton. */
    @PostMapping("/api/auth/demo")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse creer() {
        return demoService.creer();
    }
}
