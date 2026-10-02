package com.example.therapyschedule;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.concurrent.TimeUnit;

final class AppointmentReminderScheduler {
    static final String CHANNEL_ID = "therapy_appointment_reminders";
    private static final long REMINDER_OFFSET_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private AppointmentReminderScheduler() {
    }

    static void schedule(Context context, long appointmentId, long startsAt, String patientName) {
        long reminderAt = startsAt - REMINDER_OFFSET_MILLIS;
        if (reminderAt <= System.currentTimeMillis()) {
            return;
        }

        Intent intent = new Intent(context, AppointmentReminderReceiver.class);
        intent.setAction("com.example.therapyschedule.REMINDER_" + appointmentId);
        intent.putExtra("appointment_id", appointmentId);
        intent.putExtra("patient_name", patientName == null ? "" : patientName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) appointmentId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setWindow(
                        AlarmManager.RTC_WAKEUP,
                        reminderAt,
                        TimeUnit.MINUTES.toMillis(1),
                        pendingIntent
                );
                return;
            }

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent);
        } catch (SecurityException ignored) {
            alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    reminderAt,
                    TimeUnit.MINUTES.toMillis(1),
                    pendingIntent
            );
        }
    }

    static void cancel(Context context, long appointmentId) {
        Intent intent = new Intent(context, AppointmentReminderReceiver.class);
        intent.setAction("com.example.therapyschedule.REMINDER_" + appointmentId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) appointmentId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent == null) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        pendingIntent.cancel();
    }
}
