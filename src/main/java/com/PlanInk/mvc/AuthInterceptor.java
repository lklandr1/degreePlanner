package com.PlanInk.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        String path = request.getRequestURI();

        // Allow public paths
        if (path.startsWith("/login") || path.startsWith("/signup") || path.startsWith("/public") ) {
            return true;
        }

        if (session == null || session.getAttribute("currentUser") == null) {
            response.sendRedirect("/login?error=notAuthenticated");
            return false;
        }

        return true; // user is authenticated
    }
}
