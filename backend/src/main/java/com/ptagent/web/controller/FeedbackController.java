package com.ptagent.web.controller;

import com.ptagent.service.FeedbackService;
import com.ptagent.web.ApiController;
import com.ptagent.web.ApiRequest;
import com.ptagent.web.Response;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class FeedbackController implements ApiController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @Override
    public boolean handle(ApiRequest request, HttpExchange exchange) throws IOException {
        if (request.is("feedbacks") && request.method("GET")) {
            Response.json(exchange, 200, feedbackService.listFeedbacks());
            return true;
        }
        if (request.is("feedbacks") && request.method("POST")) {
            Response.json(exchange, 201, feedbackService.createFeedback(request.body()));
            return true;
        }
        return false;
    }
}
