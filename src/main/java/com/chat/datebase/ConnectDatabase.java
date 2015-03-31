package com.chat.datebase;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;

public class ConnectDatabase{
    private Connection connectBase = null;
    private TreeMap<Integer,ArrayList<JSONObject>> timingMails;
    private TreeSet<Integer> timingDialog;
    private int userID(String log){
        String command = "SELECT*FROM users WHERE login = '"+log+"'";
        try{
            Statement st = connectBase.createStatement();
            ResultSet rs = st.executeQuery(command);
            while (rs.next())
            {
                int t =  rs.getInt(3);
                st.close();
                return t;
            }
            st.close();
        }catch (SQLException e){}
        return -1;
    }
    public String userName(int id){
        String command = "SELECT*FROM users WHERE ID = '"+id+"'";
        try{
            Statement st = connectBase.createStatement();
            ResultSet rs = st.executeQuery(command);
            while (rs.next())
            {
                String t =  rs.getString("login");
                st.close();
                return t;
            }
            st.close();
        }catch (SQLException e){}
        return null;
    }
    public ConnectDatabase(){
        /*try{
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            System.out.println("Driver loading success!");
        } catch (Exception e){
            String s =e.toString();
        }*/
        try{
            InitialContext initContext= new InitialContext();
            DataSource ds = (DataSource) initContext.lookup("java:comp/env/jdbc/blackchat");
            connectBase = ds.getConnection();
        } catch (Exception e){}
        timingMails = new TreeMap<Integer, ArrayList<JSONObject>>();
        timingDialog = new TreeSet<Integer>();
    }
    public void addUser(String log, String psw){
        String md5Psw = DigestUtils.md5Hex(psw);
        String command = "INSERT INTO users (login, psw) VALUES('"+log+"','"+md5Psw+"')";
        try{
            Statement st = connectBase.createStatement();
            st.executeUpdate(command);
            st.close();
        }catch (SQLException e){}
    }
    public void destroy(){
        try{
            connectBase.close();
        }catch (SQLException e){}
    }
    public int containsUser(String log, String psw){
        String md5Psw = DigestUtils.md5Hex(psw);
        String command = "SELECT*FROM users WHERE login = '"+log+"'";
        try{
            Statement st = connectBase.createStatement();
            ResultSet rs = st.executeQuery(command);
            while (rs.next())
            {
                String s = rs.getString("psw");
                if(md5Psw.equals(s)) {
                    int t =  rs.getInt(3);
                    st.close();
                    return t;
                }
            }
            st.close();
        }catch (SQLException e){}
        return -1;
    }
    public void createNewDialog(int id, String firstID){
        timingDialog.add(Integer.parseInt(firstID));
        try {
            Statement st = connectBase.createStatement();
            ResultSet rs = st.executeQuery("SELECT*FROM dialog WHERE dialogID =(SELECT MAX(dialogID) FROM dialog)");
            rs.next();
            int dialogID = rs.getInt("dialogID") + 1;
            st.executeUpdate("INSERT INTO dialog (id, dialogID) VALUE ("+id+","+dialogID+")");
            st.executeUpdate("INSERT INTO dialog (id, dialogID) VALUE ("+firstID+","+dialogID+")");
            st.executeUpdate("CREATE TABLE dialog"+dialogID+"(id INT )");
            st.executeUpdate("INSERT INTO dialog"+dialogID+" (id) VALUE ("+id+")");
            st.executeUpdate("INSERT INTO dialog"+dialogID+" (id) VALUE ("+firstID+")");
            st.executeUpdate("INSERT INTO mail (userID, dialogID, mailID, text) VALUE (-1,"+dialogID+",-1,'a')");
        }catch (SQLException e){}
    }
    public void addNewUserDialog(JSONObject object){
        String dialogID = object.get("dialogID").toString();
        String userID = object.get("id").toString();
        timingDialog.add(Integer.parseInt(userID));
        try{
            Statement st = connectBase.createStatement();
            st.executeUpdate("INSERT INTO dialog (id, dialogID) VALUE ("+userID+","+dialogID+")");
            st.executeUpdate("INSERT INTO dialog"+dialogID+"(id) VALUE ("+userID+")");
        }catch (SQLException e){}
    }
    public void addMail(int id, JSONObject object){
        String dialogID = (String) object.get("dialogID");
        String text = (String) object.get("text");
        object.put("userName",userName(id));
        try{
            Statement st = connectBase.createStatement();
            ResultSet rs = st.executeQuery("SELECT*FROM mail WHERE mailID =(SELECT MAX(mailID) FROM mail  WHERE dialogID="+dialogID+")");
            rs.next();
            int mailID = rs.getInt("mailID") + 1;
            st.executeUpdate("INSERT INTO mail (userID, dialogID, mailID, text) VALUE " +
                    "("+id+","+dialogID+","+mailID+",'"+text+"')");
            ResultSet resultSet = st.executeQuery("SELECT * FROM dialog"+dialogID);
            while (resultSet.next()){
                int userID = resultSet.getInt("id");
                if(userID != id) {
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
        }catch (SQLException e){}
    }
    public JSONArray respUser(int id){
        JSONArray array = new JSONArray();
        try{
            Statement st = connectBase.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM users");
            while (rs.next()){
                JSONObject object = new JSONObject();
                object.put("name",rs.getString("login"));
                int usrerID = rs.getInt("ID");
                object.put("userID",usrerID);
                if(usrerID == id){
                    object.put("flag","1");
                }
                else {
                    object.put("flag","0");
                }
                array.add(object);
            }
        }catch (SQLException e){}
        return array;
    }
    public JSONArray respDialog(int id){
        JSONArray array = new JSONArray();
        try{
            Statement st = connectBase.createStatement();
            ResultSet rs = st.executeQuery("SELECT *FROM dialog WHERE id="+id);
            while (rs.next()){
                JSONObject object = new JSONObject();
                int dialogID= rs.getInt("dialogID");
                object.put("dialogID",dialogID);
                Statement stSecond = connectBase.createStatement();
                ResultSet result = stSecond.executeQuery("SELECT * FROM dialog"+dialogID);
                result.next();
                String users = userName(result.getInt("id"));
                while (result.next()){
                    users+= "<br>"+userName(result.getInt("id"));
                }
                object.put("massUsers",users);
                array.add(object);
            }
        }catch (SQLException e){}
        return array;
    }
    public JSONArray respMail(int id){
        JSONArray array = new JSONArray();
        try{
            Statement st = connectBase.createStatement();
            ResultSet rt = st.executeQuery("SELECT * FROM dialog WHERE id="+id);
            while (rt.next()){
                int dialogID = rt.getInt("dialogID");
                Statement stSecond = connectBase.createStatement();
                ResultSet resultSet = stSecond.executeQuery("SELECT * FROM mail WHERE dialogID="+dialogID+" ORDER BY mailID");
                while (resultSet.next()){
                    JSONObject object = new JSONObject();
                    object.put("userName",userName(resultSet.getInt("userID")));
                    object.put("dialogID",dialogID);
                    object.put("text",(resultSet.getString("text")));
                    array.add(object);
                }
            }
        }catch (SQLException e){}
        return array;
    }
    public JSONArray respClient(int id){
        ArrayList<JSONObject> arrayList = timingMails.get(id);
        if(arrayList == null){
            return null;
        }
        JSONArray array = new JSONArray();
        for(JSONObject object:arrayList){
            array.add(object);
        }
        arrayList.clear();
        return array;
    }
    public boolean isNewDialog(int id){
        if(timingDialog.contains(id)){
            timingDialog.remove(id);
            return true;
        }
        return false;
    }
}