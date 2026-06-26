package com.ptagent.web.controller;

import com.ptagent.service.DemandService;
import com.ptagent.web.ApiController;
import com.ptagent.web.ApiRequest;
import com.ptagent.web.Response;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class DemandController implements ApiController {
    private final DemandService demandService;

    public DemandController(DemandService demandService) {
        this.demandService = demandService;
    }

    @Override
    public boolean handle(ApiRequest request, HttpExchange exchange) throws IOException {
        if (request.is("demands") && request.method("GET")) {
            Response.json(exchange, 200, demandService.listDemands(request.query()));
            return true;
        }
        if (request.is("demands") && request.method("POST")) {
            Response.json(exchange, 201, demandService.createDemand(request.body()));
            return true;
        }
        if (request.path().size() == 2 && "demands".equals(request.path().get(0)) && request.method("GET")) {
            Response.json(exchange, 200, demandService.getDemand(request.pathId(1),
                    request.queryLong("teacherId", 0)));
            return true;
        }
        if (request.path().size() == 3 && "demands".equals(request.path().get(0))
                && "close".equals(request.path().get(2)) && !request.method("GET")) {
            Response.json(exchange, 200, demandService.closeDemand(request.pathId(1)));
            return true;
        }
        return false;
    }
}
