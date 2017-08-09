package org.sprintdragon.pses.core.test;

import org.sprintdragon.pses.core.action.supprot.CompletableAdapterActionFuture;
import org.sprintdragon.pses.core.action.supprot.TaskActionFuture;

import java.util.concurrent.Callable;

/**
 * Created by wangdi on 17-8-9.
 */
public class AdapterActionFutureTest {

    public static void main(String[] args) throws Exception {
        testTaskFuture();
    }

    public static void testTaskFuture() throws Exception {
        TaskActionFuture<String> taskActionFuture = TaskActionFuture.newFuture(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(2000);
                return "finish";
            }
        });
        taskActionFuture.run();

        System.out.println("###" + taskActionFuture.actionGet());
    }

    public static void testCompletableFuture() throws Exception {
        CompletableAdapterActionFuture<String, String> adapterActionFuture = new CompletableAdapterActionFuture<String, String>() {
            @Override
            protected String convert(String listenerResponse) {
                return listenerResponse;
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                adapterActionFuture.complete("fini");
            }
        }).start();
        System.out.println("###" + adapterActionFuture.get());
    }

}
