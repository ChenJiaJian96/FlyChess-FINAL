//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package protocol;

import java.io.Serializable;
import java.util.Vector;

public class GameMsgFromServer implements Serializable {
    public static final int NOW_YOUR_COLOR_HAVE_BEEN_INIT = 0;
    public static final int WHOSE_TURN = 1;
    public static final int CHESS_TO_CHOOSE = 2;
    public static final int LOGIC_SERIAL = 3;
    public static final int REQUEST_DICE = 4;
    private int type;
    private int myColor;
    private int nowTurnColor;
    private Vector<Integer> selectableChesses;
    private Vector<String> LogicSequence;

    public GameMsgFromServer(int _type) {
        this.type = _type;
    }

    public void setMyColor(int i) {
        this.myColor = i;
    }

    public void setNowTurnColor(int nowTurnColor) {
        this.nowTurnColor = nowTurnColor;
    }

    public void setSelectableChesses(Vector<Integer> selectableChesses, int playerId) {
        this.selectableChesses = selectableChesses;
        this.nowTurnColor = playerId;
    }

    public void setLogicSequence(Vector<String> logicSequence) {
        this.LogicSequence = logicSequence;
    }

    public String toString() {
        if (this.type == 0) {
            return "[ MsgType = GAME_MSG_FROM_SERVER    Intent = NOW_YOUR_COLOR_HAVE_BEEN_INIT    自身被初始化的颜色     myColor = " + this.myColor + " ]";
        } else if (this.type == 1) {
            return "[ MsgType = GAME_MSG_FROM_SERVER    Intent = WHOSE_TURN    谁的回合     nowTurnColor = " + this.nowTurnColor + " ]";
        } else if (this.type == 2) {
            return "[ MsgType = GAME_MSG_FROM_SERVER    Intent = CHESS_TO_CHOOSE    playerId(nowTurnColor) = " + this.nowTurnColor + "  可选棋子    " + " selectableChesses = " + this.selectableChesses + " ]";
        } else if (this.type == 3) {
            return "[ MsgType = GAME_MSG_FROM_SERVER    Intent = LOGIC_SERIAL    在线的房间     LogicSequence = " + this.LogicSequence + " ]";
        } else {
            return this.type == 4 ? "[ MsgType = GAME_MSG_FROM_SERVER    Intent = REQUEST_DICE    请求扔骰子     playerId(nowTurnColor) = " + this.nowTurnColor + " ]" : "[error type]";
        }
    }

    public int getType() {
        return this.type;
    }

    public int getMyColor() {
        return this.myColor;
    }

    public int getNowTurnColor() {
        return this.nowTurnColor;
    }

    public Vector<Integer> getSelectableChesses() {
        return this.selectableChesses;
    }

    public Vector<String> getLogicSequence() {
        return this.LogicSequence;
    }
}
