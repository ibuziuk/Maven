package com.chat.session;

import com.chat.datebase.ConnectDatabase;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/loginServlet")
public class LoginServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String user = request.getParameter("user");
        String psw = request.getParameter("pwd");
        ConnectDatabase con = new ConnectDatabase();
        int userID = con.containsUser(user, psw);
        if (userID != 0) {
            Cookie logCookie = new Cookie("user", "" + userID);
            logCookie.setMaxAge(60 * 60);
            response.addCookie(logCookie);
            response.sendRedirect("chat.html");
        } else {
            RequestDispatcher rd = getServletContext().getRequestDispatcher("/index.html");
            PrintWriter out = response.getWriter();
            out.println("<font color=red>Either user name or password is wrong.</font>");
            rd.include(request, response);
        }
        con.destroy();
    }
}