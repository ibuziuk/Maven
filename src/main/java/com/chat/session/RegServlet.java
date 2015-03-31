package com.chat.session;

import com.chat.datebase.ConnectDatabase;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@WebServlet(urlPatterns = "/RegServlet")
public class RegServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ConnectDatabase con = new ConnectDatabase();
        String login = req.getParameter("user");
        String psw = req.getParameter("pwd");
        con.addUser(login,psw);
        con.destroy();
        resp.sendRedirect("index.html");
    }
}
