package cn.wkuny;

import java.util.LinkedList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 任务链类，用于按顺序执行可重试的任务。
 * 每个任务在执行时若抛出异常，将根据配置的重试策略进行重试。
 * 如果任务抛出满足 throwPredicates 中条件的异常或错误，则立即停止重试并返回失败。
 */
public class RetryTaskChain {
    LinkedList<TaskWrapper> tasks;
    LinkedList<Predicate<Throwable>> throwPredicates;

    /**
     * 构造函数，初始化任务链和异常判断条件列表。
     */
    public RetryTaskChain() {
        tasks = new LinkedList<>();
        throwPredicates = new LinkedList<>();
    }

    /**
     * 添加一个可重试的任务。
     *
     * @param task        要执行的任务
     * @param name        任务的名称
     * @param maxAttempts 最大尝试次数
     * @param beforeRetry 每次重试前执行的操作
     * @return 返回当前任务链实例，支持链式调用
     */
    public RetryTaskChain step(Runnable task, String name, int maxAttempts, Runnable beforeRetry) {
        tasks.add(new TaskWrapper(name, task, maxAttempts,beforeRetry));
        return this;
    }

    /**
     * 添加一个可重试的任务，使用默认任务名称。
     *
     * @param task        要执行的任务
     * @param maxAttempts 最大尝试次数
     * @param beforeRetry 每次重试前执行的操作
     * @return 返回当前任务链实例，支持链式调用
     */
    public RetryTaskChain step(Runnable task, int maxAttempts,Runnable beforeRetry) {
        return step(task, "Step " + (tasks.size()+1), maxAttempts, beforeRetry);
    }

    /**
     * 添加一个可重试的任务，使用默认任务名称且无重试操作。
     *
     * @param task        要执行的任务
     * @param maxAttempts 最大尝试次数
     * @return 返回当前任务链实例，支持链式调用
     */
    public RetryTaskChain step(Runnable task, int maxAttempts) {
        return step(task, "Step " + (tasks.size()+1), maxAttempts, null);
    }

    /**
     * 添加一个可重试的任务，无重试操作。
     *
     * @param task        要执行的任务
     * @param name        任务的名称
     * @param maxAttempts 最大尝试次数
     * @return 返回当前任务链实例，支持链式调用
     */
    public RetryTaskChain step(Runnable task, String name, int maxAttempts) {
        return step(task, name, maxAttempts, null);
    }

    /**
     * 添加一个用于判断是否立即停止重试的异常判断条件。
     *
     * @param predicate 异常判断条件
     * @return 返回当前任务链实例，支持链式调用
     */
    public RetryTaskChain throwIf(Predicate<Throwable> predicate) {
        throwPredicates.add(predicate);
        return this;
    }

    /**
     * 执行任务链中的所有任务。
     * 如果任务执行失败并抛出异常，将根据配置的策略进行重试。
     * 如果任务抛出满足 throwPredicates 中条件的异常或错误，则立即停止重试并返回失败。
     *
     * @return 如果所有任务成功执行，返回 null；否则返回最后一次失败的异常信息
     */
    public RetryTaskFailedException run(){
        long begin = System.currentTimeMillis();
        for (TaskWrapper task : tasks) {
            boolean exceptionThrown = false;
            Throwable throwable = null;
            for(int i=1; i<=task.maxAttempts; i++) {
                System.out.printf("[%s + %.3fs]: %s\n",Thread.currentThread().getName(),(System.currentTimeMillis()-begin)/1e3,task.name);
                if(i>=2) {
                    System.out.printf("任务%s正在重试第%d次\n", task.name, i);
                    task.beforeRetry();
                    exceptionThrown = false;
                }
                try{
                    task.run();
                } catch (RuntimeException | Error e) {
                    if(shouldThrow(e))
                        return new RetryTaskFailedException(task.name, i, e);
                    e.printStackTrace(System.err);
                    exceptionThrown = true;
                    throwable = e;
                }
                if(!exceptionThrown) break;
            }
            if(exceptionThrown) return new RetryTaskFailedException(task.name, task.maxAttempts, throwable);
        }
        return null;
    }

    /**
     * 判断给定的异常是否应该立即停止重试。
     *
     * @param throwable 要检查的异常
     * @return 如果应立即停止重试返回 true，否则返回 false
     */
    private boolean shouldThrow(Throwable throwable) {
        for(Predicate<Throwable> predicate : throwPredicates) {
            if(predicate.test(throwable)) return true;
        }
        return false;
    }


    /**
     * 任务包装类，用于封装任务及其相关配置。
     */
    private class TaskWrapper {
        final String name;
        final Runnable task,beforeRetry;
        final int maxAttempts;
        final Consumer consumer;
        final BiConsumer biConsumer;
        final Object arg1,arg2;

        /**
         * 构造一个任务包装器。
         *
         * @param name       任务的名称
         * @param task       要执行的任务
         * @param maxAttempts 最大尝试次数
         * @param beforeRetry 每次重试前执行的操作
         */
        TaskWrapper(String name,Runnable task, int maxAttempts, Runnable beforeRetry) {
            this.name = name;
            this.task = task;
            this.maxAttempts = maxAttempts;
            this.beforeRetry = beforeRetry;

            consumer = null;
            biConsumer = null;
            arg1 = arg2 = null;
        }

        /**
         * 构造一个任务包装器，用于接受一个参数的任务。
         *
         * @param name       任务的名称
         * @param task       要执行的任务
         * @param maxAttempts 最大尝试次数
         * @param beforeRetry 每次重试前执行的操作
         * @param arg1       传递给任务的第一个参数
         */
        TaskWrapper(String name, Consumer task, int maxAttempts, Runnable beforeRetry, Object arg1){
            this.name = name;
            this.consumer = task;
            this.maxAttempts = maxAttempts;
            this.beforeRetry = beforeRetry;
            this.arg1 = arg1;

            this.arg2 = null;
            this.task = null;
            this.biConsumer = null;
        }

        /**
         * 构造一个任务包装器，用于接受两个参数的任务。
         *
         * @param name       任务的名称
         * @param task       要执行的任务
         * @param maxAttempts 最大尝试次数
         * @param beforeRetry 每次重试前执行的操作
         * @param arg1       传递给任务的第一个参数
         * @param arg2       传递给任务的第二个参数
         */
        TaskWrapper(String name, BiConsumer task, int maxAttempts, Runnable beforeRetry, Object arg1, Object arg2){
            this.name = name;
            this.biConsumer = task;
            this.maxAttempts = maxAttempts;
            this.beforeRetry = beforeRetry;
            this.arg1 = arg1;
            this.arg2 = arg2;

            this.consumer = null;
            this.task = null;
        }

        /**
         * 执行任务。
         */
        void run() {
            if(task!=null) task.run();
            if(consumer!=null) consumer.accept(arg1);
            if(biConsumer!=null) biConsumer.accept(arg1,arg2);
        }

        /**
         * 执行重试前的操作。
         */
        void beforeRetry() {
            beforeRetry.run();
        }
    }
}
