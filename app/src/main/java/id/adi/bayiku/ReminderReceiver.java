package id.adi.bayiku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Menampilkan pengingat, dan memasang ulang pengingat setelah HP dinyalakan ulang. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String a = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            Notif.rearm(c);
            return;
        }
        String id = intent.getStringExtra("id");
        if (id == null) return;
        Notif.showReminder(c, id, intent.getStringExtra("title"), intent.getStringExtra("body"));
    }
}
