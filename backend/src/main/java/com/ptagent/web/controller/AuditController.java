package com.ptagent.web.controller;

import com.ptagent.service.AuditService;
import com.ptagent.web.ApiController;
import com.ptagent.web.ApiRequest;
import com.ptagent.web.Response;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class AuditController implements ApiController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public boolean handle(ApiRequest request, HttpExchange exchange) throws IOException {
        if (request.is("audit-logs") && request.method("GET")) {
            Response.json(exchange, 200, auditService.listAuditLogs(request.actorId()));
            return true;
        }
        return false;
    }
}
