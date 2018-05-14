package ai;

import com.unity3d.flyingchess.Action;
import chess.Chess;
import chess.Pair;
import com.unity3d.flyingchess.Prop;

import game_driver.Queue;

/**
 * Created by gitfan on 3/26/18.
 */

//基础AI
public class BasicAI implements Comparable<BasicAI>{

    public int kind,color;
    public static int PEOPLE = 1,PLAYERAI = 2, AUTOAI = 3;//玩家模式，玩家挂机模式（可转换为玩家模式），全自动AI（无法转换为玩家模式）

    public Chess chesslist[];

    private String name;//玩家名

    private long st_time ;
    private long ed_time ;

    private int _score; //分数

    /***************************************************/
    //多玩模式....

    //当前玩家能够使用主动技能
    public boolean cd_condition()
    {
        if(color == Chess.RED)
        {
            //小火龙喷火必须是位于飞行途中的棋子
            for(int i = 0 ; i < 4; i ++)
            {
                if(chesslist[i].getStatus() == Chess.STATUS_FLYING) return true;
            }
            return false;
        }
        //待补充...
        return false;
    }
    //使用cd时的可选对象
    public Queue<Integer> cd_available_chess()
    {
        Queue<Integer> queue = new Queue<>();
        if(color == Chess.RED)
        {
            for(int i = 0 ; i < 4; i ++)
            {
                if(chesslist[i].getStatus() == Chess.STATUS_FLYING) queue.enqueue(i);
            }
        }
        //其他的待补充
        //.................

        return queue;
    }

    //使用cd得到的分数...
    //选择某个棋子获取的分数
    private static int cd_score(Chess choice,int dice, Chess chessboard[])
    {
        int cdScore = 0;
        if(choice.getColor() == Chess.RED)
        {
            //小火龙可以喷火攻击前面的3个人
            for(int i = 1 ; i <= 3; i ++)
            {
                if(choice.rangedAttack(chessboard[(choice.getPos() + i)%52], Prop.RED_RANGE)) cdScore += 1500;
            }
            return cdScore;
        }

        //其他的待补充
        //.................

        return cdScore;
    }



    /***************************************************/

    public void clear_ai()
    {
        for(int i = 0 ; i < 4; i++)
        {
            chesslist[i].clear_chess();
            chesslist[i] = null;
        }
        chesslist = null;
        name = null;
    }

    //复制拷贝函数
    public BasicAI(BasicAI that)
    {
        this.kind = that.kind;
        this.color = that.color;
        this.chesslist = new Chess[that.chesslist.length];
        for(int i = 0 ; i < chesslist.length; i++)
        {
            chesslist[i] = new Chess(that.chesslist[i]);
        }

        name = that.name;

        this.st_time = that.st_time;
        this.ed_time = that.ed_time;
        this._score = that._score;
    }


    //color 和 turn 一一对应
    public BasicAI(int kind,int color){

        if(illegalKind(kind)){
            System.err.println("illegal kind in BasicAI: BasicAI(int kind,int color)");
            System.exit(0);
        }
        else this.kind = kind;

        setTurn(color);
        chesslist = new Chess[4];
        for(int i = 0;i < 4 ; i++){
            chesslist[i] = new Chess(new Pair(color,i),color);
        }

        if(color == Chess.RED) name = "红色玩家";
        else if(color == Chess.YELLOW) name = "黄色玩家";
        else if(color == Chess.BLUE) name = "蓝色玩家";
        else name = "绿色玩家";
    }
    //color 和 turn 一一对应
    public BasicAI(int kind,int color,String name){

        if(illegalKind(kind)){
            System.err.println("illegal kind in BasicAI: BasicAI(int kind,int color)");
            System.exit(0);
        }
        else this.kind = kind;

        setTurn(color);
        chesslist = new Chess[4];
        for(int i = 0;i < 4 ; i++){
            chesslist[i] = new Chess(new Pair(color,i),color);
        }

        this.name = name;
    }


    //为玩家设置名字
    public void setName(String name)
    {
        this.name = name;
    }

    //设置类型
    private void setTurn(int turn)
    {
        if(turn < 0 || turn >= 4){
            System.err.print("unexpected value in BasicAi,setTurn(int turn)");
            System.exit(0);
        }
        this.color = turn;
    }

    //设置时间起点
    public void set_start_time(long st_time)
    {
        this.st_time = st_time;
    }

    //设置时间终点
    public void set_end_time(long ed_time)
    {
        this.ed_time = ed_time;
    }

    public void set_score(int score)
    {
        this._score = score;
    }

