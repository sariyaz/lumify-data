package com.altamiracorp.lumify.demoaccountweb.routes;

import com.altamiracorp.miniweb.HandlerChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CreateToken extends BaseRequestHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        // TODO: generate token and send email?
        response.sendRedirect("token-created.html");
    }
}
