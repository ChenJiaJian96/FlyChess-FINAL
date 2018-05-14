package game_driver;
import com.unity3d.flyingchess.Action;
import chess.Chess;
import chess.Pair;
import ai.PlayerAI;
import com.unity3d.flyingchess.Prop;

/**
 * Created by gitfan on 5/11/18.
 */

//道具模式的游戏
public class PropGameManager extends GameManager{

    private static int CD [];//玩家主动技能
    private static int PLAYER_CD_TURN[];//技能冷却需要的回合数
    public static final int PROP_CAPACITY = 2;//每个玩家的道具袋子容量
    private static final int MAX_PROPS = 8;//场上能产生的最多的道具数目
    static
    {
        CD = new int[4];
        PLAYER_CD_TURN = new int[4];
        CD[Chess.RED] = Prop.RED_CD;
        CD[Chess.YELLOW] = Prop.YELLOW_CD;
        CD[Chess.BLUE] = Prop.BLUE_CD;
        CD[Chess.GREEN] = Prop.GREEN_CD;
        PLAYER_CD_TURN[Chess.RED] = 2;
        PLAYER_CD_TURN[Chess.YELLOW] = 2;
        PLAYER_CD_TURN[Chess.BLUE] = 2;
        PLAYER_CD_TURN[Chess.GREEN] = 2;
    }

    private Prop propboard[];//棋盘的道具
    private Queue<Prop> props;//每轮产生新的道具

    private int [][] player_props;//玩家的道具包
    private int curr_prop [] ;//玩家当前使用的道具, 0 - 3

    private int player_cd_cnt[];//玩家使用技能后,冷却技能的累计回合

    public PropGameManager(int [] _gamekind,String name[])
    {
        super(_gamekind,name);

        propboard = new Prop[52];//道具只在0-51位置产生
        for(int i = 0 ; i < 52; i ++)
        {
            propboard[i] = new Prop(i,Prop.NO_PROP);
        }
        props = new Queue<>();
        player_props = new int[4][];
        curr_prop = new int[4];
        player_cd_cnt = new int[4];
        for(int i = 0 ; i < 4; i++)
        {
            player_props[i] = new int[PROP_CAPACITY];
            for(int j = 0 ; j < PROP_CAPACITY; j ++)
            {
                player_props[i][j] = Prop.NO_PROP;
            }
            curr_prop[i] = Prop.NO_PROP;
            player_cd_cnt[i] = PLAYER_CD_TURN[i];//开局的时候都可以使用主动技能
        }
    }
    //在某个位置随机产生道具
    private int randomPos()
    {
        int pos;
        while(true)
        {
            pos = ((int)(Math.random()*100000004))%52;
            if(chessboard[pos].getStatus() == Chess.STATUS_EMPTY && propboard[pos].getProp() == Prop.NO_PROP)
            {
                return pos;
            }
        }
    }
    private void randomProp()
    {
        props.clear();
        int possible = (int)(Math.random()*100000000)%5; // 1/5的概率生成新的道具
        if(possible == 1)
        {
            int curr_proo_cnt = 0;
            for(int i = 0 ; i < 52; i ++)
            {
                if(propboard[i].getProp() != Prop.NO_PROP) curr_proo_cnt ++;
            }
            if(curr_proo_cnt >= MAX_PROPS) return ;

            int number = (int)(Math.random()*100000000)%3 + 1;//随机生成 1-3 个道具
            if(curr_proo_cnt + number > MAX_PROPS) number = MAX_PROPS;
            for(int i = 0 ; i < number; i ++)
            {
                Prop prop = new Prop(randomPos(),Prop.randomKind());
                props.enqueue(prop);
                propboard[prop.getPos()] = prop;//更新棋盘
            }
        }
    }
    //每轮游戏开始棋盘获取新的道具发给客户端
    public Queue<Prop> getProps()
    {
        curr_prop[getTurn()] = Prop.NO_PROP;//每轮游戏开始的时候清空该玩家的上次的选择
        player_cd_cnt[getTurn()] ++ ;//每轮游戏开始都恢复一点点cd技能
        randomPos();
        return props;
    }
    //为某个玩家添加道具
    private int addProp(int playerid,int prop)
    {
        int idx= -1;
        for(int i = 0; i < PROP_CAPACITY; i ++)
        {
            if(player_props[playerid][i] == Prop.NO_PROP)
            {
                player_props[playerid][i] = prop;
                idx = i;
                break;
            }
        }
        return idx;
    }
    //玩家使用了被动道具
    private boolean useProp(int playerid,int pos)
    {
        int prop = player_props[playerid][pos];
        if(prop == Prop.NO_PROP) return false;
        player_props[playerid][pos] = Prop.NO_PROP;
        curr_prop[getTurn()] = prop;
        return true;
    }
    //判断当前玩家是否可以使用技能,开局的时候可以发送给玩家
    //ps:还要达到cd使用条件
    public boolean cd_test()
    {
        if(getTurn() == Chess.RED)
        {
            return (player_cd_cnt[Chess.RED] >= PLAYER_CD_TURN[getTurn()]) && player[Chess.RED].cd_condition();
        }
        //其他的待补充...
        return false;
    }
    //跳过扔骰子的阶段
    private void skipDice()
    {
        waitingdice = false;//玩家已经投掷骰子了
        waitingEndturn = true;//正在等待本轮回合结束
        this.dice = dice;
        this.waitingchoice = true;        //扔完骰子后开始等待玩家选择棋子
    }
    //玩家使用了主动技能(返回true表示需要跳过扔骰子的阶段)
    private boolean useCD()
    {
        //小火龙使用主动技能后不能扔骰子
        if(getTurn() == Chess.RED)
        {
            skipDice();
            curr_prop[Chess.RED] = Prop.RED_CD;
            player_cd_cnt[Chess.RED] = 0;
            return true;
        }
        //.............................
        //其他的技能待补充...

        return false;
    }

