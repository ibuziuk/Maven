package com.chat.datebase;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

public class ConnectDatabase {
    private Connection connectBase = null;
    private static TreeMap<Integer, ArrayList<JSONObject>> timingMails;
    private static TreeSet<Integer> timingDialog;

    private int userID(String log) {
        String command = "SELECT*FROM users WHERE login = '" + log + "'";
        try {
            Statement st = connectBase.createStatement();
            ResultSet rs = st.executeQuery(command);
            while (rs.next()) {
                int t = rs.getInt("userID");
                return t;
            }
        } catch (SQLException e) {
        }
        return -1;
    }

    public String userName(int id) {
        try {
            Statement st = connectBase.createStatement();
            ResultSet rs = st.executeQuery("SELECT login FROM users WHERE userID = " + id);
            while (rs.next()) {
                String t = rs.getString("login");
                st.close();
                return t;
            }
        } catch (SQLException e) {
        }
        return null;
    }

    public ConnectDatabase() {
        try {
            InitialContext initContext = new InitialContext();
            DataSource ds = (DataSource) initContext.lookup("java:comp/env/jdbc/blackchat");
            connectBase = ds.getConnection();
        } catch (Exception e) {
        }
        if (timingDialog == null && timingMails == null) {
            timingMails = new TreeMap<Integer, ArrayList<JSONObject>>();
            timingDialog = new TreeSet<Integer>();
        }
    }

    public void addUser(String log, String psw) {
        //PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        //String hashedPsw = passwordEncoder.encode(psw);
        try {
            Statement st = connectBase.createStatement();
            st.executeUpdate("INSERT INTO users (login, psw) VALUES('" + log + "','" + psw + "')");
        } catch (SQLException e) {
        }
    }

    public int containsUser(String log, String psw) {
        //PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        //String hashedPsw = passwordEncoder.encode(psw);
        try {
            Statement st = connectBase.createStatement();
            ResultSet rs = st.executeQuery("SELECT*FROM users WHERE login = '" + log + "' AND psw = '" + psw + "'");
            while (rs.next()) {
                int t = rs.getInt("userID");
                return t;
            }
        } catch (SQLException e) {
        }
        return -1;
    }

    public void createNewDialog(int id, String firstID) {
        timingDialog.add(Integer.parseInt(firstID));
        try {
            Statement st = connectBase.createStatement();
            st.executeUpdate("INSERT INTO dialogs VALUE ()");
            ResultSet rs = st.executeQuery("SELECT last_insert_id() AS last_id FROM dialogs");
            rs.next();
            int id_dialog = rs.getInt("last_id");
            st.executeUpdate("INSERT INTO users_dialogs (userID, dialogID) VALUE (" + id + "," + id_dialog + ")");
            st.executeUpdate("INSERT INTO users_dialogs (userID, dialogID) VALUE (" + firstID + "," + id_dialog + ")");
        } catch (SQLException e) {
        }
    }

    public void addNewUserDialog(JSONObject object) {
        String dialogID = object.get("dialogID").toString();
        String userID = object.get("id").toString();
        timingDialog.add(Integer.parseInt(userID));
        try {
            Statement st = connectBase.createStatement();
            st.executeUpdate("INSERT INTO users_dialogs (userID, dialogID) VALUE (" + userID + "," + dialogID + ")");
        } catch (SQLException e) {
        }
    }

    public int addMail(int id, JSONObject object) {
        String dialogID = (String) object.get("dialogID");
        String text = (String) object.get("text");
        object.put("userName", userName(id));
        try {
            Statement st = connectBase.createStatement();
            st.executeUpdate("INSERT INTO mails (dialogID, userID, text) VALUE (" + dialogID + "," + id + ",'" + text + "')");
            ResultSet rs = st.executeQuery("SELECT last_insert_id() AS last_id FROM mails");
            rs.next();
            return rs.getInt("last_id");
        } catch (SQLException e) {
        }
        ArrayList<JSONObject> arrayList = timingMails.get(id);
        if (arrayList == null) {
            arrayList = new ArrayList<JSONObject>();
            arrayList.add(object);
            timingMails.put(id, arrayList);
        } else {
            arrayList.add(object);
        }
        return -1;
    }

    public JSONArray respUser(int id) {
        JSONArray array = new JSONArray();
        try {
            Statement st = connectBase.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM users");
            while (rs.next()) {
                JSONObject object = new JSONObject();
                object.put("name", rs.getString("login"));
                int usrerID = rs.getInt("userID");
                object.put("userID", usrerID);
                if (usrerID == id) {
                    object.put("flag", "1");
                } else {
                    object.put("flag", "0");
                }
                array.add(object);
            }
        } catch (SQLException e) {
        }
        return array;
    }

    public JSONArray respDialog(int id) {
        JSONArray array = new JSONArray();
        try {
            Statement st = connectBase.createStatement();
            ResultSet rs = st.executeQuery("SELECT dialogID FROM users_dialogs WHERE userID = " + id);
            while (rs.next()) {
                JSONObject object = new JSONObject();
                int dialogID = rs.getInt("dialogID");
                object.put("dialogID", dialogID);
                Statement statement = connectBase.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT users.login AS 'login'  FROM users_dialogs " +
                        "JOIN users ON users.userID = users_dialogs.userID WHERE users_dialogs.dialogID = " + dialogID);
                String arrayUsers = new String();
                resultSet.next();
                arrayUsers += resultSet.getString("login");
                while (resultSet.next()) {
                    arrayUsers += "<br>" + resultSet.getString("login");
                }
                object.put("massUsers", arrayUsers);
                array.add(object);
            }

        } catch (SQLException e) {
        }
        return array;
    }

    public JSONArray respMail(int id) {
        JSONArray array = new JSONArray();
        try {
            Statement st = connectBase.createStatement();
            ResultSet rt = st.executeQuery("SELECT dialogID FROM users_dialogs WHERE userID = " + id);
            while (rt.next()) {
                int dialogID = rt.getInt("dialogID");
                Statement statement = connectBase.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT userID, mailID, text FROM mails" +
                        " WHERE dialogID = " + dialogID + " ORDER BY mailID");
                while (resultSet.next()) {
                    JSONObject object = new JSONObject();
                    object.put("userName", userName(resultSet.getInt("userID")));
                    object.put("dialogID", dialogID);
                    object.put("text", (resultSet.getString("text")));
                    array.add(object);
                }
            }
        } catch (SQLException e) {
        }
        return array;
    }

    public JSONArray respClient(int id) {
        ArrayList<JSONObject> arrayList = timingMails.get(id);
        if (arrayList == null) {
            return null;
        }
        JSONArray array = new JSONArray();
        for (JSONObject object : arrayList) {
            array.add(object);
        }
        arrayList.clear();
        return array;
    }

    public boolean isNewDialog(int id) {
        if (timingDialog.contains(id)) {
            timingDialog.remove(id);
            return true;
        }
        return false;
    }
}