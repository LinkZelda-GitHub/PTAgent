package com.ptagent.web.controller;

import com.ptagent.domain.RoleType;
import com.ptagent.service.CourseService;
import com.ptagent.web.ApiController;
import com.ptagent.web.ApiRequest;
import com.ptagent.web.Response;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class CourseController implements ApiController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Override
    public boolean handle(ApiRequest request, HttpExchange exchange) throws IOException {
        if (request.is("orders") && request.method("GET")) {
            Response.json(exchange, 200, courseService.listOrders(
                    request.actorIs(RoleType.TEACHER) ? request.actorId() : 0));
            return true;
        }
        if (request.path().size() == 3 && "orders".equals(request.path().get(0))
                && "records".equals(request.path().get(2)) && request.method("POST")) {
            Response.json(exchange, 201, courseService.createRecord(request.pathId(1),
                    request.bodyWithActor("teacherId")));
            return true;
        }
        if (request.is("records") && request.method("GET")) {
            Response.json(exchange, 200, courseService.listRecords(
                    request.queryLong("orderId", 0),
                    request.actorIs(RoleType.TEACHER) ? request.actorId() : 0));
            return true;
        }
        return false;
    }
}
