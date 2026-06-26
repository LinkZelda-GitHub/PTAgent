package com.ptagent.web.controller;

import com.ptagent.common.Json;
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
            Response.json(exchange, 200, authService.login(Json.str(request.body(), "username"),
                    Json.str(request.body(), "password")));
            return true;
        }
        if (request.is("auth", "register-teacher") && request.method("POST")) {
            Response.json(exchange, 201, authService.registerTeacher(request.body()));
            return true;
        }
        return false;
    }
}
