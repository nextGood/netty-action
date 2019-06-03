package thread;

import java.util.concurrent.TimeUnit;

/**
 * volatile使用
 *
 * @author nextGood
 * @date 2019/5/15
 */
public class VolatileTest {
    private static boolean stop;

    public static void main(String[] args) throws Exception {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (!stop) {
                    i++;
                    System.out.println(i);
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        TimeUnit.SECONDS.sleep(3);
        stop = true;
    }
}