package com.chat.datebase;

import com.chat.longPolling.LongPolling;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;

public class ConnectDatabase {
    private Connection connectBase = null;
    private TreeMap<Integer, ArrayList<JSONObject>> timingMails = null;
    private TreeSet<Integer> timingDialog = null;
    private LongPolling poll = null;
    final private String MESSEGE_DELETE = "this message has been removed";

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

    public ConnectDatabase(LongPolling poll) {
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
        this.poll = poll;
    }

    public ConnectDatabase() {
        try {
            InitialContext initContext = new InitialContext();
            DataSource ds = (DataSource) initContext.lookup("java:comp/env/jdbc/blackchat");
            connectBase = ds.getConnection();
        } catch (Exception e) {
        }
    }

    public void addUser(String log, String psw) {
        //PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        //String hashedPsw = passwordEncoder.encode(psw);
        try {
            Statement st = connectBase.createStatement();
            st.executeUpdate("INSERT INTO users (login, psw) VALUES('" + log + "','" + psw + "')");
            ResultSet rs = st.executeQuery("SELECT last_insert_id() AS last_id FROM mails");
            rs.next();
            int lastId = rs.getInt("last_id");
            ResultSet resultSet = st.executeQuery("SELECT userID FROM users ");
            while (resultSet.next()) {
                int id = resultSet.getInt("userID");
                if (lastId == id)
                    continue;

                if (!poll.respUpdateContent(id)) {
                    timingDialog.add(id);
                }
            }
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
        try {
            Statement st = connectBase.createStatement();
            st.executeUpdate("INSERT INTO dialogs VALUE ()");
            ResultSet rs = st.executeQuery("SELECT last_insert_id() AS last_id FROM dialogs");
            rs.next();
            int id_dialog = rs.getInt("last_id");
            st.executeUpdate("INSERT INTO users_dialogs (userID, dialogID) VALUE (" + id + "," + id_dialog + ")");
            st.executeUpdate("INSERT INTO users_dialogs (userID, dialogID) VALUE (" + firstID + "," + id_dialog + ")");
            int[] arrayID = {id, Integer.parseInt(firstID)};
            for (int a : arrayID) {
                if (!poll.respUpdateContent(a)) {
                    timingDialog.add(a);
                }
            }
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
            ResultSet resultSet = st.executeQuery("SELECT userID FROM users_dialogs WHERE dialogID=" + dialogID);
            while (resultSet.next()) {
                int id = resultSet.getInt("userID");
                if (!poll.respUpdateContent(id)) {
                    timingDialog.add(id);
                }
            }
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
            int lastId = rs.getInt("last_id");
            object.put("mailID", lastId);
            ResultSet resultSet = st.executeQuery("SELECT userID FROM users_dialogs WHERE dialogID=" + dialogID);
            while (resultSet.next()) {
                int userID = resultSet.getInt("userID");
                if (userID == id) {
                    continue;
                }
                if (!poll.respMessage(userID, object)) {
                    ArrayList<JSONObject> arrayList = timingMails.get(userID);
                    if (arrayList == null) {
                        arrayList = new ArrayList<JSONObject>();
                        arrayList.add(object);
                        timingMails.put(userID, arrayList);
                    } else {
                        arrayList.add(object);
                    }
                }
            }
            return lastId;
        } catch (SQLException e) {
        }

        return -1;
    }

    public void deleteMail(int userID, int dialogID, int mailID) {
        try {
            Statement st = connectBase.createStatement();
            st.executeUpdate("UPDATE mails SET text ='" + MESSEGE_DELETE + "'" +
                    "WHERE dialogID =" + dialogID + ", mailID =" + mailID);
            ResultSet resultSet = st.executeQuery("SELECT userID FROM users_dialogs WHERE dialogID =" + dialogID);
            while (resultSet.next()) {
                int id = resultSet.getInt("userID");
                if (id == userID) {
                    continue;
                }
                JSONObject object = poll.respDelete(id, dialogID, mailID, MESSEGE_DELETE);
                if (object != null) {
                    ArrayList<JSONObject> arrayList = timingMails.get(userID);
                    if (arrayList == null) {
                        arrayList = new ArrayList<JSONObject>();
                        arrayList.add(object);
                        timingMails.put(userID, arrayList);
                    } else {
                        arrayList.add(object);
                    }
                }
            }
        } catch (SQLException e) {
        }
    }

    public JSONArray respUser(int id) {
        timingMails.remove(id);
        timingDialog.remove(id);
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