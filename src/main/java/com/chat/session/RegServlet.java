package com.chat.session;

import com.chat.datebase.ConnectDatabase;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


@WebServlet(urlPatterns = "/RegServlet")
public class RegServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ConnectDatabase con = new ConnectDatabase();
        String login = req.getParameter("user");
        String psw = req.getParameter("pwd");
        if(con.addUser(login, psw)){
            resp.sendRedirect("index.html");
        } else {
            RequestDispatcher rd = getServletContext().getRequestDispatcher("/index.html");
            PrintWriter out = resp.getWriter();
            out.println("<font color=red>user with the same name already exists.</font>");
            rd.include(req, resp);
        }
    }
}
