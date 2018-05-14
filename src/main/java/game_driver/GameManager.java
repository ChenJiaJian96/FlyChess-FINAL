package game_driver;
import com.unity3d.flyingchess.Action;
import ai.AutoAI;
import ai.BasicAI;
import chess.Chess;
import chess.Pair;
import ai.PlayerAI;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by gitfan on 3/29/18.
 */

//普通模式的游戏
public class GameManager{

    protected BasicAI player[];
    protected Chess chessboard[];
    protected boolean waitingdice;//还在等待扔骰子
    protected boolean waitingchoice;//还在等待选择
    public boolean waitingEndturn; // 结束本轮回合

    protected int dice;//当前的骰子
    protected int turn;//当前轮到谁？
    protected int choice;//当前的选择是什么?

    protected int players_cnt = -1;//玩家人数(包括AI)
    protected int finish_cnt = 0;//目前完成的人数

    public static int AI_KIND = 0;//AI类型
    public static int PLAYER_KIND = 1;//玩家类型
    public static int NOT_USE_KIND = 2;//没有使用
    private static int FINISHED = 3; //已经完成

    protected int pos_status[];    //位置状态
    protected String name[];

    Queue<Action> curr_action_list = new Queue<>();

    //history 格式:
    // 第一行:初始化棋盘的Action列表:隐藏无关棋子
    // 第二行:设置滚动条的名字:位置 + 名字,空格分开
    //其他:第一行:恢复为当前游戏需要的指令(需要在没有产生动作序列之前)
    //下一行:当前动作序列
    protected StringBuilder history;

    public void clear_manager()
    {
        for(int i = 0 ; i < 4; i++)
        {
            if(pos_status[i] != NOT_USE_KIND)
            {
                player[i].clear_ai();
            }
            player[i] = null;
        }
        player = null;
        for(int i = 0 ; i < 72; i++)
        {
            chessboard[i].clear_chess();
            chessboard[i] = null;
        }
        chessboard = null;
        for(int i = 0; i < 4; i++)
        {
            name[i]= null;
        }
        name = null;
        history.delete(0,history.length()-1);
        history = null;
    }
    //一个四个数组的元素初始化
    public GameManager(int [] _gamekind,String name[])
    {
        if(_gamekind.length != 4)
        {
            System.err.println("Error in GameManager(int [] pos_status)");
            System.exit(1);
        }

        init();

        this.name = name;
        for(int i = 0 ; i < 4; i++){
            pos_status[i] = _gamekind[i];
        }
    }
    //获取第一个玩游戏的玩家
    private int get_first_turn()
    {
        for(int i = 0; i < 4; i++)
        {
            if(pos_status[i] == AI_KIND || pos_status[i] == PLAYER_KIND)
            {
                return i;
            }
        }
        return -1;
    }

    //显示扔骰子
    public Action show_dice(){
        return new Action(0,0, Action.SHOW_DICE, 0);
    }

    //隐藏扔骰子
    public Action hide_dice(){
        return new Action(0,0, Action.HIDE_DICE, 0);
    }

    private void init()
    {
        player = new BasicAI[4];
        chessboard = new Chess[72];
        for(int i = 0 ; i < 72; i++)
        {
            chessboard[i] = new Chess(i);
        }

        waitingdice = true;
        waitingchoice = true;

        dice = -1;
        choice = -1;

        players_cnt = 0;
        finish_cnt = 0;

        pos_status = new int[4];

        history = new StringBuilder();

    }

    //初始化游戏：隐藏无关的棋子
    public Queue<Action> init_game()
    {
        long st_time = new Date().getTime();
        turn = get_first_turn();



        for(int i = 0 ; i < 4; i++){
            if(pos_status[i] == AI_KIND){
                player[i] = new AutoAI(i);
                players_cnt ++ ;
                player[i].set_start_time(st_time);
            }
            else if(pos_status[i] == PLAYER_KIND){
                player[i] = new PlayerAI(i);
                players_cnt ++;
                player[i].set_start_time(st_time);
            }
            if(pos_status[i] != NOT_USE_KIND)
            {
                player[i].setName(name[i]);
            }
        }

        //history 第一行:初始化游戏
        Queue<Action> queue = new Queue<>();
        Action action;
        //切换为游戏场景
        action = new Action(0,0, Action.HISTORY_MODE,1);
        queue.enqueue(action);

        action = new Action(0,0, Action.HISTORY_MODE,2);
        history.append(" " + action.toActionString());//加进历史记录

        int code = 0;
        for(int i = 0; i < 4; i++)
        {
            if(pos_status[i] == NOT_USE_KIND)
            {
                for(int j = 0 ; j < 4; j++)
                {
                    action = new Action(i,j, Action.HIDE,-1);
                    queue.enqueue(action);
                    history.append(" " + action.toActionString());
                }
                code = code*10 + 2;
            }
            else code = code*10 + 1;
        }
        action = new Action(code,0, Action.HIDE_SCROLL_BAR,0);
        queue.enqueue(action);
        history.append(" " + action.toActionString() + "\n");

        //history 第二行:设置滚动条的名字:位置 + 名字,空格分开

        for(int i = 0 ; i < 4; i ++)
        {
            if(pos_status[i] != NOT_USE_KIND)
            {
                history.append(" " + i + " " + player[i].getName());
            }
        }
        history.append("\n");

        return queue;
    }

    //返回下一次轮到的位置
    protected void nextTurn()
    {
        while(true)
        {
            turn = (turn + 1)%4;//加一
            if(pos_status[turn] != NOT_USE_KIND && pos_status[turn] != FINISHED) break;
        }
    }

