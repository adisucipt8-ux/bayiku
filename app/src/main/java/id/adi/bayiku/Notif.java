package id.adi.bayiku;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

/** Notifikasi timer yang sedang berjalan dan pengingat terjadwal. */
final class Notif {
    static final String CH_TIMER = "timer";
    static final String CH_REMIND = "reminder";
    static final String PREFS = "bayiku";
    private static final String[] KINDS = {"feed", "sleep", "tummy", "pmk"};

    private Notif() { }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void ensureChannels(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel t = new NotificationChannel(CH_TIMER, "Timer berjalan", NotificationManager.IMPORTANCE_LOW);
        t.setDescription("Menampilkan timer menyusu, tidur, dan tummy time");
        t.setShowBadge(false);
        nm.createNotificationChannel(t);
        NotificationChannel r = new NotificationChannel(CH_REMIND, "Pengingat", NotificationManager.IMPORTANCE_HIGH);
        r.setDescription("Pengingat menyusu, imunisasi, dan obat/vitamin");
        nm.createNotificationChannel(r);
    }

    static Notification.Builder builder(Context c, String channel) {
        ensureChannels(c);
        if (Build.VERSION.SDK_INT >= 26) return new Notification.Builder(c, channel);
        return new Notification.Builder(c);
    }

    static PendingIntent openApp(Context c, String act, String arg, int req) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (act != null) {
            i.setAction("id.adi.bayiku.ACT." + act + "." + arg);
            i.putExtra("act", act);
            i.putExtra("arg", arg);
        }
        return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static int timerId(String kind) {
        for (int i = 0; i < KINDS.length; i++) if (KINDS[i].equals(kind)) return 100 + i;
        return 199;
    }

    /** json: [{kind,label,base,paused,elapsed}] */
    static void showTimers(Context c, String json) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        boolean[] active = new boolean[KINDS.length];
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String kind = o.getString("kind");
                int id = timerId(kind);
                if (id < 199) active[id - 100] = true;
                boolean paused = o.optBoolean("paused", false);
                Notification.Builder b = builder(c, CH_TIMER)
                        .setSmallIcon(R.drawable.ic_stat)
                        .setContentTitle(o.optString("label", "Timer"))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setContentIntent(openApp(c, null, null, id))
                        .addAction(new Notification.Action.Builder((android.graphics.drawable.Icon) null, "Selesai",
                                openApp(c, "stop", kind, id + 50)).build());
                if (paused) {
                    long s = o.optLong("elapsed", 0) / 1000;
                    b.setContentText("Dijeda · " + (s / 60) + " menit " + (s % 60) + " detik");
                } else {
                    b.setUsesChronometer(true).setShowWhen(true).setWhen(o.getLong("base"))
                            .setContentText("Ketuk untuk membuka Bayiku");
                }
                nm.notify(id, b.build());
            }
        } catch (Exception ignored) { }
        for (int i = 0; i < KINDS.length; i++) if (!active[i]) nm.cancel(100 + i);
    }

    // ---------- pengingat ----------

    private static PendingIntent reminderIntent(Context c, String id, String title, String body) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction("id.adi.bayiku.REM." + id);
        i.putExtra("id", id);
        i.putExtra("title", title);
        i.putExtra("body", body);
        return PendingIntent.getBroadcast(c, id.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** json: [{id,at,title,body}] — mengganti semua pengingat lama. */
    static void schedule(Context c, String json) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        SharedPreferences p = prefs(c);
        try {
            JSONArray old = new JSONArray(p.getString("rem", "[]"));
            for (int i = 0; i < old.length(); i++) {
                JSONObject o = old.getJSONObject(i);
                am.cancel(reminderIntent(c, o.getString("id"), "", ""));
            }
        } catch (Exception ignored) { }
        p.edit().putString("rem", json).apply();
        arm(c, am, json);
    }

    static void rearm(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) arm(c, am, prefs(c).getString("rem", "[]"));
    }

    private static void arm(Context c, AlarmManager am, String json) {
        long now = System.currentTimeMillis();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                long at = o.getLong("at");
                if (at <= now) continue;
                PendingIntent pi = reminderIntent(c, o.getString("id"), o.optString("title"), o.optString("body"));
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (Exception ignored) { }
    }

    static void showReminder(Context c, String id, String title, String body) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        Notification n = builder(c, CH_REMIND)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(openApp(c, null, null, 300))
                .build();
        nm.notify(1000 + (id.hashCode() & 0xffff), n);
    }
}
