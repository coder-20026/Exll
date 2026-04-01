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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FloatingButtonService extends Service {
    
    private static final String CHANNEL_ID = "WhtsTableFloatingChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    private WindowManager windowManager;
    private View floatingButton;
    private View overlayPopup;
    private WindowManager.LayoutParams buttonParams;
    private WindowManager.LayoutParams popupParams;
    private DataManager dataManager;
    
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private long touchStartTime;
    private boolean isLongPress = false;
    
    // Popup views
    private EditText etText1, etText2;
    private TextView tvActiveRow, tvStatus, tvPreview, btnClosePopup;
    private Button btnDone, btnClear, btnOpenApp;
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        dataManager = DataManager.getInstance(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Start as foreground service for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());
        }
        
        createFloatingButton();
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
            .setContentText("Tap floating button to add entries")
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
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
                    isLongPress = true;
                    showCloseOption();
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
                        touchStartTime = System.currentTimeMillis();
                        isLongPress = false;
                        longPressHandler.postDelayed(longPressRunnable, 500);
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            longPressHandler.removeCallbacks(longPressRunnable);
                        }
                        
                        buttonParams.x = initialX + deltaX;
                        buttonParams.y = initialY + deltaY;
                        windowManager.updateViewLayout(floatingButton, buttonParams);
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        longPressHandler.removeCallbacks(longPressRunnable);
                        
                        if (!isLongPress && System.currentTimeMillis() - touchStartTime < 500) {
                            openOverlayPopup();
                        }
                        return true;
                }
                return false;
            }
        });
    }
    
    private void showCloseOption() {
        Toast.makeText(this, "Tap again to close floating button", Toast.LENGTH_SHORT).show();
        
        final ImageView btnImage = (ImageView) floatingButton.findViewById(R.id.floatingButton);
        
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf();
                Toast.makeText(FloatingButtonService.this, "Quick Entry closed", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void openOverlayPopup() {
        if (overlayPopup != null) {
            updatePopupPreview();
            return;
        }
        
        overlayPopup = LayoutInflater.from(this).inflate(R.layout.overlay_popup_layout, null);
        
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        popupParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        );
        
        popupParams.gravity = Gravity.CENTER;
        
        windowManager.addView(overlayPopup, popupParams);
        
        initializePopupViews();
        autoPopulateFromClipboard();
    }
    
    private void initializePopupViews() {
        etText1 = (EditText) overlayPopup.findViewById(R.id.etPopupText1);
        etText2 = (EditText) overlayPopup.findViewById(R.id.etPopupText2);
        tvActiveRow = (TextView) overlayPopup.findViewById(R.id.tvPopupActiveRow);
        tvStatus = (TextView) overlayPopup.findViewById(R.id.tvPopupStatus);
        tvPreview = (TextView) overlayPopup.findViewById(R.id.tvPopupPreview);
        btnClosePopup = (TextView) overlayPopup.findViewById(R.id.btnClosePopup);
        btnDone = (Button) overlayPopup.findViewById(R.id.btnPopupDone);
        btnClear = (Button) overlayPopup.findViewById(R.id.btnPopupClear);
        btnOpenApp = (Button) overlayPopup.findViewById(R.id.btnOpenApp);
        
        etText1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                if (!text.isEmpty()) {
                    processText1(text);
                }
            }
        });
        
        etText2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                if (!text.isEmpty()) {
                    processText2(text);
                }
            }
        });
        
        btnClosePopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeOverlayPopup();
            }
        });
        
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataManager.lockActiveRow();
                Toast.makeText(FloatingButtonService.this, "Entry saved!", Toast.LENGTH_SHORT).show();
                closeOverlayPopup();
            }
        });
        
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etText1.setText("");
                etText2.setText("");
                Toast.makeText(FloatingButtonService.this, "Cleared", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnOpenApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FloatingButtonService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                closeOverlayPopup();
            }
        });
        
        etText1.requestFocus();
        etText1.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etText1, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 100);
    }
    
    private void autoPopulateFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clipData = clipboard.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0) {
                CharSequence text = clipData.getItemAt(0).getText();
                if (text != null && text.length() > 0) {
                    etText1.setText(text.toString());
                }
            }
        }
    }
    
    private void processText1(String text) {
        String bankName = extractBankName(text);
        String applicantName = extractApplicantName(text);
        String reasonOfCNV = extractReasonOfCNV(text);
        
        int newRowIndex = dataManager.createNewActiveRow(bankName, applicantName, reasonOfCNV);
        updatePopupPreview();
        
        Toast.makeText(this, "Row #" + (newRowIndex + 1) + " created", Toast.LENGTH_SHORT).show();
    }
    
    private void processText2(String text) {
        if (!dataManager.hasActiveRow()) {
            Toast.makeText(this, "Add WhatsApp text first!", Toast.LENGTH_LONG).show();
            etText2.setText("");
            return;
        }
        
        String longitude = text.trim();
        
        boolean success = dataManager.updateActiveRowWithText2(longitude);
        if (success) {
            updatePopupPreview();
            Toast.makeText(this, "Coordinates added", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updatePopupPreview() {
        DataManager.RowData activeRow = dataManager.getActiveRow();
        
        if (activeRow == null) {
            tvActiveRow.setText("Quick Entry");
            tvStatus.setText("");
            tvPreview.setText("Waiting for data...");
            return;
        }
        
        int rowNum = dataManager.getActiveRowIndex() + 1;
        tvActiveRow.setText("Active Row: #" + rowNum);
        
        StringBuilder status = new StringBuilder();
        if (activeRow.hasText1) status.append("Text-1 OK");
        if (activeRow.hasText2) status.append("  Text-2 OK");
        tvStatus.setText(status.toString());
        
        StringBuilder preview = new StringBuilder();
        preview.append("Bank: ").append(activeRow.bankName.isEmpty() ? "-" : activeRow.bankName).append("\n");
        preview.append("Name: ").append(activeRow.applicantName.isEmpty() ? "-" : activeRow.applicantName).append("\n");
        preview.append("Reason: ").append(activeRow.reasonOfCNV.isEmpty() ? "-" : activeRow.reasonOfCNV).append("\n");
        preview.append("Coords: ").append(activeRow.longitude.isEmpty() ? "-" : activeRow.longitude);
        
        tvPreview.setText(preview.toString());
    }
    
    private void closeOverlayPopup() {
        if (overlayPopup != null) {
            windowManager.removeView(overlayPopup);
            overlayPopup = null;
        }
    }
    
    private String extractBankName(String text) {
        Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
    
    private String extractReasonOfCNV(String text) {
        Pattern pattern = Pattern.compile("([^(]+)\\(");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
    
    private String extractApplicantName(String text) {
        Pattern pattern = Pattern.compile(
            "(?i)Applic[a-z]*\\s*(?:Name)?\\s*:?\\s*([^\\n\\d]+?)(?=\\n|\\d+\\)|$)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            name = name.replaceAll("[:\\-]+$", "").trim();
            name = name.replaceAll("\\s*\\d+[:\\)].*", "").trim();
            return name;
        }
        return "";
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingButton != null) {
            windowManager.removeView(floatingButton);
        }
        if (overlayPopup != null) {
            windowManager.removeView(overlayPopup);
        }
    }
}
