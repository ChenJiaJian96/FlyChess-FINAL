//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package protocol;

import java.io.Serializable;

public class GameMsgFromClient implements Serializable {
    public static final int DICE_NUM = 0;
    public static final int THE_CHESS_WHO_CHOOSE = 1;
    private int type;
    private int dice;
    private int chosenChess;

    public GameMsgFromClient(int _type) {
        this.type = _type;
    }

    public void setDice(int dice) {
        this.dice = dice;
    }

    public void setChosenChess(int chosenChess) {
        this.chosenChess = chosenChess;
    }

    public String toString() {
        if (this.type == 0) {
            return "[ MsgType = GAME_MSG_FROM_CLIENT  Intent = DICE_NUM  筛子数值   diceNum = " + this.dice + " ]";
        } else {
            return this.type == 1 ? "[ MsgType = GAME_MSG_FROM_CLIENT  Intent = THE_CHESS_WHO_CHOOSE  选的棋子   chosenChess = " + this.chosenChess + " ]" : "[error type]";
        }
    }

    public int getType() {
        return this.type;
    }

    public int getChosenChess() {
        return this.chosenChess;
    }

    public int getDice() {
        return this.dice;
    }
}
