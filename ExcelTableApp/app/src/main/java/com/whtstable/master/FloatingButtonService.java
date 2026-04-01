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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
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
    private DataManager dataManager;
    
    // Bubble drag variables
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    
    // Live Preview drag variables
    private int previewInitialX, previewInitialY;
    private float previewInitialTouchX, previewInitialTouchY;
    private boolean isPreviewDragging = false;
    
    // Live Preview views
    private EditText etLiveBank, etLiveName, etLiveReason, etLiveCoords, etLiveArea;
    private TextView tvRowNumber;
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
    private Handler clipboardHandler;
    private Runnable clipboardRunnable;
    
    // State
    private boolean isLivePreviewShowing = false;
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        dataManager = DataManager.getInstance(this);
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
            .setContentText("Copy WhatsApp text - Live Preview appears")
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
    
    private void setupClipboardMonitoring() {
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardHandler = new Handler();
        
        // Poll clipboard every 500ms (more reliable than listener on some devices)
        clipboardRunnable = new Runnable() {
            @Override
            public void run() {
                checkClipboard();
                clipboardHandler.postDelayed(this, 500);
            }
        };
        clipboardHandler.postDelayed(clipboardRunnable, 500);
        
        // Also use listener as backup
        clipListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                checkClipboard();
            }
        };
        clipboardManager.addPrimaryClipChangedListener(clipListener);
    }
    
    private void checkClipboard() {
        try {
            if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
                ClipData clipData = clipboardManager.getPrimaryClip();
                if (clipData != null && clipData.getItemCount() > 0) {
                    CharSequence text = clipData.getItemAt(0).getText();
                    if (text != null && text.length() > 0) {
                        String newText = text.toString().trim();
                        // Only process if text is different and not empty
                        if (!newText.equals(lastClipboardText) && !newText.isEmpty()) {
                            lastClipboardText = newText;
                            onClipboardTextDetected(newText);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail - clipboard access can be restricted
        }
    }
    
    private void onClipboardTextDetected(String text) {
        // Make bubble look blurred/dim
        setBubbleBlurred(true);
        
        // Auto-parse the text
        String bankName = extractBankName(text);
        String applicantName = extractApplicantName(text);
        String reasonOfCNV = extractReasonOfCNV(text);
        
        // Create new row in data manager
        int newRowIndex = dataManager.createNewActiveRow(bankName, applicantName, reasonOfCNV);
        
        // Show live preview popup automatically
        showLivePreview(bankName, applicantName, reasonOfCNV, newRowIndex);
    }
    
    private void setBubbleBlurred(boolean blurred) {
        if (floatingButton != null) {
            ImageView btnImage = (ImageView) floatingButton.findViewById(R.id.floatingButton);
            if (btnImage != null) {
                if (blurred) {
                    btnImage.setAlpha(0.5f);
                } else {
                    btnImage.setAlpha(1.0f);
                }
            }
        }
    }
    
    private void createCloseTarget() {
        if (closeTarget != null) return;
        
        closeTarget = new TextView(this);
        TextView tv = (TextView) closeTarget;
        tv.setText("X Close");
        tv.setTextSize(16);
        tv.setTextColor(0xFFFFFFFF);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(0xFFFF5252);
        tv.setPadding(0, 20, 0, 20);
        
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
                        
                        // Highlight close target if near bottom
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
                        
                        // Check if dropped on close target
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
            // Update existing popup
            if (etLiveBank != null) etLiveBank.setText(bank);
            if (etLiveName != null) etLiveName.setText(name);
            if (etLiveReason != null) etLiveReason.setText(reason);
            if (etLiveCoords != null) etLiveCoords.setText("");
            if (etLiveArea != null) etLiveArea.setText("");
            if (tvRowNumber != null) tvRowNumber.setText("Row #" + (rowIndex + 1));
            return;
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
        // Position near bubble but not overlapping
        livePreviewParams.x = buttonParams.x + 70;
        livePreviewParams.y = buttonParams.y;
        
        // Make sure it's within screen bounds
        if (livePreviewParams.x + 260 > screenWidth) {
            livePreviewParams.x = screenWidth - 270;
        }
        
        windowManager.addView(livePreviewPopup, livePreviewParams);
        isLivePreviewShowing = true;
        
        initializeLivePreviewViews(bank, name, reason, rowIndex);
    }
    
    private void initializeLivePreviewViews(String bank, String name, String reason, int rowIndex) {
        livePreviewContainer = (LinearLayout) livePreviewPopup.findViewById(R.id.livePreviewContainer);
        etLiveBank = (EditText) livePreviewPopup.findViewById(R.id.etLiveBank);
        etLiveName = (EditText) livePreviewPopup.findViewById(R.id.etLiveName);
        etLiveReason = (EditText) livePreviewPopup.findViewById(R.id.etLiveReason);
        etLiveCoords = (EditText) livePreviewPopup.findViewById(R.id.etLiveCoords);
        etLiveArea = (EditText) livePreviewPopup.findViewById(R.id.etLiveArea);
        tvRowNumber = (TextView) livePreviewPopup.findViewById(R.id.tvRowNumber);
        btnLiveOk = (Button) livePreviewPopup.findViewById(R.id.btnLiveOk);
        btnLiveClear = (Button) livePreviewPopup.findViewById(R.id.btnLiveClear);
        TextView btnClose = (TextView) livePreviewPopup.findViewById(R.id.btnCloseLivePreview);
        View livePreviewHeader = livePreviewPopup.findViewById(R.id.livePreviewHeader);
        
        // Set initial values
        etLiveBank.setText(bank);
        etLiveName.setText(name);
        etLiveReason.setText(reason);
        tvRowNumber.setText("Row #" + (rowIndex + 1));
        
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
                            
                            // Highlight close target if near bottom
                            if (closeTarget != null) {
                                int popupBottom = livePreviewParams.y + 300;
                                if (popupBottom > screenHeight - closeTargetHeight - 50) {
                                    ((TextView) closeTarget).setBackgroundColor(0xFFD32F2F);
                                } else {
                                    ((TextView) closeTarget).setBackgroundColor(0xFFFF5252);
                                }
                            }
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                            // Check if dropped on close target
                            int popupBottom = livePreviewParams.y + 300;
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
                    resetLivePreview();
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
    
    private void saveCurrentEntry() {
        if (!dataManager.hasActiveRow()) {
            Toast.makeText(this, "No active row!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get all values from editable fields
        String bank = etLiveBank.getText().toString().trim();
        String name = etLiveName.getText().toString().trim();
        String reason = etLiveReason.getText().toString().trim();
        String coords = etLiveCoords.getText().toString().trim();
        String area = etLiveArea.getText().toString().trim();
        
        // Update the active row with all data
        DataManager.RowData activeRow = dataManager.getActiveRow();
        if (activeRow != null) {
            activeRow.bankName = bank;
            activeRow.applicantName = name;
            activeRow.reasonOfCNV = reason;
            activeRow.longitude = coords;
            activeRow.area = area;
            activeRow.hasText2 = true;
            
            dataManager.updateRow(dataManager.getActiveRowIndex(), activeRow);
            dataManager.lockActiveRow();
            
            Toast.makeText(this, "Entry saved! Row #" + (dataManager.getActiveRowIndex() + 1), Toast.LENGTH_SHORT).show();
            
            // Reset for next entry
            resetLivePreview();
            setBubbleBlurred(false);
        }
    }
    
    private void resetLivePreview() {
        if (etLiveBank != null) etLiveBank.setText("");
        if (etLiveName != null) etLiveName.setText("");
        if (etLiveReason != null) etLiveReason.setText("");
        if (etLiveCoords != null) etLiveCoords.setText("");
        if (etLiveArea != null) etLiveArea.setText("");
        if (tvRowNumber != null) tvRowNumber.setText("");
        
        // Hide the popup but keep it ready
        closeLivePreview();
        setBubbleBlurred(false);
        
        // Reset last clipboard text so same text can trigger again if copied again
        lastClipboardText = "";
    }
    
    private void clearLivePreviewFields() {
        if (etLiveBank != null) etLiveBank.setText("");
        if (etLiveName != null) etLiveName.setText("");
        if (etLiveReason != null) etLiveReason.setText("");
        if (etLiveCoords != null) etLiveCoords.setText("");
        if (etLiveArea != null) etLiveArea.setText("");
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
        
        // Remove clipboard listener
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
