package id.adi.bayiku;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import org.json.JSONObject;

/** Widget layar utama: status menyusu terakhir + tombol cepat. */
public class BayiWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager mgr, int[] ids) {
        refresh(c);
    }

    static void refresh(Context c) {
        String name = "Bayiku", status = "Buka aplikasi untuk mulai", sub = "";
        try {
            JSONObject o = new JSONObject(Notif.prefs(c).getString("widget", "{}"));
            name = o.optString("name", name);
            status = o.optString("status", status);
            sub = o.optString("sub", "");
        } catch (Exception ignored) { }
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        v.setTextViewText(R.id.w_name, name);
        v.setTextViewText(R.id.w_status, status);
        v.setTextViewText(R.id.w_sub, sub);
        v.setOnClickPendingIntent(R.id.w_root, Notif.openApp(c, null, null, 400));
        v.setOnClickPendingIntent(R.id.w_left, Notif.openApp(c, "feedStart", "L", 401));
        v.setOnClickPendingIntent(R.id.w_right, Notif.openApp(c, "feedStart", "R", 402));
        v.setOnClickPendingIntent(R.id.w_diaper, Notif.openApp(c, "diaper", "", 403));
        AppWidgetManager.getInstance(c).updateAppWidget(new ComponentName(c, BayiWidget.class), v);
    }
}
