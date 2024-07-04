package com.heima.utils.threadlocal;

public class WmThreadLocalUtil {

    private final static ThreadLocal<Integer> WM_USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 添加用户
     */
    public static void  setUser(Integer userId){
        WM_USER_THREAD_LOCAL.set(userId);
    }

    /**
     * 获取用户
     */
    public static Integer getUserId(){
        return WM_USER_THREAD_LOCAL.get();
    }

    /**
     * 清理用户
     */
    public static void clear(){
        WM_USER_THREAD_LOCAL.remove();
    }
}