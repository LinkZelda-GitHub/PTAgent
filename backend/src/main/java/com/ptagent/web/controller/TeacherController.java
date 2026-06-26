package com.ptagent.web.controller;

import com.ptagent.common.Json;
import com.ptagent.service.TeacherService;
import com.ptagent.web.ApiController;
import com.ptagent.web.ApiRequest;
import com.ptagent.web.Response;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class TeacherController implements ApiController {
    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @Override
    public boolean handle(ApiRequest request, HttpExchange exchange) throws IOException {
        if (request.is("teachers") && request.method("GET")) {
            Response.json(exchange, 200, teacherService.listTeachers());
            return true;
        }
        if (request.path().size() == 3 && "teachers".equals(request.path().get(0))
                && "enabled".equals(request.path().get(2)) && !request.method("GET")) {
            Response.json(exchange, 200, teacherService.setEnabled(request.pathId(1),
                    Json.bool(request.body(), "enabled", true),
                    Json.longValue(request.body(), "adminId", 0)));
            return true;
        }
        if (request.path().size() == 3 && "teachers".equals(request.path().get(0))
                && "profile".equals(request.path().get(2)) && !request.method("GET")) {
            Response.json(exchange, 200, teacherService.updateProfile(request.pathId(1), request.body()));
            return true;
        }
        if (request.is("resumes") && request.method("GET")) {
            Response.json(exchange, 200, teacherService.listResumes());
            return true;
        }
        if (request.is("resumes") && request.method("POST")) {
            Response.json(exchange, 201, teacherService.submitResume(request.body()));
            return true;
        }
        if (request.path().size() == 3 && "resumes".equals(request.path().get(0))
                && "status".equals(request.path().get(2)) && !request.method("GET")) {
            Response.json(exchange, 200, teacherService.updateResumeStatus(request.pathId(1),
                    (int) Json.longValue(request.body(), "status", 1),
                    Json.longValue(request.body(), "adminId", 0)));
            return true;
        }
        return false;
    }
}
