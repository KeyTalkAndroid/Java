/*
 * Class  :  ServiceListingActivity
 * Description :
 *
 * Created By Jobin Mathew on 2018
 * All rights reserved @ keytalk.com
 */

package com.keytalk.nextgen5.view.activities;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.security.ProviderInstaller;
import com.keytalk.nextgen5.R;
import com.keytalk.nextgen5.application.KeyTalkApplication;
import com.keytalk.nextgen5.core.AuthenticationCallBack;
import com.keytalk.nextgen5.core.RCCDDownloadCallBack;
import com.keytalk.nextgen5.core.security.IniResponseData;
import com.keytalk.nextgen5.core.security.KeyTalkCommunicationManager;
import com.keytalk.nextgen5.core.security.RCCDFileData;
import com.keytalk.nextgen5.util.Keys;
import com.keytalk.nextgen5.util.NetworkUtil;
import com.keytalk.nextgen5.util.PreferenceManager;
import com.keytalk.nextgen5.view.component.AlertDialogFragment;
import com.keytalk.nextgen5.view.component.ErrorDialog;
import com.keytalk.nextgen5.view.component.ProviderServicesAdaptor;
import com.keytalk.nextgen5.view.util.AppConstants;

import java.security.cert.LDAPCertStoreParameters;
import java.util.ArrayList;

import static com.keytalk.nextgen5.view.util.AppConstants.DIALOG_PERMISSION_DENIED_MSG;
import static com.keytalk.nextgen5.view.util.AppConstants.REQUEST_READ_EXTERNAL_STORAGE_STATE;

/*
 * Class  :  ServiceListingActivity
 * Description : Activity for show list of downloaded RCCD file services
 *
 * Created by : KeyTalk IT Security BV on 2017
 * All rights reserved @ keytalk.com
 */
public class ServiceListingActivity extends BaseActivity implements ExpandableListView.OnChildClickListener,
        AuthenticationCallBack, ProviderInstaller.ProviderInstallListener, RCCDDownloadCallBack {
    private String alertType = AppConstants.ALERT_DIALOG_TYPE_UNKNOWN;
    private ProviderServicesAdaptor providerServiceAdaptor;
    private ExpandableListView expListView;
    private ArrayList<RCCDFileData> providerServiceList;
    private LayoutInflater layoutInflater;
    private View dialogView;
    ProgressBar mProgressBar;
    private ImageView dialogIcon;
    private TextView dialogTxtMessage;
    private String errorMessage = null;
    private boolean isShowingAlertDialog=false;
    private int currentAlertDialogID=-1;
    private AlertDialog activityAlertDialog;
    private boolean isShowingDialog = false;
    private ProgressDialog dialog;
    private Context context;
    private final String TAG = "ServiceListingActivity";
    public static String requestURL;
    private boolean installtionMessage = false;
    private static final int ERROR_DIALOG_REQUEST_CODE = 1;
    private boolean mRetryProviderInstall;
    private KeyTalkCommunicationManager keyTalkCommunicationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_service_listing);
        context=getBaseContext();
        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.import_rccd);
        TextView header = (TextView)findViewById(R.id.header_string);
        header.setText(R.string.services);

        mProgressBar = (ProgressBar) findViewById(R.id.spinner_progressbar);
        findViewById(R.id.rccd_layout).setVisibility(View.VISIBLE);
        expListView = (ExpandableListView) findViewById(R.id.servicelistscreen_listview);
        //Getting all rccd file contents
        providerServiceList = KeyTalkCommunicationManager.getAllRCCDFileContents(this);
        providerServiceAdaptor = new ProviderServicesAdaptor(this,providerServiceList,mProgressBar);
        expListView.setAdapter(providerServiceAdaptor);
        expListView.setOnChildClickListener(this);
        for (int i = 0; i < providerServiceList.size(); i++) {
            expListView.expandGroup(i);
        }
        ProviderInstaller.installIfNeededAsync(this, this);
        if (Build.VERSION.SDK_INT >= 23)
            getPermissions();
        //Utilities.checkLanguage(getApplicationContext());
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)

    // account.
    //  mAccountManager.addAccountExplicitly(account, accountPassword, null);

    //        LDAPConnection connection = null;