    //设置棋子
    public void setChess(int index, Chess chess)
    {
        if(illegalIndex(index)){
            System.err.print("index out of range in BasicAI: setChess(int index,Chess chess)");
            System.exit(0);
        }
        if(chess == null){
            System.err.print("chess in null in BasicAI:setChess(int index,Chess chess)");
            System.exit(0);
        }
        this.chesslist[index] = chess;
    }
    //获得棋子
    public Chess getChess(int index)
    {
        if(illegalIndex(index)){
            System.err.print("index out of range in BasicAI: getChess(int index,Chess chess)");
            System.exit(0);
        }
        return this.chesslist[index];
    }
    //我的所有棋子都完成了吗？
    public boolean isFinish()
    {
        for(int i = 0; i < 4; i++){
            if(chesslist[i].getStatus() != Chess.STATUS_FINISH) return false;
        }
        return true;
    }

    public int getKind()
    {
        return this.kind;
    }

    public int getColor(){
        return color;
    }

    //获取名字
    public String getName(){return name;}

    //走到终点的棋子数
    private int get_finish_cnt()
    {
        int cnt = 0;
        for(int i = 0; i < 4; i++){
            if(chesslist[i].getStatus() == Chess.STATUS_FINISH)
            {
                cnt ++;
            }
        }
        return cnt;
    }

    //形式: 颜色 名字 分数 完成数 时间
    public String get_total_score()
    {
        long ms = ed_time - st_time;

        long minute_ = ms/60000;
        long second_ = (ms/1000)%60;

        String minute = "";
        String second = "";

        if(minute_ < 10) minute ="0" + Long.toString(minute_);
        else minute = Long.toString(minute_);

        if(second_ < 10) second = "0" + Long.toString(second_);
        else second = Long.toString(second_);

        return (getColor() +" " + getName() + " " + _score + " "  + get_finish_cnt() + " " + minute +":" + second);
    }

    @Override
    public int compareTo(BasicAI o) {
        if(_score < o._score) return 1;
        else if(_score > o._score) return -1;
        else return 0;
    }

    public int score()
    {
        return _score;
    }

    //获取可直接移动的棋子
    protected Queue<Integer> getAvailableMove()
    {
        Queue<Integer> queue = new Queue<>();
        for(int i = 0;i < 4;i++)
        {
            if(chesslist[i].canMove()) queue.enqueue(i);
        }
        return queue;
    }

    //AI自动获取分数最高的棋子选择
    public int ai_choice(int dice, Chess chessboard[])
    {
        Queue<Integer> queue = getAvailableMove();
        if(queue.isEmpty())
        {
            if(dice <= 4) return  -1;
            for (int i = 0; i < 4 ;i++){
                if(chesslist[i].getStatus() == Chess.STATUS_AIRPORT) return i;
            }
        }
        else
        {
            int choice = queue.dequeue();
            int maxval = score(chesslist[choice],dice,chessboard);
            while(!queue.isEmpty()){
                int curr = queue.dequeue();
                int currval = score(chesslist[curr],dice,chessboard);
                if(maxval < currval){
                    choice = curr;
                    maxval = currval;
                }
            }
            if(dice == 5 || dice == 6)
            {
                if(maxval < 1000)
                {
                    for (int i = 0; i < 4 ;i++){
                        if(chesslist[i].getStatus() == Chess.STATUS_AIRPORT) return i;
                    }
                }
            }
            return choice;
        }
        return -1;
    }

    //范围检测
    private static boolean illegalIndex(int idx)
    {
        if(idx < 0 || idx >= 4)
        {
            return true;
        }
        return false;
    }

