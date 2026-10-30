package local.qingyin.mobile;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/** Recovered from 0.7.19; resource cleanup repaired after decompilation. */
public final class LocalNodeHost {
    private static final String TAG = "QingyinLocalNode";
    private static final AtomicBoolean started = new AtomicBoolean();
    // The preserved JNI library requires this class and native method name.
    private static native int startNodeWithArguments(String[] args);
    private LocalNodeHost() { }

    public static void start(Context context) {
        if (!started.compareAndSet(false, true)) return;
        try {
            System.loadLibrary("node");
            System.loadLibrary("qingyin_node");
            File root = new File(context.getFilesDir(), "node-runtime");
            if (!root.isDirectory() && !root.mkdirs()) {
                Log.e(TAG, "Cannot prepare node directory");
                return;
            }
            final File script = new File(root, "runner.cjs");
            try (InputStream source = context.getAssets().open("node/runner.cjs");
                 OutputStream dest = new FileOutputStream(script, false)) {
                byte[] buffer = new byte[16384];
                int count;
                while ((count = source.read(buffer)) != -1) dest.write(buffer, 0, count);
            }
            final String workspace = root.getAbsolutePath();
            Thread engine = new Thread(() -> {
                try {
                    int exit = startNodeWithArguments(new String[]{"node", script.getAbsolutePath(), workspace});
                    Log.w(TAG, "Embedded Node runtime exited " + exit);
                } catch (Throwable failure) {
                    Log.e(TAG, "Embedded Node runtime crashed", failure);
                }
            }, "Qingyin-Standalone-Node");
            engine.setDaemon(true);
            engine.start();
        } catch (java.io.IOException failure) {
            Log.e(TAG, "Cannot extract concept backend", failure);
        } catch (UnsatisfiedLinkError failure) {
            Log.e(TAG, "Device ABI is unsupported for embedded online service", failure);
        }
    }
}
