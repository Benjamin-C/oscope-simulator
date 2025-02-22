package dev.orangeben.scopeviz;

public abstract class Retimer {

    private double targetTPS;
    private double tps;
    private boolean running = false;
    private SyncSampleBuffer buff;
    private Thread timer;
    private String name;
    private long last;
    private long targetTickTime;
    private long tpslast;
    private int count;

    public Retimer(double tps, int count, int buffsize) {
        this(tps, count, buffsize, "retimer");
    }
    public Retimer(double tps, int count, int buffsize, String name) {
        this.targetTPS = tps;
        this.count = count;
        this.buff = new SyncSampleBuffer(buffsize);
        this.name = name;
        targetTickTime = (long) (1e9 / targetTPS);
    }

    public void start() {
        running = true;
        timer = new Thread(name) {
            @Override
            public void run() {
                while(running) {
                    try {
                        long tdiff = (last + targetTickTime) - System.nanoTime();
                        if(tdiff <= 0) {
                            for(int i = 0; i < count; i++) {
                                short[] s = buff.get();
                                if(s.length == 2) {
                                    tick(s[0], s[1]);
                                } else {
                                    System.out.println("Buffer " + name + " empty!");
                                }
                            }
                            long ntdiff = System.nanoTime() - (last + targetTickTime);
                            tps = 1e9d / (System.nanoTime() - tpslast);
                            tpslast = System.nanoTime();
                            if(ntdiff > targetTickTime) {
                                double skip = ntdiff / targetTickTime;
                                System.out.println(String.format("[%s] Behind, skipping %.1f ticks with %d waiting", name, skip, buff.getSize()));
                                last = System.nanoTime();
                            } else {
                                last += targetTickTime;
                            }
                        } else {
                            try {
                                sleep((long) (tdiff / 1e6), (int) (tdiff % 1e6));
                            } catch (InterruptedException e) {
                                // continue as normal
                            }
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
                }
            }
        };
        timer.start();
    }

    public void stop() {
        running = false;
        try {
            timer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean add(short l, short r) {
        return buff.add(l, r);
    }

    public double getTPS() {
        return tps;
    }

    public abstract void tick(short l, short r);

}
