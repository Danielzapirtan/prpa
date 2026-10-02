package com.example.therapyschedule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class AppointmentReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String patientName = intent.getStringExtra("patient_name");
        long appointmentId = intent.getLongExtra("appointment_id", -1L);
        AppointmentNotificationHelper.show(context, appointmentId, patientName);
    }
}
