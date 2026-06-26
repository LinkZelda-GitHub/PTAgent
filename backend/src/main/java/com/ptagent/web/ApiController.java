package com.ptagent.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public interface ApiController {
    boolean handle(ApiRequest request, HttpExchange exchange) throws IOException;
}