    private void exec_cd()
    {
        Action action;
        //红色小火龙主动技能
        if(getTurn() == Chess.RED)
        {
            //红色小火龙的技能需要选择棋子
            if(waitingchoice)
            {
                System.err.println("unexpected error in exec_cd()f:red");
                System.exit(0);
            }
            Chess chess = player[getTurn()].getChess(choice);
            Chess tmpchess;
            action = new Action(getTurn(),choice,Action.FIRE_ATTACK);
            curr_action_list.enqueue(action);
            //小火龙可以吃掉前面的三个人(如果有人)
            for(int i = 1; i <= 3; i++)
            {
                tmpchess = chessboard[(chess.getPos() + i)%52];
                if(chess.rangedAttack(tmpchess, Prop.RED_RANGE))
                {
                    for(Pair pair:tmpchess.getIndexlist())
                    {
                        player[pair.playerId].chesslist[pair.chessId].setFallen();
                        action = new Action(pair.playerId,pair.chessId,Action.FALLEN);
                        curr_action_list.enqueue(action);
                    }
                    tmpchess.clear_chess();
                }
            }
            //小火龙使用技能后直接轮到下一个人
            nextTurn();
        }
        //其他的待补充...
    }
    private void exec_ariport()
    {
        Chess chess = player[getTurn()].getChess(choice);
        if((dice == 5) || (dice == 6))
        {
            chess.setStatus(Chess.STATUS_STARTLINE);
            chess.setPos(Chess.ORIGINALPOS[chess.getColor()]);
            chess.clearIndexList();
            chess.insertToIndexList(new Pair(getTurn(),choice));

            Action action = new Action(getTurn(),choice, Action.MOVE_TO_STARTLINE);
            curr_action_list.enqueue(action);
        }
    }

