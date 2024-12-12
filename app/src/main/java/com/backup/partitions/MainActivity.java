package com.backup.partitions;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private Button backupButton;
    private Button restoreButton;
    private TextView statusText;
    private Spinner partitionSpinner;
    private String baseBackupPath;
    
    private String[][] partitions = {
        {"/dev/block/bootdevice/by-name/boot,/dev/block/platform/*/by-name/boot,/dev/block/by-name/boot", "Boot", "boot.img"},
        {"/dev/block/bootdevice/by-name/recovery,/dev/block/platform/*/by-name/recovery,/dev/block/by-name/recovery", "Recovery", "recovery.img"},
        {"/dev/block/bootdevice/by-name/efs,/dev/block/mmcblk0p3,/dev/block/by-name/efs", "EFS", "efs.img"},
        {"/dev/block/bootdevice/by-name/modem,/dev/block/mmcblk0p1,/dev/block/by-name/modem", "Modem", "modem.img"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create main scroll view
        ScrollView scrollView = new ScrollView(this);
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);
        
        // Create and setup spinner
        partitionSpinner = new Spinner(this);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        spinnerParams.bottomMargin = 32;
        partitionSpinner.setLayoutParams(spinnerParams);
        mainLayout.addView(partitionSpinner);
        
        // Create buttons layout
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonLayoutParams.bottomMargin = 32;
        buttonLayout.setLayoutParams(buttonLayoutParams);
        
        // Create backup button
        backupButton = new Button(this);
        backupButton.setText("BACKUP");
        LinearLayout.LayoutParams backupParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        backupParams.rightMargin = 16;
        backupButton.setLayoutParams(backupParams);
        
        // Create restore button
        restoreButton = new Button(this);
        restoreButton.setText("RESTORE");
        LinearLayout.LayoutParams restoreParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        restoreParams.leftMargin = 16;
        restoreButton.setLayoutParams(restoreParams);
        
        buttonLayout.addView(backupButton);
        buttonLayout.addView(restoreButton);
        mainLayout.addView(buttonLayout);
        
        // Create status text
        statusText = new TextView(this);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.bottomMargin = 32;
        statusText.setLayoutParams(statusParams);
        mainLayout.addView(statusText);
        
        // Add guide text
        addGuideSection(mainLayout);
        
        scrollView.addView(mainLayout);
        setContentView(scrollView);
        
        // Initialize app
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        }
        
        baseBackupPath = "/sdcard/PartitionBackups/";
        createBackupDirectories();
        setupSpinner();
        setupButtons();
    }

    private void addGuideSection(LinearLayout mainLayout) {
        TextView guideTitle = new TextView(this);
        guideTitle.setText("Guide & Information");
        guideTitle.setTextSize(18);
        guideTitle.setPadding(0, 16, 0, 16);
        mainLayout.addView(guideTitle);

        String[] sections = {
            "Available Partitions:",
            "• Boot: Contains kernel and boot images\n" +
            "• Recovery: Custom recovery partition\n" +
            "• EFS: Contains IMEI and network data\n" +
            "• Modem: Radio/baseband firmware\n",
            
            "How to Use:",
            "1. Select partition from dropdown\n" +
            "2. Click BACKUP to create backup\n" +
            "3. Click RESTORE to restore latest backup\n" +
            "Backups are stored in: /sdcard/PartitionBackups/\n",
            
            "Important Notes:",
            "• Root access required\n" +
            "• Do NOT interrupt backup/restore process\n" +
            "• Always backup EFS before flashing ROMs\n" +
            "• Keep backups in safe location\n" +
            "• Backup files named with timestamp\n\n" +
            "⚠️ Warning: Incorrect restoration may brick device"
        };

        for (int i = 0; i < sections.length; i += 2) {
            TextView sectionTitle = new TextView(this);
            sectionTitle.setText(sections[i]);
            sectionTitle.setTextSize(16);
            sectionTitle.setPadding(0, 16, 0, 8);
            
            TextView sectionContent = new TextView(this);
            sectionContent.setText(sections[i + 1]);
            sectionContent.setPadding(16, 0, 0, 16);
            
            mainLayout.addView(sectionTitle);
            mainLayout.addView(sectionContent);
        }
    }

    private void setupSpinner() {
        String[] partitionNames = new String[partitions.length];
        for (int i = 0; i < partitions.length; i++) {
            partitionNames[i] = partitions[i][1];
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, partitionNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        partitionSpinner.setAdapter(adapter);
    }

    private void setupButtons() {
        backupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = partitionSpinner.getSelectedItemPosition();
                performBackup(partitions[position]);
            }
        });

        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = partitionSpinner.getSelectedItemPosition();
                performRestore(partitions[position]);
            }
        });
    }

    private void createBackupDirectories() {
        File baseDir = new File(baseBackupPath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        
        for (String[] partition : partitions) {
            File partitionDir = new File(baseBackupPath + partition[1]);
            if (!partitionDir.exists()) {
                partitionDir.mkdirs();
            }
        }
    }

    private String findPartitionPath(String partitionPaths) {
        try {
            String[] paths = partitionPaths.split(",");
            for (String path : paths) {
                if (new File(path).exists()) {
                    return path;
                }
            }

            Process process = Runtime.getRuntime().exec("su");
            java.io.DataOutputStream os = new java.io.DataOutputStream(process.getOutputStream());
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));

            for (String path : paths) {
                if (path.contains("*")) {
                    String searchPath = path.substring(0, path.indexOf("*"));
                    os.writeBytes("find " + searchPath + " -name \"" + 
                                path.substring(path.lastIndexOf("/") + 1) + "\" 2>/dev/null\n");
                    os.flush();
                    String foundPath = reader.readLine();
                    if (foundPath != null && !foundPath.isEmpty()) {
                        return foundPath;
                    }
                }
            }

            for (String path : paths) {
                os.writeBytes("ls " + path + " 2>/dev/null\n");
                os.flush();
                String result = reader.readLine();
                if (result != null && !result.isEmpty()) {
                    return path;
                }
            }

            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void performBackup(String[] partition) {
        String partitionPaths = partition[0];
        String folderName = partition[1];
        String fileName = partition[2];
        
        try {
            Process process = Runtime.getRuntime().exec("su");
            java.io.DataOutputStream os = new java.io.DataOutputStream(process.getOutputStream());
            
            String timestamp = getTimestamp();
            String backupPath = baseBackupPath + folderName + "/" + timestamp + "_" + fileName;
            
            os.writeBytes("mkdir -p " + baseBackupPath + folderName + "\n");
            os.writeBytes("chmod 777 " + baseBackupPath + folderName + "\n");
            
            String actualPath = findPartitionPath(partitionPaths);
            if (actualPath == null) {
                updateStatus("Could not find partition path for " + folderName);
                return;
            }

            updateStatus("Backing up " + folderName + " from " + actualPath);
            
            if (folderName.equals("EFS") || folderName.equals("Modem")) {
                os.writeBytes("dd if=" + actualPath + " of=" + backupPath + " 2>/dev/null || " +
                            "cat " + actualPath + " > " + backupPath + " 2>/dev/null || " +
                            "cp " + actualPath + " " + backupPath + "\n");
            } else {
                os.writeBytes("dd if=" + actualPath + " of=" + backupPath + "\n");
            }
            
            os.writeBytes("chmod 777 " + backupPath + "\n");
            os.writeBytes("sync\n");
            os.writeBytes("exit\n");
            os.flush();
            
            process.waitFor();
            
            File backupFile = new File(backupPath);
            if (backupFile.exists() && backupFile.length() > 0) {
                updateStatus(folderName + " backup completed: " + backupPath);
            } else {
                updateStatus(folderName + " backup failed or empty!");
            }
        } catch (Exception e) {
            updateStatus("Error backing up " + folderName + ": " + e.getMessage());
        }
    }

    private void performRestore(String[] partition) {
        String partitionPaths = partition[0];
        String folderName = partition[1];
        
        try {
            File backupDir = new File(baseBackupPath + folderName);
            File[] backups = backupDir.listFiles();
            
            if (backups == null || backups.length == 0) {
                updateStatus("No backup found for " + folderName);
                return;
            }
            
            File latestBackup = backups[backups.length - 1];
            String actualPath = findPartitionPath(partitionPaths);
            if (actualPath == null) {
                updateStatus("Could not find partition path for " + folderName);
                return;
            }
            
            Process process = Runtime.getRuntime().exec("su");
            java.io.DataOutputStream os = new java.io.DataOutputStream(process.getOutputStream());
            
            updateStatus("Restoring " + folderName + "...");
            
            if (folderName.equals("EFS") || folderName.equals("Modem")) {
                os.writeBytes("dd if=" + latestBackup.getAbsolutePath() + " of=" + actualPath + " || " +
                            "cat " + latestBackup.getAbsolutePath() + " > " + actualPath + "\n");
            } else {
                os.writeBytes("dd if=" + latestBackup.getAbsolutePath() + " of=" + actualPath + "\n");
            }
            
            os.writeBytes("sync\n");
            os.writeBytes("exit\n");
            os.flush();
            
            process.waitFor();
            
            updateStatus(folderName + " restore completed!");
        } catch (Exception e) {
            updateStatus("Error restoring " + folderName + ": " + e.getMessage());
        }
    }

    private void updateStatus(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusText.setText(message);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}