//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package protocol;

import java.io.Serializable;
import java.util.Vector;

public class MsgUnity implements Serializable {
    public static final int UPDATE_ACTION_LIST = 1;
    public static final int CHANGE_NAME_IN_UI = 2;
    public static final int THROW_DICE = 3;
    public static final int SET_GIVEN_FORCE = 4;
    public static final int SET_DICE = 5;
    public static final int SET_CHOICE = 6;
    public static final int SET_SWITCH = 7;
    public static final int SET_END_THIS_TURN = 8;
    public static final int UPDATE_FORCE = 9;
    public static final int THROW_DICE_UI = 10;
    public static final int EXIT_GAME  = 11;//玩家在游戏中途退出游戏(最后一人退出时删除线程,否则切换为AI模式,但是都要删除socket)
    public static final int SHOW_RANKING_LIST = 12;//排行榜
    public static final int REPLAY_GAME = 13;//回放游戏
    public static final int TO_NETWORK_MODE = 14;//网络模式
    public static final int FORCE_EXIT = 15;//强制退出
    public static final int AUTO_THROW_DICE = 16;//服务器自动帮玩家扔骰子
    
    private int type;
    private Vector<String> vector;
    private int num;
    private int num_o;
    private String str;
    private float force;

    public MsgUnity(int _type) {
        this.type = _type;
    }

    public void set_ranking_list(String ranking_list)
    {
        this.str = ranking_list;
    }
    public String getRankingList()
    {
        return str;
    }
    public void set_filename(String filename)
    {
        vector = new Vector<>();
        vector.add(filename);
    }
    public String get_filename()
    {
        return vector.get(0);
    }
    public void set_history(String history)
    {
        vector.add(history);
    }
    public String get_history()
    {
        return vector.get(1);
    }

    public void setActionList(Vector<String> actionList) {
        this.vector = actionList;
    }

    public void setChangeNameInUi(Vector<String> changeNameInUi) {
        this.vector = changeNameInUi;
    }

    public void setDice(int num) {
        this.num = num;
    }

    public void setRoomname(String roomname) {
        this.str = roomname;
    }

    public void setChoice(int choice) {
        this.num = choice;
    }

    public void setTurn(int turn) {
        this.num_o = turn;
    }

    public void setForce(float force) {
        this.force = force;
    }

    public String toString() {
        if (this.type == UPDATE_ACTION_LIST) {
            return "[ MsgType = UPDATE_ACTION_LIST    Intent = Action_List    server向client发送动作序列\n actionlist = " + this.vector + " ]";
        } else if (this.type == CHANGE_NAME_IN_UI) {
            return "[ MsgType = CHANGE_NAME_IN_UI    Intent = name_list    server向client发送滚动条序列\n namelist = " + this.vector + " ]";
        } 
        else if(this.type == THROW_DICE)
        {
        	return "[ MsgType = THROW_DICE  dice : " + getDice();
        }
        else if(this.type == SET_GIVEN_FORCE)
        {
        	return "[ MsgType = SET_GIVEN_FORCE:";
        }
        else if(this.type == SET_DICE)
        {
        	return "[ MsgType = SET_DICE dice :" + getDice();
        }
        else if(this.type == SET_CHOICE)
        {
        	return "[ MsgType = SET_CHOICE choice :" + getDice();
        }
        else if(this.type == SET_SWITCH)
        {
        	return "[ MsgType = SET_SWITCH";
        }
        else if(this.type == SET_END_THIS_TURN)
        {
        	return "[ MsgType = SET_END_THIS_TURN";
        }
        else if(this.type == UPDATE_FORCE)
        {
            return "[ MsgType = UPDATE_FORCE";
        }
        else if(this.type == THROW_DICE_UI)
        {
        	return "[ MsgType = THROW_DICE_UI dice :" + getDice();
        }
        else if(this.type == EXIT_GAME)
        {
            return "[ MsgType = EXIT_GAME";
        }
        return "[error type]";
    }

    public int getType() {
        return this.type;
    }

    public Vector<String> getActionlist() {
        return this.vector;
    }

    public Vector<String> getChangeNameInUi() {
        return this.vector;
    }

    public int getDice() {
        return this.num;
    }

    public String getRoomname() {
        return this.str;
    }

    public int getChoice() {
        return this.num;
    }

    public int getTurn() {
        return this.num_o;
    }

    public float getForce() {
        return this.force;
    }
}
