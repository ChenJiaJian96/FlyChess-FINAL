package com.unity3d.flyingchess;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import chess.Chess;
import game_driver.GameManager;
import game_driver.Queue;
import protocol.MsgLogic;
import protocol.MsgUnity;
import protocol.Protocol;
import protocol.UnifiedStandard;

import static game_driver.GameManager.AI_KIND;
import static game_driver.GameManager.NOT_USE_KIND;
import static game_driver.GameManager.PLAYER_KIND;
import static com.unity3d.player.UnityPlayer.UnitySendMessage;


public class MainActivity extends UnityPlayerActivity {

    public static final String TAG = "MainActivity";

    //单人模式中用到的全局变量
    public static int[] init_num;           //构造房间时传入的玩家类型数组（玩家、AI或未选择）
    public static String[] init_name;       //构造房间时传入的玩家姓名数组（玩家自定义名字、COM或空）
    public static int player_num;           //实际选择玩家人人数
    public static int ai_num;               //实际选择AI人数
    public static boolean has_given_force = false;//判断是否已经将force发送给Unity(主要用于解决在骰子动画中设置托管问题)
    static
    {
        init_num = new int[4];
        init_name = new String[4];
    }

    //联网模式中用到的全局变量
    public static String net_player_name;   //联网模式中的玩家的名字
    public static String net_room_name;     //联网模式中创建或需要加入的房间名

    //回放模式中用到的全局变量
    public static String history_name;      //回放文件名

    //消息类型,必须是static final类型
    private static final int TURN = 1;//设置现在轮到谁
    private static final int REQUESTDICE = 2;//请求扔骰子
    private static final int THROWDICE = 3;//真正扔骰子，播放扔骰子的动画
    private static final int SHOW_THROWDICE_BUTTON = 4;//显示扔骰子的那个按钮
    private static final int SHOW_AVAILABLE_CHESS = 5;//设置可用的棋子
    private static final int EXECUTE_ACTION = 6;//在Unity中执行动作
    private static final int SELECT_CHESS = 7;//AI选择棋子

    //网络专用消息类型
    private static final int UPDATE_ONLINE_ROOM = 21;//刷新在线房间
    private static final int UPDATE_ROOM_STATUS = 22;//更新房间状态
    private static final int UPDATE_XP = 23;//更新p数
    private static final int JOIN_ROOM_ASSESS = 24;//是否可以加入房间？
    private static final int START_GAME_ASSES = 25;//是否可以开始游戏？
    private static final int UPDATE_ACTION_LIST = 26;//根据ActionList刷新游戏UI
    private static final int CHANGE_NAME_IN_UI = 27;//初始化轮次滚动条的名字
    private static final int THROW_DICE = 28;//根据数字扔骰子
    private static final int UPDATE_FORCE = 29;//通过力播放扔骰子
    public static final int THROW_DICE_UI = 30;//不setdice的扔骰子
    private static final int SHOW_RANKING_LIST = 31;//显示排行榜
    private static final int REPLAY_GAME = 32;//回放游戏
    private static final int TO_NETWORK_MODE   = 33;//网络模式扔骰子
    private static final int AUTO_THROW_DICE = 34;////服务器自动帮玩家扔骰子

    //游戏模式;ps:在单机创建房间时设置为single_mode,在联网创建房间或者加入房间时设置为network_mode,在进入回放的房间时设置为history_mode
    private static int SINGLE_MODE = 1;//单机模式 add1
    private static int NETWORK_MODE = 2;//联网模式
    private static int HISTORY_MODE = 3;//回放模式
    private static int game_mode = SINGLE_MODE;

    //界面中的各种组件初始化
    public DrawerLayout mDrawerLayout;
    public LinearLayout unity_view;
//    public NavigationView nav_view;
//    public RecyclerView action_recycleview;
//    public List<Action_message> messages;

    //游戏使用线程
    public RunThread runThread;         //单机线程
    public RecvThread recvThread;       //后台线程:不断接收信息
    public HistoryThread historyThread; //回放线程

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        //运行recvThread接受服务器消息(不终止)
        recvThread = new RecvThread();
        recvThread.start();

    }
    //初始化
    //定义组件及创建线程，开启线程
    private void init(){
        //将错误信息保存到本机
        //CrashHandler.getInstance().init(this);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        unity_view = findViewById(R.id.unity_view);
//        action_recycleview = findViewById(R.id.action_list);
//        nav_view = findViewById(R.id.log_view);

        //将Unity视图添加到Unity_View中
        View mView = mUnityPlayer.getView();
        unity_view.addView(mView);

//        messages = new ArrayList<>();
//        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
//        action_recycleview.setLayoutManager(layoutManager);
//        ActionAdapter adapter = new ActionAdapter(messages);
//        action_recycleview.setAdapter(adapter);
    }

