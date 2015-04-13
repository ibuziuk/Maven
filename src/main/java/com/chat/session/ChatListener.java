package com.chat.session;

import com.chat.datebase.ConnectDatabase;
import com.chat.longPolling.LongPolling;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/ChatListener", asyncSupported = true)
public class ChatListener extends HttpServlet {
    private JSONParser jsonParser = null;
    private ConnectDatabase con = null;
    private LongPolling longPolling = null;

    @Override
    public void init() throws ServletException {
        longPolling = new LongPolling();
        con = new ConnectDatabase(longPolling);
        jsonParser = new JSONParser();
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        Cookie[] massCook = req.getCookies();
        int id = -1;
        for (Cookie c : massCook) {
            if (c.getName().equals("user")) {
                id = Integer.parseInt(c.getValue());
            }
        }
        if (id == -1)
            return;
        PrintWriter out = resp.getWriter();
        if (con.isNewDialog(id)) {
            JSONObject object = new JSONObject();
            object.put("update", "1");
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");
            byte[] arrayByte = object.toJSONString().getBytes();
            resp.setContentLength(arrayByte.length);
            resp.getOutputStream().write(arrayByte);
            return;
        }
        JSONArray array = con.respClient(id);
        if (array != null) {
            out.print(array);
            return;
        }
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(1000000);
        asyncContext.addListener(new AsyncListener() {
            public void onComplete(AsyncEvent asyncEvent) throws IOException {

            }

            public void onTimeout(AsyncEvent asyncEvent) throws IOException {

            }

            public void onError(AsyncEvent asyncEvent) throws IOException {

            }

            public void onStartAsync(AsyncEvent asyncEvent) throws IOException {

            }
        });
        longPolling.addAsync(id, asyncContext);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Cookie[] massCook = req.getCookies();
        int id = -1;
        for (Cookie c : massCook) {
            if (c.getName().equals("user")) {
                id = Integer.parseInt(c.getValue());
            }
        }
        if (id == -1)
            return;
        StringBuffer js = new StringBuffer();
        String line;
        try {
            BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null)
                js.append(line);
        } catch (Exception e) {
        }

        JSONObject jObject = new JSONObject();
        try {
            jObject = (JSONObject) jsonParser.parse(js.toString());
        } catch (ParseException e) {
        }
        if (jObject.containsKey("flag")) {
            String str = jObject.get("flag").toString();
            if (str != null && str.equals("1")) {
                PrintWriter out = resp.getWriter();
                out.print(con.respUser(id));
                return;
            }
            if (str != null && str.equals("2")) {
                PrintWriter out = resp.getWriter();
                out.print(con.respDialog(id));
                return;
            }
            if (str != null && str.equals("3")) {
                PrintWriter out = resp.getWriter();
                out.print(con.respMail(id));
                return;
            }
            if (jObject.containsKey("id")) {
                String strSecond = jObject.get("id").toString();
                if (str != null && str.equals("4")) {
                    con.createNewDialog(id, strSecond);
                    return;
                }
                if (str != null && str.equals("5")) {
                    con.addNewUserDialog(jObject);
                    return;
                }
            }
        }
        PrintWriter out = resp.getWriter();
        int t = con.addMail(id, jObject);
        out.print(t);
        out.flush();
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Cookie[] massCook = req.getCookies();
        int id = -1;
        for (Cookie c : massCook) {
            if (c.getName().equals("user")) {
                id = Integer.parseInt(c.getValue());
            }
        }
        if (id == -1)
            return;
        StringBuffer js = new StringBuffer();
        String line;
        try {
            BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null)
                js.append(line);
        } catch (Exception e) {
        }

        JSONObject jObject = new JSONObject();
        try {
            jObject = (JSONObject) jsonParser.parse(js.toString());
        } catch (ParseException e) {
        }
        int dialogID = Integer.parseInt(jObject.get("dialogID").toString());
        int mailID = Integer.parseInt(jObject.get("mailID").toString());
        con.deleteMail(id, dialogID, mailID);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Cookie[] massCook = req.getCookies();
        int id = -1;
        for (Cookie c : massCook) {
            if (c.getName().equals("user")) {
                id = Integer.parseInt(c.getValue());
            }
        }
        if (id == -1)
            return;
        StringBuffer js = new StringBuffer();
        String line;
        try {
            BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null)
                js.append(line);
        } catch (Exception e) {
        }

        JSONObject jObject = new JSONObject();
        try {
            jObject = (JSONObject) jsonParser.parse(js.toString());
        } catch (ParseException e) {
        }
        int dialogID = Integer.parseInt(jObject.get("dialogID").toString());
        int mailID = Integer.parseInt(jObject.get("mailID").toString());
        String text = (String) jObject.get("text");
        con.putMail(id, dialogID, mailID, text);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doOptions(req, resp);
    }
}