//        try {
//            connection = ldapServer.getConnection();
//            if (connection != null) {
//                RootDSE s = connection.getRootDSE();
//                String[] baseDNs = null;
//                if (s != null) {
//                    baseDNs = s.getNamingContextDNs();
//                }
//
//                sendResult(baseDNs, true, handler, context, null);
//                return true;
//            }
//        } catch (LDAPException e) {
//            Log.e(TAG, "Error authenticating", e);
//            sendResult(null, false, handler, context, e.getMessage());
//            return false;
//        } finally {
//            if (connection != null) {
//                connection.close();
//            }
//        }
//        return false;
    public void saveLocalCA(){
        String path= PreferenceManager.getString(context,AppConstants.PATH);
        String destinationPath=PreferenceManager.getString(context,AppConstants.DESTINATIONPATH);
        validateRequestURL();
      //  FileCompression.unzip(path,destinationPath,context,true);
    }
    private void validateRequestURL() {
        if (TextUtils.isEmpty(requestURL) || requestURL == null) {
            alertType = AppConstants.ALERT_DIALOG_TYPE_EMPTY_URL;
            DialogFragment alertDialog = AlertDialogFragment.newInstance(getString(R.string.import_rccd_alert_title), getString(R.string.no_values_entered), getString(R.string.OK_text), null);
            alertDialog.show(getSupportFragmentManager(), "dialog");
        } else {
			/*Pattern pattern = Pattern.compile("(@)?(href=')?(HREF=')?(HREF=\")?(href=\")?(http://)?[a-zA-Z_0-9\\-]+(\\.\\w[a-zA-Z_0-9\\-]+)+(/[#&\\n\\-=?\\+\\%/\\.\\w]+)?(.rccd){1}");
			Matcher matcher = pattern.matcher(rccdInputEditText.getText().toString());
		    if(matcher.matches()) {*/
            if(NetworkUtil.getInstance(this).isNetworkAvailable(true)) {
                String providerURL = requestURL.trim();
                if(providerURL.length() < 3 /*|| !providerURL.substring(providerURL.length() - 5).equals(".rccd")*/) {
                    alertType = AppConstants.ALERT_DIALOG_TYPE_EMPTY_URL;
                    DialogFragment alertDialog = AlertDialogFragment.newInstance(getString(R.string.import_rccd_alert_title), getString(R.string.no_values_entered), getString(R.string.OK_text), null);
                    alertDialog.show(getSupportFragmentManager(), "dialog");
                } else {
                    boolean isSecure = false;
                    if(providerURL.startsWith("https://")) {
                        isSecure = true;
                    }
                    providerURL = (providerURL.replace("http://", "")).replace("https://", "");
                    if(providerURL.split("/").length <= 1) {
                        alertType = AppConstants.ALERT_DIALOG_TYPE_EMPTY_URL;
                        DialogFragment alertDialog = AlertDialogFragment.newInstance(getString(R.string.import_rccd_alert_title), getString(R.string.no_values_entered), getString(R.string.OK_text), null);
                        alertDialog.show(getSupportFragmentManager(), "dialog");
                    } else {
                        if(!providerURL.substring(providerURL.length() - 5).equals(".rccd")) {
                            if(providerURL.split("\\.").length > 2) {
                                if(providerURL.substring(providerURL.lastIndexOf(".") + 1) == null || providerURL.substring(providerURL.lastIndexOf(".") + 1).isEmpty()) {
                                    providerURL = providerURL + "rccd";
                                } else {
                                    providerURL = providerURL+ ".rccd"; ;
                                }
                            } else {
                                if(providerURL.substring(providerURL.lastIndexOf(".") + 1) == null || providerURL.substring(providerURL.lastIndexOf(".") + 1).isEmpty()) {
                                    providerURL = providerURL + "rccd";
                                } else {
                                    providerURL = providerURL + ".rccd";
                                }
                            }
                        }
                        if(isSecure) {
                            providerURL = "https://"+providerURL;
                        } else {
                            providerURL = "http://"+providerURL;
                        }
                        KeyTalkCommunicationManager.addToLogFile("","RCCD request initiated :"+providerURL);
                      //  mProgressBar.setVisibility(View.VISIBLE);
                        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        KeyTalkCommunicationManager keyTalkCommunicationManager = new KeyTalkCommunicationManager(this);
                        keyTalkCommunicationManager.getRCCDFileFromURL(KeyTalkApplication.getCommunicationLooper(), providerURL);
                    }
                }
            } else {
                KeyTalkCommunicationManager.addToLogFile(TAG,"Tapped ok button without valid network connection");
                DialogFragment alertDialog = AlertDialogFragment.newInstance(getString(R.string.import_rccd_alert_title), getString(R.string.no_network_message), getString(R.string.OK_text), null);
                alertDialog.show(getSupportFragmentManager(), "dialog");
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void getPermissions() {
        String[] permissionArray = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        checkPermissions(permissionArray);
    }

    private boolean checkPermissions(String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M  && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE_STATE);
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE_STATE);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE_STATE:
                if(grantResults.length > 0) {
                    boolean isAllApproved = true;
                    for(int i = 0; i<grantResults.length; i++) {
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            isAllApproved = false;
                            break;
                        }
                    }
                    if(!isAllApproved)
                        showDialog(DIALOG_PERMISSION_DENIED_MSG);
                }
                break;
            default:
                break;
        }
    }


    @Override
    protected void onDestroy() {
        if(activityAlertDialog!=null && currentAlertDialogID!=-1 && isShowingAlertDialog) {
            dissmissAlert(activityAlertDialog, currentAlertDialogID);
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)  {
        super.onConfigurationChanged(newConfig);
    }
    private void showAlert() {
        try {

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.authorise_title));


            builder.setMessage(getString(R.string.authorise_msg));

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    saveLocalCA();
                   // hideSoftInputFromWindow();
                  //  validateRequestURL();
                }
            });

            builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {

                }
            });
            builder.show();
        }catch (Exception e)
        {

        }
    }
    private static int groupPostions = -1;
    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        v.requestFocus();

        if(childPosition==0) {
            groupPostions = groupPosition;
            showDialog(AppConstants.DIALOG_RCCD_UPDATE_SERVER_URL);
        } else {
            // TODO Auto-generated method stub
            if(NetworkUtil.getInstance(this).isNetworkAvailable(true)) {
                KeyTalkCommunicationManager.addToLogFile("ServiceListingActivity","Tapped on service list");
               //showAlert();
                 showDialog(getString(R.string.authenticating));
                RCCDFileData selectedRCCDFileData =  (RCCDFileData)providerServiceList.get(groupPosition);
                keyTalkCommunicationManager = new KeyTalkCommunicationManager(this);
                keyTalkCommunicationManager.initiateAuthenticationProcess(KeyTalkApplication.getCommunicationLooper(),selectedRCCDFileData, groupPosition, childPosition-1);
            } else {
                KeyTalkCommunicationManager.addToLogFile("ServiceListingActivity","Tapped on service list without valid network");
                showDialog(AppConstants.DIALOG_NETWORK_ERROR);
            }
        }
        v.clearFocus();
        return false;
    }


    public void showDialog(String message) {
        if (!isFinishing() && isShowingDialog) {
            dismissDialog();
        }
        isShowingDialog = true;
        dialog = ProgressDialog.show(this, "", message, true, false);
    }


    public final void dismissDialog()  {
        if (!isFinishing() && isShowingDialog)  {
            if(dialog!=null) {
                dialog.cancel();
                dialog.dismiss();
                dialog=null;
                isShowingDialog = false;
            }
        }
    }

    public void onDetachedFromWindow()
    {
        try
        {
            if (dialog != null && isShowingDialog)
            {
                dialog.cancel();
                dialog.dismiss();
                dialog=null;
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public Dialog onCreateDialog(final int id) {
        KeyTalkCommunicationManager.addToLogFile("ServiceListingActivity","onCreateDialog id: "+id);

        layoutInflater = LayoutInflater.from(this);
        dialogView = layoutInflater.inflate(R.layout.custom_dialog, null);
        dialogIcon = (ImageView) dialogView.findViewById(R.id.dialog_image);
        dialogTxtMessage = (TextView) dialogView.findViewById(R.id.dialog_text);
        if (isFinishing()) {
            return null;
        }
        AlertDialog alertDialog = null;
        currentAlertDialogID=id;
        isShowingAlertDialog=true;
        switch (id) {
            case AppConstants.DIALOG_NETWORK_ERROR:
                alertDialog = new ErrorDialog(this, getResources().getString(
                        R.string.alert_dialog_title), getResources().getString(
                        R.string.no_network_message));
                alertDialog.setCanceledOnTouchOutside(false);
                return alertDialog;

            case AppConstants.DIALOG_RCCD_AUTH_ERROR_MSG:
                dialogIcon.setImageResource(R.drawable.icon_info_transparent);
                dialogTxtMessage.setText(errorMessage);
                dialogTxtMessage.setTextSize(18);
                activityAlertDialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setIcon(0)
                        .setPositiveButton(R.string.OK_text,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                    }
                                })
                        .setNegativeButton(R.string.report_button,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                        reportWithEmail();
                                    }
                                }).create();
                activityAlertDialog.setCanceledOnTouchOutside(false);
                return activityAlertDialog;

            case AppConstants.DIALOG_PERMISSION_DENIED_MSG:
                dialogIcon.setImageResource(R.drawable.icon_info_transparent);
                dialogTxtMessage.setText(getString(R.string.permission_denied_message));
                dialogTxtMessage.setTextSize(18);
                activityAlertDialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setIcon(0)
                        .setPositiveButton(R.string.yes_text,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                        getPermissions();
                                    }
                                })
                        .setNegativeButton(R.string.No_text,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                    }
                                }).create();
                activityAlertDialog.setCanceledOnTouchOutside(false);
                return activityAlertDialog;

            case AppConstants.DIALOG_CERT_SUCESSFULLY_RECEIVED_MSG:
                dialogView = layoutInflater.inflate(R.layout.scrollable_custom_dialog, null);
                dialogTxtMessage = (TextView) dialogView.findViewById(R.id.dialog_text);
                dialogIcon.setImageResource(R.drawable.icon_info_transparent);
                String msgTexts = getString(R.string.server_message)+ " \n\n";
                try {
                    final String serviceMessages[] = KeyTalkCommunicationManager.getServerMessage();
                    if (serviceMessages != null	&& serviceMessages.length > 0) {
                        for(int i = 0; i < serviceMessages.length; i = i+2 ) {
                            if(i+1 < serviceMessages.length) {
                                if(serviceMessages[i].trim() != null && !serviceMessages[i].trim().equals("") && !serviceMessages[i].trim().isEmpty() &&
                                        serviceMessages[i+1].trim() != null && !serviceMessages[i+1].trim().equals("") && !serviceMessages[i+1].trim().isEmpty()) {
                                    msgTexts = msgTexts + serviceMessages[i] +"\n"+serviceMessages[i+1]+"\n\n";
                                }
                            }
                        }
                    } else {
                        msgTexts = msgTexts + getString(R.string.server_message_not_available)+"\n\n";
                    }
                } catch(Exception e) {
                    msgTexts = getString(R.string.server_message) + " \n\n" + getString(R.string.server_message_not_available)+"\n\n";
                }
                dialogTxtMessage.setText(msgTexts);
                dialogTxtMessage.setTextSize(18);
                activityAlertDialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setIcon(0)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.OK_text),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                        boolean isNative = PreferenceManager.getBoolean(ServiceListingActivity.this, Keys.PreferenceKeys.DEVICE_TYPE);
                                        if(android.os.Build.VERSION.SDK_INT >= 14 && isNative) {
                                            showDialog(AppConstants.DIALOG_CERT_SUCESSFULLY_RECEIVED);
                                        } else {
                                            String targetURL = KeyTalkCommunicationManager.getUrl();
                                            if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                                                showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                                            } else {
                                                Intent intent = new Intent(ServiceListingActivity.this, WebViewActivity.class);
                                                startActivity(intent);
                                                ServiceListingActivity.this.finish();
                                            }
                                        }
                                    }
                                }).create();
                activityAlertDialog.setCanceledOnTouchOutside(false);
                return activityAlertDialog;
            case AppConstants.DIALOG_CERT_SUCESSFULLY_RECEIVED:
                dialogIcon.setImageResource(R.drawable.icon_info_transparent);
                String msgText = "";
                final String serviceName = KeyTalkCommunicationManager.getServiceName().trim();
                if(android.os.Build.VERSION.SDK_INT >= 14) {
                    if(!installtionMessage) {
                        msgText =  getString(R.string.cert_received_dialog_msg_for_ics, serviceName);
                    } else {
                        msgText =  getString(R.string.reinstall_cert_received_dialog_msg_for_ics);
                    }
                } else {
                    msgText =  getString(R.string.cert_received_dialog_msg_for_below_ics, serviceName);
                }
                installtionMessage = false;
                dialogTxtMessage.setText(msgText);
                dialogTxtMessage.setTextSize(18);
                activityAlertDialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setIcon(0)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.OK_text),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                        boolean isNative = PreferenceManager.getBoolean(ServiceListingActivity.this, Keys.PreferenceKeys.DEVICE_TYPE);
                                        if(android.os.Build.VERSION.SDK_INT >= 14 && isNative) {
                                            //install on the native kit
                                            boolean isSucess = KeyTalkCommunicationManager.installCertificateonNativeKeyChain(ServiceListingActivity.this, AppConstants.REQUEST_CODE_CERT_INSTALL_ACTIVITY);
                                            if(!isSucess) {
                                                String targetURL = KeyTalkCommunicationManager.getUrl();
                                                if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                                                    showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                                                } else {
                                                    Intent intent = new Intent(ServiceListingActivity.this, WebViewActivity.class);
                                                    startActivity(intent);
                                                    ServiceListingActivity.this.finish();
                                                }
                                            }
                                        } else {
                                            String targetURL = KeyTalkCommunicationManager.getUrl();
                                            if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                                                showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                                            } else {
                                                Intent intent = new Intent(ServiceListingActivity.this, WebViewActivity.class);
                                                startActivity(intent);
                                                ServiceListingActivity.this.finish();
                                            }
                                        }
                                    }
                                }).create();
                activityAlertDialog.setCanceledOnTouchOutside(false);
                return activityAlertDialog;
            case AppConstants.DIALOG_REINSTALL_CERTIFICATE:
                dialogIcon.setImageResource(R.drawable.icon_info_transparent);
                final String serviceNames = KeyTalkCommunicationManager.getServiceName().trim();
                dialogTxtMessage.setText(getString(R.string.reinstall_cert, serviceNames));
                dialogTxtMessage.setTextSize(18);
                activityAlertDialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setIcon(0)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.OK_text),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                        boolean isNative = PreferenceManager.getBoolean(ServiceListingActivity.this, Keys.PreferenceKeys.DEVICE_TYPE);
                                        if(android.os.Build.VERSION.SDK_INT >= 14 && isNative) {
                                            //install on the native kit
                                            boolean isSucess = KeyTalkCommunicationManager.installCertificateonNativeKeyChain(ServiceListingActivity.this, AppConstants.REQUEST_CODE_CERT_INSTALL_ACTIVITY);
                                            if(!isSucess) {
                                                String targetURL = KeyTalkCommunicationManager.getUrl();
                                                if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                                                    showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                                                } else {
                                                    Intent intent = new Intent(ServiceListingActivity.this, WebViewActivity.class);
                                                    startActivity(intent);
                                                    ServiceListingActivity.this.finish();
                                                }
                                            }
                                        } else {
                                            String targetURL = KeyTalkCommunicationManager.getUrl();
                                            if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                                                showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                                            } else {
                                                Intent intent = new Intent(ServiceListingActivity.this, WebViewActivity.class);
                                                startActivity(intent);
                                                ServiceListingActivity.this.finish();
                                            }
                                        }
                                    }
                                }).create();
                activityAlertDialog.setCanceledOnTouchOutside(false);
                return activityAlertDialog;

            case AppConstants.DIALOG_CERT_INSTALLATION_FAILED:
                dialogIcon.setImageResource(R.drawable.icon_info_transparent);
                dialogTxtMessage.setText(getString(R.string.cert_dialog_failed_again_msgs, KeyTalkCommunicationManager.getServiceName().trim()));
                //dialogTxtMessage.setText("My Data");
                dialogTxtMessage.setTextSize(18);
                activityAlertDialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setIcon(0)
                        .setCancelable(false)
                        .setPositiveButton(R.string.retry,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                        boolean isNative = PreferenceManager.getBoolean(ServiceListingActivity.this, Keys.PreferenceKeys.DEVICE_TYPE);
                                        if(android.os.Build.VERSION.SDK_INT >= 14 && isNative) {
                                            installtionMessage = true;
                                            showDialog(AppConstants.DIALOG_CERT_SUCESSFULLY_RECEIVED);
                                        } else {
                                            try {
                                                KeyTalkCommunicationManager.updateNativeKeyStoreInstallationStatus(ServiceListingActivity.this, KeyTalkCommunicationManager.getUrl(),false);
                                            } catch(Exception e) { }
                                            KeyTalkCommunicationManager.removeCertificatePreparedForNativeKeyChain(ServiceListingActivity.this);
                                        }
                                    }
                                })
                        .setNegativeButton(R.string.cancel_text,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        try {
                                            KeyTalkCommunicationManager.updateNativeKeyStoreInstallationStatus(ServiceListingActivity.this, KeyTalkCommunicationManager.getUrl(),false);
                                        } catch(Exception e) { }
                                        KeyTalkCommunicationManager.removeCertificatePreparedForNativeKeyChain(ServiceListingActivity.this);
                                        dissmissAlert(dialog, id);
                                    }
                                })
                        .setPositiveButton(R.string.embedded_browser,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                        String targetURL = KeyTalkCommunicationManager.getUrl();
                                        if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                                            showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                                        } else {
                                            Intent intent = new Intent(ServiceListingActivity.this, WebViewActivity.class);
                                            startActivity(intent);
                                            ServiceListingActivity.this.finish();
                                        }
                                    }
                                }).create();
                activityAlertDialog.setCanceledOnTouchOutside(false);
                return activityAlertDialog;

            case AppConstants.DIALOG_RCCD_UPDATE_SERVER_URL:
                dialogIcon.setImageResource(R.drawable.icon_info_transparent);
                dialogTxtMessage.setText(getString(R.string.update_url_message));
                dialogTxtMessage.setTextSize(18);
                activityAlertDialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setIcon(0)
                        .setPositiveButton(R.string.update_text,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                        try{
                                            IniResponseData iniResponseData = providerServiceList.get(groupPostions).getServiceData();
                                            IniResponseData providerData = iniResponseData.getIniArrayValue(AppConstants.INI_FILE_PROVIDER_TEXT).get(0);
                                            String serverURL = providerData.getStringValue("Server");
                                            String rccdFilePath = providerServiceList.get(groupPostions).getRccdFilePath();

                                            Intent intent = new Intent(ServiceListingActivity.this, ChangeServerURLActivity.class);
                                            intent.putExtra(AppConstants.IS_NEW_SERVER_URL_ADDED, true);
                                            intent.putExtra(AppConstants.SERVER_URL_FROM_RCCD,serverURL);
                                            intent.putExtra(AppConstants.SERVER_URL_RCCD_FILE_NAME,rccdFilePath);
                                            startActivityForResult(intent,AppConstants.REQUEST_CODE_UPDATE_SERVER_URL_ACTIVITY);

                                        } catch(Exception e) { }

                                    }
                                }).setNegativeButton(R.string.cancel_text,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog,id);
                                    }
                                }).create();
                activityAlertDialog.setCanceledOnTouchOutside(false);
                return activityAlertDialog;

            case AppConstants.DIALOG_EMPTY_TARGET_URL:
                dialogIcon.setImageResource(R.drawable.icon_info_transparent);
                dialogTxtMessage.setText(getResources().getString(
                        R.string.no_valid_url));
                dialogTxtMessage.setTextSize(18);
                activityAlertDialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setIcon(0)
                        .setPositiveButton(R.string.OK_text,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                    }
                                })
                        .setNegativeButton(R.string.report_button,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                        reportWithEmail();
                                    }
                                }).create();
                activityAlertDialog.setCanceledOnTouchOutside(false);
                return activityAlertDialog;

            case AppConstants.DIALOG_REPORT_TO_ADMIN:
                dialogIcon.setImageResource(R.drawable.icon_info_transparent);
                dialogTxtMessage.setText(getResources().getString( R.string.report_to_admin));
                dialogTxtMessage.setTextSize(18);
                activityAlertDialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setIcon(0)
                        .setPositiveButton(R.string.cancel_text,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                    }
                                })
                        .setNegativeButton(R.string.report_button,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        dissmissAlert(dialog, id);
                                        reportWithEmail();
                                    }
                                }).create();
                activityAlertDialog.setCanceledOnTouchOutside(false);
                return activityAlertDialog;
        }
        return super.onCreateDialog(id);
    }

    public void dissmissAlert(DialogInterface dialog,int id)
    {
        try
        {
            if(id!=-1)
            {
                removeDialog(id);
            }
            if(dialog!=null)
            {
                dialog.cancel();
            }
            if(activityAlertDialog!=null)
            {
                activityAlertDialog.cancel();
                activityAlertDialog.dismiss();
            }
            currentAlertDialogID=-1;
            isShowingAlertDialog=false;
            activityAlertDialog=null;
            dialog=null;
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void credentialRequest(String serviceUsers, boolean isUserNameRequested, boolean isPasswordRequested, String passwordText, boolean isPinRequested, boolean isResponseRequested, String challenge) {
        // TODO Auto-generated method stub

        dismissDialog();

        Intent intent = null;
        if(isUserNameRequested) {
            //Go to user name screen
            intent = new Intent(this, UserNameInputScreen.class);
        } else if(isPasswordRequested) {
            //Go to password screen
            intent = new Intent(this, PasswordScreenActivity.class);
        } else if(isPinRequested) {
            //Go to pin number screen
            intent = new Intent(this, PinNumberScreenActivity.class);
        } else if(isResponseRequested) {
            //Go to responseRequested
            intent = new Intent(this, ChallengeResponseScreenActivity.class);
        }
        if(intent != null) {
            intent.putExtra(AppConstants.AUTH_SERVICE_USERS, serviceUsers);
            intent.putExtra(AppConstants.IS_AUTH_REQUIRED_USER_NAME, isUserNameRequested);
            intent.putExtra(AppConstants.IS_AUTH_REQUIRED_PASSWORD, isPasswordRequested);
            intent.putExtra(AppConstants.IS_AUTH_REQUIRED_PASSWORD_TEXT, passwordText);
            intent.putExtra(AppConstants.IS_AUTH_REQUIRED_PIN, isPinRequested);
            intent.putExtra(AppConstants.IS_AUTH_REQUIRED_RESPONSE, isResponseRequested);
            intent.putExtra(AppConstants.AUTH_SERVICE_CHALLENGE, challenge);

            startActivityForResult(intent,AppConstants.REQUEST_CODE_CERT_REQUEST_CREDENTIAL_ACTIVITY);
        } else {
            //Error
            //Display Error message
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        KeyTalkCommunicationManager.addToLogFile("ServiceListingActivity","onActivityResult requestCode and resultcode: "+requestCode+","+resultCode);
        if (requestCode == AppConstants.REQUEST_CODE_CERT_REQUEST_CREDENTIAL_ACTIVITY && resultCode == RESULT_OK) {
            if(data != null && data.hasExtra(AppConstants.IS_CERT_REQUEST_ERROR) && data.hasExtra(AppConstants.CERT_REQUEST_ERROR_MSG)) {
                this.errorMessage = data.getStringExtra(AppConstants.CERT_REQUEST_ERROR_MSG);
                showDialog(AppConstants.DIALOG_RCCD_AUTH_ERROR_MSG);
            } else if(data != null && data.hasExtra(AppConstants.IS_CERT_REQUEST_SUCESS)) {
                providerServiceList = KeyTalkCommunicationManager.getAllRCCDFileContents(ServiceListingActivity.this);
                //showDialog(AppConstants.DIALOG_CERT_SUCESSFULLY_RECEIVED);
                try {
                    final String serviceMessages[] = KeyTalkCommunicationManager.getServerMessage();
                    if (serviceMessages != null && serviceMessages.length > 0) {
                        boolean isMsg = false;
                        for (int i = 0; i < serviceMessages.length; i++) {
                            if (i + 1 < serviceMessages.length) {
                                if (serviceMessages[i].trim() != null && !serviceMessages[i].trim().equals("") && !serviceMessages[i].trim().isEmpty() && serviceMessages[i + 1].trim() != null && !serviceMessages[i + 1].trim().equals(null) && !serviceMessages[i + 1].trim().equals("") && !serviceMessages[i + 1].trim().isEmpty()) {
                                    isMsg = true;
                                    break;
                                }
                            }
                        }
                        if (isMsg) {
                            showDialog(AppConstants.DIALOG_CERT_SUCESSFULLY_RECEIVED_MSG);
                        } else {
                            //showDialog(AppConstants.DIALOG_CERT_SUCESSFULLY_RECEIVED);
                            boolean isNative = PreferenceManager.getBoolean(ServiceListingActivity.this, Keys.PreferenceKeys.DEVICE_TYPE);
                            if(android.os.Build.VERSION.SDK_INT >= 14 && isNative) {
                                showDialog(AppConstants.DIALOG_CERT_SUCESSFULLY_RECEIVED);
                            } else {
                                String targetURL = KeyTalkCommunicationManager.getUrl();
                                if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                                    showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                                } else {
                                    Intent intent = new Intent(ServiceListingActivity.this, WebViewActivity.class);
                                    startActivity(intent);
                                    ServiceListingActivity.this.finish();
                                }
                            }
                        }
                    } else {
                        //showDialog(AppConstants.DIALOG_CERT_SUCESSFULLY_RECEIVED);
                        boolean isNative = PreferenceManager.getBoolean(ServiceListingActivity.this, Keys.PreferenceKeys.DEVICE_TYPE);
                        if(android.os.Build.VERSION.SDK_INT >= 14 && isNative) {
                            showDialog(AppConstants.DIALOG_CERT_SUCESSFULLY_RECEIVED);
                        } else {
                            String targetURL = KeyTalkCommunicationManager.getUrl();
                            if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                                showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                            } else {
                                Intent intent = new Intent(ServiceListingActivity.this, WebViewActivity.class);
                                startActivity(intent);
                                ServiceListingActivity.this.finish();
                            }
                        }
                    }
                } catch (Exception e) {
                    //showDialog(AppConstants.DIALOG_CERT_SUCESSFULLY_RECEIVED);
                    boolean isNative = PreferenceManager.getBoolean(ServiceListingActivity.this, Keys.PreferenceKeys.DEVICE_TYPE);
                    if(android.os.Build.VERSION.SDK_INT >= 14 && isNative) {
                         showDialog(AppConstants.DIALOG_CERT_SUCESSFULLY_RECEIVED);
                    } else {
                        String targetURL = KeyTalkCommunicationManager.getUrl();
                        if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                            showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                        } else {
                            Intent intent = new Intent(ServiceListingActivity.this, WebViewActivity.class);
                            startActivity(intent);
                            ServiceListingActivity.this.finish();
                        }
                    }
                }
            } else if(data != null && data.hasExtra(AppConstants.IS_CERT_REQUEST_DELAY_CREDENTIALS)) {
                credentialRequest(data.getStringExtra(AppConstants.AUTH_SERVICE_USERS),
                        data.getBooleanExtra(AppConstants.IS_AUTH_REQUIRED_USER_NAME, false),
                        data.getBooleanExtra(AppConstants.IS_AUTH_REQUIRED_PASSWORD, false),
                        data.getStringExtra(AppConstants.IS_AUTH_REQUIRED_PASSWORD_TEXT),
                        data.getBooleanExtra(AppConstants.IS_AUTH_REQUIRED_PIN, false),
                        data.getBooleanExtra(AppConstants.IS_AUTH_REQUIRED_RESPONSE, false),
                        data.getStringExtra(AppConstants.AUTH_SERVICE_CHALLENGE));
            } else if(data != null && data.hasExtra(AppConstants.IS_RESET_CREDENTIALS_REQUEST) && data.getBooleanExtra(AppConstants.IS_RESET_CREDENTIALS_REQUEST, false)) {

                Intent intent = new Intent(this, ChangePasswordActivity.class);
                if(data.hasExtra(AppConstants.IS_RESET_REQUEST_FROM_SERVER_USER)) {
                    intent.putExtra(AppConstants.IS_RESET_REQUEST_FROM_SERVER_USER, data.getStringExtra(AppConstants.IS_RESET_REQUEST_FROM_SERVER_USER));
                }
                if(data.hasExtra(AppConstants.IS_RESET_REQUEST_FROM_SERVER_PWD)) {
                    intent.putExtra(AppConstants.IS_RESET_REQUEST_FROM_SERVER_PWD, data.getStringExtra(AppConstants.IS_RESET_REQUEST_FROM_SERVER_PWD));
                }
                startActivityForResult(intent,AppConstants.REQUEST_CODE_RESET_PWD_ACTIVITY);

            } else if(data != null && data.hasExtra(AppConstants.IS_CHALLENGE_CREDENTIALS_REQUEST) && data.getBooleanExtra(AppConstants.IS_CHALLENGE_CREDENTIALS_REQUEST, false)) {

                Intent intent = new Intent(this, ChallengeRequestActivity.class);
                if(data.hasExtra(AppConstants.IS_CHALLENGE_CREDENTIALS_DATA)) {
                    intent.putExtra(AppConstants.IS_CHALLENGE_CREDENTIALS_DATA, data.getStringArrayExtra(AppConstants.IS_CHALLENGE_CREDENTIALS_DATA));
                }
                startActivityForResult(intent,AppConstants.REQUEST_CODE_CERT_REQUEST_CREDENTIAL_ACTIVITY);

            } else if(data != null && data.hasExtra(AppConstants.IS_NEW_CHALLENGE_CREDENTIALS_REQUEST) && data.getBooleanExtra(AppConstants.IS_NEW_CHALLENGE_CREDENTIALS_REQUEST, false)) {

                Intent intent = new Intent(this, NewChallengeResponseScreenActivity.class);
                if(data.hasExtra(AppConstants.IS_NEW_CHALLENGE_CREDENTIALS_DATA)) {
                    intent.putExtra(AppConstants.IS_NEW_CHALLENGE_CREDENTIALS_DATA, data.getStringArrayExtra(AppConstants.IS_NEW_CHALLENGE_CREDENTIALS_DATA));
                }
                if(data.hasExtra(AppConstants.IS_NEW_RESPONSE_CREDENTIALS_DATA)) {
                    intent.putExtra(AppConstants.IS_NEW_RESPONSE_CREDENTIALS_DATA, data.getStringArrayExtra(AppConstants.IS_NEW_RESPONSE_CREDENTIALS_DATA));
                }
                startActivityForResult(intent,AppConstants.REQUEST_CODE_CERT_REQUEST_CREDENTIAL_ACTIVITY);

            }

        } else if (requestCode == AppConstants.REQUEST_CODE_CERT_INSTALL_ACTIVITY){
            if(resultCode == RESULT_OK) {
                //Installed show dialog
                try{
                    KeyTalkCommunicationManager.updateNativeKeyStoreInstallationStatus(ServiceListingActivity.this, KeyTalkCommunicationManager.getUrl(),true);
                    boolean isNative = PreferenceManager.getBoolean(ServiceListingActivity.this, Keys.PreferenceKeys.DEVICE_TYPE);
                    if(isNative) {
                        String targetURL = KeyTalkCommunicationManager.getUrl();
                        if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                            showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                        } else {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(KeyTalkCommunicationManager.getUrl()));
                            startActivity(browserIntent);
                        }
                    } else {
                        String targetURL = KeyTalkCommunicationManager.getUrl();
                        if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                            showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                        } else {
                            Intent intent = new Intent(ServiceListingActivity.this, WebViewActivity.class);
                            startActivity(intent);
                            ServiceListingActivity.this.finish();
                        }

                    }
                } catch(Exception e) {
                    String targetURL = KeyTalkCommunicationManager.getUrl();
                    if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                        showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                    } else {
                        Intent intent = new Intent(ServiceListingActivity.this, WebViewActivity.class);
                        startActivity(intent);
                        ServiceListingActivity.this.finish();
                    }

                }
                //showDialog(AppConstants.DIALOG_CERT_INSTALLATION_SUCESS);
            } else {
                showDialog(AppConstants.DIALOG_CERT_INSTALLATION_FAILED);
            }
        } else if(requestCode == AppConstants.REQUEST_CODE_RESET_PWD_ACTIVITY && resultCode == RESULT_OK) {
            if(data != null && data.hasExtra(AppConstants.IS_RESET_REQUEST_ERROR) && data.hasExtra(AppConstants.CERT_REQUEST_ERROR_MSG)) {
                this.errorMessage = data.getStringExtra(AppConstants.CERT_REQUEST_ERROR_MSG);
                showDialog(AppConstants.DIALOG_RCCD_AUTH_ERROR_MSG);
            } else if(data != null && data.hasExtra(AppConstants.IS_CERT_REQUEST_DELAY_CREDENTIALS)) {
                credentialRequest(data.getStringExtra(AppConstants.AUTH_SERVICE_USERS),
                        data.getBooleanExtra(AppConstants.IS_AUTH_REQUIRED_USER_NAME, false),
                        data.getBooleanExtra(AppConstants.IS_AUTH_REQUIRED_PASSWORD, false),
                        data.getStringExtra(AppConstants.IS_AUTH_REQUIRED_PASSWORD_TEXT),
                        data.getBooleanExtra(AppConstants.IS_AUTH_REQUIRED_PIN, false),
                        data.getBooleanExtra(AppConstants.IS_AUTH_REQUIRED_RESPONSE, false),
                        data.getStringExtra(AppConstants.AUTH_SERVICE_CHALLENGE));
            }
        } else if(requestCode == AppConstants.REQUEST_CODE_UPDATE_SERVER_URL_ACTIVITY && resultCode == RESULT_OK) {
            if(data != null && data.hasExtra(AppConstants.IS_NEW_SERVER_URL_ADDED)) {
                if(data.getBooleanExtra(AppConstants.IS_NEW_SERVER_URL_ADDED, false)) {
                    providerServiceList = KeyTalkCommunicationManager.getAllRCCDFileContents(ServiceListingActivity.this);
                    providerServiceAdaptor = new ProviderServicesAdaptor(this,providerServiceList, mProgressBar);
                    expListView.setAdapter(providerServiceAdaptor);
                    expListView.setOnChildClickListener(this);
                    for (int i = 0; i < providerServiceList.size(); i++) {
                        expListView.expandGroup(i);
                    }
                    providerServiceAdaptor.notifyDataSetChanged();
                }
            }
        } else if (requestCode == ERROR_DIALOG_REQUEST_CODE) {
            // Adding a fragment via GooglePlayServicesUtil.showErrorDialogFragment
            // before the instance state is restored throws an error. So instead,
            // set a flag here, which will cause the fragment to delay until
            // onPostResume.
            mRetryProviderInstall = true;
        }

    }


    //REQUEST_CODE_CERT_REQUEST_CREDENTIAL_ACTIVITY
    @Override
    public void displayError(String errorMessage) {
        // TODO Auto-generated method stub
        dismissDialog();
        this.errorMessage = errorMessage;
        showDialog(AppConstants.DIALOG_RCCD_AUTH_ERROR_MSG);

    }

    @Override
    public void validCertificateAvailable() {
        // TODO Auto-generated method stub
        dismissDialog();
        if(android.os.Build.VERSION.SDK_INT < 14) {
            String targetURL = KeyTalkCommunicationManager.getUrl();
            if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
            } else {
                Intent intent = new Intent(this, WebViewActivity.class);
                startActivity(intent);
                ServiceListingActivity.this.finish();
            }
        } else if(android.os.Build.VERSION.SDK_INT >= 14 && android.os.Build.VERSION.SDK_INT <= 18) {
            boolean isNative = PreferenceManager.getBoolean(ServiceListingActivity.this, Keys.PreferenceKeys.DEVICE_TYPE);
            if(isNative) {
                boolean isAddedToNativeKeyStore = KeyTalkCommunicationManager.getNativeKeyStoreInstallationStatus(ServiceListingActivity.this, KeyTalkCommunicationManager.getUrl());
                if(isAddedToNativeKeyStore) {
                    String targetURL = KeyTalkCommunicationManager.getUrl();
                    if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                        showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                    } else {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse( KeyTalkCommunicationManager.getUrl()));
                        startActivity(browserIntent);
                    }
                } else {
                    showDialog(AppConstants.DIALOG_REINSTALL_CERTIFICATE);
                }
            } else {
                String targetURL = KeyTalkCommunicationManager.getUrl();
                if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                    showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                } else {
                    Intent intent = new Intent(this, WebViewActivity.class);
                    startActivity(intent);
                    ServiceListingActivity.this.finish();
                }
            }
        } else {
            boolean isAddedToNativeKeyStore = KeyTalkCommunicationManager.getNativeKeyStoreInstallationStatus(ServiceListingActivity.this, KeyTalkCommunicationManager.getUrl());
            if(isAddedToNativeKeyStore) {
                String targetURL = KeyTalkCommunicationManager.getUrl();
                if(targetURL == null || targetURL.isEmpty() || TextUtils.isEmpty(targetURL)) {
                    showDialog(AppConstants.DIALOG_EMPTY_TARGET_URL);
                } else {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(KeyTalkCommunicationManager.getUrl()));
                    startActivity(browserIntent);
                }
            } else {
                showDialog(AppConstants.DIALOG_REINSTALL_CERTIFICATE);
            }
        }
    }

    public void reportWithEmail() {
        boolean isSucess = false;
        try {
            isSucess = KeyTalkCommunicationManager.isLogFileAvailable(this);

            Intent email = new Intent(Intent.ACTION_SEND);
         //   email.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.emailscreen_email_address)});
            email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.emailscreen_subject));
            //email.putExtra(Intent.EXTRA_TEXT,getString(R.string.emailscreen_message));
            if (isSucess) {
                email.putExtra(Intent.EXTRA_TEXT, getString(R.string.emailscreen_message));
            } else {
                try {
                    email.putExtra(Intent.EXTRA_TEXT, getString(R.string.emailscreen_message) + KeyTalkCommunicationManager.getLogContents(this));
                } catch (Exception e) {
                    e.printStackTrace();
                    email.putExtra(Intent.EXTRA_TEXT, getString(R.string.emailscreen_message));
                }
            }
            email.setType("message/rfc822");
            KeyTalkCommunicationManager.addToLogFile("ServiceList", "Log file available? : " + isSucess);
            if (isSucess) {
                try {
                    Uri uri = KeyTalkCommunicationManager.getLogDetailsAsUri(this);
                    email.putExtra(Intent.EXTRA_STREAM, uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
            startActivity(Intent.createChooser(email, "Choose an Email client :"));
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_rccd_service_screen, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_services) {
            Intent intent = new Intent(ServiceListingActivity.this, RCCDImportScreenActivity.class);
            intent.putExtra("REFRESH",true);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.action_report) {
            showDialog(AppConstants.DIALOG_REPORT_TO_ADMIN);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }


    /**
     * On resume, check to see if we flagged that we need to reinstall the
     * provider.
     */
    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (mRetryProviderInstall) {
            // We can now safely retry installation.
            ProviderInstaller.installIfNeededAsync(this, this);
        }
        mRetryProviderInstall = false;
    }

    private void onProviderInstallerNotAvailable() {
        // This is reached if the provider cannot be updated for some reason.
        // App should consider all HTTP communication to be vulnerable, and take
        // appropriate action.
        Toast.makeText(this, getString(R.string.update_and_ssl_unavailable), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderInstalled() {
        // Provider is up-to-date, app can make secure network calls.
        KeyTalkCommunicationManager.addToLogFile("Security Provider Installed");

    }

    @Override
    public void onProviderInstallFailed(int errorCode, Intent intent) {
        if (GooglePlayServicesUtil.isUserRecoverableError(errorCode)) {
            // Recoverable error. Show a dialog prompting the user to
            // install/update/enable Google Play services.
            GooglePlayServicesUtil.showErrorDialogFragment(
                    errorCode,
                    this,
                    ERROR_DIALOG_REQUEST_CODE,
                    new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            // The user chose not to take the recovery action
                            onProviderInstallerNotAvailable();
                        }
                    });
        } else {
            // Google Play services is not available.
            onProviderInstallerNotAvailable();
        }
    }

    @Override
    public void rccdDownloadCallBack(String providerName, int serviceCount, int downloadStatus) {

    }

}

