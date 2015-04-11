package com.chat.longPolling;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import java.io.IOException;
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
            byte[] arrayByte = array.toJSONString().getBytes();
            response.setContentLength(arrayByte.length);
            try {
                response.getOutputStream().write(arrayByte);
            } catch (IOException e) {
            }
            asyncContext.complete();
            poll.remove(id);

            return true;
        }
        return false;
    }

    public JSONObject respDelete(int id, int dialogID, int mailID, String text) {
        JSONObject object = new JSONObject();
        object.put("mailID", mailID);
        object.put("dialogId", dialogID);
        object.put("text", text);
        object.put("status", 1);
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

            byte[] arrayByte = object.toJSONString().getBytes();
            response.setContentLength(arrayByte.length);
            try {
                response.getOutputStream().write(arrayByte);
            } catch (IOException e) {
            }
            asyncContext.complete();
            poll.remove(id);

            return true;
        }
        return false;
    }
}
