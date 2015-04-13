package com.chat.longPolling;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class LongPolling {
    private HashMap<Integer, AsyncContext> poll;

    public LongPolling() {
        poll = new HashMap<Integer, AsyncContext>();
    }

    public void addAsync(int id, AsyncContext asyncContext) {
        poll.put(id, asyncContext);
    }

    public boolean respMessage(int id, JSONObject object) {
        AsyncContext asyncContext = poll.get(id);
        if (asyncContext != null) {
            ServletResponse response = asyncContext.getResponse();
            response.setContentType("text/plain");
            response.setCharacterEncoding("UTF-8");
            JSONArray array = new JSONArray();
            array.add(object);
            try {
                PrintWriter out = response.getWriter();
                out.print(array);
                out.flush();
            } catch (IOException e) {
            }
            asyncContext.complete();
            poll.remove(id);

            return true;
        }
        return false;
    }

    public JSONObject respDelete(int id, int dialogID, int mailID, String text, int status) {
        JSONObject object = new JSONObject();
        object.put("mailID", mailID);
        object.put("dialogID", dialogID);
        object.put("text", text);
        object.put("status", status);
        if (respMessage(id, object)) {
            return null;
        } else {
            return object;
        }
    }

    public boolean respUpdateContent(int id) {
        AsyncContext asyncContext = poll.get(id);
        if (asyncContext != null) {
            ServletResponse response = asyncContext.getResponse();
            response.setContentType("text/plain");
            response.setCharacterEncoding("UTF-8");
            JSONObject object = new JSONObject();
            object.put("update", "1");
            try {
                PrintWriter out = response.getWriter();
                out.print(object);
                out.flush();
            } catch (IOException e) {
            }
            asyncContext.complete();
            poll.remove(id);

            return true;
        }
        return false;
    }
}
