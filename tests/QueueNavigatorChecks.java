package local.qingyin.mobile;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

public final class QueueNavigatorChecks {
    private static void eq(int actual,int expected,String message) {
        if (actual!=expected) throw new AssertionError(message+": "+actual+" != "+expected);
    }
    public static void main(String[] ignored) {
        QueueNavigator q = new QueueNavigator(new Random(1213));
        eq(q.index(),-1,"empty");
        eq(q.step(1,true),-1,"empty advance");
        q.load(3,1);
        eq(q.step(1,true),2,"next");
        eq(q.step(1,true),0,"loop wrap");
        eq(q.step(-1,false),2,"previous wrap");
        q.setMode(QueueNavigator.Mode.ONE);
        eq(q.step(1,true),2,"single repeat");
        eq(q.step(1,false),0,"manual advance under ONE");
        q.setMode(QueueNavigator.Mode.ORDER);
        q.choose(2);
        eq(q.step(1,true),-1,"ordered end");
        eq(q.index(),2,"ordered end keeps selection");
        eq(q.step(1,false),2,"ordered manual end");
        q.choose(0);
        eq(q.step(-1,false),0,"ordered manual start");
        q.setMode(QueueNavigator.Mode.SHUFFLE);
        q.load(8,4);
        List<Integer> first = q.upcoming(15);
        eq(first.size(),8,"shuffle length");
        eq(first.get(0),4,"shuffle initial");
        eq(new HashSet<>(first).size(),8,"shuffle distinct");
        for (int i=1;i<8;i++) {
            eq(q.step(1,true),first.get(i),"shuffle advance "+i);
        }
        int last=q.index();
        int next=q.step(1,true);
        if (last==next) throw new AssertionError("shuffle cycle repeated current song");
        int backwards=q.step(-1,false);
        eq(backwards,last,"shuffle previous navigates actual history");
        q.choose(3);
        eq(q.index(),3,"select within shuffle");
        eq(q.upcoming(8).get(0),3,"selected first in shuffled queue");
        q.load(1,0);
        eq(q.step(1,true),0,"single shuffle song");
        q.load(0,0);
        eq(q.index(),-1,"empty load");
        int permutations = 0;
        for (int n = 1; n <= 40; n++) {
            for (int seed = 0; seed < 20; seed++) {
                QueueNavigator subject = new QueueNavigator(new Random(seed));
                subject.load(n, n / 2);
                subject.setMode(QueueNavigator.Mode.SHUFFLE);
                List<Integer> order = subject.upcoming(n);
                eq(order.size(), n, "permutation size");
                eq(new HashSet<>(order).size(), n, "permutation unique");
                for (int i = 1; i < n; i++) eq(subject.step(1, true), order.get(i), "permutation step");
                int previous = subject.index();
                int fresh = subject.step(1, true);
                if (n > 1 && fresh == previous)
                    throw new AssertionError("shuffle immediate repeat at new cycle");
                if (n > 1) eq(subject.step(-1, false), previous, "shuffle history");
                permutations++;
            }
        }
        System.out.println("QUEUE_TESTS_PASS 800 seeded permutations; 1-40 songs; queue boundaries");
    }
}
