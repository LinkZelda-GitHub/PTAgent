package com.ptagent.web.controller;

import com.ptagent.service.AuthService;
import com.ptagent.web.ApiController;
import com.ptagent.web.ApiRequest;
import com.ptagent.web.Response;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class AuthController implements ApiController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean handle(ApiRequest request, HttpExchange exchange) throws IOException {
        if (request.is("auth", "login") && request.method("POST")) {
            Response.json(exchange, 200, authService.login(request.body()));
            return true;
        }
        if (request.is("auth", "phone-code") && request.method("POST")) {
            Response.json(exchange, 200, authService.issuePhoneCode(
                    String.valueOf(request.body().getOrDefault("phoneNumber", ""))));
            return true;
        }
        if (request.is("auth", "me") && request.method("GET")) {
            Response.json(exchange, 200, authService.currentSession(request.bearerToken()));
            return true;
        }
        if (request.is("auth", "logout") && request.method("POST")) {
            authService.logout(request.bearerToken());
            Response.noContent(exchange);
            return true;
        }
        if (request.is("auth", "register-teacher") && request.method("POST")) {
            Response.json(exchange, 201, authService.registerTeacher(request.body()));
            return true;
        }
        return false;
    }
}
