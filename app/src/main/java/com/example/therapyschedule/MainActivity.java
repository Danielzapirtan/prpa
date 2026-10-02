package com.example.therapyschedule;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int INK = Color.rgb(34, 48, 44);
    private static final int MUTED = Color.rgb(107, 120, 114);
    private static final int GREEN = Color.rgb(57, 118, 108);
    private static final int PALE_GREEN = Color.rgb(229, 240, 235);
    private static final int SURFACE = Color.WHITE;
    private static final int BACKGROUND = Color.rgb(245, 247, 245);
    private static final int LINE = Color.rgb(231, 236, 232);

    private AppointmentDatabase database;
    private LinearLayout root;
    private LinearLayout content;
    private Calendar selectedDay;
    private boolean showingPatients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new AppointmentDatabase(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        for (AppointmentDatabase.Appointment appointment : database.getUpcomingAppointments()) {
            AppointmentReminderScheduler.schedule(this, appointment.id, appointment.startsAt, appointment.clientName);
        }
        selectedDay = Calendar.getInstance();
        selectedDay.set(Calendar.HOUR_OF_DAY, 0);
        selectedDay.set(Calendar.MINUTE, 0);
        selectedDay.set(Calendar.SECOND, 0);
        selectedDay.set(Calendar.MILLISECOND, 0);
        Window window = getWindow();
        window.setStatusBarColor(BACKGROUND);
        window.setNavigationBarColor(SURFACE);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        render();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void render() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(18), dp(24), dp(24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView brand = text("PRIVATE PRACTICE", 11, GREEN, true);
        brand.setLetterSpacing(0.12f);
        content.addView(brand);
        addSpace(content, 8);
        content.addView(text(showingPatients ? "Patients" : "Schedule", 30, INK, true));
        addSpace(content, 5);
        content.addView(text("A calm space to keep your practice in order.", 14, MUTED, false));
        addSpace(content, 24);

        if (showingPatients) {
            renderPatients();
        } else {
            renderSchedule();
        }
        renderNavigation();
    }

    private void renderSchedule() {
        LinearLayout dateCard = card();
        content.addView(dateCard);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);
        dateCard.addView(dateRow);

        Button previous = textButton("‹");
        dateRow.addView(previous, buttonSize(42, 42));
        previous.setOnClickListener(v -> {
            selectedDay.add(Calendar.DAY_OF_MONTH, -1);
            render();
        });

        TextView dateLabel = text(dateTitle(selectedDay), 17, INK, true);
        dateLabel.setGravity(Gravity.CENTER);
        dateRow.addView(dateLabel, new LinearLayout.LayoutParams(0, dp(44), 1));
        dateLabel.setOnClickListener(v -> chooseDate(selectedDay, chosen -> {
            selectedDay = chosen;
            render();
        }));

        Button next = textButton("›");
        dateRow.addView(next, buttonSize(42, 42));
        next.setOnClickListener(v -> {
            selectedDay.add(Calendar.DAY_OF_MONTH, 1);
            render();
        });
        dateLabel.setOnLongClickListener(v -> {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            selectedDay = today;
            render();
            return true;
        });

        addSpace(content, 23);
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView dayHeading = text(isToday(selectedDay) ? "Today’s sessions" : "Sessions", 19, INK, true);
        heading.addView(dayHeading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button addSession = primaryButton("+  New session");
        addSession.setOnClickListener(v -> showAppointmentDialog(-1));
        heading.addView(addSession);
        content.addView(heading);
        addSpace(content, 14);

        long dayStart = selectedDay.getTimeInMillis();
        Calendar nextDay = (Calendar) selectedDay.clone();
        nextDay.add(Calendar.DAY_OF_MONTH, 1);
        List<AppointmentDatabase.Appointment> appointments =
                database.getAppointmentsForDay(dayStart, nextDay.getTimeInMillis());
        if (appointments.isEmpty()) {
            LinearLayout empty = card();
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(22), dp(30), dp(22), dp(30));
            empty.addView(text("Your day is open", 17, INK, true));
            addSpace(empty, 7);
            empty.addView(text("Add a session when you’re ready.", 14, MUTED, false));
            content.addView(empty);
        } else {
            for (AppointmentDatabase.Appointment appointment : appointments) {
                addAppointmentCard(appointment);
                addSpace(content, 10);
            }
        }
        addSpace(content, 18);
        TextView privacy = text("Your schedule is stored only on this device.", 12, MUTED, false);
        privacy.setGravity(Gravity.CENTER);
        content.addView(privacy);
    }

    private void addAppointmentCard(AppointmentDatabase.Appointment appointment) {
        LinearLayout item = card();
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(item);

        LinearLayout stripe = new LinearLayout(this);
        stripe.setBackground(round(GREEN, 8));
        item.addView(stripe, new LinearLayout.LayoutParams(dp(4), dp(48)));
        addSpaceHorizontal(item, 14);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        item.addView(details, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        details.addView(text(timeLabel(appointment.startsAt), 15, GREEN, true));
        addSpace(details, 5);
        details.addView(text(appointment.clientName, 17, INK, true));
        addSpace(details, 3);
        details.addView(text(appointment.durationMinutes + " min session", 13, MUTED, false));

        Button cancel = textButton("Cancel");
        item.addView(cancel, buttonSize(68, 40));
        cancel.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Cancel this session?")
                .setMessage("The appointment will be removed from your schedule.")
                .setNegativeButton("Keep session", null)
                .setPositiveButton("Cancel session", (dialog, which) -> {
                    database.cancelAppointment(appointment.id);
                    AppointmentReminderScheduler.cancel(this, appointment.id);
                    render();
                }).show());
    }

    private void renderPatients() {
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text("Your clients", 19, INK, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button addPatient = primaryButton("+  Add client");
        addPatient.setOnClickListener(v -> showClientDialog());
        heading.addView(addPatient);
        content.addView(heading);
        addSpace(content, 14);

        List<AppointmentDatabase.Client> clients = database.getClients();
        if (clients.isEmpty()) {
            LinearLayout empty = card();
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(22), dp(30), dp(22), dp(30));
            empty.addView(text("No clients yet", 17, INK, true));
            addSpace(empty, 7);
            empty.addView(text("Add a client to start scheduling sessions.", 14, MUTED, false));
            content.addView(empty);
            return;
        }

        for (AppointmentDatabase.Client client : clients) {
            LinearLayout item = card();
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setOrientation(LinearLayout.HORIZONTAL);
            content.addView(item);

            TextView initials = text(initials(client.name), 14, GREEN, true);
            initials.setGravity(Gravity.CENTER);
            initials.setBackground(round(PALE_GREEN, 28));
            item.addView(initials, buttonSize(44, 44));
            addSpaceHorizontal(item, 13);

            TextView name = text(client.name, 16, INK, true);
            item.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            Button schedule = textButton("Schedule");
            item.addView(schedule, buttonSize(82, 40));
            schedule.setOnClickListener(v -> {
                showAppointmentDialog(client.id);
            });
            addSpace(content, 10);
        }
    }

    private void renderNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(16), dp(10), dp(16), dp(10));
        nav.setBackgroundColor(SURFACE);
        nav.setElevation(dp(8));

        Button schedule = navigationButton("Schedule", !showingPatients);
        Button patients = navigationButton("Patients", showingPatients);
        nav.addView(schedule, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(patients, new LinearLayout.LayoutParams(0, dp(48), 1));
        schedule.setOnClickListener(v -> {
            showingPatients = false;
            render();
        });
        patients.setOnClickListener(v -> {
            showingPatients = true;
            render();
        });
        root.addView(nav);
    }

    private void showClientDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), dp(4));
        TextView label = text("Client name", 14, MUTED, true);
        form.addView(label);
        addSpace(form, 8);
        android.widget.EditText nameInput = new android.widget.EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setTextSize(16);
        nameInput.setHint("Full name");
        nameInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        form.addView(nameInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add a client")
                .setView(form)
                .setNegativeButton("Not now", null)
                .setPositiveButton("Save client", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        nameInput.setError("Enter a name");
                        return;
                    }
                    try {
                        database.addClient(name);
                        dialog.dismiss();
                        render();
                    } catch (android.database.sqlite.SQLiteConstraintException e) {
                        nameInput.setError("A client with this name already exists");
                    }
                }));
        dialog.show();
    }

    private void showAppointmentDialog(long preselectedClientId) {
        List<AppointmentDatabase.Client> clients = database.getClients();
        if (clients.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Add a client first")
                    .setMessage("You’ll need a client on your list before scheduling a session.")
                    .setPositiveButton("Add client", (dialog, which) -> {
                        showingPatients = true;
                        render();
                        showClientDialog();
                    })
                    .setNegativeButton("Not now", null)
                    .show();
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(4), dp(24), dp(4));
        form.addView(text("Client", 14, MUTED, true));
        addSpace(form, 6);
        Spinner clientPicker = new Spinner(this);
        ArrayList<String> names = new ArrayList<>();
        int initialSelection = 0;
        for (int i = 0; i < clients.size(); i++) {
            AppointmentDatabase.Client client = clients.get(i);
            names.add(client.name);
            if (client.id == preselectedClientId) initialSelection = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        clientPicker.setAdapter(adapter);
        clientPicker.setSelection(initialSelection);
        form.addView(clientPicker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        addSpace(form, 14);

        Calendar appointmentTime = (Calendar) selectedDay.clone();
        appointmentTime.set(Calendar.HOUR_OF_DAY, 9);
        appointmentTime.set(Calendar.MINUTE, 0);
        appointmentTime.set(Calendar.SECOND, 0);
        appointmentTime.set(Calendar.MILLISECOND, 0);

        Button datePicker = pickerButton(dateLabel(appointmentTime));
        Button timePicker = pickerButton(timeLabel(appointmentTime.getTimeInMillis()));
        datePicker.setOnClickListener(v -> chooseDate(appointmentTime, chosen -> {
            appointmentTime.set(Calendar.YEAR, chosen.get(Calendar.YEAR));
            appointmentTime.set(Calendar.MONTH, chosen.get(Calendar.MONTH));
            appointmentTime.set(Calendar.DAY_OF_MONTH, chosen.get(Calendar.DAY_OF_MONTH));
            datePicker.setText(dateLabel(appointmentTime));
        }));
        timePicker.setOnClickListener(v -> {
            TimePickerDialog picker = new TimePickerDialog(this, (TimePicker view, int hour, int minute) -> {
                appointmentTime.set(Calendar.HOUR_OF_DAY, hour);
                appointmentTime.set(Calendar.MINUTE, minute);
                timePicker.setText(timeLabel(appointmentTime.getTimeInMillis()));
            }, appointmentTime.get(Calendar.HOUR_OF_DAY), appointmentTime.get(Calendar.MINUTE), false);
            picker.show();
        });
        form.addView(text("Date", 14, MUTED, true));
        addSpace(form, 6);
        form.addView(datePicker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        addSpace(form, 12);
        form.addView(text("Start time", 14, MUTED, true));
        addSpace(form, 6);
        form.addView(timePicker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        addSpace(form, 12);
        form.addView(text("Length", 14, MUTED, true));
        addSpace(form, 6);
        Spinner durationPicker = new Spinner(this);
        String[] durations = {"30 minutes", "45 minutes", "50 minutes", "60 minutes", "75 minutes", "90 minutes"};
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, durations);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationPicker.setAdapter(durationAdapter);
        durationPicker.setSelection(2);
        form.addView(durationPicker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("New session")
                .setView(form)
                .setNegativeButton("Not now", null)
                .setPositiveButton("Save session", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int[] minutes = {30, 45, 50, 60, 75, 90};
                    int duration = minutes[durationPicker.getSelectedItemPosition()];
                    long startsAt = appointmentTime.getTimeInMillis();
                    long clientId = clients.get(clientPicker.getSelectedItemPosition()).id;
                    long appointmentId = database.addAppointment(clientId, startsAt, duration);
                    if (appointmentId != -1L) {
                        AppointmentReminderScheduler.schedule(
                                this,
                                appointmentId,
                                startsAt,
                                clients.get(clientPicker.getSelectedItemPosition()).name
                        );
                        dialog.dismiss();
                        showingPatients = false;
                        selectedDay.setTimeInMillis(startsAt);
                        selectedDay.set(Calendar.HOUR_OF_DAY, 0);
                        selectedDay.set(Calendar.MINUTE, 0);
                        selectedDay.set(Calendar.SECOND, 0);
                        selectedDay.set(Calendar.MILLISECOND, 0);
                        render();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Time already booked")
                                .setMessage("This session overlaps another scheduled appointment. Choose a different time.")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }));
        dialog.show();
    }

    private void chooseDate(Calendar initial, DateChosen callback) {
        DatePickerDialog picker = new DatePickerDialog(this,
                (DatePicker view, int year, int month, int day) -> {
                    Calendar chosen = (Calendar) initial.clone();
                    chosen.set(Calendar.YEAR, year);
                    chosen.set(Calendar.MONTH, month);
                    chosen.set(Calendar.DAY_OF_MONTH, day);
                    callback.onDate(chosen);
                },
                initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }

    private LinearLayout card() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(16), dp(16), dp(16), dp(16));
        view.setBackground(round(SURFACE, 18));
        view.setElevation(dp(1));
        return view;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(round(GREEN, 14));
        return button;
    }

    private Button textButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setTextColor(GREEN);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setMinWidth(0);
        button.setBackground(round(PALE_GREEN, 12));
        return button;
    }

    private Button navigationButton(String label, boolean selected) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(selected ? GREEN : MUTED);
        button.setBackgroundColor(Color.TRANSPARENT);
        return button;
    }

    private Button pickerButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(15);
        button.setTextColor(INK);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setAllCaps(false);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(round(BACKGROUND, 10));
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private LinearLayout.LayoutParams buttonSize(int width, int height) {
        return new LinearLayout.LayoutParams(dp(width), dp(height));
    }

    private void addSpace(LinearLayout parent, int height) {
        View spacer = new View(this);
        parent.addView(spacer, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private void addSpaceHorizontal(LinearLayout parent, int width) {
        View spacer = new View(this);
        parent.addView(spacer, new LinearLayout.LayoutParams(dp(width), 1));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String dateTitle(Calendar date) {
        if (isToday(date)) return "Today, " + new SimpleDateFormat("MMM d", Locale.getDefault()).format(date.getTime());
        return new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(date.getTime());
    }

    private String dateLabel(Calendar date) {
        return new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(date.getTime());
    }

    private String timeLabel(long time) {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(time);
    }

    private boolean isToday(Calendar date) {
        Calendar today = Calendar.getInstance();
        return date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    private String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.getDefault());
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase(Locale.getDefault());
    }

    private interface DateChosen {
        void onDate(Calendar date);
    }
}
