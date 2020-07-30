package com.flux.test;

import org.junit.Test;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * @author hanlipeng
 * @date 2020/7/28
 */
public class TestFluxSimple {

    /**
     * 单纯构建一个队列并做输出.
     * 整体操作都在main线程并不会切换线程 具体输出可以看 {@link #testSubscribeOnMainThread()}
     */
    @Test
    public void testSimple() {
        Flux.range(1, 10)
                .subscribe(System.out::println);
    }

    /**
     * 单纯构建队列并做输出..输出时打印thread name 由于没有使用任何publishOn和subscribeOn方法..所以不会切换线程
     * 具体调度器相关的操作可以看{@link TestScheduler}
     */
    @Test
    public void testSubscribeOnMainThread() {
        Flux.range(1, 10)
                .subscribe(TestUtils::printValueAndThreadName);
    }

    /**
     * 类比jdk的stream方法,此方法中演示了map方法
     */
    @Test
    public void likeJdkStream1() {
        Flux.range(1, 10)
                //like peek
                .doOnNext(t -> System.out.println(String.format("first do on next {%s}", t)))
                //like stream.map
                .map(t -> t * 2)
                .doOnNext(t -> System.out.println(String.format("second do on next {%s}", t)))
                //like stream.foreach
                .subscribe(TestUtils::printValueAndThreadName);
    }

    /**
     * 使用doOnNext可以修改值中的内容
     */
    @Test
    public void testDoOnNext() {
        Flux.range(1, 10)
                .map(t -> {
                    HashMap<String, Integer> hashMap = new HashMap<>();
                    hashMap.put("value", t);
                    return hashMap;
                })
                //like peek
                .doOnNext(t -> t.put("value", 1))
                //like stream.foreach
                .subscribe(TestUtils::printValueAndThreadName);
    }

    /**
     * 类比jdk的stream方法,此方法中演示了flatmap方法,通过flatMap将结果翻倍了..
     */
    @Test
    public void likeJdkStream2() {
        Flux.range(1, 10)
                //like stream.flatMap
                .flatMap(t -> Flux.range(t, 2))
                //like stream.foreach
                .count()
                .subscribe(TestUtils::printValueAndThreadName);
    }

    /**
     * 甚至直接支持stream的collector的方法
     */
    @Test
    public void testCollectMethod() {
        Flux.range(1, 10)
                .collect(Collectors.toList())
                .subscribe(TestUtils::printValueAndThreadName);
    }

    /**
     * 类比Stream的groupBy方法 却别是Stream的groupBy方法是一个聚簇方法..会结束stream..
     * 而Flux的groupBy方法只是一个区分方法
     */
    @Test
    public void testGroupBy() {
        Flux.range(1, 10)
                .doOnNext(TestUtils::printValueAndThreadName)
                .groupBy(t -> t % 2)
                .subscribe(group -> group.subscribe(value -> TestUtils.printValueAndThreadName(String.format("key:{%s} value:{%s}", group.key(), value))));
    }

    /**
     * 类比Stream的filter方法
     */
    @Test
    public void likeFilter() {
        Flux.range(1, 10)
                .doOnNext(t -> System.out.println(String.format("before filter %s", t)))
                .filter(t -> t > 4)
                .doOnNext(t -> System.out.println(String.format("after filter %s", t)))
                .subscribe(TestUtils::printValueAndThreadName);
    }

}
