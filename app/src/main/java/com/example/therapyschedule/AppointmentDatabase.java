package com.example.therapyschedule;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

final class AppointmentDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "therapy_schedule.db";
    private static final int DATABASE_VERSION = 1;

    AppointmentDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE clients (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL COLLATE NOCASE UNIQUE)");
        db.execSQL("CREATE TABLE appointments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "client_id INTEGER NOT NULL REFERENCES clients(id), " +
                "starts_at INTEGER NOT NULL, " +
                "duration_minutes INTEGER NOT NULL CHECK(duration_minutes BETWEEN 15 AND 180), " +
                "status TEXT NOT NULL DEFAULT 'scheduled' CHECK(status IN ('scheduled', 'cancelled')))");
        db.execSQL("CREATE INDEX appointments_by_start ON appointments(starts_at, status)");
        db.execSQL("CREATE INDEX appointments_by_client ON appointments(client_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("No database migration exists for version " + newVersion);
    }

    List<Client> getClients() {
        ArrayList<Client> clients = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "clients", new String[]{"id", "name"}, null, null, null, null, "name COLLATE NOCASE")) {
            while (cursor.moveToNext()) {
                clients.add(new Client(cursor.getLong(0), cursor.getString(1)));
            }
        }
        return clients;
    }

    long addClient(String name) {
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        return getWritableDatabase().insertOrThrow("clients", null, values);
    }

    List<Appointment> getAppointmentsForDay(long dayStart, long nextDayStart) {
        ArrayList<Appointment> appointments = new ArrayList<>();
        String sql = "SELECT a.id, a.client_id, c.name, a.starts_at, a.duration_minutes " +
                "FROM appointments a JOIN clients c ON c.id = a.client_id " +
                "WHERE a.status = 'scheduled' AND a.starts_at >= ? AND a.starts_at < ? " +
                "ORDER BY a.starts_at";
        try (Cursor cursor = getReadableDatabase().rawQuery(
                sql, new String[]{Long.toString(dayStart), Long.toString(nextDayStart)})) {
            while (cursor.moveToNext()) {
                appointments.add(new Appointment(
                        cursor.getLong(0), cursor.getLong(1), cursor.getString(2),
                        cursor.getLong(3), cursor.getInt(4)));
            }
        }
        return appointments;
    }

    boolean addAppointment(long clientId, long startsAt, int durationMinutes) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long endsAt = startsAt + durationMinutes * 60_000L;
            try (Cursor cursor = db.rawQuery(
                    "SELECT 1 FROM appointments WHERE status = 'scheduled' " +
                            "AND starts_at < ? AND starts_at + duration_minutes * 60000 > ? LIMIT 1",
                    new String[]{Long.toString(endsAt), Long.toString(startsAt)})) {
                if (cursor.moveToFirst()) {
                    return false;
                }
            }
            ContentValues values = new ContentValues();
            values.put("client_id", clientId);
            values.put("starts_at", startsAt);
            values.put("duration_minutes", durationMinutes);
            db.insertOrThrow("appointments", null, values);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    void cancelAppointment(long appointmentId) {
        ContentValues values = new ContentValues();
        values.put("status", "cancelled");
        getWritableDatabase().update(
                "appointments", values, "id = ? AND status = 'scheduled'",
                new String[]{Long.toString(appointmentId)});
    }

    static final class Client {
        final long id;
        final String name;

        Client(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static final class Appointment {
        final long id;
        final long clientId;
        final String clientName;
        final long startsAt;
        final int durationMinutes;

        Appointment(long id, long clientId, String clientName, long startsAt, int durationMinutes) {
            this.id = id;
            this.clientId = clientId;
            this.clientName = clientName;
            this.startsAt = startsAt;
            this.durationMinutes = durationMinutes;
        }
    }
}
