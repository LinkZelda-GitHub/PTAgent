package com.ptagent.web.controller;

import com.ptagent.service.NotificationService;
import com.ptagent.web.ApiController;
import com.ptagent.web.ApiRequest;
import com.ptagent.web.Response;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class NotificationController implements ApiController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public boolean handle(ApiRequest request, HttpExchange exchange) throws IOException {
        if (request.is("notifications") && request.method("GET")) {
            Response.json(exchange, 200, notificationService.list(request.actorId()));
            return true;
        }
        return false;
    }
}
