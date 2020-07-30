package com.flux.test;

import java.util.concurrent.TimeUnit;

/**
 * @author hanlipeng
 * @date 2020/7/28
 */
public class TestUtils {
    /**
     * 输出值和当前线程名称
     *
     * @param value 值
     */
    public static void printValueAndThreadName(Object value) {
        System.out.println(String.format("thread name {%s}, value : {%s}", Thread.currentThread().getName(), value));
    }

    public static void printThreadName(String prefix) {
        System.out.println(prefix + String.format("current thread name is {%s}",Thread.currentThread().getName()));
    }

    public static void sleepOneSecond() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
