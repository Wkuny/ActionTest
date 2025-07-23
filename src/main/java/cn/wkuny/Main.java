package cn.wkuny;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static final Lock lock = new ReentrantLock();
    public static volatile String verifyCode = null;
    public static void main(String[] args) {

    }
}
