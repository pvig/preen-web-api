package com.pvig.preenWebApi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class HomeController {

@GetMapping("/")
    public String hello() {
        return """
        L'API fonctionne ! <br/>
        /<br/>
        /error<br/>
        /v3/api-docs<br/>
        /v3/api-docs/**<br/>
        /swagger-ui<br/>
        /swagger-ui/** <br/>
        /swagger-ui.html    <br/>
        /webjars/**<br/>
        /api/auth/**<br/>
        """;
    }

}
