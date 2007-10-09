package bright.obb;

public class TimeBomb extends Thread {
    long delay;                 // delay time in milliseconds 
    SimpleListener callback;    // callback object
    boolean disarmed;           // has the bomb been disarmed?
   
    public TimeBomb(long delay, SimpleListener callback) {
        this.delay    = delay;
        this.callback = callback;
    }
   
    public synchronized void run() {
        try {
            wait(delay);
        }
        catch (InterruptedException ie) { }
      
        if (!disarmed) 
            callback.act(null);
    }
   
    public synchronized void disarm() {
        disarmed = true;
    }
}
