package com.ptagent.web.controller;

import com.ptagent.service.DashboardService;
import com.ptagent.web.ApiController;
import com.ptagent.web.ApiRequest;
import com.ptagent.web.Response;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class DashboardController implements ApiController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    public boolean handle(ApiRequest request, HttpExchange exchange) throws IOException {
        if (request.is("bootstrap") && request.method("GET")) {
            Response.json(exchange, 200, dashboardService.dashboard());
            return true;
        }
        return false;
    }
}
