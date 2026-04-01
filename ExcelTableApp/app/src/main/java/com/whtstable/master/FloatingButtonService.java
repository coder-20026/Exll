package com.whtstable.master;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FloatingButtonService extends Service {
    
    private static final String CHANNEL_ID = "WhtsTableFloatingChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    private WindowManager windowManager;
    private View floatingButton;
    private View livePreviewPopup;
    private View closeTarget;
    private WindowManager.LayoutParams buttonParams;
    private WindowManager.LayoutParams livePreviewParams;
    private WindowManager.LayoutParams closeTargetParams;
    private DatabaseHelper dbHelper;
    
    // Bubble drag variables
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    
    // Live Preview drag variables
    private int previewInitialX, previewInitialY;
    private float previewInitialTouchX, previewInitialTouchY;
    private boolean isPreviewDragging = false;
    
    // Live Preview views
    private EditText etLiveDate, etLiveBank, etLiveName, etLiveReason, etLiveCoords, etLiveArea;
    private TextView tvRowNumber, tvDatePreview;
    private Button btnLiveOk, btnLiveClear;
    private LinearLayout livePreviewContainer;
    
    // Screen dimensions
    private int screenHeight;
    private int screenWidth;
    private int closeTargetHeight = 120;
    
    // Clipboard monitoring
    private ClipboardManager clipboardManager;
    private ClipboardManager.OnPrimaryClipChangedListener clipListener;
    private String lastClipboardText = "";
    private String lastProcessedText = "";
    private String initialClipboardText = "";
    private Handler clipboardHandler;
    private Runnable clipboardRunnable;
    private boolean isInitialized = false;
    
    // State
    private boolean isLivePreviewShowing = false;
    private String currentMonth;
    private String currentYear;
    private String currentDay = "";
    private int entryCount = 0;
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        dbHelper = DatabaseHelper.getInstance(this);
        currentMonth = dbHelper.getSelectedMonth();
        currentYear = dbHelper.getSelectedYear();
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Get screen dimensions
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;
        
        // Start as foreground service for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());
        }
        
        createFloatingButton();
        setupClipboardMonitoring();
        
        // Show live preview immediately (always visible)
        showLivePreview("", "", "", 0);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "WhtsTable Quick Entry",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Quick Entry floating button service");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        
        return builder
            .setContentTitle("WhtsTable Quick Entry")
            .setContentText("Copy WhatsApp text - Auto fills popup")
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
    
    private void setupClipboardMonitoring() {
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardHandler = new Handler();
        
        // Store current clipboard text at enable time - we will IGNORE this
        try {
            if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
                ClipData clipData = clipboardManager.getPrimaryClip();
                if (clipData != null && clipData.getItemCount() > 0) {
                    CharSequence text = clipData.getItemAt(0).getText();
                    if (text != null) {
                        initialClipboardText = text.toString().trim();
                        lastClipboardText = initialClipboardText;
                    }
                }
            }
        } catch (Exception e) {}
        
        // Mark as initialized after short delay
        clipboardHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isInitialized = true;
            }
        }, 500);
        
        // Poll clipboard every 500ms
        clipboardRunnable = new Runnable() {
            @Override
            public void run() {
                checkClipboard();
                clipboardHandler.postDelayed(this, 500);
            }
        };
        clipboardHandler.postDelayed(clipboardRunnable, 1000);
        
        // Also use listener as backup
        clipListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                if (isInitialized) {
                    checkClipboard();
                }
            }
        };
        clipboardManager.addPrimaryClipChangedListener(clipListener);
    }
    
    private void checkClipboard() {
        if (!isInitialized) return;
        
        try {
            if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
                ClipData clipData = clipboardManager.getPrimaryClip();
                if (clipData != null && clipData.getItemCount() > 0) {
                    CharSequence text = clipData.getItemAt(0).getText();
                    if (text != null && text.length() > 0) {
                        String newText = text.toString().trim();
                        
                        // Skip if same as initial, last, or already processed
                        if (newText.equals(initialClipboardText) ||
                            newText.equals(lastClipboardText) ||
                            newText.equals(lastProcessedText)) {
                            return;
                        }
                        
                        lastClipboardText = newText;
                        onClipboardTextDetected(newText);
                    }
                }
            }
        } catch (Exception e) {}
    }
    
    private void onClipboardTextDetected(String text) {
        lastProcessedText = text;
        
        // Make bubble look blurred/dim
        setBubbleBlurred(true);
        
        // Auto-parse the text
        String bankName = extractBankName(text);
        String applicantName = extractApplicantName(text);
        String reasonOfCNV = extractReasonOfCNV(text);
        
        // Update live preview with parsed data
        updateLivePreview(bankName, applicantName, reasonOfCNV);
    }
    
    private void setBubbleBlurred(boolean blurred) {
        if (floatingButton != null) {
            ImageView btnImage = (ImageView) floatingButton.findViewById(R.id.floatingButton);
            if (btnImage != null) {
                btnImage.setAlpha(blurred ? 0.5f : 1.0f);
            }
        }
    }
    
    private void createCloseTarget() {
        if (closeTarget != null) return;
        
        closeTarget = new TextView(this);
        TextView tv = (TextView) closeTarget;
        tv.setText("X CLOSE");
        tv.setTextSize(14);
        tv.setTextColor(0xFFFFFFFF);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(0xFFFF5252);
        tv.setPadding(0, 16, 0, 16);
        
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        closeTargetParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            closeTargetHeight,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        );
        
        closeTargetParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        
        windowManager.addView(closeTarget, closeTargetParams);
    }
    
    private void removeCloseTarget() {
        if (closeTarget != null) {
            try {
                windowManager.removeView(closeTarget);
            } catch (Exception e) {}
            closeTarget = null;
        }
    }
    
    private void createFloatingButton() {
        floatingButton = LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null);
        
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        buttonParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        buttonParams.gravity = Gravity.TOP | Gravity.START;
        buttonParams.x = 0;
        buttonParams.y = 200;
        
        windowManager.addView(floatingButton, buttonParams);
        
        final ImageView btnImage = (ImageView) floatingButton.findViewById(R.id.floatingButton);
        
        btnImage.setOnTouchListener(new View.OnTouchListener() {
            private Handler longPressHandler = new Handler();
            private Runnable longPressRunnable = new Runnable() {
                @Override
                public void run() {
                    isDragging = true;
                    createCloseTarget();
                }
            };
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = buttonParams.x;
                        initialY = buttonParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        longPressHandler.postDelayed(longPressRunnable, 200);
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true;
                            longPressHandler.removeCallbacks(longPressRunnable);
                            createCloseTarget();
                        }
                        
                        buttonParams.x = initialX + deltaX;
                        buttonParams.y = initialY + deltaY;
                        windowManager.updateViewLayout(floatingButton, buttonParams);
                        
                        if (closeTarget != null) {
                            int bubbleBottom = buttonParams.y + 60;
                            if (bubbleBottom > screenHeight - closeTargetHeight - 100) {
                                ((TextView) closeTarget).setBackgroundColor(0xFFD32F2F);
                            } else {
                                ((TextView) closeTarget).setBackgroundColor(0xFFFF5252);
                            }
                        }
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        longPressHandler.removeCallbacks(longPressRunnable);
                        
                        int bubbleBottom = buttonParams.y + 60;
                        if (isDragging && bubbleBottom > screenHeight - closeTargetHeight - 100) {
                            removeCloseTarget();
                            closeLivePreview();
                            stopSelf();
                            Toast.makeText(FloatingButtonService.this, "Quick Entry closed", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        
                        removeCloseTarget();
                        return true;
                }
                return false;
            }
        });
    }
    
    private void showLivePreview(String bank, String name, String reason, int rowIndex) {
        if (livePreviewPopup != null) {
            return;  // Already showing
        }
        
        livePreviewPopup = LayoutInflater.from(this).inflate(R.layout.live_preview_layout, null);
        
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        livePreviewParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        );
        
        livePreviewParams.gravity = Gravity.TOP | Gravity.START;
        livePreviewParams.x = 10;
        livePreviewParams.y = 50;
        
        windowManager.addView(livePreviewPopup, livePreviewParams);
        isLivePreviewShowing = true;
        
        initializeLivePreviewViews();
    }
    
    private void updateLivePreview(String bank, String name, String reason) {
        if (etLiveBank != null) etLiveBank.setText(bank);
        if (etLiveName != null) etLiveName.setText(name);
        if (etLiveReason != null) etLiveReason.setText(reason);
        if (etLiveCoords != null) etLiveCoords.setText("");
        if (etLiveArea != null) etLiveArea.setText("");
        
        entryCount++;
        if (tvRowNumber != null) tvRowNumber.setText("#" + entryCount);
    }
    
    private void initializeLivePreviewViews() {
        livePreviewContainer = (LinearLayout) livePreviewPopup.findViewById(R.id.livePreviewContainer);
        etLiveDate = (EditText) livePreviewPopup.findViewById(R.id.etLiveDate);
        etLiveBank = (EditText) livePreviewPopup.findViewById(R.id.etLiveBank);
        etLiveName = (EditText) livePreviewPopup.findViewById(R.id.etLiveName);
        etLiveReason = (EditText) livePreviewPopup.findViewById(R.id.etLiveReason);
        etLiveCoords = (EditText) livePreviewPopup.findViewById(R.id.etLiveCoords);
        etLiveArea = (EditText) livePreviewPopup.findViewById(R.id.etLiveArea);
        tvRowNumber = (TextView) livePreviewPopup.findViewById(R.id.tvRowNumber);
        tvDatePreview = (TextView) livePreviewPopup.findViewById(R.id.tvDatePreview);
        btnLiveOk = (Button) livePreviewPopup.findViewById(R.id.btnLiveOk);
        btnLiveClear = (Button) livePreviewPopup.findViewById(R.id.btnLiveClear);
        TextView btnClose = (TextView) livePreviewPopup.findViewById(R.id.btnCloseLivePreview);
        View livePreviewHeader = livePreviewPopup.findViewById(R.id.livePreviewHeader);
        
        tvRowNumber.setText("Ready");
        updateDatePreview();
        
        // Date field listener - auto update preview
        etLiveDate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateDatePreview();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Header drag for live preview
        if (livePreviewHeader != null) {
            livePreviewHeader.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            previewInitialX = livePreviewParams.x;
                            previewInitialY = livePreviewParams.y;
                            previewInitialTouchX = event.getRawX();
                            previewInitialTouchY = event.getRawY();
                            isPreviewDragging = false;
                            createCloseTarget();
                            return true;
                            
                        case MotionEvent.ACTION_MOVE:
                            int deltaX = (int) (event.getRawX() - previewInitialTouchX);
                            int deltaY = (int) (event.getRawY() - previewInitialTouchY);
                            
                            if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
                                isPreviewDragging = true;
                            }
                            
                            livePreviewParams.x = previewInitialX + deltaX;
                            livePreviewParams.y = previewInitialY + deltaY;
                            windowManager.updateViewLayout(livePreviewPopup, livePreviewParams);
                            
                            if (closeTarget != null) {
                                int popupBottom = livePreviewParams.y + 250;
                                if (popupBottom > screenHeight - closeTargetHeight - 50) {
                                    ((TextView) closeTarget).setBackgroundColor(0xFFD32F2F);
                                } else {
                                    ((TextView) closeTarget).setBackgroundColor(0xFFFF5252);
                                }
                            }
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                            int popupBottom = livePreviewParams.y + 250;
                            if (isPreviewDragging && popupBottom > screenHeight - closeTargetHeight - 50) {
                                removeCloseTarget();
                                closeLivePreview();
                                stopSelf();
                                Toast.makeText(FloatingButtonService.this, "Quick Entry closed", Toast.LENGTH_SHORT).show();
                                return true;
                            }
                            removeCloseTarget();
                            return true;
                    }
                    return false;
                }
            });
        }
        
        // Close button
        if (btnClose != null) {
            btnClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeLivePreview();
                    stopSelf();
                    Toast.makeText(FloatingButtonService.this, "Quick Entry closed", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // OK button - Save the entry
        btnLiveOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentEntry();
            }
        });
        
        // Clear button
        btnLiveClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLivePreviewFields();
            }
        });
    }
    
    private void updateDatePreview() {
        String day = etLiveDate != null ? etLiveDate.getText().toString().trim() : "";
        if (day.isEmpty()) {
            if (tvDatePreview != null) tvDatePreview.setText("");
            return;
        }
        
        // Pad day with zero if needed
        if (day.length() == 1) {
            day = "0" + day;
        }
        
        String fullDate = day + " " + currentMonth + " " + currentYear;
        if (tvDatePreview != null) tvDatePreview.setText(fullDate);
        currentDay = day;
    }
    
    private String getFullDate() {
        String day = etLiveDate != null ? etLiveDate.getText().toString().trim() : "";
        if (day.isEmpty()) return "";
        
        if (day.length() == 1) {
            day = "0" + day;
        }
        
        return day + " " + currentMonth + " " + currentYear;
    }
    
    private void saveCurrentEntry() {
        // Validate date
        String day = etLiveDate != null ? etLiveDate.getText().toString().trim() : "";
        if (day.isEmpty()) {
            Toast.makeText(this, "Enter date first!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get all values
        String bank = etLiveBank != null ? etLiveBank.getText().toString().trim() : "";
        String name = etLiveName != null ? etLiveName.getText().toString().trim() : "";
        String reason = etLiveReason != null ? etLiveReason.getText().toString().trim() : "";
        String coords = etLiveCoords != null ? etLiveCoords.getText().toString().trim() : "";
        String area = etLiveArea != null ? etLiveArea.getText().toString().trim() : "";
        
        // Validate at least some data
        if (bank.isEmpty() && name.isEmpty()) {
            Toast.makeText(this, "Enter bank or name!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create entry
        DatabaseHelper.EntryData entry = new DatabaseHelper.EntryData();
        entry.dateFull = getFullDate();
        entry.day = day.length() == 1 ? "0" + day : day;
        entry.month = currentMonth;
        entry.year = currentYear;
        entry.bankName = bank;
        entry.applicantName = name;
        entry.reasonCnv = reason;
        entry.latlongTo = coords;
        entry.area = area;
        
        // Save to database
        long id = dbHelper.addEntry(entry);
        
        if (id > 0) {
            Toast.makeText(this, "Saved: " + entry.dateFull, Toast.LENGTH_SHORT).show();
            
            // Clear fields but keep popup visible
            clearFieldsAndReset();
            setBubbleBlurred(false);
        } else {
            Toast.makeText(this, "Save failed!", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void clearFieldsAndReset() {
        // Clear all fields except date (keep date for continuous entry)
        if (etLiveBank != null) etLiveBank.setText("");
        if (etLiveName != null) etLiveName.setText("");
        if (etLiveReason != null) etLiveReason.setText("");
        if (etLiveCoords != null) etLiveCoords.setText("");
        if (etLiveArea != null) etLiveArea.setText("");
        if (tvRowNumber != null) tvRowNumber.setText("Ready");
        
        // Reset processed text
        lastProcessedText = "";
    }
    
    private void clearLivePreviewFields() {
        if (etLiveDate != null) etLiveDate.setText("");
        if (etLiveBank != null) etLiveBank.setText("");
        if (etLiveName != null) etLiveName.setText("");
        if (etLiveReason != null) etLiveReason.setText("");
        if (etLiveCoords != null) etLiveCoords.setText("");
        if (etLiveArea != null) etLiveArea.setText("");
        if (tvDatePreview != null) tvDatePreview.setText("");
        Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show();
    }
    
    private void closeLivePreview() {
        if (livePreviewPopup != null) {
            try {
                windowManager.removeView(livePreviewPopup);
            } catch (Exception e) {}
            livePreviewPopup = null;
            isLivePreviewShowing = false;
        }
    }
    
    // Parsing methods
    private String extractBankName(String text) {
        try {
            Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) {}
        return "";
    }
    
    private String extractReasonOfCNV(String text) {
        try {
            Pattern pattern = Pattern.compile("([^(]+)\\(");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) {}
        return "";
    }
    
    private String extractApplicantName(String text) {
        try {
            Pattern pattern = Pattern.compile(
                "(?i)Applic[a-z]*\\s*(?:Name)?\\s*:?\\s*[-]?\\s*([^\\n\\d]+?)(?=\\n|\\d+\\)|$)",
                Pattern.CASE_INSENSITIVE
            );
            
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String name = matcher.group(1).trim();
                name = name.replaceAll("[:\\-]+$", "").trim();
                name = name.replaceAll("\\s*\\d+[:\\)].*", "").trim();
                return name;
            }
        } catch (Exception e) {}
        return "";
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (clipboardManager != null && clipListener != null) {
            clipboardManager.removePrimaryClipChangedListener(clipListener);
        }
        if (clipboardHandler != null && clipboardRunnable != null) {
            clipboardHandler.removeCallbacks(clipboardRunnable);
        }
        
        if (floatingButton != null) {
            try {
                windowManager.removeView(floatingButton);
            } catch (Exception e) {}
        }
        if (livePreviewPopup != null) {
            try {
                windowManager.removeView(livePreviewPopup);
            } catch (Exception e) {}
        }
        if (closeTarget != null) {
            try {
                windowManager.removeView(closeTarget);
            } catch (Exception e) {}
        }
    }
}
