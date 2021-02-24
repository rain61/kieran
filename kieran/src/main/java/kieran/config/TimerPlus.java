package kieran.config;

import java.util.Timer;
import java.util.TimerTask;

public class TimerPlus extends Timer {



    @Override
    public void schedule(TimerTask task, long delay) {
        super.schedule(task, delay);
    }
}
