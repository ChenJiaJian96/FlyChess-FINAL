package com.unity3d.flyingchess;

/**
 * Created by gitfan on 5/11/18.
 */

//道具类
//ps:修改道具种类时记得修改MIN,MAX
public class Prop{

    public static final int NO_PROP = 0;//没有道具


    //攻击距离
    public static final int RED_RANGE = 3;//小火龙攻击距离为3


    /***************************************************************************/
    //玩家主动技能
    //不能继续扔骰子的
    public static final int RED_CD = 1;

    //可以继续扔骰子的
    public static final int YELLOW_CD = 2;
    public static final int BLUE_CD = 3;
    public static final int GREEN_CD = 4;

    /***************************************************************************/
    //障碍类
    public static final int TORNADO = 5;//龙卷风
    public static final int BARRIER = 6;//路障
    public static final int BOMB = 7;//炸弹



    /***************************************************************************/
    //被动道具类
    public static final int PROTECTION =8;//保护罩
    public static final int DOUBLE = 9;//扔出点数翻倍
    public static final int ADD = 10;//扔出点数后再随机加一个数值
    public static final int AGAIN = 11;//再来一次



    //private static final int MIN = 1;
    private static final int MAX = 11;//道具的范围

    private int pos;
    private int prop;

    public Prop(int pos,int prop)
    {
        this.pos = pos;
        this.prop = prop;
    }
    public void clear()
    {
        pos = -1;
        prop = NO_PROP;
    }
    public void setProp(int prop)
    {
        this.prop = prop;
    }
    public void setPos(int pos)
    {
        this.pos = pos;
    }

    public int getPos(){ return pos; }
    public int getProp(){ return prop; }

    public static int randomKind()
    {
        return ((int)(Math.random()*100000004))%MAX + 1;
    }
}
