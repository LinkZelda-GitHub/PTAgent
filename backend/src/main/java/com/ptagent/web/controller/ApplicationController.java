package com.ptagent.web.controller;

import com.ptagent.domain.RoleType;
import com.ptagent.service.ApplicationService;
import com.ptagent.web.ApiController;
import com.ptagent.web.ApiRequest;
import com.ptagent.web.Response;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class ApplicationController implements ApiController {
    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public boolean handle(ApiRequest request, HttpExchange exchange) throws IOException {
        if (request.path().size() == 3 && "demands".equals(request.path().get(0))
                && "applications".equals(request.path().get(2)) && request.method("POST")) {
            Response.json(exchange, 201, applicationService.apply(request.pathId(1),
                    request.bodyWithActor("teacherId")));
            return true;
        }
        if (request.is("applications") && request.method("GET")) {
            Response.json(exchange, 200, applicationService.listApplications(
                    request.query().getOrDefault("status", "ALL"),
                    request.actorIs(RoleType.TEACHER) ? request.actorId() : 0));
            return true;
        }
        if (request.path().size() == 3 && "applications".equals(request.path().get(0))
                && "review".equals(request.path().get(2)) && request.method("POST")) {
            Response.json(exchange, 200, applicationService.review(request.pathId(1),
                    request.bodyWithActor("adminId")));
            return true;
        }
        return false;
    }
}
