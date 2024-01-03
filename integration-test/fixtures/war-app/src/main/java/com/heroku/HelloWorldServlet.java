package com.heroku;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServlet;
import java.io.IOException;

public class HelloWorldServlet extends HttpServlet {
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        res.getOutputStream().print("Hello World!");
    }
}