    public boolean isGameOver(boolean muti_round)
    {
        if(muti_round)
        {
            for(int i = 0; i < 4; i++)
            {
                if(pos_status[i] != NOT_USE_KIND && pos_status[i] != FINISHED)
                {
                    if(player[i].isFinish()){

                        pos_status[i] = FINISHED;
                        finish_cnt += 1;
                        long ed_time = new Date().getTime();
                        player[i].set_end_time(ed_time);
                        player[i].set_score((players_cnt - finish_cnt +1 )*1000);

                        String str = "";

                        if(i == Chess.RED) str = "红色玩家";
                        else if(i == Chess.YELLOW) str = "黄色玩家";
                        else if(i == Chess.BLUE) str = "蓝色玩家";
                        else str = "绿色玩家";

                        System.out.println(str+"完成");
                    }
                }
            }

            if(finish_cnt + 1 == players_cnt)
            {
                for(int i = 0; i < 4; i++)
                {
                    if(pos_status[i] != NOT_USE_KIND && pos_status[i] != FINISHED)
                    {
                        player[i].set_score(1000);
                        player[i].set_end_time(new Date().getTime());
                        pos_status[i] = FINISHED;
                        break;
                    }
                }
                return true;
            }
            return false;
        }
        else
        {
            for(int i = 0; i < 4; i++)
            {
                if(pos_status[i] != NOT_USE_KIND)
                {
                    if(player[i].isFinish()){
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public Queue<Action> recover_game_aciton()
    {
        Queue<Action> queue = new  Queue<>();
        for(int i = 0; i < 4;i++)
        {
            if(player[i] != null)
            {
                for(Action action : player[i].to_recover_action())
                {
                    queue.enqueue(action);
                }
            }
        }
        return queue;
    }
    //恢复棋盘需要的一系列动作
    protected String recover_game()
    {
        StringBuilder str = new StringBuilder();
        for(int i = 0; i < 4;i++)
        {
            if(player[i] != null)
            {
                str.append(player[i].to_recover());
            }
        }
        str.append("\n");//换行符
        return str.toString();
    }
    //提供给扔骰子的按钮，按按钮时调用这个函数
    public void setDice(int dice){

        if(dice <= 0 || dice > 6){
            System.err.println("dice out of range in GameManager: setDice(int dice)");
            //System.exit(0);
            return;
        }

        waitingdice = false;//玩家已经投掷骰子了
        waitingEndturn = true;//正在等待本轮回合结束
        this.dice = dice;
        //扔完骰子后开始等待玩家选择棋子
        this.waitingchoice = true;

        //历史
        history.append(recover_game());//第一行:恢复为当前游戏需要的指令
        Action action = new Action(-1,-1, Action.THROW_DICE_BY_NUMBER,dice);
        history.append(" " + action.toActionString()+"\n");//第二行:当前动作序列
    }
    //提供给点击棋子的按钮，玩家点击棋子时调用这个函数
    //用来设置当前选择的棋子,如果没有棋子可以选择请使用setChoice(-1)
    public void setChoice(int choice)
    {
        //扔了骰子才可以进行选择
        if(waitingdice){
            System.err.println("unexpected error in GameManager: setChoice(int choice)");
            //System.exit(0);
            return;
        }

        waitingchoice = false;//取消等待选择的标记
        this.choice = choice;

        //为下一轮游戏做准备
        waitingdice = true;
    }

    //设置本轮回合结束
    public void setEndthisturn()
    {
        waitingEndturn = false;
    }

    //检测骰子是否已经准备好
    public boolean waitDice()
    {
        return waitingdice;
    }

    //检测玩家是否已经选择棋子
    public boolean waitChoice()
    {
        return waitingchoice;
    }

    //检测本轮次是否已经结束
    public boolean waitEndTurn(){return waitingEndturn;}



    //返回当前玩家可以选择的棋子
    //前提：扔了骰子才可以调用
    //如果返回的queue的size为0，那么没有棋子可以选择
    public Queue<Integer> getChessAvailable()
    {
        if(waitingdice){
            System.err.println("Unexpected error in GameManager: getChessAvailable(int playerid)");
            return null;
            //System.exit(0);
        }
        PlayerAI playerAI = (PlayerAI) player[getTurn()];
        return playerAI.available_choice(dice);
    }

    //返回当前玩家可以选择的棋子(已翻译成一系列指令集)
    //前提：扔了骰子才可以调用
    //如果返回的queue的size为0，那么没有棋子可以选择
    public Queue<Action> getChessAvailable_Action()
    {
        if(waitingdice){
            System.err.println("Unexpected error in GameManager: getChessAvailable(int playerid)");
            return null;
            //System.exit(0);
        }
        PlayerAI playerAI = (PlayerAI) player[getTurn()];
        Queue<Integer> choices = playerAI.available_choice(dice);
        Queue<Action> actionlist = new Queue<Action>();
        for(Integer choose:choices)
        {
            actionlist.enqueue(new Action(getTurn(),choose, Action.ACTIVATE));
        }
        return actionlist;
    }

    //0 -  3 的数字
    public boolean isChoiceAvailable(int choice)
    {
        for(Integer i:getChessAvailable())
        {
            if(choice == i) return true;
        }
        return false;
    }

    //设置玩家为挂机模式
    public void switchToAI(int playerid)
    {
        //只有玩家模式才可以转换位挂机
        if(player[playerid].getKind() != BasicAI.PEOPLE)
        {
            System.err.println("Unexpected error in GameManager: switchToAI(int playerid)");
            System.exit(0);
        }

        PlayerAI playerAI = (PlayerAI) player[playerid];
        playerAI.switchToAI();
        player[playerid] = playerAI;
    }

    //从挂机模式中恢复
    public void switchToUser(int playerid)
    {
        //只有玩家AI模式才可以恢复为玩家模式，全自动AI不可以切换为玩家模式
        if(player[playerid].getKind() != BasicAI.PLAYERAI)
        {
            System.err.println("Unexpected error in GameManager: switchToUser(int playerid)");
            System.exit(0);
        }

        PlayerAI playerAI = (PlayerAI) player[playerid];
        playerAI.switchToUser();
        player[playerid] = playerAI;
    }

    //AI自己选择棋子
    public int getAIChoice()
    {
        //只有扔了骰子AI才可以自动选择
        if(waitingdice){
            System.err.println("Unexpected error in GameManager: getAIChoice(int playerid)");
            System.exit(0);
        }
        return player[getTurn()].ai_choice(dice,chessboard);
    }

    //联机部分可能用到
    //主要提供给UI界面，获取现在轮到谁玩游戏
    //UI那里可以根据这个函数确定轮到谁玩游戏
    //然后设置相关的界面（比如扔骰子，只有轮到的人才出现那个可以出现扔骰子的按钮）
    public int getTurn()
    {
        return turn;
    }

    public Action getTurn_Action()
    {
        history.append(recover_game());

        Action action = new Action(getTurn(),0, Action.SHOWTURN,0);

        history.append(" " + action.toActionString() + "\n");
        return action;
    }

    //设置开始游戏的第一个玩家
    public void setTurn(int color)
    {
        if(color < 0 || color >= 4){
            System.err.println("color out of range in GameManager: setTurn(int color)");
            System.exit(1);
        }
        if(pos_status[color] == NOT_USE_KIND || pos_status[color] == FINISHED){
            System.err.println("undefined color(player) in GameManager: setTurn(int color)");
            System.exit(1);
        }
        turn = color;
    }

    //判断现在是不是AI在玩游戏
    //还是提供给UI界面，主要是给UI界面用来确定扔骰子时是自动扔，还是等人点击才扔?
    public boolean isAI()
    {
        return (player[getTurn()].getKind() == BasicAI.AUTOAI || player[getTurn()].getKind() == BasicAI.PLAYERAI );
    }
    public boolean isAI(int playid)
    {
        //没加范围检测，应该不会有问题吧...
        return (player[playid].getKind() == BasicAI.AUTOAI || player[playid].getKind() == BasicAI.PLAYERAI );
    }


    private static final int RIGHT = 1;
    private static final int LEFT = 2;
    private static final int NOTURN = 3;

    //转向测试
    private static int turnTest(int prePos,int afterPos)
    {
        if((prePos == 0 || (prePos <= 51 && prePos > 47 ) ) && afterPos > 1 && afterPos <= 6) return RIGHT;
        if(prePos <= 4 && afterPos >= 5) return LEFT;
        if(prePos < 8 && afterPos > 8) return RIGHT;
        if(prePos < 14 && afterPos > 14) return RIGHT;
        if(prePos <= 17 && afterPos >= 18) return LEFT;
        if(prePos < 21 && afterPos > 21) return RIGHT;
        if(prePos < 27 && afterPos > 27) return RIGHT;
        if(prePos <= 30 && afterPos >= 31) return LEFT;
        if(prePos < 34 && afterPos > 34) return RIGHT;
        if(prePos < 40 && afterPos > 40) return RIGHT;
        if(prePos <= 43 && afterPos >= 44) return LEFT;
        if(prePos < 47 && afterPos > 47) return RIGHT;
        return NOTURN;
    }
    private static boolean turnLeftTest(int prePos,int afterPos)
    {
        if(prePos <= 4 && afterPos >= 5) return true;
        if(prePos <= 17 && afterPos >= 18) return true;
        if(prePos <= 30 && afterPos >= 31) return true;
        if(prePos <= 43 && afterPos >= 44) return true;
        return false;
    }
    private static boolean turnRightTest(int prePos,int afterPos)
    {
        if((prePos == 0 || (prePos <= 51 && prePos > 47 ) ) && afterPos > 1 && afterPos <= 4) return true;
        if(prePos < 8 && afterPos > 8) return true;
        if(prePos < 14 && afterPos > 14) return true;
        if(prePos < 21 && afterPos > 21) return true;
        if(prePos < 27 && afterPos > 27) return true;
        if(prePos < 34 && afterPos > 34) return true;
        if(prePos < 40 && afterPos > 40) return true;
        if(prePos < 47 && afterPos > 47) return true;
        return false;
    }
    private static int getRightCorner(int prePos,int afterPos)
    {
        if(prePos < 8 && afterPos > 8) return 8;
        if(prePos < 14 && afterPos > 14) return 14;
        if(prePos < 21 && afterPos > 21) return 21;
        if(prePos < 27 && afterPos > 27) return 27;
        if(prePos < 34 && afterPos > 34) return 34;
        if(prePos < 40 && afterPos > 40) return 40;
        if(prePos < 47 && afterPos > 47) return 47;
        if((prePos == 0 || (prePos <= 51 && prePos >47 )) && afterPos > 1) return 1;
        return -1;
    }
    private static int getLeftCorner(int prePos,int afterPos)
    {
        if(prePos <= 4 && afterPos >= 5) return 5;
        if(prePos <= 17 && afterPos >= 18) return 18;
        if(prePos <= 30 && afterPos >= 31) return 31;
        if(prePos <= 43 && afterPos >= 44) return 44;
        return -1;
    }
    private static boolean isLeftCorner(int pos)
    {
        if(pos == 5 || pos == 18 || pos == 31 || pos == 44) return true;
        return false;
    }
    private static boolean isRightCorner(int pos)
    {
        if(pos == 8 || pos == 14 || pos == 21 || pos == 27 || pos == 34 || pos == 40 || pos == 47 || pos == 1) return true;
        return false;
    }

    //显示排行榜
    public Action show_ranking_list()
    {
        return new Action(0,0, Action.SHOW_RANKING_LIST);
    }

    //获取排行榜
    public String get_ranking_list_by_time_with_name()
    {
        BasicAI[] ai_list = new BasicAI[4];
        int list_cnt = 0;
        for(int i = 0; i < 4;i++)
        {
            if(pos_status[i] != NOT_USE_KIND){
                ai_list[list_cnt++] = new BasicAI(player[i]);
            }
        }

        Arrays.sort(ai_list,0,list_cnt);

        StringBuilder str = new StringBuilder();

        str.append(1 + " " + ai_list[0].get_total_score());

        for(int i = 1 ; i < list_cnt;i++)
        {
            str.append(" " + (i+1) + " " +ai_list[i].get_total_score());
        }
        for(int i = list_cnt; i < 4; i++)
        {
            str.append(" "+(i+1) +" -1 default_name 0 0 0");
        }

        return str.toString();
    }

    //用户选完棋子后，产生的一系列动作
    //在指令集后面加了结束回合指令
    public Queue<Action> actionlist()
    {
        //历史
        history.append(recover_game());//第一行:恢复为当前游戏需要的指令(需要在没有产生动作序列之前)

        pre_actionlist();
        Action action = new Action(0,0, Action.ENDTHISTURN);
        curr_action_list.enqueue(action);


        for(Action act:curr_action_list)
        {
            history.append(" " + act.toActionString());//第二行:当前动作序列
        }
        history.append("\n");


        return curr_action_list;
    }

    //获得历史记录
    public String getHistory()
    {
        return history.toString();
    }

    public String get_file_name()
    {
        //用时间_胜利者_人数 作为文件名: eg:   2018-04-19-23:18_gitfan_4
        Date date = new Date();
        String time_str = String.format("%tF",date);
        time_str += String.format("-%tR",date);

        int score = -1000 , idx = -1;
        for(int i = 0 ; i < 4 ; i++)
        {
            if(pos_status[i] != NOT_USE_KIND)
            {
                if(player[i].score() > score)
                {
                    score = player[i].score();
                    idx = i;
                }
            }
        }
        time_str += "_" + player[idx].getName() + "_" + players_cnt;
        return time_str;
    }
    //用户选完棋子后，产生的一系列动作
    //没有结束回合指令
    private Queue<Action> pre_actionlist()
    {
        //只有选了棋子才能发生动作
        if(waitingchoice)
        {
            System.err.println("Unexpected error in GameManager: actionlist()");
            System.exit(0);
        }
        curr_action_list.clear();
        Action action;
        Chess chess;
        int playerid = getTurn();
        int chessindex = choice;
        int currPos = -1, leftStep = -1,turnKind = NOTURN,currStep = -1;
        if(chessindex < 0)
        {
            nextTurn();
            return  curr_action_list;
        }
        else
        {
            chess = new Chess(player[playerid].getChess(chessindex));
            //位于停机坪
            if(chess.getStatus() == Chess.STATUS_AIRPORT)
            {
                if(dice >= 5 && dice <= 6)
                {
                    chess.setStatus(Chess.STATUS_STARTLINE);
                    chess.setPos(Chess.ORIGINALPOS[chess.getColor()]);
                    chess.clearIndexList();
                    chess.insertToIndexList(new Pair(playerid,chessindex));

                    //记得更新棋盘或者玩家的棋子(自己的棋子或者他人的棋子)

                    player[playerid].setChess(chessindex,chess);//更新玩家棋子，当前棋盘无影响，无需更新棋盘

                    action = new Action(playerid,chessindex, Action.MOVE_TO_STARTLINE);
                    curr_action_list.enqueue(action);

                    if(dice == 5){
                        nextTurn(); //轮到下一个人
                        return curr_action_list;
                    }
                    else
                    {
                        return curr_action_list;//不用改变tern，还是这个人
                    }
                }
                else
                {
                    nextTurn();
                    return  curr_action_list;
                }
            }
            //位于起飞点
            //finish
            else if(chess.getStatus() == Chess.STATUS_STARTLINE)
            {
                currPos = chess.getPos();
                chess.setStatus(Chess.STATUS_FLYING);
                chess.setPos((chess.getPos() + dice) % 52);
                leftStep = dice;

                if(!chess.eatTest(chessboard[chess.getPos()]))
                {
//                    //初步移动
//                    action = new Action(playerid,chessindex,Action.NORMAL_MOVE,dice);
//                    queue.enqueue(action);

                    action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                    curr_action_list.enqueue(action);
                    currPos = (currPos + 1)%52;
                    leftStep -= 1;

                    action = new Action(playerid,chessindex, Action.TURNLEFT);
                    curr_action_list.enqueue(action);

                    if(leftStep != 0)
                    {
                        //需要左转
                        if(leftStep >= 4)
                        {
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,4);
                            curr_action_list.enqueue(action);
                            action = new Action(playerid,chessindex, Action.TURNLEFT);
                            curr_action_list.enqueue(action);
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep - 3);
                            curr_action_list.enqueue(action);
                            currPos = (currPos + leftStep) % 52;
                            leftStep = 0;
                        }
                        //直接走
                        else
                        {
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                            curr_action_list.enqueue(action);
                            currPos = (currPos + leftStep)%52;
                            leftStep -= leftStep;

                        }
                    }
                }

                //可以和自己人合体
                //记得更新自己的棋子和棋盘棋子
                if(chess.mergeTest(chessboard[chess.getPos()]))
                {
                    for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                    {
                        //插入自己的棋子列表
                        chess.insertToIndexList(pair);
                        //记得隐藏其他的棋子
                        player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_HIDING);
                        action = new Action(pair.playerId,pair.chessId, Action.HIDE);
                        curr_action_list.enqueue(action);
                    }
                    //更新棋盘
                    chessboard[chess.getPos()] = new Chess(chess);
                    //更新自己的棋子
                    player[playerid].setChess(chessindex,chess);
                }
                //可以吃掉其他玩家
                //记得更新别的玩家的棋子，自己的棋子，以及棋盘
                else if(chess.eatTest(chessboard[chess.getPos()]))
                {
//                    action = new Action(playerid,chessindex,Action.NORMAL_MOVE,dice - 1);
//                    if(dice - 1 != 0) queue.enqueue(action);

                    boolean lefflag = false;

                    if(dice - 1 != 0)
                    {
                        leftStep = dice -1;
                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                        curr_action_list.enqueue(action);
                        currPos = (currPos + 1)%52;
                        leftStep -= 1;

                        //左转
                        action = new Action(playerid,chessindex, Action.TURNLEFT);
                        curr_action_list.enqueue(action);

                        if(leftStep != 0)
                        {
                            //需要左转
                            if(leftStep >= 4)
                            {
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,4);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.TURNLEFT);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep - 3);
                                curr_action_list.enqueue(action);
                                currPos = (currPos + leftStep) % 52;
                                leftStep = 0;
                            }
                            else if(leftStep == 3)
                            {
                                lefflag = true;
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep + 1);
                                curr_action_list.enqueue(action);
                                currPos = (currPos + leftStep)%52;
                                leftStep -= leftStep;
                                action = new Action(playerid,chessindex, Action.TURNLEFT);
                                curr_action_list.enqueue(action);

                            }
                            else
                            {
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                                curr_action_list.enqueue(action);
                                currPos = (currPos + leftStep)%52;
                                leftStep -= leftStep;
                            }
                        }
                    }


                    for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                    {
                        //坠落，回到停机坪
                        player[pair.playerId].chesslist[pair.chessId].setFallen();
                        action = new Action(pair.playerId,pair.chessId, Action.FALLEN);
                        curr_action_list.enqueue(action);
                    }

//                    action = new Action(playerid,chessindex,Action.NORMAL_MOVE,1);
//                    queue.enqueue(action);

                    leftStep = 1;
                    turnKind = turnTest(currPos,(currPos + 1)%52);
                    //需要左转
                    if(turnKind == LEFT)
                    {
                        if(lefflag)
                        {
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                            curr_action_list.enqueue(action);
                            leftStep -= 1;
                            currPos = (currPos + 1)%52;
                        }
                        else
                        {
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                            curr_action_list.enqueue(action);
                            action = new Action(playerid,chessindex, Action.TURNLEFT);
                            curr_action_list.enqueue(action);
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                            curr_action_list.enqueue(action);
                            leftStep -= 1;
                            currPos = (currPos + 1)%52;
                        }

                    }
                    else
                    {
                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                        curr_action_list.enqueue(action);
                        if(isRightCorner((currPos + 1)%52))
                        {
                            action = new Action(playerid,chessindex, Action.TURNLEFT);
                            curr_action_list.enqueue(action);
                        }
                        leftStep = 0;
                        currPos = (currPos + 1)%52;
                    }

                    //更新棋盘
                    chessboard[chess.getPos()] = new Chess(chess);
                    //更新自己的棋子
                    player[playerid].setChess(chessindex,chess);
                }
                //很普通的一步，没有合并或者吃掉，但是有可能是跳步
                //记得更新自己的棋子和棋盘
                else
                {
                    //更新棋盘
                    chessboard[chess.getPos()] = new Chess(chess);
                    //更新自己的棋子
                    player[playerid].setChess(chessindex,chess);
                }

                //正常步
                if(!chess.isLucky())
                {
                    if(dice != 6) nextTurn();
                    return curr_action_list;
                }
                //跳步！！！
                //位于起飞点的棋子不可能会有飞步
                else
                {
                    //有可能合并或者吃掉，但不能再连跳

                    //跳之后记得清空跳之前的位置的棋盘

                    //清空之前的棋子
                    chessboard[chess.getPos()].setStatus(Chess.STATUS_EMPTY);
                    chessboard[chess.getPos()].clearIndexList();

                    currPos = chess.getPos();
                    chess.setPos((chess.getPos() + 4)%52);
                    leftStep = 4;

                    if(!chess.eatTest(chessboard[chess.getPos()]))
                    {
                        //移动
//                        action = new Action(playerid,chessindex,Action.QUICK_MOVE,4);
//                        queue.enqueue(action);
                        turnKind = turnTest(currPos,(currPos + leftStep)%52);
                        if(turnKind == LEFT)
                        {
                            //quickmove很烦啊，还要判断下是不是偶数，不是偶数需要分解
                            if((getLeftCorner(currPos,currPos + leftStep) - currPos)%2 == 0)
                            {
                                action = new Action(playerid,chessindex, Action.QUICK_MOVE,getLeftCorner(currPos,currPos + leftStep) - currPos);
                                curr_action_list.enqueue(action);
                            }
                            else
                            {
                                action = new Action(playerid,chessindex, Action.QUICK_MOVE,getLeftCorner(currPos,currPos + leftStep) - currPos -1);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                curr_action_list.enqueue(action);
                            }
                            leftStep -= (getLeftCorner(currPos,currPos + leftStep) - currPos - 1);
                            action = new Action(playerid,chessindex, Action.TURNLEFT);
                            curr_action_list.enqueue(action);
                            //warning:quick move without check
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                            curr_action_list.enqueue(action);
                            leftStep = 0;
                            currPos = (currPos + 4)%52;
                        }
                        else if(turnKind == RIGHT)
                        {
                            if((getRightCorner(currPos,currPos + leftStep) - currPos)%2 == 0)
                            {
                                action = new Action(playerid,chessindex, Action.QUICK_MOVE,getRightCorner(currPos,currPos + leftStep) - currPos);
                                curr_action_list.enqueue(action);
                            }
                            else
                            {
                                action = new Action(playerid,chessindex, Action.QUICK_MOVE,getRightCorner(currPos,currPos + leftStep) - currPos -1);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                curr_action_list.enqueue(action);
                            }
                            leftStep -= (getRightCorner(currPos,currPos + leftStep) - currPos);
                            action = new Action(playerid,chessindex, Action.TURNRIGHT);
                            curr_action_list.enqueue(action);
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                            curr_action_list.enqueue(action);
                            leftStep -= leftStep;
                            currPos = (currPos + 4)%52;
                        }
                        else
                        {
                            //移动
                            action = new Action(playerid,chessindex, Action.QUICK_MOVE,leftStep);
                            curr_action_list.enqueue(action);
                            if(isRightCorner((currPos + leftStep)%52))
                            {
                                action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                curr_action_list.enqueue(action);
                            }
                            leftStep -= leftStep;
                            currPos = (currPos + leftStep)%52;
                        }
                    }


                    //可以和自己人合体
                    //记得更新自己的棋子和棋盘棋子
                    if(chess.mergeTest(chessboard[chess.getPos()]))
                    {
                        for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                        {
                            //插入自己的棋子列表
                            chess.insertToIndexList(pair);
                            //记得隐藏其他的棋子
                            player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_HIDING);
                            action = new Action(pair.playerId,pair.chessId, Action.HIDE);
                            curr_action_list.enqueue(action);
                        }
                        //更新棋盘
                        chessboard[chess.getPos()] = new Chess(chess);
                        //更新自己的棋子
                        player[playerid].setChess(chessindex,chess);
                    }
                    //可以吃掉其他玩家
                    //记得更新别的玩家的棋子，自己的棋子，以及棋盘
                    else if(chess.eatTest(chessboard[chess.getPos()]))
                    {
//                        action = new Action(playerid,chessindex,Action.QUICK_MOVE,2);
//                        queue.enqueue(action);
//                        action = new Action(playerid,chessindex,Action.NORMAL_MOVE,1);
//                        queue.enqueue(action);

                        leftStep = 2;
                        action = new Action(playerid,chessindex, Action.QUICK_MOVE,2);
                        curr_action_list.enqueue(action);
                        if(isRightCorner(currPos + leftStep))
                        {
                            action = new Action(playerid,chessindex, Action.TURNRIGHT);
                            curr_action_list.enqueue(action);
                        }

                        leftStep = 1;
                        currPos = currPos + 2;

                        turnKind = turnTest(currPos,currPos + 1);

                        if(turnKind == LEFT)
                        {
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                            curr_action_list.enqueue(action);
                            action = new Action(playerid,chessindex, Action.TURNLEFT);
                            curr_action_list.enqueue(action);
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                            curr_action_list.enqueue(action);
                        }
                        else
                        {
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                            curr_action_list.enqueue(action);
                        }

                        leftStep = 0;
                        currPos = currPos + 1;

                        for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                        {
                            //坠落，回到停机坪
                            player[pair.playerId].chesslist[pair.chessId].setFallen();
                            action = new Action(pair.playerId,pair.chessId, Action.FALLEN);
                            curr_action_list.enqueue(action);
                        }
                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                        curr_action_list.enqueue(action);
                        leftStep = 0;
                        currPos = currPos + 1;

                        //更新棋盘
                        chessboard[chess.getPos()] = new Chess(chess);
                        //更新自己的棋子
                        player[playerid].setChess(chessindex,chess);
                    }
                    //很普通的一步，没有合并或者吃掉，但是有可能是跳步
                    //记得更新自己的棋子和棋盘
                    else
                    {
                        //更新棋盘
                        chessboard[chess.getPos()] = new Chess(chess);
                        //更新自己的棋子
                        player[playerid].setChess(chessindex,chess);
                    }

                    if(dice != 6) nextTurn();
                    return curr_action_list;
                }

            }
            //位于飞行途中
            //注意！！！棋子还没有正式移动！！！！
            else if(chess.getStatus() == Chess.STATUS_FLYING)
            {

                //是否接近终点线？？？
                //finish
                if(chess.presprint(dice))
                {
                    //是否可以合并自己人？？
                    //是否可以直接到达终点？？

                    //记得清除原来的棋盘的位置

                    chessboard[chess.getPos()].setStatus(Chess.STATUS_EMPTY);
                    chessboard[chess.getPos()].clearIndexList();

                    int lastposition = chess.getPos();
                    currPos = chess.getPos();



                    chess.setEndLine(dice);

//                    //移动
//                    action = new Action(playerid,chessindex,Action.NORMAL_MOVE,dice);
//                    queue.enqueue(action);

                    if(lastposition != chess.getEntry())
                    {
//                        action = new Action(playerid,chessindex,Action.NORMAL_MOVE,chess.getEntry() - lastposition);
//                        queue.enqueue(action);

                        leftStep = chess.getEntry() - lastposition;

                        turnKind = turnTest(currPos,(currPos + leftStep)%52) ;

                        if(turnKind == RIGHT)
                        {
                            currStep = getRightCorner(currPos,(currPos+leftStep)%52) - currPos;
                            if(currStep < 0) currStep += 52;
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                            curr_action_list.enqueue(action);
                            action = new Action(playerid,chessindex, Action.TURNRIGHT);
                            curr_action_list.enqueue(action);
                            leftStep -= currStep;
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                            curr_action_list.enqueue(action);
                        }
                        else
                        {
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                            curr_action_list.enqueue(action);
                        }

                        action = new Action(playerid,chessindex, Action.TURNRIGHT);
                        curr_action_list.enqueue(action);
                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,dice - (chess.getEntry() - lastposition));
                        curr_action_list.enqueue(action);
                    }
                    //刚刚好在入口就不用转身了，直接走就可以了
                    else
                    {
                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,dice);
                        curr_action_list.enqueue(action);

                    }

                    if(chess.getStatus() == Chess.STATUS_FINISH)
                    {
                        for(Pair pair:chess.getIndexlist())
                        {
                            player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_FINISH);
                            action = new Action(pair.playerId,pair.chessId, Action.FINISHED);
                            curr_action_list.enqueue(action);
                        }
                    }
                    //还要考虑是否合并自己人
                    else
                    {
                        //可以和自己人合体
                        //记得更新自己的棋子和棋盘棋子
                        if(chess.mergeTest(chessboard[chess.getPos()]))
                        {
                            for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                            {
                                chess.insertToIndexList(pair);

                                player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_HIDING);
                                action = new Action(pair.playerId,pair.chessId, Action.HIDE);
                                curr_action_list.enqueue(action);
                            }
                            //更新棋盘
                            chessboard[chess.getPos()] = new Chess(chess);
                            //更新自己的棋子
                            player[playerid].setChess(chessindex,chess);
                        }
                        //记得更新自己的棋子和棋盘
                        else
                        {
                            //更新棋盘
                            chessboard[chess.getPos()] = new Chess(chess);
                            //更新自己的棋子
                            player[playerid].setChess(chessindex,chess);
                        }
                    }

                    if(dice != 6) nextTurn();
                    return curr_action_list;

                }
                //是否已经进入终点线
                //finish
                else if(chess.sprint())
                {

                    chessboard[chess.getPos()].setStatus(Chess.STATUS_EMPTY);
                    chessboard[chess.getPos()].clearIndexList();

                    int lastpos = chess.getPos();

                    //反弹~~~
                    boolean rebounded = chess.rebound(dice);


                    //记得添加action
                    //需要考虑直接到达终点,直接到达终点不用考虑反弹
                    //否则需要考虑反弹并且需要考虑是否合并

                    if(chess.getStatus() == Chess.STATUS_FINISH)
                    {
                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,dice);
                        curr_action_list.enqueue(action);
                        for(Pair pair:chess.getIndexlist())
                        {
                            player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_FINISH);
                            action = new Action(pair.playerId,pair.chessId, Action.FINISHED);
                            curr_action_list.enqueue(action);
                        }
                    }
                    else
                    {
                        //如果反弹
                        if(rebounded)
                        {
                            int endpoint = chess.endPoint();

                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,endpoint - lastpos);
                            curr_action_list.enqueue(action);
                            action = new Action(playerid,chessindex, Action.REVERSE);
                            curr_action_list.enqueue(action);
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,endpoint - chess.getPos());
                            curr_action_list.enqueue(action);
                            action = new Action(playerid,chessindex, Action.REVERSE);
                            curr_action_list.enqueue(action);
                        }
                        //否则
                        else
                        {
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,dice);
                            curr_action_list.enqueue(action);
                        }

                        //可以和自己人合体
                        //记得更新自己的棋子和棋盘棋子
                        if(chess.mergeTest(chessboard[chess.getPos()]))
                        {
                            for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                            {
                                chess.insertToIndexList(pair);

                                player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_HIDING);
                                action = new Action(pair.playerId,pair.chessId, Action.HIDE);
                                curr_action_list.enqueue(action);
                            }
                            //更新棋盘
                            chessboard[chess.getPos()] = new Chess(chess);
                            //更新自己的棋子
                            player[playerid].setChess(chessindex,chess);
                        }
                        //记得更新自己的棋子和棋盘
                        else
                        {
                            //更新棋盘
                            chessboard[chess.getPos()] = new Chess(chess);
                            //更新自己的棋子
                            player[playerid].setChess(chessindex,chess);
                        }
                    }

                    if(dice != 6) nextTurn();
                    return curr_action_list;
                }
                //普通线路
                else
                {
                    //先清除以前的棋盘
                    chessboard[chess.getPos()].setStatus(Chess.STATUS_EMPTY);
                    chessboard[chess.getPos()].clearIndexList();

                    currPos = chess.getPos();//50
                    leftStep = dice;//5

                    //基础移动
                    chess.setPos((chess.getPos() + dice) % 52);//3

                    if( !chess.eatTest(chessboard[chess.getPos()]))
                    {
//                        action = new Action(playerid,chessindex,Action.NORMAL_MOVE,dice);
//                        queue.enqueue(action);

                        turnKind = turnTest(currPos,(currPos + leftStep) % 52);

                        boolean changeturn = false;
                        boolean turnlef = false;
                        boolean turnrig = false;
                        boolean emptySpace = false;

                        //需要左转
                        if(turnKind == LEFT)
                        {

                            currStep = getLeftCorner(currPos,(currPos + leftStep) % 52) - currPos;// 2
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                            curr_action_list.enqueue(action);
                            action = new Action(playerid,chessindex, Action.TURNLEFT);
                            curr_action_list.enqueue(action);

                            leftStep -= (currStep - 1);//5
                            currPos = (currPos + currStep - 1)%52;//30
                            if(currStep -1 == 0) emptySpace = true;
                            changeturn = true;
                            turnlef = true;
                        }
                        //需要右转
                        else if(turnKind == RIGHT)
                        {

                            currStep = getRightCorner(currPos,(currPos + leftStep) % 52) - currPos;
                            if(currStep < 0) currStep += 52;
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                            curr_action_list.enqueue(action);
                            action = new Action(playerid,chessindex, Action.TURNRIGHT);
                            curr_action_list.enqueue(action);
                            leftStep -= currStep;
                            currPos = (currPos + currStep)%52;
                            changeturn = true;
                            turnrig = true;
                        }
                        else
                        {
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,dice);
                            curr_action_list.enqueue(action);
                            if(isRightCorner((currPos + dice)%52))
                            {
                                action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                curr_action_list.enqueue(action);
                            }
                            currPos = (currPos + dice)%52;
                            leftStep -= dice;
                            changeturn = false;
                        }

                        if(changeturn)
                        {
                            // 30 30+5
                            turnKind = turnTest(currPos,(currPos + leftStep) % 52);//LEFT

                            if(emptySpace){

                                if(turnRightTest(currPos,(currPos + leftStep) %52)){
                                    turnKind = RIGHT;
                                }
                            }
                            if(turnlef)
                            {
                                if(turnRightTest(currPos,(currPos + leftStep) %52)){
                                    turnKind = RIGHT;
                                }
                            }
                            if(turnrig)
                            {
                                if(turnLeftTest(currPos,(currPos + leftStep)%52))
                                {
                                    turnKind = LEFT;
                                }
                            }

                            //需要左转
                            if(turnKind == LEFT && !turnlef)
                            {
                                currStep = getLeftCorner(currPos,(currPos + leftStep) % 52) - currPos;
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.TURNLEFT);
                                curr_action_list.enqueue(action);
                                leftStep -= (currStep - 1);
                                currPos = (currPos + currStep - 1)%52;

                                currStep = leftStep;
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                                curr_action_list.enqueue(action);
                                leftStep -= leftStep;
                                currPos = (currPos + leftStep)%52;
                            }
                            //需要右转
                            else if(turnKind == RIGHT && !turnrig)
                            {
                                currStep = getRightCorner(currPos,(currPos + leftStep) % 52) - currPos;
                                if(currStep < 0) currStep += 52;
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                curr_action_list.enqueue(action);
                                leftStep -= currStep;
                                currPos = (currPos + currStep)%52;

                                currStep = leftStep;
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                                curr_action_list.enqueue(action);
                                leftStep -= leftStep;
                                currPos = (currPos + leftStep)%52;

                            }
                            else
                            {
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                                curr_action_list.enqueue(action);
                                if(isRightCorner((currPos + leftStep)%52))
                                {
                                    action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                    curr_action_list.enqueue(action);
                                }
                                currPos = (currPos + leftStep)%52;
                                leftStep -= leftStep;
                            }
                        }
                    }

                    //可以和自己人合体
                    //记得更新自己的棋子和棋盘棋子
                    if(chess.mergeTest(chessboard[chess.getPos()]))
                    {
                        for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                        {
                            //插入自己的棋子列表
                            chess.insertToIndexList(pair);
                            //记得隐藏其他的棋子
                            player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_HIDING);
                            action = new Action(pair.playerId,pair.chessId, Action.HIDE);
                            curr_action_list.enqueue(action);
                        }
                        //更新棋盘
                        chessboard[chess.getPos()] = new Chess(chess);
                    }
                    //可以吃掉其他玩家
                    //记得更新别的玩家的棋子，自己的棋子，以及棋盘
                    else if(chess.eatTest(chessboard[chess.getPos()]))
                    {
                        boolean lefflag = false;
                        boolean changeturn = false;
                        boolean turnlef = false;
                        boolean turnrig = false;
                        boolean emptyspace = false;

                        leftStep = dice -1;//4
                        if(leftStep != 0)
                        {
//                            action = new Action(playerid,chessindex,Action.NORMAL_MOVE,dice - 1);
//                            queue.enqueue(action);
//5

                            // 0， 0+5=5
                            turnKind = turnTest(currPos,(currPos + leftStep)%52);

                            if(turnKind == LEFT)
                            {
                                currStep = getLeftCorner(currPos,(currPos + leftStep)%52) - currPos;//5 - 4 = 1
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.TURNLEFT);
                                curr_action_list.enqueue(action);

                                leftStep -= (currStep - 1);//1
                                currPos = (currPos + currStep - 1)%52;//4
                                if(currStep -1 == 0) emptyspace = true;
                                changeturn = true;
                                turnlef = true;
                            }
                            else if(turnKind == RIGHT)
                            {
                                currStep = getRightCorner(currPos,(currPos + leftStep) %52) - currPos;
                                if(currStep < 0) currStep += 52;

                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                curr_action_list.enqueue(action);

                                leftStep -= currStep;
                                currPos = (currPos + currStep)%52;
                                changeturn = true;
                                turnrig = true;

                                System.out.println("currpos: " + currPos +" leftstep: " + leftStep);
                            }
                            else if(isLeftCorner((currPos + dice)))
                            {
                                lefflag = true;
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep + 1);
                                curr_action_list.enqueue(action);
                                currPos = (currPos + currStep)%52;
                                leftStep -= leftStep;
                                action = new Action(playerid,chessindex, Action.TURNLEFT);
                                curr_action_list.enqueue(action);
                            }
                            else
                            {

                                currStep = leftStep;
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                                curr_action_list.enqueue(action);

                                if(isRightCorner((currPos + currStep)%52))
                                {
                                    action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                    curr_action_list.enqueue(action);
                                }

                                currPos = (currPos + currStep)%52;
                                leftStep -= currStep;
                                changeturn = false;
                            }
                        }

