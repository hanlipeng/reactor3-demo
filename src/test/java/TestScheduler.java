import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;

/**
 * 优先给出结论 :
 * <p>
 * subscribeOn改变的是publisher的运行线程
 * <p>
 * publishOn改变的是publishOn后的方法运行的线程
 * <p>
 * 此段引用reactor指南的话
 *
 * <link url= "https://htmlpreview.github.io/?https://github.com/get-set/reactor-core/blob/master-zh/src/docs/index.html#schedulers" />
 * 基于此，我们仔细研究一下 publishOn 和 subscribeOn 这两个操作符：
 * <p>
 * publishOn 的用法和处于订阅链（subscriber chain）中的其他操作符一样。
 * 它将上游 信号传给下游，同时执行指定的调度器 Scheduler 的某个工作线程上的回调。 它会 改变后续的操作符的执行所在线程 （直到下一个 publishOn 出现在这个链上）。
 * <p>
 * subscribeOn 用于订阅（subscription）过程，作用于那个向上的订阅链（发布者在被订阅 时才激活，订阅的传递方向是向上游的）。
 * 所以，无论你把 subscribeOn 至于操作链的什么位置， 它都会影响到源头的线程执行环境（context）。 但是，它不会影响到后续的 publishOn，后者仍能够切换其后操作符的线程执行环境。
 *
 * @author hanlipeng
 * @date 2020/7/28
 */
public class TestScheduler {

    /**
     * 此方法表示当使用subscribeOn方法之后会切换subscribe运行时线程
     */
    @Test
    public void testSubscribeOnOtherThread() throws InterruptedException {
        Flux.range(1, 10)
                .subscribeOn(Schedulers.parallel())
                .subscribe(TestUtils::printValueAndThreadName);
        TimeUnit.SECONDS.sleep(1);
    }

    /**
     * 此方法表示当使用publishOn到其他线程时subscribe也会运行到对应的线程上
     */
    @Test
    public void testPublishOnOtherThread() throws InterruptedException {
        Flux.range(1, 10)
                .publishOn(Schedulers.parallel())
                .subscribe(TestUtils::printValueAndThreadName);
        TimeUnit.SECONDS.sleep(1);
    }

    /**
     * 当发布到其他线程上时其他线程的daemon属性是true
     * 主线程销毁时工作线程也会销毁.
     * 所以最后需要使用TimeUnit.sleep来组织进程结束
     */
    @Test
    public void testSchedulerDaemonStatus() throws InterruptedException {
        Flux.range(1, 1)
                .publishOn(Schedulers.parallel())
                .subscribe(value -> {
                    Thread thread = Thread.currentThread();
                    System.out.println(String.format("thread name : {%s}, thread daemon status: {%s}", thread.getName(), thread.isDaemon()));
                    TestUtils.printValueAndThreadName(value);
                });
        Flux.range(1, 1)
                .subscribeOn(Schedulers.parallel())
                .subscribe(value -> {
                    Thread thread = Thread.currentThread();
                    System.out.println(String.format("thread name : {%s}, thread daemon status: {%s}", thread.getName(), thread.isDaemon()));
                    TestUtils.printValueAndThreadName(value);
                });
        TimeUnit.SECONDS.sleep(1);
    }

    /**
     * 当使用publishOn时消费线程会切换到parallel线程上但是创建线程还是在main线程上
     */
    @Test
    public void testCreateAndPublishOtherThread() throws InterruptedException {
        Flux.create(t -> {
            System.out.println(String.format("flux create on Thread : {%s}", Thread.currentThread().getName()));
            t.next(1);
            t.complete();
        }).publishOn(Schedulers.parallel())
                .subscribe(TestUtils::printValueAndThreadName);
        TimeUnit.SECONDS.sleep(1);
    }

    /**
     * 但当使用subscribeOn时创建线程也会切换到parallel
     */
    @Test
    public void testCreateAndSubscribeOnOtherThread() throws InterruptedException {
        Flux.create(t -> {
            System.out.println(String.format("flux create on Thread : {%s}", Thread.currentThread().getName()));
            t.next(1);
            t.complete();
        }).subscribeOn(Schedulers.parallel())
                .subscribe(TestUtils::printValueAndThreadName);
        TimeUnit.SECONDS.sleep(1);
    }

    /**
     * 但当使用subscribeOn时创建线程也会切换到parallel
     */
    @Test
    public void testCreateAndPublishOnOtherThreadThenSubscribeOnOtherThread() throws InterruptedException {
        Flux.create(t -> {
            System.out.println(String.format("flux create on Thread : {%s}", Thread.currentThread().getName()));
            t.next(1);
            t.complete();
        })
                .publishOn(Schedulers.elastic())
                .map(t -> {
                    TestUtils.printValueAndThreadName(t);
                    return t;
                })
                .subscribeOn(Schedulers.parallel())
                .subscribe(TestUtils::printValueAndThreadName);
        TimeUnit.SECONDS.sleep(1);
    }

    /**
     * subscribeOn会改变整个流运行源头的线程,写在任何地方不会影响结果
     * publishOn会改变之后线程运行的线程
     */
    @Test
    public void testChangeSchedulers() {
        Flux.generate(t -> {
            TestUtils.printThreadName("on generate ");
            t.next(1);
            t.complete();
        })
                .doOnNext(t -> TestUtils.printThreadName("after generate "))
                .publishOn(Schedulers.parallel())
                .doOnNext(t -> TestUtils.printThreadName("after publish on parallel "))
                .publishOn(Schedulers.single())
                .doOnNext(t -> TestUtils.printThreadName("after publish on single "))
                .subscribeOn(Schedulers.elastic())
                .doOnNext(t -> TestUtils.printThreadName("after subscribe on elastic  "))
                .subscribe(TestUtils::printValueAndThreadName);
        TestUtils.sleepOneSecond();
    }


    /**
     * 多次subscribeOn会选取最后一次subscribeOn选择的调度器.
     * 盲猜在webflux的框架中会在消费之前改变订阅的频道
     */
    @Test
    public void testSubscribeOnTwice() {
        Flux.generate(t -> {
            TestUtils.printThreadName("on generate ");
            t.next(1);
            t.complete();
        })
                .subscribeOn(Schedulers.single())
                .subscribeOn(Schedulers.newSingle("second-single"))
                .subscribe(TestUtils::printValueAndThreadName);
        TestUtils.sleepOneSecond();
    }


}
