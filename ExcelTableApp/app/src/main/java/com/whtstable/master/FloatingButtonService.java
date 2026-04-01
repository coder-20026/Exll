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
    private View overlayPopup;
    private View closeTarget;
    private View rightInputBox;
    private WindowManager.LayoutParams buttonParams;
    private WindowManager.LayoutParams popupParams;
    private WindowManager.LayoutParams closeTargetParams;
    private WindowManager.LayoutParams rightInputParams;
    private DataManager dataManager;
    
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private long touchStartTime;
    private boolean isLongPress = false;
    private boolean isDragging = false;
    
    // Popup drag variables
    private int popupInitialX, popupInitialY;
    private float popupInitialTouchX, popupInitialTouchY;
    private boolean isPopupDragging = false;
    private long popupTouchStartTime;
    
    // Popup views
    private EditText etText1;
    private TextView tvActiveRow, tvStatus, tvPreview, btnClosePopup;
    private Button btnDone, btnClear, btnOpenApp;
    
    // Right input box views
    private EditText etRightInput;
    private Button btnRightOk;
    private LinearLayout rightInputContainer;
    
    // Screen dimensions
    private int screenHeight;
    private int closeTargetHeight = 150;
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        dataManager = DataManager.getInstance(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Get screen height
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        
        // Start as foreground service for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());
        }
        
        createFloatingButton();
        createRightInputBox();
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
    
    private void createCloseTarget() {
        if (closeTarget != null) return;
        
        closeTarget = new TextView(this);
        TextView tv = (TextView) closeTarget;
        tv.setText("✕ Close");
        tv.setTextSize(18);
        tv.setTextColor(0xFFFFFFFF);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(0xFFFF5252);
        tv.setPadding(0, 30, 0, 30);
        
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
            windowManager.removeView(closeTarget);
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
                    isLongPress = true;
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
                        touchStartTime = System.currentTimeMillis();
                        isLongPress = false;
                        isDragging = false;
                        longPressHandler.postDelayed(longPressRunnable, 300);
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true;
                            if (!isLongPress) {
                                longPressHandler.removeCallbacks(longPressRunnable);
                                createCloseTarget();
                            }
                        }
                        
                        buttonParams.x = initialX + deltaX;
                        buttonParams.y = initialY + deltaY;
                        windowManager.updateViewLayout(floatingButton, buttonParams);
                        
                        // Update right input box position
                        updateRightInputPosition();
                        
                        // Highlight close target if bubble is near bottom
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
                            stopSelf();
                            Toast.makeText(FloatingButtonService.this, "Quick Entry closed", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        
                        removeCloseTarget();
                        
                        if (!isDragging && System.currentTimeMillis() - touchStartTime < 300) {
                            openOverlayPopup();
                        }
                        return true;
                }
                return false;
            }
        });
    }
    
    private void createRightInputBox() {
        rightInputBox = LayoutInflater.from(this).inflate(R.layout.right_input_layout, null);
        
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        rightInputParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        );
        
        rightInputParams.gravity = Gravity.TOP | Gravity.START;
        rightInputParams.x = buttonParams.x + 70;
        rightInputParams.y = buttonParams.y;
        
        windowManager.addView(rightInputBox, rightInputParams);
        
        rightInputContainer = (LinearLayout) rightInputBox.findViewById(R.id.rightInputContainer);
        etRightInput = (EditText) rightInputBox.findViewById(R.id.etRightInput);
        btnRightOk = (Button) rightInputBox.findViewById(R.id.btnRightOk);
        
        // Initially hidden
        rightInputContainer.setVisibility(View.GONE);
        
        btnRightOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processRightInput();
            }
        });
    }
    
    private void updateRightInputPosition() {
        if (rightInputBox != null && rightInputParams != null) {
            rightInputParams.x = buttonParams.x + 70;
            rightInputParams.y = buttonParams.y;
            windowManager.updateViewLayout(rightInputBox, rightInputParams);
        }
    }
    
    private void showRightInputBox() {
        if (rightInputContainer != null) {
            rightInputContainer.setVisibility(View.VISIBLE);
            rightInputContainer.setAlpha(1.0f);
        }
    }
    
    private void hideRightInputBox() {
        if (rightInputContainer != null) {
            rightInputContainer.setVisibility(View.GONE);
        }
    }
    
    private void disableRightInputBox() {
        if (rightInputContainer != null) {
            rightInputContainer.setVisibility(View.VISIBLE);
            rightInputContainer.setAlpha(0.4f);
            etRightInput.setEnabled(false);
            btnRightOk.setEnabled(false);
        }
    }
    
    private void enableRightInputBox() {
        if (rightInputContainer != null) {
            rightInputContainer.setVisibility(View.VISIBLE);
            rightInputContainer.setAlpha(1.0f);
            etRightInput.setEnabled(true);
            btnRightOk.setEnabled(true);
        }
    }
    
    private void processRightInput() {
        if (!dataManager.hasActiveRow()) {
            Toast.makeText(this, "Pehle WhatsApp text add karo!", Toast.LENGTH_LONG).show();
            return;
        }
        
        String input = etRightInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Coordinates aur Area enter karo!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Parse input - Line 1: coordinates, Line 2: area
        String[] lines = input.split("\n");
        String longitude = "";
        String area = "";
        
        if (lines.length >= 1) {
            longitude = lines[0].trim();
        }
        if (lines.length >= 2) {
            area = lines[1].trim();
        }
        
        // Update active row with longitude and area
        boolean success = dataManager.updateActiveRowWithCoordinatesAndArea(longitude, area);
        if (success) {
            dataManager.lockActiveRow();
            Toast.makeText(this, "Entry saved!", Toast.LENGTH_SHORT).show();
            etRightInput.setText("");
            hideRightInputBox();
            closeOverlayPopup();
        } else {
            Toast.makeText(this, "Error saving entry!", Toast.LENGTH_SHORT).show();
        }
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
        
        popupParams.gravity = Gravity.TOP | Gravity.START;
        
        // Position popup in center of screen
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        popupParams.x = (metrics.widthPixels - 320) / 2;
        popupParams.y = (metrics.heightPixels - 400) / 2;
        
        windowManager.addView(overlayPopup, popupParams);
        
        initializePopupViews();
        autoPopulateFromClipboard();
    }
    
    private void initializePopupViews() {
        etText1 = (EditText) overlayPopup.findViewById(R.id.etPopupText1);
        tvActiveRow = (TextView) overlayPopup.findViewById(R.id.tvPopupActiveRow);
        tvStatus = (TextView) overlayPopup.findViewById(R.id.tvPopupStatus);
        tvPreview = (TextView) overlayPopup.findViewById(R.id.tvPopupPreview);
        btnClosePopup = (TextView) overlayPopup.findViewById(R.id.btnClosePopup);
        btnDone = (Button) overlayPopup.findViewById(R.id.btnPopupDone);
        btnClear = (Button) overlayPopup.findViewById(R.id.btnPopupClear);
        btnOpenApp = (Button) overlayPopup.findViewById(R.id.btnOpenApp);
        
        // Get popup header for drag functionality
        View popupHeader = overlayPopup.findViewById(R.id.popupHeader);
        
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
                    // Enable right input box when text1 has content
                    enableRightInputBox();
                } else {
                    // Disable right input box when text1 is empty
                    disableRightInputBox();
                }
            }
        });
        
        // Popup drag functionality on header
        if (popupHeader != null) {
            popupHeader.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            popupInitialX = popupParams.x;
                            popupInitialY = popupParams.y;
                            popupInitialTouchX = event.getRawX();
                            popupInitialTouchY = event.getRawY();
                            popupTouchStartTime = System.currentTimeMillis();
                            isPopupDragging = false;
                            return true;
                            
                        case MotionEvent.ACTION_MOVE:
                            int deltaX = (int) (event.getRawX() - popupInitialTouchX);
                            int deltaY = (int) (event.getRawY() - popupInitialTouchY);
                            
                            if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
                                isPopupDragging = true;
                            }
                            
                            popupParams.x = popupInitialX + deltaX;
                            popupParams.y = popupInitialY + deltaY;
                            windowManager.updateViewLayout(overlayPopup, popupParams);
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                            return true;
                    }
                    return false;
                }
            });
        }
        
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
                hideRightInputBox();
                closeOverlayPopup();
            }
        });
        
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etText1.setText("");
                if (etRightInput != null) {
                    etRightInput.setText("");
                }
                disableRightInputBox();
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
        
        // Initially disable right input box
        disableRightInputBox();
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
        
        // Show and enable right input box
        showRightInputBox();
        
        Toast.makeText(this, "Row #" + (newRowIndex + 1) + " created", Toast.LENGTH_SHORT).show();
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
        preview.append("Coords: ").append(activeRow.longitude.isEmpty() ? "-" : activeRow.longitude).append("\n");
        preview.append("Area: ").append(activeRow.area.isEmpty() ? "-" : activeRow.area);
        
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
        if (closeTarget != null) {
            windowManager.removeView(closeTarget);
        }
        if (rightInputBox != null) {
            windowManager.removeView(rightInputBox);
        }
    }
}