//                        if(turnlef)
//                        {
//                            action = new Action(playerid,chessindex,Action.NORMAL_MOVE,1);
//                            queue.enqueue(action);
//                            leftStep -= 1;
//                            currPos = (currPos + 1)%52;
//                        }

                        if(!lefflag)
                        {
                            //1 1+4
                            turnKind = turnTest(currPos,(currPos + leftStep)%52);

                            if(changeturn)
                            {
                                if(emptyspace)
                                {
                                    if(turnRightTest(currPos,(currPos+leftStep)%52)){
                                        turnKind = RIGHT;
                                    }
                                }
                            }
                            if(turnlef)
                            {
                                if(turnRightTest(currPos,(currPos + leftStep) %52)){
                                    turnKind = RIGHT;
                                }
                            }

                            if(turnrig)
                            {
                                if(turnLeftTest(currPos,(currPos + leftStep)%52))
                                {
                                    turnKind = LEFT;
                                }
                            }


                            //需要左转
                            if(turnKind == LEFT && !turnlef)
                            {
                                currStep = getLeftCorner(currPos,(currPos + leftStep) % 52) - currPos;
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.TURNLEFT);
                                curr_action_list.enqueue(action);
                                leftStep -= (currStep - 1);
                                currPos = (currPos + currStep - 1)%52;

                                currStep = leftStep;
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                                curr_action_list.enqueue(action);
                                leftStep -= leftStep;
                                currPos = (currPos + leftStep)%52;
                                turnlef = true;
                            }
                            //需要右转
                            else if(turnKind == RIGHT && !turnrig)
                            {
                                currStep = getRightCorner(currPos,(currPos + leftStep) % 52) - currPos;
                                if(currStep < 0) currStep += 52;
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                curr_action_list.enqueue(action);
                                leftStep -= currStep;
                                currPos = (currPos + currStep)%52;

                                currStep = leftStep;
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                                curr_action_list.enqueue(action);
                                leftStep -= leftStep;
                                currPos = (currPos + leftStep)%52;
                                turnrig = true;
                            }
                            else
                            {
                                if(leftStep != 0)
                                {
                                    action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                                    curr_action_list.enqueue(action);
                                }
                                if(isRightCorner((currPos + leftStep)%52))
                                {
                                    action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                    curr_action_list.enqueue(action);
                                }
                                currPos = (currPos + leftStep)%52;
                                leftStep -= leftStep;
                            }
                        }



                        for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                        {
                            //坠落，回到停机坪
                            player[pair.playerId].chesslist[pair.chessId].setFallen();
                            action = new Action(pair.playerId,pair.chessId, Action.FALLEN);
                            curr_action_list.enqueue(action);
                        }

                        // 4 5
                        turnKind = turnTest(currPos,(currPos + 1)%52);//LEFT
                        if(turnKind == LEFT)
                        {
                            if(lefflag)
                            {
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                curr_action_list.enqueue(action);
                                leftStep -= 1;
                                currPos = (currPos + 1)%52;
                            }
                            else if(turnlef)
                            {
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                curr_action_list.enqueue(action);
                                leftStep -= 1;
                                currPos = (currPos + 1)%52;
                            }
                            else
                            {
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.TURNLEFT);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                curr_action_list.enqueue(action);
                                leftStep -= 1;
                                currPos = (currPos + 1)%52;
                            }
                        }
                        else
                        {
                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                            curr_action_list.enqueue(action);
                            if(isRightCorner(currPos + 1))
                            {
                                action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                curr_action_list.enqueue(action);
                            }
                            leftStep -= 1;
                            currPos = (currPos + 1)%52;
                        }
                        //更新棋盘
                        chessboard[chess.getPos()] = new Chess(chess);
                    }
                    else
                    {
                        //更新棋盘
                        chessboard[chess.getPos()] = new Chess(chess);
                    }


                    //正常步
                    if(!chess.isLucky())
                    {
                        //更新棋盘
                        chessboard[chess.getPos()] = new Chess(chess);
                        //更新自己的棋子
                        player[playerid].setChess(chessindex,chess);

                    }
                    else
                    {
                        boolean flag = false;

                        //先特判一下是不是可以导致飞步的跳步；
                        if(chess.getPos() == chess.getPreFlyingPoint())
                        {
                            flag = true;//可以导致飞步的跳步

                            //先清除以前的棋盘
                            chessboard[chess.getPos()].setStatus(Chess.STATUS_EMPTY);
                            chessboard[chess.getPos()].clearIndexList();

                            //再移动
                            leftStep = 4;
                            currPos = chess.getPos();
                            chess.setPos((chess.getPos() + 4)%52);

                            if(!chess.eatTest(chessboard[chess.getPos()]))
                            {
                                action = new Action(playerid,chessindex, Action.QUICK_MOVE,4);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.TURNLEFT);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                curr_action_list.enqueue(action);
                            }

                            //可以和自己人合体
                            //记得更新自己的棋子和棋盘棋子
                            if(chess.mergeTest(chessboard[chess.getPos()]))
                            {
                                for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                                {
                                    //插入自己的棋子列表
                                    chess.insertToIndexList(pair);
                                    //记得隐藏其他的棋子
                                    player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_HIDING);
                                    action = new Action(pair.playerId,pair.chessId, Action.HIDE);
                                    curr_action_list.enqueue(action);
                                }
                                //更新棋盘
                                chessboard[chess.getPos()] = new Chess(chess);
                            }
                            //可以吃掉其他玩家
                            //记得更新别的玩家的棋子，自己的棋子，以及棋盘
                            else if(chess.eatTest(chessboard[chess.getPos()]))
                            {
                                action = new Action(playerid,chessindex, Action.QUICK_MOVE,4);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.TURNLEFT);
                                curr_action_list.enqueue(action);
                                for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                                {
                                    //坠落，回到停机坪
                                    player[pair.playerId].chesslist[pair.chessId].setFallen();
                                    action = new Action(pair.playerId,pair.chessId, Action.FALLEN);
                                    curr_action_list.enqueue(action);
                                }
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                curr_action_list.enqueue(action);
                                //更新棋盘
                                chessboard[chess.getPos()] = new Chess(chess);
                            }
                            else
                            {
                                //更新棋盘
                                chessboard[chess.getPos()] = new Chess(chess);
                            }
                        }

                        //要先先判断飞步，因为飞步也是跳步的一种
                        if(chess.isSuperLucky())
                        {
                            //飞步

                            //是否可以攻击别人？？？

                            //飞步前要先旋转！！！！
                            action = new Action(playerid,chessindex, Action.TURNRIGHT);
                            curr_action_list.enqueue(action);

                            int attackpos = chess.getAttackPos();

                            boolean isAttack = false;

                            if(chess.attackTest(chessboard[attackpos]))
                            {
                                action = new Action(playerid,chessindex, Action.QUICK_MOVE,2);
                                curr_action_list.enqueue(action);

                                //记得修改玩家的棋子和棋盘
                                for(Pair pair:chessboard[attackpos].getIndexlist())
                                {
                                    //坠落，回到停机坪
                                    player[pair.playerId].chesslist[pair.chessId].setFallen();
                                    action = new Action(pair.playerId,pair.chessId, Action.FALLEN);
                                    curr_action_list.enqueue(action);
                                }

                                //清空中间的棋盘
                                chessboard[attackpos].setStatus(Chess.STATUS_EMPTY);
                                chessboard[attackpos].clearIndexList();

                                Chess testchess = new Chess(chess);
                                testchess.setPos(testchess.getFlyingPoint());
                                if(!testchess.eatTest(chessboard[testchess.getPos()]))
                                {
                                    //踢完人继续走
                                    action = new Action(playerid,chessindex, Action.QUICK_MOVE,4);
                                    curr_action_list.enqueue(action);
                                }
                                isAttack = true;
                            }
                            else
                            {
                                Chess testchess = new Chess(chess);
                                testchess.setPos(testchess.getFlyingPoint());
                                if(!testchess.eatTest(chessboard[testchess.getPos()]))
                                {
                                    //直接飞过对面
                                    action = new Action(playerid,chessindex, Action.QUICK_MOVE,6);
                                    curr_action_list.enqueue(action);
                                }
                            }

                            //先清除棋子以前的棋盘
                            chessboard[chess.getPos()].setStatus(Chess.STATUS_EMPTY);
                            chessboard[chess.getPos()].clearIndexList();
                            //再前进
                            chess.setPos(chess.getFlyingPoint());

                            //有人吗？？自己人还是别人？

                            //合体
                            if(chess.mergeTest(chessboard[chess.getPos()]))
                            {
                                for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                                {
                                    //插入自己的棋子列表
                                    chess.insertToIndexList(pair);
                                    //记得隐藏其他的棋子
                                    player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_HIDING);
                                    action = new Action(pair.playerId,pair.chessId, Action.HIDE);
                                    curr_action_list.enqueue(action);
                                }
                            }
                            //吃掉
                            else if(chess.eatTest(chessboard[chess.getPos()]))
                            {
                                if(isAttack)
                                {
                                    //踢完人继续走
                                    action = new Action(playerid,chessindex, Action.QUICK_MOVE,2);
                                    curr_action_list.enqueue(action);
                                    action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                    curr_action_list.enqueue(action);
                                }
                                else
                                {
                                    //直接飞过对面
                                    action = new Action(playerid,chessindex, Action.QUICK_MOVE,4);
                                    curr_action_list.enqueue(action);
                                    action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                    curr_action_list.enqueue(action);
                                }
                                for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                                {
                                    //坠落，回到停机坪
                                    player[pair.playerId].chesslist[pair.chessId].setFallen();
                                    action = new Action(pair.playerId,pair.chessId, Action.FALLEN);
                                    curr_action_list.enqueue(action);
                                }
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                curr_action_list.enqueue(action);
                            }

                            //右转
                            action = new Action(playerid,chessindex, Action.TURNRIGHT);
                            curr_action_list.enqueue(action);



                            //更新棋盘
                            chessboard[chess.getPos()] = new Chess(chess);
                            //更新自己的棋子
                            player[playerid].setChess(chessindex,chess);

                            if(!flag)
                            {
                                action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                curr_action_list.enqueue(action);
                                action = new Action(playerid,chessindex, Action.TURNLEFT);
                                curr_action_list.enqueue(action);


                                //直接删除棋盘，因为还要继续跳
                                chessboard[chess.getPos()].setStatus(Chess.STATUS_EMPTY);
                                chessboard[chess.getPos()].clearIndexList();


                                //在走四步
                                //会有人吗？
                                //会是自己人吗，还是其他人

                                currPos = chess.getPos();
                                leftStep = 4;
                                chess.setPos((chess.getPos() + 4)%52);

                                if(!chess.eatTest(chessboard[chess.getPos()]))
                                {
                                    action = new Action(playerid,chessindex, Action.QUICK_MOVE,4);
                                    curr_action_list.enqueue(action);
                                    action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                    curr_action_list.enqueue(action);
                                }

                                //合体
                                if(chess.mergeTest(chessboard[chess.getPos()]))
                                {
                                    for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                                    {
                                        //插入自己的棋子列表
                                        chess.insertToIndexList(pair);
                                        //记得隐藏其他的棋子
                                        player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_HIDING);
                                        action = new Action(pair.playerId,pair.chessId, Action.HIDE);
                                        curr_action_list.enqueue(action);
                                    }
                                }
                                //吃掉
                                else if(chess.eatTest(chessboard[chess.getPos()]))
                                {
                                    action = new Action(playerid,chessindex, Action.QUICK_MOVE,2);
                                    curr_action_list.enqueue(action);
                                    action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                    curr_action_list.enqueue(action);
                                    for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                                    {
                                        //坠落，回到停机坪
                                        player[pair.playerId].chesslist[pair.chessId].setFallen();
                                        action = new Action(pair.playerId,pair.chessId, Action.FALLEN);
                                        curr_action_list.enqueue(action);
                                    }
                                    action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                    curr_action_list.enqueue(action);
                                    action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                    curr_action_list.enqueue(action);
                                }

                                //更新棋盘
                                chessboard[chess.getPos()] = new Chess(chess);
                                //更新自己的棋子
                                player[playerid].setChess(chessindex,chess);
                            }
                        }
                        else
                        {
                            //如果不在entry才可以跳步，否则就会超过entry了
                            if(chess.getPos() != chess.getEntry())
                            {
                                //普通跳步
                                chessboard[chess.getPos()].setStatus(Chess.STATUS_EMPTY);
                                chessboard[chess.getPos()].clearIndexList();

                                //前进
                                //前面有人吗？
                                //自己人还是别人？
                                currPos = chess.getPos();
                                leftStep = 4;
                                chess.setPos((chess.getPos() + 4)%52);

                                if(!chess.eatTest(chessboard[chess.getPos()]))
                                {
//                                    action = new Action(playerid,chessindex,Action.QUICK_MOVE,4);
//                                    queue.enqueue(action);

                                    turnKind = turnTest(currPos,(currPos + leftStep) %52);
                                    if(turnKind == LEFT)
                                    {
                                        //quickmove很烦啊，还要判断下是不是偶数，不是偶数需要分解
                                        if((getLeftCorner(currPos,currPos + leftStep) - currPos)%2 == 0)
                                        {
                                            action = new Action(playerid,chessindex, Action.QUICK_MOVE,getLeftCorner(currPos,currPos + leftStep) - currPos);
                                            curr_action_list.enqueue(action);
                                        }
                                        else
                                        {
                                            if(getLeftCorner(currPos,currPos + leftStep) - currPos -1 != 0)
                                            {
                                                action = new Action(playerid,chessindex, Action.QUICK_MOVE,getLeftCorner(currPos,currPos + leftStep) - currPos -1);
                                                curr_action_list.enqueue(action);
                                            }
                                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                            curr_action_list.enqueue(action);
                                        }
                                        leftStep -= (getLeftCorner(currPos,currPos + leftStep) - currPos - 1);
                                        action = new Action(playerid,chessindex, Action.TURNLEFT);
                                        curr_action_list.enqueue(action);
                                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                                        curr_action_list.enqueue(action);
                                        leftStep = 0;
                                        currPos = (currPos + 4)%52;
                                        if(isRightCorner(currPos))
                                        {
                                            action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                            curr_action_list.enqueue(action);
                                        }
                                    }
                                    else if(turnKind == RIGHT)
                                    {
                                        currStep = getRightCorner(currPos,(currPos + leftStep)%52) - currPos;
                                        if(currStep < 0) currStep += 52;

                                        if(currStep%2 == 0)
                                        {
                                            action = new Action(playerid,chessindex, Action.QUICK_MOVE,currStep);
                                            curr_action_list.enqueue(action);
                                        }
                                        else
                                        {
                                            action = new Action(playerid,chessindex, Action.QUICK_MOVE,currStep - 1);
                                            if(currStep - 1 != 0) curr_action_list.enqueue(action);
                                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                            curr_action_list.enqueue(action);
                                        }
                                        leftStep -= currStep;

                                        action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                        curr_action_list.enqueue(action);
                                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                                        curr_action_list.enqueue(action);
                                        leftStep -= leftStep;
                                        currPos = (currPos + 4)%52;

                                    }
                                    else
                                    {
                                        //移动
                                        action = new Action(playerid,chessindex, Action.QUICK_MOVE,4);
                                        curr_action_list.enqueue(action);
                                        if(isRightCorner((currPos + leftStep)%52))
                                        {
                                            action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                            curr_action_list.enqueue(action);
                                        }
                                        leftStep -= leftStep;
                                        currPos = (currPos + 4)%52;
                                    }
                                }

                                //合体
                                if(chess.mergeTest(chessboard[chess.getPos()]))
                                {
                                    for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                                    {
                                        //插入自己的棋子列表
                                        chess.insertToIndexList(pair);
                                        //记得隐藏其他的棋子
                                        player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_HIDING);
                                        action = new Action(pair.playerId,pair.chessId, Action.HIDE);
                                        curr_action_list.enqueue(action);
                                    }
                                }
                                //吃掉
                                else if(chess.eatTest(chessboard[chess.getPos()]))
                                {
//                                    action = new Action(playerid,chessindex,Action.QUICK_MOVE,2);
//                                    queue.enqueue(action);
//                                    action = new Action(playerid,chessindex,Action.NORMAL_MOVE,1);
//                                    queue.enqueue(action);

                                    leftStep = 3;
                                    turnKind = turnTest(currPos,(currPos + leftStep)%52);

                                    //需要左转
                                    if(turnKind == LEFT)
                                    {
                                        currStep = getLeftCorner(currPos,(currPos + leftStep)%52) - currPos;
                                        //fucking code，又要分解一波，心态是如何炸的
                                        if(currStep % 2 == 0)
                                        {
                                            action = new Action(playerid,chessindex, Action.QUICK_MOVE,currStep);
                                            curr_action_list.enqueue(action);
                                        }
                                        else
                                        {
                                            if(currStep - 1 != 0)
                                            {
                                                action = new Action(playerid,chessindex, Action.QUICK_MOVE,currStep - 1);
                                                curr_action_list.enqueue(action);
                                            }
                                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                            curr_action_list.enqueue(action);
                                        }

                                        action = new Action(playerid,chessindex, Action.TURNLEFT);
                                        curr_action_list.enqueue(action);

                                        leftStep -= (currStep - 1 );
                                        currPos = (currPos + currStep - 1)%52;

                                        currStep = leftStep;
                                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,leftStep);
                                        curr_action_list.enqueue(action);
                                        leftStep -= leftStep;
                                        currPos = (currPos + currStep)%52;
                                    }
                                    else if(turnKind == RIGHT)
                                    {
                                        currStep = getRightCorner(currPos,(currPos + leftStep)%52) - currPos;
                                        if(currStep < 0) currStep += 52;
                                        if(currStep %2 == 0)
                                        {
                                            action = new Action(playerid,chessindex, Action.QUICK_MOVE,currStep);
                                            curr_action_list.enqueue(action);
                                        }
                                        else
                                        {
                                            if(currStep - 1 != 0)
                                            {
                                                action = new Action(playerid,chessindex, Action.QUICK_MOVE,currStep - 1);
                                                curr_action_list.enqueue(action);
                                            }
                                            action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                            curr_action_list.enqueue(action);
                                        }

                                        currPos = (currPos + currStep) %52;
                                        leftStep -= currStep;

                                        action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                        curr_action_list.enqueue(action);
                                        currStep = leftStep;
                                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                                        curr_action_list.enqueue(action);
                                        leftStep -= leftStep;
                                        currPos = (currPos + currStep)%52;
                                    }
                                    else
                                    {
                                        action = new Action(playerid,chessindex, Action.QUICK_MOVE,2);
                                        curr_action_list.enqueue(action);
                                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                        curr_action_list.enqueue(action);
                                        currPos = (currPos + 3)%52;
                                        leftStep -= 3;
                                        if(isRightCorner(currPos))
                                        {
                                            action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                            curr_action_list.enqueue(action);
                                        }
                                    }

                                    for(Pair pair:chessboard[chess.getPos()].getIndexlist())
                                    {
                                        //坠落，回到停机坪
                                        player[pair.playerId].chesslist[pair.chessId].setFallen();
                                        action = new Action(pair.playerId,pair.chessId, Action.FALLEN);
                                        curr_action_list.enqueue(action);
                                    }
//                                    action = new Action(playerid,chessindex,Action.NORMAL_MOVE,1);
//                                    queue.enqueue(action);

                                    leftStep = 1;
                                    turnKind = turnTest(currPos,(currPos + leftStep)%52) - currPos;
                                    if(turnKind == LEFT)
                                    {
                                        currStep = getLeftCorner(currPos,(currPos + leftStep)%52) - currPos;
                                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                                        if(currStep != 0) curr_action_list.enqueue(action);

                                        leftStep -= (currStep -1);
                                        currPos = (currPos + currStep -1)%52;
                                        action = new Action(playerid,chessindex, Action.TURNLEFT);
                                        curr_action_list.enqueue(action);

                                        currStep = leftStep;
                                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                                        if(currStep != 0) curr_action_list.enqueue(action);

                                        currPos = (currPos + currStep)%52;
                                        leftStep -= currStep;
                                    }
                                    else if(turnKind == RIGHT)
                                    {
                                        currStep = getRightCorner(currPos,(currPos + leftStep)%52) - currPos;
                                        if(currStep < 0) currStep += 52;
                                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                                        if(currStep != 0) curr_action_list.enqueue(action);

                                        currPos = (currPos + currStep)%52;
                                        leftStep -= currStep;

                                        currStep = leftStep;
                                        action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                        curr_action_list.enqueue(action);

                                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,currStep);
                                        if(currStep != 0) curr_action_list.enqueue(action);

                                        currPos = (currPos + currStep)%52;
                                        leftStep -= currStep;
                                    }
                                    else
                                    {
                                        action = new Action(playerid,chessindex, Action.NORMAL_MOVE,1);
                                        curr_action_list.enqueue(action);
                                        currPos = (currPos + 1)%52;
                                        if(isRightCorner(currPos))
                                        {
                                            action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                            curr_action_list.enqueue(action);
                                        }
                                    }
                                }
                            }

                            //刚好到达入口，需要右转
                            if(chess.getPos() == chess.getEntry())
                            {
                                action = new Action(playerid,chessindex, Action.TURNRIGHT);
                                curr_action_list.enqueue(action);
                            }

                            player[playerid].chesslist[chessindex] = new Chess(chess);
                            chessboard[chess.getPos()] = new Chess(chess);
                        }
                    }
                    if(dice != 6) nextTurn();

                    return curr_action_list;
                }
            }
            else
            {
                if(dice != 6) nextTurn();
                return curr_action_list;
            }
        }
    }
    public static void main(String[] args) throws InterruptedException, IOException {

        int kind[] = {GameManager.AI_KIND, GameManager.NOT_USE_KIND, GameManager.NOT_USE_KIND, GameManager.AI_KIND};
        String name[] = {"gitfan","hello","word","bye"};

        GameManager gameManager = new GameManager(kind,name);

        String str;
//
//
//
//        for (Action action:gameManager.init_game())
//        {
//            System.out.println(action);
//        }
//
//        gameManager.nextTurn();
//        if(gameManager.getTurn() == Chess.RED) str = "红色玩家";
//        else if(gameManager.getTurn() == Chess.YELLOW) str = "黄色玩家";
//        else if(gameManager.getTurn() == Chess.BLUE) str = "蓝色玩家";
//        else str = "绿色玩家";
//
//        System.out.println("现在轮到" + str);
//        System.out.println("throwing a dice...");
//
//        int dice1 = 5;
//        int choice1 = 0;
//
//        Chess chess = new Chess(new Pair(Chess.RED,0),Chess.RED);
//        chess.setStatus(Chess.STATUS_FLYING);
//        chess.setPos(3);
//        gameManager.chessboard[3] = new Chess(chess);
//        gameManager.player[Chess.RED].chesslist[0] = new Chess(chess);
//
//        chess = new Chess(new Pair(Chess.GREEN,0),Chess.GREEN);
//        chess.setStatus(Chess.STATUS_FLYING);
//        chess.setPos(50);
//        gameManager.chessboard[50] = new Chess(chess);
//        gameManager.player[Chess.GREEN].chesslist[0] = new Chess(chess);
//
//        chess = new Chess(new Pair(Chess.BLUE,1),Chess.BLUE);
//        chess.setStatus(Chess.STATUS_FLYING);
//        chess.setPos(18);
//        gameManager.chessboard[18] = new Chess(chess);
//        gameManager.player[Chess.BLUE].chesslist[1] = new Chess(chess);
//
//        chess = new Chess(new Pair(Chess.BLUE,0),Chess.BLUE);
//        chess.setStatus(Chess.STATUS_FLYING);
//        chess.setPos(64);
//        gameManager.chessboard[64] = new Chess(chess);
//        gameManager.player[Chess.BLUE].chesslist[0] = new Chess(chess);
//
//
//        chess = new Chess(new Pair(Chess.YELLOW,2),Chess.YELLOW);
//        chess.setStatus(Chess.STATUS_FLYING);
//        chess.setPos(34);
//        gameManager.chessboard[34] = new Chess(chess);
//        gameManager.player[Chess.YELLOW].chesslist[2] = new Chess(chess);

//        System.out.println("dice: " + dice1);
//
//        gameManager.setDice(dice1);
//
//        System.out.println("selecting a choice...");
//
//        gameManager.setChoice(choice1);
//
//        System.out.println("choice: " + choice1);
//
//        Queue<Action> actions1 = gameManager.actionlist();
//        for(Action action:actions1)
//        {
//            System.out.println(action);
//        }


//        /**********************************************************************/
//
        System.out.println(gameManager.init_game());
        while(!gameManager.isGameOver(true))
        {
            if(gameManager.getTurn() == Chess.RED) str = "红色玩家";
            else if(gameManager.getTurn() == Chess.YELLOW) str = "黄色玩家";
            else if(gameManager.getTurn() == Chess.BLUE) str = "蓝色玩家";
            else str = "绿色玩家";

            System.out.println("现在轮到" + str);

            //System.out.println("throwing a dice...");
//            Thread.sleep(2);

            int dice = ((int)(Math.random()*1000000))%6 + 1;

            //System.out.println("dice: " + dice);

            gameManager.setDice(dice);

            //System.out.println("selecting a choice...");

            //Thread.sleep(10);

            int choice = gameManager.getAIChoice();

            gameManager.setChoice(choice);

            //System.out.println("choice: " + choice);


            Queue<Action> actions = gameManager.actionlist();

            for(Action action:actions)
            {
                //System.out.println(action);
            }
            //Thread.sleep(10);
        }
//
        System.out.print(gameManager.getHistory());
//
//        //System.out.println(gameManager.show_ranking_list());
//        //System.out.println(gameManager.get_ranking_list_by_time());
//        //System.out.println(gameManager.get_ranking_list_by_time_with_name());
//
//        String curDir = System.getProperty("user.dir");
//        //System.out.println("你当前的工作目录为 :" + curDir);
    }
}