    //返回true表示走玩这步要停止
    private boolean exec_single_move()
    {
        Chess chess = player[getTurn()].getChess(choice);
        Action action;
        if(chess.isLeftCorner())
        {
            action = new Action(getTurn(),choice,Action.NORMAL_MOVE,1);
            curr_action_list.enqueue(action);
            action = new Action(getTurn(),choice,Action.TURNLEFT);
            curr_action_list.enqueue(action);
        }

        chess.setPos((chess.getPos() + 1)%52);
        action = new Action(getTurn(),choice,Action.NORMAL_MOVE,1);
        curr_action_list.enqueue(action);

        int prop_board = propboard[chess.getPos()].getProp();

        //中间终止类道具,获取类道具
        if(chess.getPos() >= 0 && chess.getPos() <= 51)
        {
            //如果是中间终止类道具
            if(prop_board == Prop.BARRIER)
            {
                //消去道具(中间类,获取类才需要消去道具,终点类不需要消去道具)...
                action = new Action(chess.getPos(),prop_board,Action.HIDE_PROP);
                curr_action_list.enqueue(action);
                propboard[chess.getPos()].setProp(Prop.NO_PROP);

                //如果是类似保护罩之类的东西
                if(curr_prop[getTurn()] == Prop.PROTECTION)
                {
                    return false;
                }
                else return true;
            }
            //如果是拾取类的东西
            else if(prop_board == Prop.PROTECTION)
            {
                //消去道具(中间类,获取类才需要消去道具,终点类不需要消去道具)...
                action = new Action(chess.getPos(),prop_board,Action.HIDE_PROP);
                curr_action_list.enqueue(action);
                propboard[chess.getPos()].setProp(Prop.NO_PROP);

                //获取道具
                int pos = addProp(getTurn(),prop_board);
                //格式(pos,prop,action,kind),kind=0为暗色,kind=1为亮色
                if(pos != -1)
                {
                    action = new Action(pos,prop_board,Action.ADD_PROP,0);
                    curr_action_list.enqueue(action);
                }
            }
        }

        if(chess.getStatus() == Chess.STATUS_STARTLINE)
        {
            chess.setStatus(Chess.STATUS_FLYING);
            action = new Action(getTurn(),choice,Action.TURNLEFT);
            curr_action_list.enqueue(action);
        }
        else if(chess.isRightCorner())
        {
            action = new Action(getTurn(),choice,Action.TURNRIGHT);
            curr_action_list.enqueue(action);
        }
        else if(chess.entry())
        {
            action = new Action(getTurn(),choice,Action.TURNRIGHT);
            curr_action_list.enqueue(action);
        }
        return false;
    }
    //最后一步
    private boolean exec_last_move()
    {
        System.out.println("fucking");
        //终点类道具,中间类道具,拾取类道具,吃棋子,合并,(以及在起点时,吃棋子时的特殊情况)
        Chess chess = player[getTurn()].chesslist[choice];
        Action action;
        if(chess.isLeftCorner())
        {
            action = new Action(getTurn(),choice,Action.NORMAL_MOVE,1);
            curr_action_list.enqueue(action);
            action = new Action(getTurn(),choice,Action.TURNLEFT);
            curr_action_list.enqueue(action);
        }
        chess.setPos((chess.getPos() + 1)%52);

        if(chess.eatTest(chessboard[chess.getPos()]))
        {
            System.out.println("eating...");
            Chess tmpchess = chessboard[chess.getPos()];
            for(Pair pair:tmpchess.getIndexlist())
            {
                action = new Action(pair.playerId,pair.chessId,Action.FALLEN);
                curr_action_list.enqueue(action);
                player[pair.playerId].chesslist[pair.chessId].setFallen();
            }
            action = new Action(getTurn(),choice,Action.NORMAL_MOVE,1);
            curr_action_list.enqueue(action);
            chessboard[chess.getPos()] = new Chess(chess);//必须
            if(chess.getStatus() == Chess.STATUS_STARTLINE)
            {
                action = new Action(getTurn(),choice,Action.TURNLEFT);
                curr_action_list.enqueue(action);
                chess.setStatus(Chess.STATUS_FLYING);
            }
            else if(chess.entry() || chess.isRightCorner())
            {
                action = new Action(getTurn(),choice,Action.TURNRIGHT);
                curr_action_list.enqueue(action);
            }
            return false;
        }
        action = new Action(getTurn(),choice,Action.NORMAL_MOVE,1);
        curr_action_list.enqueue(action);

        if(chess.mergeTest(chessboard[chess.getPos()]))
        {
            Chess tmpchess = chessboard[chess.getPos()];
            for(Pair pair:tmpchess.getIndexlist())
            {
                action = new Action(pair.playerId,pair.chessId,Action.HIDE);
                curr_action_list.enqueue(action);
                chess.insertToIndexList(pair);
            }
            tmpchess.clear_chess();
            chessboard[chess.getPos()] = new Chess(chess);//必须
            return false;
        }

        int prop_board = propboard[chess.getPos()].getProp();

        if(chess.getPos() >= 0 && chess.getPos() <= 51)
        {
            //终点类道具
            if(prop_board == Prop.TORNADO)//龙卷风
            {
                //消去道具
                action = new Action(chess.getPos(),prop_board,Action.HIDE_PROP);
                curr_action_list.enqueue(action);
                propboard[chess.getPos()].setProp(Prop.NO_PROP);

                //如果是类似保护罩之类的东西
                if(curr_prop[getTurn()] == Prop.PROTECTION)
                {
                    if(chess.isRightCorner())
                    {
                        action = new Action(getTurn(),choice,Action.TURNRIGHT);
                        curr_action_list.enqueue(action);
                    }
                    else if(chess.entry())
                    {
                        action = new Action(getTurn(),choice,Action.TURNRIGHT);
                        curr_action_list.enqueue(action);
                    }
                    chessboard[chess.getPos()] = new Chess(chess);
                    return false;
                }
                else if(prop_board == Prop.PROTECTION)
                {
                    int random_pos = randomPos();
                    action = new Action(getTurn(),choice,Action.FALLEN);
                    curr_action_list.enqueue(action);
                    action = new Action(getTurn(),choice,Action.RECOVER_CHESS_BY_POS,random_pos);
                    curr_action_list.enqueue(action);
                    chess.setPos(random_pos);

                    chessboard[chess.getPos()] = new Chess(chess);

                    return true;
                }
            }
            //中间类道具
            else if(prop_board == Prop.BARRIER)
            {
                //消去道具(中间类,获取类才需要消去道具,终点类不需要消去道具)...
                action = new Action(chess.getPos(),prop_board,Action.HIDE_PROP);
                curr_action_list.enqueue(action);
                propboard[chess.getPos()].setProp(Prop.NO_PROP);

                chessboard[chess.getPos()] = new Chess(chess);

                //如果是类似保护罩之类的东西
                if(curr_prop[getTurn()] == Prop.PROTECTION)
                {
                    if(chess.isRightCorner())
                    {
                        action = new Action(getTurn(),choice,Action.TURNRIGHT);
                        curr_action_list.enqueue(action);
                    }
                    else if(chess.entry())
                    {
                        action = new Action(getTurn(),choice,Action.TURNRIGHT);
                        curr_action_list.enqueue(action);
                    }
                    return false;
                }
                else return true;
            }
            //拾取类道具
            else if(prop_board == Prop.PROTECTION)
            {
                //消去道具(中间类,获取类才需要消去道具,终点类不需要消去道具)...
                action = new Action(chess.getPos(),prop_board,Action.HIDE_PROP);
                curr_action_list.enqueue(action);
                propboard[chess.getPos()].setProp(Prop.NO_PROP);

                chessboard[chess.getPos()] = new Chess(chess);

                //获取道具
                int pos = addProp(getTurn(),prop_board);
                //格式(pos,prop,action,kind),kind=0为暗色,kind=1为亮色
                if(pos != -1)
                {
                    action = new Action(pos,prop_board,Action.ADD_PROP,0);
                    curr_action_list.enqueue(action);
                }
            }
        }

        if(chess.isRightCorner())
        {
            action = new Action(getTurn(),choice,Action.TURNRIGHT);
            curr_action_list.enqueue(action);
        }
        else if(chess.entry())
        {
            action = new Action(getTurn(),choice,Action.TURNRIGHT);
            curr_action_list.enqueue(action);
        }

        return false;
    }
    private void exec_eat(Chess that)
    {
        Action action;
        for(Pair pair:that.getIndexlist())
        {
            action = new Action(pair.playerId,pair.chessId,Action.FALLEN);
            curr_action_list.enqueue(action);
            player[pair.playerId].chesslist[pair.chessId].setFallen();
        }
        chessboard[that.getPos()].clear_chess();
    }
    private boolean exec_superlucky()
    {
        Chess chess = player[getTurn()].chesslist[choice];

        Action action = new Action(getTurn(),choice,Action.TURNRIGHT);
        curr_action_list.enqueue(action);

        action = new Action(getTurn(),choice,Action.NORMAL_MOVE,2);
        curr_action_list.enqueue(action);

        int attack_pos = chess.getAttackPos();
        Chess tmpchess = chessboard[attack_pos];
        if(chess.attackTest(tmpchess))
        {
            exec_eat(tmpchess);
        }
        action = new Action(getTurn(),choice,Action.NORMAL_MOVE,3);
        curr_action_list.enqueue(action);

        chess.setPos(chess.getFlyingPoint());

        //终点类道具,中间类道具,拾取类道具,吃棋子,合并,(以及在起点时,吃棋子时的特殊情况)
        tmpchess = chessboard[chess.getPos()];
        if(chess.eatTest(tmpchess))
        {

            for(Pair pair:tmpchess.getIndexlist())
            {
                action = new Action(pair.playerId,pair.chessId,Action.FALLEN);
                curr_action_list.enqueue(action);
                player[pair.playerId].chesslist[pair.chessId].setFallen();
            }
            chessboard[chess.getPos()].clear_chess();
        }
        else if(chess.mergeTest(tmpchess))
        {
            for(Pair pair:tmpchess.getIndexlist())
            {
                chess.insertToIndexList(pair);
                player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_HIDING);
                action = new Action(pair.playerId,pair.chessId,Action.HIDE);
                curr_action_list.enqueue(action);
            }
            chessboard[chess.getPos()].clear_chess();
        }
        else
        {
            action = new Action(getTurn(),choice,Action.NORMAL_MOVE,1);
            curr_action_list.enqueue(action);

            int prop_board = propboard[chess.getPos()].getProp();
            //终点类道具
            if(prop_board == Prop.TORNADO)
            {
                //消去道具
                action = new Action(chess.getPos(),prop_board,Action.HIDE_PROP);
                curr_action_list.enqueue(action);
                propboard[chess.getPos()].setProp(Prop.NO_PROP);

                //如果是类似保护罩之类的东西
                if(curr_prop[getTurn()] == Prop.PROTECTION)
                {
                    chessboard[chess.getPos()] = new Chess(chess);
                }
                else
                {
                    int random_pos = randomPos();
                    action = new Action(getTurn(),choice,Action.FALLEN);
                    curr_action_list.enqueue(action);
                    action = new Action(getTurn(),choice,Action.RECOVER_CHESS_BY_POS,random_pos);
                    curr_action_list.enqueue(action);
                    chess.setPos(random_pos);

                    chessboard[chess.getPos()] = new Chess(chess);

                    return true;
                }
            }
            else if(prop_board == Prop.BARRIER)
            {
                //消去道具(中间类,获取类才需要消去道具,终点类不需要消去道具)...
                action = new Action(chess.getPos(),prop_board,Action.HIDE_PROP);
                curr_action_list.enqueue(action);
                propboard[chess.getPos()].setProp(Prop.NO_PROP);

                chessboard[chess.getPos()] = new Chess(chess);

                //如果是类似保护罩之类的东西
                if(curr_prop[getTurn()] == Prop.PROTECTION)
                {
                }
                else return true;
            }
            //拾取类道具
            else if(prop_board == Prop.PROTECTION)
            {
                //消去道具(中间类,获取类才需要消去道具,终点类不需要消去道具)...
                action = new Action(chess.getPos(),prop_board,Action.HIDE_PROP);
                curr_action_list.enqueue(action);
                propboard[chess.getPos()].setProp(Prop.NO_PROP);

                chessboard[chess.getPos()] = new Chess(chess);

                //获取道具
                int pos = addProp(getTurn(),prop_board);
                //格式(pos,prop,action,kind),kind=0为暗色,kind=1为亮色
                if(pos != -1)
                {
                    action = new Action(pos,prop_board,Action.ADD_PROP,0);
                    curr_action_list.enqueue(action);
                }
            }

            action = new Action(getTurn(),choice,Action.TURNRIGHT);
            curr_action_list.enqueue(action);
        }
        return false;
    }
    private void exec_move(int step)
    {

        Chess chess = player[getTurn()].chesslist[choice];
        boolean isLucky = false;
        chessboard[chess.getPos()].clear_chess();
        for(int i = 1; i < step; i++)
        {
            if(exec_single_move())return;
        }

        if(exec_last_move()) return;
        //System.out.println("flag1");
        if(chess.isLucky() && !chess.isSuperLucky())
        {
            //System.out.println("flag2");
            chessboard[chess.getPos()].clear_chess();
            isLucky = true;
            for(int i = 1; i < 4; i++)
            {
                if(exec_single_move()) return;
            }
            if(exec_last_move())  return;
        }

        if(chess.isSuperLucky())
        {
            //System.out.println("flag3");
            chessboard[chess.getPos()].clear_chess();
            if(exec_superlucky()) return;
            if(!isLucky)
            {
                //System.out.println("flag4");
                chessboard[chess.getPos()].clear_chess();
                for(int i = 1; i < 4; i++)
                {
                    if(exec_single_move()) return ;
                }
                if(exec_last_move()) return;
            }
        }
    }
    private void exec_merge()
    {
        Chess chess = player[getTurn()].chesslist[choice];
        Action action;
        if(chess.mergeTest(chessboard[chess.getPos()]))
        {
            for(Pair pair:chessboard[chess.getPos()].getIndexlist())
            {
                chess.insertToIndexList(pair);
                player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_HIDING);
                action = new Action(pair.playerId,pair.chessId, Action.HIDE);
                curr_action_list.enqueue(action);
            }
        }
    }

    private void exec_sprint()
    {
        Chess chess = player[getTurn()].chesslist[choice];
        chessboard[chess.getPos()].setStatus(Chess.STATUS_EMPTY);
        chessboard[chess.getPos()].clearIndexList();

        int lastpos = chess.getPos();

        //反弹~~~
        boolean rebounded = chess.rebound(dice);
        Action action;
        if(chess.getStatus() == Chess.STATUS_FINISH)
        {
            action = new Action(getTurn(),choice, Action.NORMAL_MOVE,dice);
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

                action = new Action(getTurn(),choice, Action.NORMAL_MOVE,endpoint - lastpos);
                curr_action_list.enqueue(action);
                action = new Action(getTurn(),choice, Action.REVERSE);
                curr_action_list.enqueue(action);
                action = new Action(getTurn(),choice, Action.NORMAL_MOVE,endpoint - chess.getPos());
                curr_action_list.enqueue(action);
                action = new Action(getTurn(),choice, Action.REVERSE);
                curr_action_list.enqueue(action);
            }
            //否则
            else
            {
                action = new Action(getTurn(),choice, Action.NORMAL_MOVE,dice);
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
                player[getTurn()].setChess(choice,chess);
            }
            //记得更新自己的棋子和棋盘
            else
            {
                chessboard[chess.getPos()] = new Chess(chess);
                player[getTurn()].setChess(choice,chess);
            }
        }
    }
    public Queue<Action> prop_action_list()
    {
        curr_action_list.clear();
        //没有使用技能或者道具...
        if(curr_prop[getTurn()] == Prop.NO_PROP)
        {
            //只有选了棋子才能发生动作
            if(waitingchoice)
            {
                System.err.println("Unexpected error in PropGameManager: prop_pre_action_list()");
                System.exit(0);
            }


            if(choice < 0){
                nextTurn();
                return curr_action_list;
            }
            //Chess chess = new Chess(player[getTurn()].getChess(choice));
            Chess chess = player[getTurn()].getChess(choice);
            //位于飞机砰
            if(chess.getStatus() == Chess.STATUS_AIRPORT)
            {
                System.out.println("ariport...");
                exec_ariport();

            }
            //位于起飞点
            else if(chess.getStatus() == Chess.STATUS_STARTLINE)
            {
                System.out.println("startline...");
                exec_move(dice);
            }
            //接近终点线
            else if(chess.presprint(dice))
            {
                int pre_pos = chess.getPos();
                chessboard[chess.getPos()].clear_chess();
                int entry = chess.getEntry();
                int step = entry - chess.getPos();
                for(int i = 1; i <= step; i ++)
                {
                    exec_single_move();
                }
                step = dice - step;
                Action action = new Action(getTurn(),choice,Action.NORMAL_MOVE,step);
                curr_action_list.enqueue(action);
                chess.setEndLine(dice - (chess.getPos() - pre_pos));
                if(chess.getStatus() == Chess.STATUS_FINISH)
                {
                    for(Pair pair:chess.getIndexlist())
                    {
                        action = new Action(pair.playerId,pair.chessId,Action.FINISHED);
                        curr_action_list.enqueue(action);
                        player[pair.playerId].chesslist[pair.chessId].setStatus(Chess.STATUS_FINISH);
                    }
                }
                else
                {
                    exec_merge();
                }
            }
            //已经在终点线
            else if(chess.sprint())
            {
                System.out.println("sprint...");
                exec_sprint();
            }
            //普通路线
            else
            {
                System.out.println("normal...");
                exec_move(dice);
            }

            //更新棋盘
            if(chess.getStatus() == Chess.STATUS_FLYING)
                chessboard[chess.getPos()] = new Chess(chess);

            //如果使用了某些道具,可能不用轮到下一个人
            if(curr_prop[getTurn()] == Prop.AGAIN)
            {

            }
            else
            {
                if(dice <= 5){
                    nextTurn();
                }
            }
        }
        else
        {
            //to do....
            if(curr_prop[getTurn()] == Prop.RED_CD)
            {
                exec_cd();
            }
        }

        Action action = new Action(0,0, Action.ENDTHISTURN);
        curr_action_list.enqueue(action);

        history.append(recover_game());
        for(Action act:curr_action_list)
        {
            history.append(" " + act.toActionString());//第二行:当前动作序列
        }
        history.append("\n");

        return curr_action_list;
    }
    @Override
    public Queue<Integer> getChessAvailable()
    {
        PlayerAI playerAI = (PlayerAI) player[getTurn()];
        if(curr_prop[getTurn()] == Prop.RED_CD)
        {
            return playerAI.cd_available_chess();
        }
        //其他的待补充


        //...

        //没有使用技能
        else if(curr_prop[getTurn()] == Prop.NO_PROP)
        {
            return super.getChessAvailable();
        }
        return null;
    }

    public static void test(Chess chess)
    {
        chess.setPos(4);
    }
    public static void main(String[] args) {

        int kind[] = {GameManager.AI_KIND, GameManager.NOT_USE_KIND, GameManager.AI_KIND, GameManager.NOT_USE_KIND};
        String name[] = {"gitfan","hello","word","bye"};
        PropGameManager gameManager = new PropGameManager(kind,name);

        String str;
        System.out.println(gameManager.init_game());

        gameManager.nextTurn();

        int dice1 = 1;
        int choice1 = 0;

        Chess chess = new Chess(new Pair(Chess.BLUE,0),Chess.BLUE);
        chess.setStatus(Chess.STATUS_FLYING);
        chess.setPos(4);
        gameManager.chessboard[4] = new Chess(chess);
        gameManager.player[Chess.BLUE].chesslist[0] = new Chess(chess);

        System.out.println(gameManager.player[Chess.BLUE].chesslist[0].getPos());

        chess = new Chess(new Pair(Chess.RED,0),Chess.RED);
        chess.setStatus(Chess.STATUS_FLYING);
        chess.setPos(5);
        gameManager.chessboard[5] = new Chess(chess);
        gameManager.player[Chess.RED].chesslist[0] = new Chess(chess);


        gameManager.setDice(dice1);
        gameManager.setChoice(choice1);

        Queue<Action> actions_ = gameManager.prop_action_list();

        for(Action action:actions_)
        {
            System.out.println(action);
        }
        System.out.println(gameManager.player[Chess.BLUE].chesslist[0].getPos());



//        while(!gameManager.isGameOver(true))
//        {
//            if(gameManager.getTurn() == Chess.RED) str = "红色玩家";
//            else if(gameManager.getTurn() == Chess.YELLOW) str = "黄色玩家";
//            else if(gameManager.getTurn() == Chess.BLUE) str = "蓝色玩家";
//            else str = "绿色玩家";
//
//            System.out.println("现在轮到" + str);
//
//            int dice = ((int)(Math.random()*1000000))%6 + 1;
//
//            System.out.println("dice: " + dice);
//
//            gameManager.setDice(dice);
//
//            int choice = gameManager.getAIChoice();
//
//            gameManager.setChoice(choice);
//
//            Queue<Action> actions = gameManager.prop_action_list();
//
//            for(Action action:actions)
//            {
//                System.out.println(action);
//            }
//        }
    }
}