    private static boolean illegalKind(int kind)
    {
        if(kind != BasicAI.AUTOAI && kind != BasicAI.PLAYERAI && kind != BasicAI.PEOPLE){
            return true;
        }
        return false;
    }
    //选择某个棋子获取的分数
    private static int score(Chess chess, int dice, Chess chessboard[])
    {
        if (chess.sprint())
        {
            if(chess.testGoal(dice)) return 10000;
            else if(54 + chess.getColor()*5 == chess.getPos() && dice != 6) return 500;
            else if(54 + chess.getColor()*5 == chess.getPos() && dice == 6) return 0;
            else return 50;
        }
        else
        {
            if(chess.entry())
            {
                if (dice == 6) return 10000;
                else if(dice == 3) return 4000;
                else return 5000;
            }
            else if(chess.presprint(dice)) return 5000;
            else if(chess.eatTest(chessboard[(chess.getPos() + dice)%52])){
                return (chessboard[(chess.getPos() + dice)%52].getIndexlist().size()+chess.getIndexlist().size())*1500;
            }
            else if(chess.mergeTest(chessboard[(chess.getPos() + dice)%52])){
                return (chessboard[(chess.getPos() + dice)%52].getIndexlist().size()+chess.getIndexlist().size())*1000;
            }
            else
            {
                Chess tmp = new Chess(chess);
                tmp.setPos((chess.getPos() + dice)%52);
                if(tmp.isSuperLucky()) return 3000;
                else if(tmp.isLucky()) return 2000;
                else
                {
                    if(chess.getColor() == Chess.RED) return (chess.getPos()+dice)*40;
                    else if(chess.getColor() == Chess.YELLOW)
                    {
                        if(chess.getPos() + dice >= 14 && chess.getPos() + dice <=51) return (52-(64-chess.getPos()-dice))*40;
                        else return (52-(11 - (chess.getPos()+dice)%52))*40;
                    }
                    else if(chess.getColor() == Chess.BLUE)
                    {
                        if(chess.getPos() + dice >= 27 && chess.getPos() + dice <=51) return (52-(77-chess.getPos()-dice))*40;
                        else return (52-(24 - (chess.getPos()+dice)%52))*40;
                    }
                    else
                    {
                        if(chess.getPos() + dice >= 40 && chess.getPos() + dice <=51) return (52-(90-chess.getPos()-dice))*40;
                        else return (52-(37 - (chess.getPos()+dice)%52))*40;
                    }
                }
            }
        }
    }
    //恢复棋盘需要的动作
    public Queue<Action> to_recover_action()
    {
    	Queue<Action> queue = new Queue<>();
    	Action action;
        for(int i = 0; i < 4; i++)
        {
            if(chesslist[i].getStatus() == Chess.STATUS_AIRPORT)
            {
                action = new Action(color,i,Action.FADE_START,0);
                queue.enqueue(action);
                action = new Action(color,i, Action.FALLEN_INSTANT,0);
                queue.enqueue(action);
            }
            else if(chesslist[i].getStatus() == Chess.STATUS_STARTLINE)
            {
                action = new Action(color,i,Action.FADE_START,0);
                queue.enqueue(action);
                action = new Action(color,i, Action.FALLEN_INSTANT,0);
                queue.enqueue(action);
                action = new Action(color,i, Action.MOVE_TO_STARTLINE_INSTANT,0);
                queue.enqueue(action);
            }
            else if(chesslist[i].getStatus() == Chess.STATUS_FLYING)
            {
                action = new Action(color,i,Action.FADE_START,0);
                queue.enqueue(action);
                action = new Action(color,i, Action.RECOVER_CHESS_BY_POS,chesslist[i].getPos());
                queue.enqueue(action);
            }
            else if(chesslist[i].getStatus() == Chess.STATUS_FINISH)
            {
                action = new Action(color,i, Action.FINISHED,0);
                queue.enqueue(action);
            }
            else if(chesslist[i].getStatus() == Chess.STATUS_HIDING)
            {
                action = new Action(color,i,Action.FADE_START,0);
                queue.enqueue(action);
                action = new Action(color,i, Action.HIDE,0);
                queue.enqueue(action);
            }
        }
        return queue;
    }
    
    //要恢复为当前的玩家的四个棋子的状态,需要的动作
    public String to_recover()
    {
        StringBuilder str = new StringBuilder();
        Action action;
        for(int i = 0; i < 4; i++)
        {
            //5种棋子状态:飞机砰，起飞线，飞行中途,已完成，隐藏状态(多个棋子合体时用到)
            //public static int STATUS_AIRPORT = 0,STATUS_STARTLINE = 1 ,STATUS_FLYING = 2,STATUS_FINISH = 3,STATUS_HIDING = 4;
            if(chesslist[i].getStatus() == Chess.STATUS_AIRPORT)
            {
                action = new Action(color,i,Action.FADE_START,0);
                str.append(" " + action.toActionString());
                action = new Action(color,i, Action.FALLEN_INSTANT,0);
                str.append(" " + action.toActionString());
            }
            else if(chesslist[i].getStatus() == Chess.STATUS_STARTLINE)
            {
                action = new Action(color,i,Action.FADE_START,0);
                str.append(" " + action.toActionString());
                action = new Action(color,i, Action.FALLEN_INSTANT,0);
                str.append(" " + action.toActionString());
                action = new Action(color,i, Action.MOVE_TO_STARTLINE_INSTANT,0);
                str.append(" " + action.toActionString());
            }
            else if(chesslist[i].getStatus() == Chess.STATUS_FLYING)
            {
                action = new Action(color,i,Action.FADE_START,0);
                str.append(" " + action.toActionString());
                action = new Action(color,i, Action.RECOVER_CHESS_BY_POS,chesslist[i].getPos());
                str.append(" " + action.toActionString());
            }
            else if(chesslist[i].getStatus() == Chess.STATUS_FINISH)
            {
                action = new Action(color,i, Action.FINISHED,0);
                str.append(" " + action.toActionString());
            }
            else if(chesslist[i].getStatus() == Chess.STATUS_HIDING)
            {
                action = new Action(color,i,Action.FADE_START,0);
                str.append(" " + action.toActionString());
                action = new Action(color,i, Action.HIDE,0);
                str.append(" " + action.toActionString());
            }
        }
        return str.toString();
    }
}
