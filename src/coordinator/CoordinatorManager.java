package coordinator;

import server.Server;
import util.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import model.Message;

// Manages coordinator logic and periodic ping to all clients
public class CoordinatorManager {
    // Ping interval in seconds
    private static final int PING_INTERVAL = 60;
    // private static final int PING_TIMEOUT  = 180;  // timeout time 

    private final Server server;
    private final ScheduledExecutorService scheduler;

    //Singleton instance
    private static CoordinatorManager instance;

     // Track last successful ping time 
    //private long lastPingTime = System.currentTimeMillis();

    private CoordinatorManager(Server server) {
        this.server = server;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(); //replace
        //this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        //    Thread t = new Thread(r, "PingScheduler");
        //    t.setDaemon(true); // doesn't block shutdown
        //    return t; });
    }






    public static CoordinatorManager getInstance(Server server) {
        if (instance == null) {
            instance = new CoordinatorManager(server);
        }
        return instance;
    }
    // Starts periodic ping to all clients
    public void startPing() {
        scheduler.scheduleAtFixedRate(() -> {
            String coordinatorId = server.getCoordinatorId();

            if (coordinatorId == null) {
                Logger.getInstance().logSystem(
                        "No coordinator — skipping ping" //chnge?? to ogger only???
                        //Logger.getInstance().logSystem("No coordinator — skipping ping") 

                );
                return;
            }

            //belowe suggestion changess for check timeout
            //long now = System.currentTimeMillis();
            //    long gapSeconds = (now - lastPingTime) / 1000;
            //    if (gapSeconds > PING_TIMEOUT) {
            //        Logger.getInstance().logSystem(
            //                "WARNING: Ping gap was " + gapSeconds + "s — exceeds timeout of " + PING_TIMEOUT + "s");}
 
            //    lastPingTime = now;
            //    Logger.getInstance().logSystem("Sending coordinator ping. Coordinator: " + coordinatorId);
 
                //  Build ping using Message.serialize() 
            //    String pingMessage = new Message(
            //            Message.Type.SYSTEM,
            //            "SERVER",
            //            null,
            //            "PING — Coordinator is: " + coordinatorId
            //    ).serialize();
 
            //} catch (Exception e) {
                //  Fault tolerance  just to show what we know what it is? Or keep it low??
            //    Logger.getInstance().logSystem("Ping task error: " + e.getMessage());}
 
        //}, PING_INTERVAL, PING_INTERVAL, TimeUnit.SECONDS);
 
        //Logger.getInstance().logSystem("Ping started every " + PING_INTERVAL + " seconds");}





            Logger.getInstance().logSystem(
                    "Coordinator ping: " + coordinatorId
            );

            // Broadcast ping to all clients
            server.broadcast(
                    "PING|SYSTEM|null|Coordinator is: " + coordinatorId,
                    null
            );

        }, PING_INTERVAL, PING_INTERVAL, TimeUnit.SECONDS);

        Logger.getInstance().logSystem("Ping started every " + PING_INTERVAL + " seconds");
    }

    // Stops the ping scheduler
    public void stopPing() {
        scheduler.shutdown();
        Logger.getInstance().logSystem("Ping stopped");
    }


}
