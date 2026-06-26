package com.ptagent.web.controller;

import com.ptagent.service.DemandImportService;
import com.ptagent.web.ApiController;
import com.ptagent.web.ApiRequest;
import com.ptagent.web.Response;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class ImportController implements ApiController {
    private final DemandImportService demandImportService;

    public ImportController(DemandImportService demandImportService) {
        this.demandImportService = demandImportService;
    }

    @Override
    public boolean handle(ApiRequest request, HttpExchange exchange) throws IOException {
        if (request.is("import", "demands", "xlsx") && request.method("POST")) {
            Response.json(exchange, 201, demandImportService.importDemandXlsx(request.body()));
            return true;
        }
        return false;
    }
}
