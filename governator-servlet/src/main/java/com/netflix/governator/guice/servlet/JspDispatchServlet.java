package com.netflix.governator.guice.servlet;

import java.io.IOException;

import javax.inject.Singleton;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * This service is a hack to solve a bug in Guice's serving of jsp files.  
 *
 * To enable add this line to your ServletModule
 * <pre>
 * {@code
 * serveRegex("/.*\\.jsp").with(JspDispatchServlet.class);
 * bind(JspDispatchServlet.class).asEagerSingleton();
 * }
 * </pre>
 *             
 * @author elandau
 */
@Singleton
public class JspDispatchServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RequestDispatcher rd = getServletContext().getRequestDispatcher(req.getRequestURI());
        req.setAttribute("org.apache.catalina.jsp_file", req.getRequestURI());
        
        // Wrap ServletPath with an empty value to avoid Guice's null getServletPath() bug from within a filter/servlet chain
        HttpServletRequest wrapped = new HttpServletRequestWrapper(req) {
            public String getServletPath() {
                return "";
            }
        };

        rd.include(wrapped, resp);
    }
}