//    @Override
//    protected void onPause() {
//
//        new Thread()
//        {
//            @Override
//            public void run() {
//                Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_UNITY_UPDATE_UI);
//                msg_send.set_msgUnity(new MsgUnity(MsgUnity.SET_PAUSE_STATUS));
//                Socket socket = null;
//                try {
//                    socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
//                    Protocol.socketSerilize(socket,msg_send);
//                    socket.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }.start();
//        Log.d(TAG, "onPause: ");
//        super.onPause();
//    }
    @Override
    protected void onRestart() {
        super.onRestart();
        recvThread.interrupt();
        recvThread = new RecvThread();
        recvThread.start();
    }
    @Override
    protected void onStop() {
        new Thread()
        {
            @Override
            public void run() {
                Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_UNITY_UPDATE_UI);
                msg_send.set_msgUnity(new MsgUnity(MsgUnity.FORCE_EXIT));
                Socket socket = null;
                try {
                    socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                    Protocol.socketSerilize(socket,msg_send);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        Log.d(TAG, "onStop: ");
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        new Thread()
        {
            @Override
            public void run() {
                Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_UNITY_UPDATE_UI);
                msg_send.set_msgUnity(new MsgUnity(MsgUnity.FORCE_EXIT));
                Socket socket = null;
                try {
                    socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                    Protocol.socketSerilize(socket,msg_send);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
    }


    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case TURN:{
                    //在LOG中显示当前轮次
                    int turn = msg.getData().getInt("turn");
                    //更改UI
                    recyclerview_addItem("现在轮到 ： " + Integer.toString(turn));
                    break;
                }
                case REQUESTDICE:{
                    //Unity视图所有人物设为不可选
                    break;
                }
                case THROWDICE:{
                    int dice = msg.getData().getInt("dice");
                    recyclerview_addItem("骰子结果为 ： " + Integer.toString(dice));
                    //即使是AI投掷骰子，也需要显示骰子动画
                    UnitySendMessage("ChessManager", "throwDiceByNum", Integer.toString(dice));
                    Log.d(TAG, "throwDiceByNum: " + dice);
                    break;
                }
                case SHOW_THROWDICE_BUTTON:{
                    break;
                }
                case SHOW_AVAILABLE_CHESS:{
                    //获取可选棋子列表，显示可选棋子
                    break;
                }
                case SELECT_CHESS:{
                    int choice = msg.getData().getInt("choice");
                    //更改UI
                    recyclerview_addItem("玩家选择为 ： " + Integer.toString(choice));
                    break;
                }
                case EXECUTE_ACTION:{
                    String cur_action = msg.getData().getString("cur_action");
                    String cur_action_list = msg.getData().getString("cur_action_list");
                    //显示当前动作
                    recyclerview_addItem(cur_action);
                    //将动作指令字符串传给Unity
                    UnitySendMessage("ChessManager", "execActionInAndroid", cur_action_list);
                    break;
                }

                //网络消息传输

                //刷新在线房间
                case UPDATE_ONLINE_ROOM:
                {
                    Bundle bundle = msg.getData();
                    //形式:roomname_roomowner_1_4
                    Vector<String> rooms = (Vector<String>) bundle.getSerializable("online_rooms");
                    for(String room : rooms){
                        UnitySendMessage("Script", "AddRoom", room);
                    }
                    Log.d(TAG,rooms.toString());
                    break;
                }
                //刷新房间状态
                case UPDATE_ROOM_STATUS:
                {
                    Bundle bundle = msg.getData();
                    Vector<String> rooms = (Vector<String>) bundle.getSerializable("room_status");
                    for(String room : rooms){
                        UnitySendMessage("CharacterSelect", "GetState", room);
                    }
                    Log.d(TAG,rooms.toString());
                    break;
                }
                //更新p数
                case UPDATE_XP:
                {
                    Bundle bundle = msg.getData();
                    int xp = bundle.getInt("update_xp");
                    UnitySendMessage("CharacterSelect", "init_room_symbol", Integer.toString(xp));
                    Log.d(TAG,"get xp: " + xp);
                    break;
                }
                //加入房间测试 add2
                case JOIN_ROOM_ASSESS:
                {
                    Bundle bundle = msg.getData();
                    boolean assess = bundle.getBoolean("join_room_assess");
                    if(assess)
                    {
                        Log.d(TAG, "Accept.");
                        //如果可以加入房间,切换场景,记得在c#那里请求获得p数,再请求更新房间状态!!!!
                        game_mode = NETWORK_MODE;//联网时加入房间
                        //请求房间成功后加载场景
                        UnitySendMessage("Script", "LoadSelectScene", "0");
                        //刷新场景
                        new Thread(){
                            @Override
                            public void run() {
                                try {
                                    send_getxp_to_server();
                                    //send_update_roomstatus_to_server();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }
                    else
                    {
                        //不要切换场景,同时给出提示
                        Toast.makeText(MainActivity.this,"对不起,加入失败",Toast.LENGTH_SHORT).show();
                    }
                    Log.d(TAG,Boolean.toString(assess));
                    break;
                }
                //是否可以开始游戏 //add2
                case START_GAME_ASSES:
                {
                    Bundle bundle = msg.getData();
                    boolean assess = bundle.getBoolean("start_game_assess");
                    if(assess)
                    {
                        //然后切换场景...
                        //调用Unity中的GameStart()函数
                        UnitySendMessage("CharacterSelect", "GameStart", "0");
                    }
                    else
                    {
                        //不要切换场景,同时给出提示
                        Toast.makeText(MainActivity.this,"对不起,暂时不能开始游戏",Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                //通过ActionList改变游戏UI
                case UPDATE_ACTION_LIST:
                {
                    Bundle bundle = msg.getData();
                    Vector<String> action_list = (Vector<String>) bundle.getSerializable("update_action_list");
                    //将动作指令字符串传给Unity
                    for(String action:action_list)
                    {
                        String arr[] = action.split(" ");
                        Action aa = new Action(Integer.parseInt(arr[0]),Integer.parseInt(arr[1]),Integer.parseInt(arr[2]),Integer.parseInt(arr[3]));
                        Log.d(TAG, "action : " + aa.toString());
                        UnitySendMessage("ChessManager", "execActionInAndroid", action);
                    }
                    break;
                }
                //初始化轮次滚动条
                case CHANGE_NAME_IN_UI:
                {
                    Bundle bundle = msg.getData();
                    Vector<String> change_name_in_ui = (Vector<String>) bundle.getSerializable("change_name_in_ui");
                    for(String str:change_name_in_ui)
                    {
                        //str:pos_string + " " + init_name[pos]
                        UnitySendMessage("ChessManager", "ChangeNameInUI",str);
                    }
                    Log.d(TAG, "change_name" + change_name_in_ui.toString());
                    break;
                }
                //根据数字扔骰子
                case THROW_DICE:
                {
                    Bundle bundle = msg.getData();
                    int dice = bundle.getInt("throw_dice");
                    UnitySendMessage("ChessManager", "throwDiceByNum", Integer.toString(dice));
                    Log.d(TAG, "Dice :" + Integer.toString(dice));
                    break;
                }
                case THROW_DICE_UI:
                {
                    Bundle bundle = msg.getData();
                    int dice = bundle.getInt("throw_dice");
                    UnitySendMessage("ChessManager", "throwDiceByNumOnlyUI", Integer.toString(dice));
                    Log.d(TAG, "Dice_UI:" + Integer.toString(dice));
                    break;
                }
                //通过力播放扔骰子
                case UPDATE_FORCE:
                {
                    Bundle bundle = msg.getData();

                    float force = bundle.getFloat("update_force");
                    UnitySendMessage("ChessManager", "throwDiceByForceOnlyUI", Float.toString(force));
                    break;
                }
                case SHOW_RANKING_LIST:
                {
                    Bundle bundle = msg.getData();
                    String ranking_list = bundle.getString("show_ranking_list");
                    //....
                    Game_End_Info(ranking_list);
                    break;
                }
                case REPLAY_GAME:
                {
                    Bundle bundle = msg.getData();
                    String filename = bundle.getString("get_filename");
                    String history = bundle.getString("get_history");
                    //...
                    writeHistoryToFile(filename,history);
                    break;
                }
                case TO_NETWORK_MODE:
                {
                    UnitySendMessage("ChessManager", "intoMultiMode","0");
                    break;
                }
                case AUTO_THROW_DICE:
                {
                    Bundle bundle = msg.getData();
                    int _dice = bundle.getInt("get_dice");
                    Toast.makeText(MainActivity.this,"你已超时，系统自动帮你扔骰子,结果为:"+Integer.toString(_dice),Toast.LENGTH_SHORT).show();
                    break;
                }
                default:
                    break;
            }
        }
    };


    //单机游戏专用线程
    class RunThread extends Thread {
        public GameManager gameManager;
        //public PropGameManager gameManager;
        int choice;

        public RunThread(GameManager gameManager) {
            this.gameManager = gameManager;
        }


//        //测试历史记录专用run函数
//        @Override
//        public void run() {
//
//            Log.d(TAG, "RunThread.run() called");
//            //gameManager = new GameManager(new int[]{0,0,0,0},new String[]{"你好","谢谢","再见","好的"});
//            gameManager = new PropGameManager(new int[]{0,0,0,0},new String[]{"你好","谢谢","再见","好的"});
//            gameManager.init_game();
//
//            while(!gameManager.isGameOver(true))
//            {
//                gameManager.getTurn_Action();
//
//                int dice = ((int)(Math.random()*1000000))%6 + 1;
//
//                 gameManager.setDice(dice);
//
//                int choice = gameManager.getAIChoice();
//
//                gameManager.setChoice(choice);
//
//                //Queue<Action> actions = gameManager.actionlist();
//                Queue<Action> actions = gameManager.prop_action_list();
//            }
//
//            Log.d(TAG, "GAME OVER.");
//            Log.d(TAG, gameManager.get_ranking_list_by_time_with_name());
//            //sendSingleActionToUnity(gameManager.show_ranking_list());
//            Game_End_Info(gameManager.get_ranking_list_by_time_with_name());
//            //保存游戏记录
//            writeHistoryToFile(gameManager.get_file_name(),gameManager.getHistory());
//        }


        @Override
        public void run() {
            try {
                sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "RunThread.run() called");
            sendActionToUnity(gameManager.init_game());

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //设置滚动条的名字
            for(int pos = 0; pos < 4; pos++){
                if(init_num[pos] != NOT_USE_KIND) {
                    String pos_string = Integer.toString(pos);
                    UnitySendMessage("ChessManager", "ChangeNameInUI", pos_string + " " + init_name[pos]);
                    Log.d(TAG, "get name" + init_name[pos]);
                }
            }
            UnitySendMessage("ChessManager", "intoSingleMode","0");


            //用于测试排行榜
//            gameManager.player[0].chesslist[0].setStatus(Chess.STATUS_FINISH);
//            gameManager.player[0].chesslist[1].setStatus(Chess.STATUS_FINISH);
//            gameManager.player[0].chesslist[2].setStatus(Chess.STATUS_FINISH);
//            gameManager.player[0].chesslist[3].setStatus(Chess.STATUS_FINISH);

            //游戏结束线程停止
            //isGameOver(true)表示多人结束后才结束
            //isGameOver(false)表示第一个人结束后就结束
            while(!gameManager.isGameOver(true))
            {
                //首先将轮次显示在屏幕上
                showTurn();

                //请求骰子
                requestDice();
                //设置骰子结果操作都在Unity中执行
                //检查是否已经返回骰子结果
                while (gameManager.waitDice()){
                    try{
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                sendSingleActionToUnity(gameManager.hide_dice());

//                if(gameManager.isAI()){
//                    //延长一下时间，让AI别那么快
//                    try{
//                        Thread.sleep(4000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }

                //复位has_given_force
                has_given_force = false;
                //请求选定棋子
                requestChess();
                //检查是否已经返回骰子结果
                while (gameManager.waitChoice()){
                    try{
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //对于取消托管很重要的一件事，AI的睡眠必须放在这里
                //因为有时间差，睡眠放在requestDice里面会出问题的
                //原因自己思考...

//                if(gameManager.isAI()) {
//                    //睡眠一下，免得AI的动作太快...
//                    try {
//                        Thread.sleep(2000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }

                //既然已经选好了棋子，就可以根据action指令集进行操作了
                sendActionToUnity(gameManager.actionlist());

//                if(gameManager.isAI() && choice >= 0) {
//                    //睡眠一下，免得AI的动作太快...
//                    try {
//                        Thread.sleep(4000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }

                //检查当前轮次是否已经结束
                while (gameManager.waitEndTurn()){
                    try{
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            Log.d(TAG, "GAME OVER.");
            Log.d(TAG, gameManager.get_ranking_list_by_time_with_name());
            //sendSingleActionToUnity(gameManager.show_ranking_list());
            Game_End_Info(gameManager.get_ranking_list_by_time_with_name());
            //保存游戏记录
            writeHistoryToFile(gameManager.get_file_name(),gameManager.getHistory());
        }

        //显示轮次
        private void showTurn(){
            //隐藏骰子
            sendSingleActionToUnity(gameManager.hide_dice());
            //将当前轮次发送给Unity
            sendSingleActionToUnity(gameManager.getTurn_Action());
            Message message = new Message();
            message.what = TURN;
            Bundle data = new Bundle();
            data.putInt("turn", gameManager.getTurn());
            message.setData(data);
            handler.sendMessage(message);
        }
        //随机产生骰子
        public int newDice(){
            int num = ((int)(Math.random()*10000000))%6 + 1;
            return num;
        }
        //请求投掷一个骰子
        private void requestDice(){
            //先判断是人还是AI
            //请求骰子时：
            //step 1:设置所有的棋子不能被选择
            //step 2:扔骰子的按钮以及动画
            Message message = new Message();
            message.what = REQUESTDICE;
            handler.sendMessage(message);

            //如果是AI
            if(gameManager.isAI()){
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                message = new Message();
                //AI主动投骰子
                int dice = newDice();
                //播放投骰子动画
                message.what = THROWDICE;
                Bundle data = new Bundle();
                data.putInt("dice", dice);
                message.setData(data);
                handler.sendMessage(message);

                //直接使用dice传参

//                //延长一下时间，让AI别那么快
//                try{
//                    Thread.sleep(2000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                //gameManager.setDice(dice);
            }
            //如果是人
            //如果是人，则应该出现投掷的按钮，并让玩家自己选择
            else{
                //显示骰子
                sendSingleActionToUnity(gameManager.show_dice());
                Log.d(TAG, "Player should throw a dice immediately.");
            }
        }
        //请求选定一个棋子
        private void requestChess() {
            //请求选择棋子时：
            //step1:向handler发送可选棋子；
            Message message = new Message();
            message.what = SHOW_AVAILABLE_CHESS;
            Bundle data = new Bundle();
            //data.putString("chessList","123");
            message.setData(data);
            handler.sendMessage(message);

            //如果是AI
            //step2:AI直接选择棋子,向handler发送选择;
            //step3:在manager中设置选择
            if (gameManager.isAI()) {

                //通过函数自主选择
                choice = gameManager.getAIChoice();
                //发送选择
                message = new Message();
                message.what = SELECT_CHESS;
                data = new Bundle();
                data.putInt("choice", choice);
                message.setData(data);
                handler.sendMessage(message);


                try {
                    sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                gameManager.setChoice(choice);
            }
            //如果是人
            else {
                //恢复可选棋子点击
                Queue<Action> actionlist = gameManager.getChessAvailable_Action();
                sendActionToUnity(actionlist);
                if(actionlist.size() == 0){
                    try {
                        sleep(2500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    gameManager.setChoice( -1 );
                }
            }
        }

        //将Action_list发送给Unity
        private void sendActionToUnity(Queue<Action> actionlist){
            //递归发送UI消息
            for(Action action:actionlist) {
                Message message = new Message();
                message.what = EXECUTE_ACTION;
                Bundle data = new Bundle();
                String cur_action = action.toString();
                String cur_action_list = action.toActionString();
                data.putString("cur_action", cur_action);
                data.putString("cur_action_list", cur_action_list);
                message.setData(data);
                handler.sendMessage(message);
            }
        }
        //发送单一消息给Unity（如当前轮次）
        private void sendSingleActionToUnity(Action action){
                Message message = new Message();
                message.what = EXECUTE_ACTION;
                Bundle data = new Bundle();
                String cur_action = action.toString();
                String cur_action_list = action.toActionString();
                data.putString("cur_action", cur_action);
                data.putString("cur_action_list", cur_action_list);
                message.setData(data);
                handler.sendMessage(message);
        }
    }

    //接受服务器信息专用线程
    class RecvThread extends Thread {
        Socket read_socket;
        public RecvThread(){

        }
        //使用消息机制刷新UI
        Message message;
        Bundle data;

        private void initMsg(int msg_kind)
        {
            message = new Message();
            message.what = msg_kind;
            data = new Bundle();
        }
        private void putInt(String key,int val)
        {
            data.putInt(key,val);
        }
        private void putString(String key,String val)
        {
            data.putString(key,val);
        }
        //add2
        private void putBoolean(String key,boolean assess) { data.putBoolean(key,assess);}
        private void putFloat(String key,float val)
        {
            data.putFloat(key,val);
        }
        private void putVector(String key,Vector<String> vector)
        {
            data.putSerializable(key,vector);
        }
        private void send_msg()
        {
            message.setData(data);
            handler.sendMessage(message);
        }

        //解析从服务器接发送过来的信息,根据消息类型刷新UI
        private void process_msg(Protocol protocol)
        {
            //消息类型
            switch (protocol.getMsg_type())
            {
                case Protocol.MSG_TYPE_LOGI_MSG:
                {
                    //获取具体消息
                    MsgLogic msgLogic = protocol.get_msgLogic();

                    //再细分消息类型
                    switch (msgLogic.getType())
                    {
                        //服务器发过来的刷新房间消息的消息
                        case MsgLogic.UPDATE_ONLINE_ROOM:
                        {
                            //获取新的房间列表
                            Vector<String> _online_rooms = msgLogic.getOnlineRooms();

                            //将新的房间列表_online_rooms放入message,然后通过handle发送message更新UI
                            initMsg(UPDATE_ONLINE_ROOM);
                            putVector("online_rooms",_online_rooms);
                            send_msg();
                            break;
                        }
                        //更新房间状态
                        case MsgLogic.UPDATE_ROOM_STATUS:
                        {
                            //获取新的房间状态
                            Vector<String> room_status = msgLogic.getRoomStatus();

                            //将新的房间状态room_status放入message,然后通过handle发送message更新UI
                            initMsg(UPDATE_ROOM_STATUS);
                            putVector("room_status",room_status);
                            send_msg();
                            break;
                        }
                        //更新p数
                        case MsgLogic.UPDATE_XP:
                        {
                            //获得p数
                            int xp = msgLogic.getXP();

                            //将p数发送到handler
                            initMsg(UPDATE_XP);
                            putInt("update_xp",xp);
                            send_msg();
                            break;
                        }
                        //加入房间的结果:true表示成功加入,false表示无法加入        //add1
                        case MsgLogic.JOIN_ROOM_ANSWER:
                        {
                            //assess?
                            boolean assess = msgLogic.getAssess();

                            //将是否准入发送到handler
                            initMsg(JOIN_ROOM_ASSESS);
                            putBoolean("join_room_assess",assess);
                            send_msg();
                            break;
                        }
                        //是否可以开始游戏 //add1
                        case MsgLogic.START_GAME_ANSWER:
                        {
                            //assess?
                            boolean assess = msgLogic.getAssess();

                            initMsg(START_GAME_ASSES);
                            putBoolean("start_game_assess",assess);
                            send_msg();
                            break;
                        }
                        default:
                            break;
                    }

                    break;
                }
                case Protocol.MSG_TYPE_LOGI_UNITY_UPDATE_UI:
                {
                    MsgUnity msgUnity = protocol.get_msgUnity();

                    switch (msgUnity.getType())
                    {
                        case MsgUnity.UPDATE_ACTION_LIST:
                        {
                            Vector<String> vector = msgUnity.getActionlist();

                            initMsg(UPDATE_ACTION_LIST);
                            putVector("update_action_list",vector);
                            send_msg();
                            break;
                        }
                        case MsgUnity.CHANGE_NAME_IN_UI:
                        {
                            Vector<String> vector = msgUnity.getChangeNameInUi();

                            initMsg(CHANGE_NAME_IN_UI);
                            putVector("change_name_in_ui",vector);
                            send_msg();
                            break;
                        }
                        case MsgUnity.THROW_DICE:
                        {
                            int dice = msgUnity.getDice();

                            initMsg(THROW_DICE);
                            putInt("throw_dice",dice);
                            send_msg();
                            break;
                        }
                        case MsgUnity.THROW_DICE_UI:
                        {
                            int dice = msgUnity.getDice();

                            initMsg(THROW_DICE_UI);
                            putInt("throw_dice",dice);
                            send_msg();
                            break;
                        }
                        case MsgUnity.UPDATE_FORCE:
                        {
                            float force = msgUnity.getForce();

                            initMsg(UPDATE_FORCE);
                            putFloat("update_force",force);
                            send_msg();
                            break;
                        }
                        case MsgUnity.SHOW_RANKING_LIST:
                        {
                            String ranking_list = msgUnity.getRankingList();

                            initMsg(SHOW_RANKING_LIST);
                            putString("show_ranking_list",ranking_list);
                            send_msg();
                            break;
                        }
                        case MsgUnity.REPLAY_GAME:
                        {
                            String filename = msgUnity.get_filename();
                            String history = msgUnity.get_history();

                            initMsg(REPLAY_GAME);
                            putString("get_filename",filename);
                            putString("get_history",history);
                            send_msg();
                            break;
                        }
                        case MsgUnity.TO_NETWORK_MODE:
                        {
                            initMsg(TO_NETWORK_MODE);
                            send_msg();
                            break;
                        }
                        case MsgUnity.AUTO_THROW_DICE:
                        {
                            int dice = msgUnity.getDice();
                            initMsg(AUTO_THROW_DICE);
                            putInt("get_dice",dice);
                            send_msg();
                            break;
                        }
                        default:
                            break;
                    }
                    break;
                }
                default:
                    break;
            }
        }

        private void start_thread() {

            try {
                read_socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_WRITE_PORT);
                //不断接收信息
                while(true)
                {
                    //获取得到的Protocol
                    Protocol recvMsg = Protocol.socketUnSerilize(read_socket);
                    if(recvMsg == null)
                    {
                        Log.d(TAG, "fucking object null");
                        continue;
                    }
                    Log.d(TAG, "recv_msg: " + recvMsg.toString());

                    process_msg(recvMsg);//处理接收到的信息

                    Log.d(TAG, "finish ");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            start_thread();
        }
    }

    //回放游戏专用线程
    class HistoryThread extends Thread {
        Vector<Action> initGame;//初始化游戏,隐藏无关棋子
        Vector<String> initName;//初始化滚动条的玩家名
        Vector<Vector<Action>> recover_game;//恢复棋盘
        Vector<Vector<Action>> action_list;//动作序列
        int size,index;
        boolean isPause;//是否暂停
        boolean isStop;//是否停止
        boolean isSlided;//是否滑动了进度条
        boolean waitEndTurn;
        public HistoryThread(String filename)
        {
            Replayer replayer = null;
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = getFileStream(filename);
                replayer = new Replayer(fileInputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

            initGame = replayer.getInitGame();
            initName = replayer.getInitName();
            recover_game = replayer.getRecoverGame();
            action_list = replayer.getActionlist();
            size = replayer.size();
            index = 0;

            isPause = false;
            isStop = false;
            isSlided = false;
            waitEndTurn = true;
        }
        //终止进程:退出播放时调用
        public void setStop()
        {
            isPause = true;
            isStop = true;
            isSlided = false;
        }
        //暂停进程
        public void setPause()
        {
            isStop = false;
            isPause = true;
            isSlided = false;
        }
        //让进程继续走
        public void setContinous()
        {
            isStop = false;
            isPause = false;
            isSlided = false;
        }
        //滑动进度条时调用,左键按下的瞬间调用slide();
        //左键跳起的瞬间先获取进度条百分比,然后setIndex(percent),再调用setContinous();
        public void slide()
        {
            isStop = false;
            isPause = true;
            isSlided = true;
        }
        //设置进度条:percnt是进度条的百分比
        public void setIndex(float percent)
        {
            float value = (float)(size -1);
            value = value*percent;
            index = (int)(value);
        }

        public void setEndThisTurn()
        {
            waitEndTurn = false;
        }

        //将Action_list发送给Unity
        private void sendActionToUnity(Vector<Action> actionlist){
            //递归发送UI消息
            for(Action action:actionlist) {
                Message message = new Message();
                message.what = EXECUTE_ACTION;
                Bundle data = new Bundle();
                String cur_action = action.toString();
                String cur_action_list = action.toActionString();
                data.putString("cur_action", cur_action);
                data.putString("cur_action_list", cur_action_list);
                message.setData(data);
                handler.sendMessage(message);
                Log.d(TAG, "HisTory Action: " + action.toString());
            }
        }
        //发送单一消息给Unity（如当前轮次）
        private void sendSingleActionToUnity(Action action){
            Message message = new Message();
            message.what = EXECUTE_ACTION;
            Bundle data = new Bundle();
            String cur_action = action.toString();
            String cur_action_list = action.toActionString();
            data.putString("cur_action", cur_action);
            data.putString("cur_action_list", cur_action_list);
            message.setData(data);
            handler.sendMessage(message);
        }

        @Override
        public void run() {

            //sendActionToUnity(initGame);//初始化游戏
            //设置滚动条的名字
//            for(int i = 0 ; i < initName.size(); i ++)
//            {
//                String arr[] = initName.get(i).split(" ");
//                UnitySendMessage("ChessManager", "ChangeNameInUI", arr[0] + " " + arr[1]);
//            }


            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendActionToUnity(initGame);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //pos name
            for(String name: initName)
            {
                String[] temp = name.split(" ");
                Log.d(TAG, "run: temp.length = " + temp.length);
                Log.d(TAG, "run: temp[0] = " + Integer.parseInt(temp[0]));
                Log.d(TAG, "run: temp[1] = " + temp[1]);
                UnitySendMessage("ChessManager", "ChangeNameInUI", name);
                Log.d(TAG, "change name in ui:" + name);
            }


            UnitySendMessage("ChessManager", "intoReplayMode", "0");

//            //设置滚动条的名字
//            for(int pos = 0; pos < 4; pos++){
//                if(init_num[pos] != NOT_USE_KIND) {
//                    String pos_string = Integer.toString(pos);
//                    UnitySendMessage("ChessManager", "ChangeNameInUI", pos_string + " " + init_name[pos]);
//                }
//            }

            long time_st = new Date().getTime(),time_ed;
            waitEndTurn = false;
            while(!isStop)
            {
                //暂停状态,不播放动画
                if(isPause)
                {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "flag 0");
                    continue;
                }

                while(waitEndTurn)
                {
                    if(isSlided) break;
                    if(isStop) break;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "flag  1");
                }



                //请求清空unity的Action队列
                if(index != size){
                    try {
                        sleep(400);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    UnitySendMessage("ChessManager","flushActionQueue","0");
                }


                //先恢复棋盘,再跑记录
                if(index != size)
                {
                    try {
                        sleep(400);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    sendActionToUnity(recover_game.get(index));//恢复游戏
                }
                for(;index < size; index ++)
                {
                    if(isSlided) break;
                    if(isStop) break;
                    //如果点击了暂停键
                    while(isPause)
                    {
                        if(isSlided) break;
                        if(isStop) break;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "flag 2 ");
                    }
                    while(waitEndTurn)
                    {
                        if(isSlided) break;
                        if(isStop) break;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "flag 3 ");
                    }
                    time_ed = new Date().getTime();
                    if((time_ed - time_st) < 2000)
                    {
                        long time_left = time_ed - time_st;
                        while(true)
                        {
                            if(isSlided) break;
                            if(isStop) break;
                            time_left -=100;
                            if(time_left < 0) break;
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    waitEndTurn = true;
                    Vector<Action> actionVector = action_list.get(index);

                    //发送当前进度点给unity
                    //sendPercentToUnity(index*1.0/((size-1)*1.0)
                    float percent_ = (float)(index*1.0/((size-1)*1.0));
                    String progress = Float.toString(percent_);
                    UnitySendMessage("PlayandPause","setProgressPercentage", progress);

                    for(Action action:actionVector)
                    {
                        if(isSlided) break;
                        if(isStop) break;
                        //如果点击了暂停键
                        while(isPause)
                        {
                            if(isSlided) break;
                            if(isStop) break;
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Log.d(TAG, "flag 4");
                        }
                        //发送Action给unity
                        sendSingleActionToUnity(action);
                    }

                    time_st = new Date().getTime();

                    if(actionVector.get(actionVector.size()-1).getAction() == Action.SHOWTURN){
                        Log.d(TAG, " showturn");
                        setEndThisTurn();
                    }
                }
                if(index == size)
                {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //投骰子的力与骰子结果对应表
    private static float force_table[];
    static
    {
        force_table = new float[7];
        force_table[1] = 0.80f;
        force_table[2] = 0.85f;
        force_table[3] = 0.63f;
        force_table[4] = 0.75f;
        force_table[5] = 0.90f;
        force_table[6] = 0.70f;

        //0.63 0.70 0.75  0.80 0.85 0.90
    }

    public  float random_network_force() {
        int num = ((int)(Math.random()*10000000))%6 + 1;
        return force_table[num];
    }

    //随机产生投骰子的力
    //力的范围在0.5f~1.0f
    public float randomForce(){

        float min = 0.65f;
        float max = 0.95f;
        float floatBounded = min + new Random().nextFloat() * (max - min);
        Log.d(TAG, "Android create a random force for Unity, force equals " + floatBounded);

        if(game_mode == SINGLE_MODE) {
            has_given_force = true;
        }
        else if(game_mode == NETWORK_MODE){

            floatBounded = random_network_force();
            final float m_force = floatBounded;
            new Thread()
            {
                @Override
                public void run() {
                    //发送设置has_given_force为true的指令给服务器 指令4 参数:指令号,房间名字 数据:无
                    Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_UNITY_UPDATE_UI);

                    msg_send.set_msgUnity(new MsgUnity(MsgUnity.SET_GIVEN_FORCE),net_room_name,m_force);

                    //连接服务器,发送请求
                    Socket socket = null;
                    try {
                        socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //protocol对象序列化传送
                    Protocol.socketSerilize(socket,msg_send);

                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        return floatBounded;
    }

    //RecyclerView添加元素
    private void recyclerview_addItem(String str){
//        if(game_mode == SINGLE_MODE) {
//            final ActionAdapter adapter = new ActionAdapter(messages);
//            //注册adapter的Observer
//            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
//                @Override
//                public void onChanged() {
//                    super.onChanged();
//                }
//            });
//            final int pre_pos = adapter.getItemCount() - 1;
//            Action_message message = new Action_message(str);
//            messages.add(message);
//            //虽然该函数在Android的主线程中，但是Unity调用该函数可能创建子线程
//            //所以需要使用runOnUIThread
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    action_recycleview.setAdapter(adapter);
//                    action_recycleview.scrollToPosition(pre_pos);
//                    int cur_pos = adapter.getItemCount() - 1;
//                    action_recycleview.smoothScrollToPosition(cur_pos);
//                }
//            });
//        }
    }

    //Unity调用setDice函数
    private void Player_setDice(int dice){
        if(game_mode == SINGLE_MODE){
            Log.d(TAG, "Player_setDice: " + dice);
            runThread.gameManager.setDice(dice);
        }
        else if(game_mode == NETWORK_MODE){
            //发送set_dice的指令给服务器 指令5 参数:指令号,房间名字 数据:int dice
            //记得在服务器判断下:只有当前颜色的玩家才可以setdice

            final int m_dice = dice;
            new Thread()
            {
                @Override
                public void run() {

                    Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_UNITY_UPDATE_UI);

                    msg_send.set_msgUnity(new MsgUnity(MsgUnity.SET_DICE), net_room_name, m_dice);

                    //连接服务器,发送请求
                    Socket socket = null;
                    try {
                        socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //protocol对象序列化传送
                    Protocol.socketSerilize(socket,msg_send);

                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        else if(game_mode == HISTORY_MODE)
        {
            historyThread.setEndThisTurn();
        }
    }

    //Unity调用setChess函数
    private void Player_setChess(int choice){
        int choose_turn = choice / 4;
        int choose_num = choice % 4;
        if(game_mode == SINGLE_MODE) {
            if (choose_turn == runThread.gameManager.getTurn() && runThread.gameManager.isChoiceAvailable(choose_num)) {
                runThread.gameManager.setChoice(choose_num);
            } else {
                recyclerview_addItem("Please choose the legal chess.");
            }
        }
        else if(game_mode == NETWORK_MODE){

            final int m_turn = choose_turn;
            final int m_num = choose_num;
            new Thread()
            {
                @Override
                public void run() {

                    //发送 设置 choose_turn,choose_num的指令给服务器 指令6 参数:指令号,房间名字 数据:choose_turn,choose_num
                    Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_UNITY_UPDATE_UI);

                    msg_send.set_msgUnity(new MsgUnity(MsgUnity.SET_CHOICE),net_room_name,m_turn,m_num);

                    //连接服务器,发送请求
                    Socket socket = null;
                    try {
                        socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //protocol对象序列化传送
                    Protocol.socketSerilize(socket,msg_send);

                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    //玩家托管
    private void Player_switchToAI(){

        if(game_mode == SINGLE_MODE){
            //因为这里是单机游戏，所以定义单机游戏：只有玩家一（红色）才是人，其他都是AI
            //如果是我的回合
            if(runThread.gameManager.getTurn() == Chess.RED)
            {
                //已经扔骰子了吗？

                //还在等待骰子，说明还没扔骰子
                if(runThread.gameManager.waitDice())
                {
                    //如果以前是AI模式，说明现在需要取消托管
                    if(runThread.gameManager.isAI(Chess.RED))
                    {

                        //当前状态：现在是我的回合，并且我想切换到手动模式，并且还没有扔骰子

                        //下一步：直接显示扔骰子的按钮，切换到User模式，让玩家自己扔就完事了
                        //但是还得注意下：设置玩家选择不能选择棋子，以免误触

                        //显示扔骰子的按钮

                        runThread.gameManager.switchToUser(Chess.RED);

                    }
                    //以前不是AI模式，说明现在要进行托管
                    else
                    {

                        //当前状态：现在是我的回合，并且我现在想切换到AI托管模式，并且我还没有扔骰子（即没点击那个扔骰子的按钮）

                        //下一步：直接取消显示扔骰子的按钮，并且随机生成一个骰子，并用gamemanager.setDice(),同时把设置玩家不能选择棋子，以免误触

                        //顺序好像挺重要的，先切换模式，再setDice

                        //取消显示扔骰子的按钮
//                    message.what = EXECUTE_ACTION;
//                    sendSingleActionToUnity(runThread.gameManager.hide_dice());

                        runThread.gameManager.switchToAI(Chess.RED);//先切换

                        if(!has_given_force) {
                            Message message = new Message();
                            int dice = runThread.newDice();
                            message.what = THROWDICE;
                            Bundle data = new Bundle();
                            data.putInt("dice", dice);
                            message.setData(data);
                            handler.sendMessage(message);
                        }
                        //runThread.gameManager.setDice(dice);//setDice 放最后

                    }
                }
                //已经扔了骰子
                else
                {
                    //还没选择棋子
                    if(runThread.gameManager.waitChoice())
                    {
                        //如果以前是AI模式，说明现在需要取消托管
                        if(runThread.gameManager.isAI(Chess.RED))
                        {
                            //当前状态：我想切换到手动模式，并且已经扔了骰子了，并且我还没有选择棋子

                            runThread.gameManager.switchToUser(Chess.RED);
                        }
                        //以前不是AI模式，说明现在要进行托管
                        else
                        {
                            //当前状态：我想切换到AI托管，并且我已经扔了骰子了，并且我还没有选择棋子

                            //下一步：让AI替你选择呗，先切换，再选择

                            runThread.gameManager.switchToAI(Chess.RED);

                            int playerid = runThread.gameManager.getTurn();
                            int choice = runThread.gameManager.getAIChoice();//AI选择一个棋子

                            //最后要设置棋子
                            runThread.gameManager.setChoice(choice);

                        }
                    }
                    //已经选择棋子了
                    else
                    {
                        //既然现在是我的回合，并且已经选好棋子了，那就可以直接切换呀

                        //如果以前是AI模式，说明现在需要取消托管
                        if(runThread.gameManager.isAI(Chess.RED))
                        {
                            runThread.gameManager.switchToUser(Chess.RED);
                        }
                        //以前不是AI模式，说明现在要进行托管
                        else{
                            runThread.gameManager.switchToAI(Chess.RED);
                        }
                    }

                }

            }
            //如果不是我的回合，直接切换就好
            else
            {
                //如果以前是AI模式，说明现在需要取消托管
                if(runThread.gameManager.isAI(Chess.RED))
                {
                    runThread.gameManager.switchToUser(Chess.RED);
                }
                //以前不是AI模式，说明现在要进行托管
                else{
                    runThread.gameManager.switchToAI(Chess.RED);
                }
            }
        }
        else if(game_mode == NETWORK_MODE){
            //发送切换为AI的指令给服务器 指令7 参数:指令号,房间名字 数据:无

            new Thread()
            {
                @Override
                public void run() {
                    Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_UNITY_UPDATE_UI);

                    msg_send.set_msgUnity(new MsgUnity(MsgUnity.SET_SWITCH),net_room_name);

                    //连接服务器,发送请求
                    Socket socket = null;
                    try {
                        socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //protocol对象序列化传送
                    Protocol.socketSerilize(socket,msg_send);

                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }


    }

    //回合结束
    private void End_Turn(){
        Log.d(TAG, "This turn end.");
        if(game_mode == SINGLE_MODE) {
            runThread.gameManager.setEndthisturn();
        }
        else if(game_mode == NETWORK_MODE){

            new Thread()
            {
                @Override
                public void run() {

                    //发送end_this_turn的指令给服务器 指令8 参数:指令号,房间名 数据:无; ps:只有玩家颜色为第一个玩家,服务器才响应请求,设置setEndThisTurn
                    //记得在服务器判断一下颜色
                    Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_UNITY_UPDATE_UI);

                    msg_send.set_msgUnity(new MsgUnity(MsgUnity.SET_END_THIS_TURN), net_room_name);

                    //连接服务器,发送请求
                    Socket socket = null;
                    try {
                        socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //protocol对象序列化传送
                    Protocol.socketSerilize(socket,msg_send);

                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();


        }
        else if(game_mode == HISTORY_MODE)
        {
            historyThread.setEndThisTurn();
        }

    }

    //游戏结束,让UI显示排行榜、播放音乐
    //单机模式和联机模式都进行相同的操作
    private void Game_End_Info(String str){
        Log.d(TAG, "Returning the game result to Unity. Calling the function Game_Enc_Info()");
        UnitySendMessage("ChessManager","gameEndAction",str);
    }

    //单机环境-用户接收Unity传来的Player数和AI数
    public void set_Player_AI_num(int mplayer_num, int mai_num){
        Log.d(TAG, "Unity send num_Players and num_AIs to Android, Setting Player_AI_num");
        player_num = mplayer_num;
        ai_num = mai_num;
        Log.d(TAG, "set_Player_AI_num: " + Integer.toString(player_num) + " " + Integer.toString(ai_num));
        //将游戏模式设置为单机
        game_mode = SINGLE_MODE;
        Log.d(TAG, "Set single mode.");
    }

    //单机环境-进入选择角色房间，传入用户设定的Player数和AI数
    public void start_choose_room(){
        Log.d(TAG, "User send num_Players and num_AIs to Unity, Entering choose room");
        String num1 = Integer.toString(player_num);
        String num2 = Integer.toString(ai_num);
        String num = num1 + " " + num2;
        Log.d(TAG, "Set_Num" + num);
        UnitySendMessage("CharacterSelect", "Set_Num", num);
    }
    private boolean right_init_data()
    {
        int player_cnt = 0,ai_cnt = 0;
        for(int i = 0; i < 4; i++)
        {
            if(init_num[i] == PLAYER_KIND) player_cnt ++;
            else if(init_num[i] == AI_KIND) ai_cnt ++;
        }
        if(player_cnt == player_num && ai_cnt == ai_num) return  true;
        return  false;
    }
    //单机环境-获取创建房间的信息（玩家类型+名字）
    public void get_init_data(int[] num, String[] name_list){
        Log.d(TAG, "Trans_num: " + Integer.toString(num[0]) + " " + Integer.toString(num[1]) + " " + Integer.toString(num[2]) + " " + Integer.toString(num[3]));
        Log.d(TAG, "Trans_name: " + name_list[0] + " " + name_list[1] + " " + name_list[2] + " " + name_list[3]);
        for(int i = 0; i < 4; i++){
            if(num[i] == 0) init_num[i] = NOT_USE_KIND;
            else if(num[i] <= 4) init_num[i] = PLAYER_KIND;
            else if(num[i] <= 7) init_num[i] = AI_KIND;
            else Log.d(TAG, "UnExpected init_num");
        }
        for(int j = 0; j < 4; j++){
            if(init_num[j] != NOT_USE_KIND){
                if(init_num[j] == PLAYER_KIND){
                    if(name_list[j].compareTo("") != 0){
                        init_name[j] = name_list[j];
                    }else{
                        if(j == 0) init_name[j] = "红色玩家";
                        if(j == 1) init_name[j] = "黄色玩家";
                        if(j == 2) init_name[j] = "蓝色玩家";
                        if(j == 3) init_name[j] = "绿色玩家";
                    }
                }
                else{
                    if(j == 0) init_name[j] = "红色AI";
                    if(j == 1) init_name[j] = "黄色AI";
                    if(j == 2) init_name[j] = "蓝色AI";
                    if(j == 3) init_name[j] = "绿色AI";
                }
            }
            else{
                init_name[j] = "NULL";
            }
        }
        if(!right_init_data())
        {
            for(int i = 0 ; i < player_num; i ++)
            {
                init_name[i] = "玩家" + (i+1);
                init_num[i] = PLAYER_KIND;
            }
            for(int i = player_num; i < player_num + ai_num; i++)
            {
                init_name[i] = "ai" + (i+1);
                init_num[i] = AI_KIND;
            }
            for(int i = player_num + ai_num; i < 4; i++)
            {
                init_name[i] = "default";
                init_num[i] = NOT_USE_KIND;
            }
        }
        Log.d(TAG, "Init_num: " + Integer.toString(init_num[0]) + " " + Integer.toString(init_num[1]) + " " + Integer.toString(init_num[2]) + " " + Integer.toString(init_num[3]));
        Log.d(TAG, "Init_name: " + init_name[0] + " " + init_name[1] + " " + init_name[2] + " " + init_name[3]);
    }

    //按照给定数组创建房间
    void start_game(){
        Log.d(TAG, "start_game: called.");
        if(game_mode == SINGLE_MODE){
            runThread = new RunThread(new GameManager(init_num, init_name));
            Log.d(TAG, "User enter chess-room in single room. Starting the runThread.");
            //进程开始执行
            runThread.start();
        }
        else if(game_mode == NETWORK_MODE){

        }
        else if(game_mode == HISTORY_MODE)
        {
            Log.d(TAG, "User enter chess-room in history room. Starting the historyThread.");
            historyThread = new HistoryThread(history_name);
            historyThread.start();
        }
        else{
            Log.d(TAG, "start game error.");
        }

    }

    //中途退出游戏-点击右上角退出按钮
    void quit_game(){
        if(game_mode == SINGLE_MODE){
            Log.d(TAG, "User quit chess-room in single mode. Interrupting the runThread.");
            //中断单机游戏线程
            runThread.interrupt();
        }
        else if(game_mode == NETWORK_MODE){
            //请求当前玩家切换为AI
            try {
                send_exit_game(net_room_name);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(game_mode == HISTORY_MODE){
            Log.d(TAG, "User quit chess-room in history mode. Interrupting the runThread.");
            //中断回放游戏线程
            historyThread.interrupt();
        }
        else{
            Log.d(TAG, "quit game error.");
        }
    }

    /*
    联网请求
     */

    //发送请求刷新在线房间给服务器
    private void send_updateroom_request_to_server() {

        new Thread()
        {
            @Override
            public void run() {
                Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_MSG);

                msg_send.set_msgLogic(new MsgLogic(MsgLogic.REQUEST_ONLINE_ROOM));

                //连接服务器,发送请求
                Socket socket = null;
                try {
                    socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                    //protocol对象序列化传送
                    Protocol.socketSerilize(socket,msg_send);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
    //创建房间
    private void send_createroom_request_to_server(String roomname,String roomowner) throws IOException {

        net_room_name = roomname;
        net_player_name = roomowner;

        game_mode = NETWORK_MODE; //add1
        final String m_name = roomname;
        final String m_owner = roomowner;
        new Thread()
        {
            @Override
            public void run() {
                Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_MSG);

                msg_send.set_msgLogic(new MsgLogic(MsgLogic.REQUEST_CREATE_ROOM),m_name,m_owner);

                //连接服务器,发送请求
                Socket socket = null;
                try {
                    socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //protocol对象序列化传送
                Protocol.socketSerilize(socket,msg_send);

                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();


    }

    //请求刷新房间状态
    private void send_update_roomstatus_to_server() throws IOException {

        String roomname = net_room_name;

        final String m_roomname = roomname;
        new Thread()
        {
            @Override
            public void run() {
                Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_MSG);

                msg_send.set_msgLogic(new MsgLogic(MsgLogic.REQUEST_ROOM_STATUS),m_roomname);

                //连接服务器,发送请求
                Socket socket = null;
                try {
                    socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //protocol对象序列化传送
                Protocol.socketSerilize(socket,msg_send);

                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();


    }

    private void send_getxp_to_server()throws IOException{

        new Thread()
        {
            @Override
            public void run() {
                String roomname = net_room_name;

                Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_MSG);

                msg_send.set_msgLogic(new MsgLogic(MsgLogic.REQUEST_XP),roomname);

                //连接服务器,发送请求
                Socket socket = null;
                try {
                    socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //protocol对象序列化传送
                Protocol.socketSerilize(socket,msg_send);

                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();


    }

    //请求某个位置(包括退出某个房间时时,也调用这个函数)        //add2
    //int posId,int playerKind,String playerName,int state
    public void send_chair_status_to_server(String status) throws IOException {
        //status:posId playerKind playerName state

        final String m_status = status;
        new Thread()
        {
            @Override
            public void run() {
                String roomname = net_room_name;

                Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_MSG);
                msg_send.set_msgLogic(new MsgLogic(MsgLogic.REQUEST_CHAIR),roomname,m_status);

                //连接服务器,发送请求
                Socket socket = null;
                try {
                    socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //protocol对象序列化传送
                Protocol.socketSerilize(socket,msg_send);

                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();


    }

    //请求加入一个房间:ps 记得初始化一下net_roomname和net_playername!!!        //add2
    public void send_request_join_room(String roomname,String playername) throws IOException {

        net_player_name = playername;
        net_room_name = roomname;

        final String m_name = roomname;
        final String m_play = playername;
        new Thread()
        {
            @Override
            public void run() {
                Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_MSG);
                msg_send.set_msgLogic(new MsgLogic(MsgLogic.REQUEST_JOIN_ROOM),m_name,m_play);

                //连接服务器,发送请求
                Socket socket = null;
                try {
                    socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //protocol对象序列化传送
                Protocol.socketSerilize(socket,msg_send);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    //请求开始游戏
    public void send_request_start_game() throws IOException {

        new Thread()
        {
            @Override
            public void run() {
                String roomname = net_room_name;

                Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_MSG);
                msg_send.set_msgLogic(new MsgLogic(MsgLogic.REQUEST_START_GAME),roomname);

                //连接服务器,发送请求
                Socket socket = null;
                try {
                    socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //protocol对象序列化传送
                Protocol.socketSerilize(socket,msg_send);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    //中途退出游戏
    public void send_exit_game(String roomname) throws IOException {

        final String m_name = roomname;
        new Thread()
        {
            @Override
            public void run() {
                Protocol msg_send = new Protocol(Protocol.MSG_TYPE_LOGI_UNITY_UPDATE_UI);

                msg_send.set_msgUnity(new MsgUnity(MsgUnity.EXIT_GAME),m_name);

                //连接服务器,发送请求
                Socket socket = null;
                try {
                    socket = new Socket(UnifiedStandard.SERVER_ADDR, UnifiedStandard.SERVER_READ_PORT);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //protocol对象序列化传送
                Protocol.socketSerilize(socket,msg_send);

                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    /*******************************************************************/
    //关于历史记录回放的 调用方式: writeHistoryToFile(gameManager.get_file_name(),gameManager.getHistory()) add3
    public void writeHistoryToFile(String filename,String history) {
        FileOutputStream out = null;
        BufferedWriter writer = null;

        Log.d(TAG, "writeHistoryToFile original filename:" + filename);
        try {
            out = openFileOutput(filename, Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write(history);
            Log.d(TAG, "writeHistoryToFile success.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(writer != null)
            {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d(TAG, "writeHistoryToFile: " + getFileItem());
    }

    private static boolean isCorrectFileName(String filename) {
        if(filename.length() <= 18) return false;
        if(filename.charAt(4) ==  '-' && filename.charAt(7) == '-'
                && filename.charAt(10) == '-' && filename.charAt(13) == ':' && filename.charAt(16) == '_'
        )
            return true;
        else return false;

    }
    //以文件名的形式列出一条历史记录项
    public String getFileItem() {
        //文件名以"_"分隔
        File dir = getFilesDir();
        File [] allfile = dir.listFiles();
        String str = "";
        for(File file: allfile)
        {
            if(isCorrectFileName(file.getName()))
                str = str + file.getName() + " ";
            else Log.d(TAG, "getFileItem: Error File name appeared: " + file.getName());
        }
        return str;
    }

    public FileInputStream getFileStream(String filename) throws FileNotFoundException {
        return openFileInput(filename);
    }

    public void getHistoryName(String filename) {
        game_mode = HISTORY_MODE;
        history_name = filename;
        Log.d(TAG, "getHistoryName:" + history_name + " successful");
    }

    public void deleteFileByName(String fileName) {
        File dir = getFilesDir();
        File [] allfile = dir.listFiles();
        for(File file: allfile)
        {
            if(file.getName().equals(fileName))
            {
                file.delete();
                Log.d(TAG, "deleteFileByName delete file: " + fileName);
                return;
            }
        }
        Log.d(TAG, "deleteFileByName: Can not find the file: " + fileName);
    }

    //滑动进度条时调用,左键按下的瞬间调用slide();
    //左键跳起的瞬间先获取进度条百分比,然后setIndex(percent),再调用setContinous();
    
    //Unity在回放时调用以下函数
    public void main_setStop(){
        historyThread.setStop();
    }
    public void main_setPause(){
        historyThread.setPause();
    }
    public void main_setContinous(){
        Log.d(TAG, "main_setContinous:");
        historyThread.setContinous();
    }
    public void main_slide(){
        Log.d(TAG, "main_slide: ");
        historyThread.slide();
    }
    public void main_setIndex(String str){
        float percent = Float.parseFloat(str);
        Log.d(TAG, "main_setIndex: ");
        historyThread.setIndex(percent);
    }


}

