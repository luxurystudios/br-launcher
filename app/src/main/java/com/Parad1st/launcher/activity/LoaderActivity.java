package com.byparad1st.launcher.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.byparad1st.game.R;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadQueueSet;
import com.liulishuo.filedownloader.FileDownloadSampleListener;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoaderActivity extends AppCompatActivity {
    static final String BASE = "https://huggingface.co/datasets/LuxuryStudios/br-cache/resolve/main/";

    RoundCornerProgressBar progressbar;
    TextView textProgress, textLoading;
    File root;
    long totalBytes, doneBytes;
    int totalFiles, finished, failed;
    final Map<Integer, Integer> partial = new ConcurrentHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_loader);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        progressbar = (RoundCornerProgressBar) findViewById(R.id.progress_bar);
        textProgress = (TextView) findViewById(R.id.loading_percent);
        textLoading = (TextView) findViewById(R.id.loading_text);
        FileDownloader.setup(this);
        FileDownloader.getImpl().setMaxNetworkThreadCount(4);
        root = Environment.getExternalStorageDirectory();
        startDownload();
    }

    public void startDownload() {
        textLoading.setText("Verificando archivos del juego...");
        new Thread(() -> {
            try {
                final List<String> paths = new ArrayList<>();
                final List<Long> sizes = new ArrayList<>();
                HttpURLConnection c = (HttpURLConnection) new URL(BASE + "manifest.txt").openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                String line;
                boolean first = true;
                while ((line = br.readLine()) != null) {
                    if (first) { line = line.replace("\uFEFF", ""); first = false; }
                    line = line.trim();
                    int i = line.lastIndexOf('|');
                    if (i <= 0) continue;
                    String rel = line.substring(0, i);
                    long size = Long.parseLong(line.substring(i + 1).trim());
                    File f = new File(root, local(rel));
                    if (f.isFile() && f.length() == size) continue; // ya está completo
                    if (f.exists()) f.delete(); // tamaño distinto: rehacer
                    paths.add(rel);
                    sizes.add(size);
                }
                br.close();
                runOnUiThread(() -> launch(paths, sizes));
            } catch (Exception e) {
                runOnUiThread(() -> fail());
            }
        }).start();
    }

    private void launch(List<String> paths, List<Long> sizes) {
        totalFiles = paths.size();
        if (totalFiles == 0) { afterDownload(); return; }
        for (long s : sizes) totalBytes += s;

        FileDownloadSampleListener listener = new FileDownloadSampleListener() {
            @Override
            protected void progress(BaseDownloadTask task, int soFarBytes, int total) {
                partial.put(task.getId(), soFarBytes);
                updateUi();
            }

            @Override
            protected void completed(BaseDownloadTask task) {
                partial.remove(task.getId());
                doneBytes += (Long) task.getTag();
                finished++;
                updateUi();
                checkDone();
            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {
                partial.remove(task.getId());
                failed++;
                finished++;
                checkDone();
            }

            @Override
            protected void warn(BaseDownloadTask task) {
                failed++;
                finished++;
                checkDone();
            }
        };

        List<BaseDownloadTask> tasks = new ArrayList<>();
        for (int i = 0; i < totalFiles; i++) {
            String rel = paths.get(i);
            File f = new File(root, local(rel));
            f.getParentFile().mkdirs();
            tasks.add(FileDownloader.getImpl().create(BASE + encodePath(rel))
                    .setPath(f.getAbsolutePath(), false)
                    .setTag(sizes.get(i))
                    .setCallbackProgressTimes(20)
                    .setMinIntervalUpdateSpeed(200));
        }
        FileDownloadQueueSet qs = new FileDownloadQueueSet(listener);
        qs.setAutoRetryTimes(3);
        qs.downloadTogether(tasks);
        qs.start();
    }

    private static String local(String rel) { return rel.replaceFirst("^BlackRussia/", "LUXRUSSIA/"); }

    private static String encodePath(String rel) {
        String[] parts = rel.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(Uri.encode(parts[i])); // UTF-8, espacios -> %20
        }
        return sb.toString();
    }

    private void updateUi() {
        long cur = doneBytes;
        for (int v : partial.values()) cur += v;
        int pct = totalBytes > 0 ? (int) (cur * 100L / totalBytes) : 0;
        textLoading.setText("Descargando el juego... " + finished + "/" + totalFiles);
        textProgress.setText(human(cur) + " de " + human(totalBytes));
        progressbar.setProgress(pct);
    }

    private static String human(long b) {
        if (b >= 1073741824L) return String.format("%.2f GB", b / 1073741824.0);
        return String.format("%.0f MB", b / 1048576.0);
    }

    private void checkDone() {
        if (finished < totalFiles) return;
        if (failed > 0) fail(); else afterDownload();
    }

    private void fail() {
        Toast.makeText(getApplicationContext(), "Ocurrió un error, inicia la instalación de nuevo", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void afterDownload() {
        Toast.makeText(this, "¡Juego instalado con éxito!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
