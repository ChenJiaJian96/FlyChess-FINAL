//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package protocol;

import java.io.Serializable;
import java.util.Vector;

public class MsgLogic implements Serializable {
    public static final int REQUEST_ONLINE_ROOM = 1;
    public static final int REQUEST_CREATE_ROOM = 2;
    public static final int UPDATE_ONLINE_ROOM = 3;
    public static final int REQUEST_ROOM_STATUS = 4;
    public static final int UPDATE_ROOM_STATUS = 5;
    public static final int REQUEST_XP = 6;
    public static final int UPDATE_XP = 7;
    public static final int REQUEST_CHAIR = 8;
    public static final int REQUEST_JOIN_ROOM = 9;
    public static final int JOIN_ROOM_ANSWER = 10;
    public static final int REQUEST_START_GAME = 11;
    public static final int START_GAME_ANSWER = 12;
    private static final int ANSWER_ASSESS = 13;
    private static final int ANSWER_FORBIDDEN = 14;
    private int type;
    private int num;
    private String roomname = null;
    private String usr = null;
    private String str = null;
    private Vector<String> vector;

    public MsgLogic(int _type) {
        this.type = _type;
    }

    public void setRoomName(String roomname) {
        this.roomname = roomname;
    }

    public String getRoomName() {
        return this.roomname;
    }

    public void setUsr(String usr) {
        this.usr = usr;
    }

    public String getUsr() {
        return this.usr;
    }

    public void setAssess(boolean assess) {
        if (assess) {
            this.num = 13;
        } else {
            this.num = 14;
        }

    }

    public boolean getAssess() {
        return this.num == 13;
    }

    public void setOnlineRooms(Vector<String> onlineRooms) {
        this.vector = onlineRooms;
    }

    public Vector<String> getOnlineRooms() {
        return this.vector;
    }

    public void setRoomStatus(Vector<String> roomStatus) {
        this.vector = roomStatus;
    }

    public Vector<String> getRoomStatus() {
        return this.vector;
    }

    public void setXP(int xp) {
        this.num = xp;
    }

    public int getXP() {
        return this.num;
    }

    public void setChairState(String str) {
        this.str = str;
    }

    public String getChairState() {
        return this.str;
    }

    public String toString() {
        if (this.type == 1) {
            return "[ REQUEST_ONLINE_ROOM ]";
        } else if (this.type == 2) {
            return "[ REQUEST_CREATE_ROOM ]";
        } else if (this.type == 4) {
            return "[ REQUEST_ROOM_STATUS ]";
        } else if (this.type == 6) {
            return "[ REQUEST_XP ]";
        } else if (this.type == 7) {
            return "[ UPDATE_XP ]";
        } else if (this.type == 8) {
            return "[ In " + this.roomname + "REQUEST_CHAIR " + this.str + " ]";
        } else if (this.type == 9) {
            return "[ " + this.usr + "REQUEST_JOIN_ROOM " + this.roomname + " ]";
        } else if (this.type == 10) {
            return "[ JOIN_ROOM_ANSWER: " + this.getAssess() + " ]";
        } else if (this.type == 11) {
            return "[ REQUEST_START_GAME ]";
        } else if (this.type == 12) {
            return "[ START_GAME_ANSWER ]";
        } else if (this.type == 3) {
            return "[ MsgType = UPDATE_ONLINE_ROOM    Intent = LOGIC_SERIAL    在线房间     online_rooms = " + this.vector.toString() + " ]";
        } else {
            return this.type == 5 ? "[ MsgType = UPDATE_ROOM_STATUS    Intent = LOGIC_SERIAL    状态房间     room_status = " + this.vector.toString() + " ]" : "[error type]";
        }
    }

    public int getType() {
        return this.type;
    }
}
