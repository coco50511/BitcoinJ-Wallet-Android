package com.androin.bitcoinwallet.main;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androin.bitcoinwallet.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import net.glxn.qrgen.android.QRCode;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.ColorRes;
import org.androidannotations.annotations.res.StringRes;

import java.util.ArrayList;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.menu_main)
public class MainActivity extends AppCompatActivity implements MainActivityContract.MainActivityView {

    private MainActivityContract.MainActivityPresenter presenter;

    @ViewById
    protected FrameLayout flDownloadContent_LDP;
    @ViewById
    protected ProgressBar pbProgress_LDP;
    @ViewById
    protected TextView tvPercentage_LDP;

    @ViewById
    protected Toolbar toolbar_AT;
    @ViewById
    protected SwipeRefreshLayout srlContent_AM;
    @ViewById
    protected TextView tvMyBalance_AM;
    @ViewById
    protected TextView tvMyAddress_AM;
    @ViewById
    protected ImageView ivMyQRAddress_AM;
    @ViewById
    protected TextView tvWalletFilePath_AM;
    @ViewById
    protected EditText etAmount_AM;
    @ViewById
    protected Button btnSend_AM;
    @ViewById
    protected EditText etRecipientCount;
    @ViewById
    protected ImageView ivCopy_AM;

    protected ArrayList<RecipientFragment> m_recipientFragments;
    private RecipientFragment m_curRecipientFragment;

    @SystemService
    protected ClipboardManager clipboardManager;

    @StringRes(R.string.scan_recipient_qr)
    protected String strScanRecipientQRCode;
    @StringRes(R.string.about)
    protected String strAbout;

    public int colorGreenDark;
    public int colorGreyDark;
    public int colorRedDark;
    public int colorBlueDark;

    @AfterInject
    protected void initData() {
        new MainActivityPresenter(this, getCacheDir());
    }

    @AfterViews
    protected void initUI() {
        colorGreenDark = getResources().getColor(android.R.color.holo_green_dark);
        colorGreyDark = getResources().getColor(android.R.color.darker_gray);
        colorRedDark = getResources().getColor(android.R.color.holo_red_dark);
        colorBlueDark = getResources().getColor(android.R.color.holo_blue_dark);

        initRecipientFragments();
        initToolbar();
        setListeners();

        presenter.subscribe();
    }

    @OptionsItem(R.id.menuInfo_MM)
    protected void clickMenuInfo() {
        presenter.getInfoDialog();
    }

    private void initToolbar() {
        setSupportActionBar(toolbar_AT);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Wallet");
        }
    }


    @Override
    public void setPresenter(MainActivityContract.MainActivityPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    @UiThread
    public void displayDownloadContent(boolean isShown) {
        flDownloadContent_LDP.setVisibility(isShown ? View.VISIBLE : View.GONE);
    }

    @Override
    @UiThread
    public void displayProgress(int percent) {
        if(pbProgress_LDP.isIndeterminate()) pbProgress_LDP.setIndeterminate(false);
        pbProgress_LDP.setProgress(percent);
    }

    @Override
    @UiThread
    public void displayPercentage(int percent) {
        tvPercentage_LDP.setText(String.valueOf(percent) + " %");
    }

    @Override
    @UiThread
    public void displayMyBalance(String myBalance) {
        tvMyBalance_AM.setText(myBalance);
    }

    @Override
    @UiThread
    public void displayWalletPath(String walletPath) {
        tvWalletFilePath_AM.setText(walletPath);
    }

    @Override
    @UiThread
    public void displayMyAddress(String myAddress) {
        tvMyAddress_AM.setText(myAddress);
        Bitmap bitmapMyQR = QRCode.from(myAddress).bitmap();   //base58 address
        ivMyQRAddress_AM.setImageBitmap(bitmapMyQR);
        if(srlContent_AM.isRefreshing()) srlContent_AM.setRefreshing(false);

    }

    @Override
    public void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public String getAmount() {
        return etAmount_AM.getText().toString();
    }

    @Override
    public void clearAmount() {
        etAmount_AM.setText(null);
    }

    public void startScanQR(RecipientFragment fragment) {
        m_curRecipientFragment = fragment;
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    public void displayInfoDialog(String myAddress) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About");
        builder.setMessage(Html.fromHtml(strAbout));
        builder.setCancelable(true);
        builder.setPositiveButton("GOT IT", (dialog, which) -> dialog.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        TextView msgTxt = (TextView) alertDialog.findViewById(android.R.id.message);
        msgTxt.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
        alertDlg.setMessage("Are you sure you want to exit?");
        alertDlg.setCancelable(false); // We avoid that the dialong can be cancelled, forcing the user to choose one of the options
        alertDlg.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                MainActivity.super.onBackPressed();
            }
        });
        alertDlg.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDlg.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            m_curRecipientFragment.displayRecipientAddress(scanResult.getContents());
        }
    }

    private void setListeners() {
        srlContent_AM.setOnRefreshListener(() -> presenter.refresh());
        btnSend_AM.setOnClickListener(v -> presenter.send());
        etRecipientCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                int count;
                String strCount = s.toString().trim();
                if(strCount.length() == 0) {
                    return;
                } else {
                    try {
                        count = Integer.parseInt(strCount);
                    } catch(Exception e) {
                        count = 1;
                    }

                    if (count < 1) {
                        count = 1;
                        etRecipientCount.setText(Integer.toString(count));
                    } else if (5 < count) {
                        count = 5;
                        etRecipientCount.setText(Integer.toString(count));
                    }
                }

                onChangedRecipientCount(count);
            }
        });
        etAmount_AM.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().trim().length() == 0)
                    etAmount_AM.setText("0.00");
            }
        });
        ivCopy_AM.setOnClickListener(v -> {
            ClipData clip = ClipData.newPlainText("My wallet address", tvMyAddress_AM.getText().toString());
            clipboardManager.setPrimaryClip(clip);
            Toast.makeText(MainActivity.this, "Copied", Toast.LENGTH_SHORT).show();
        });
    }

    //----------------------------------------- Recipient -----------------------------------------------
    private void initRecipientFragments() {
        m_recipientFragments = new ArrayList<RecipientFragment>();
        onChangedRecipientCount(1);
    }

    private void onChangedRecipientCount(int count) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        int cur_cnt = m_recipientFragments.size();
        if (count < cur_cnt) {
            for (int i = cur_cnt - 1; count <= i; i --) {
                Fragment fragment = m_recipientFragments.get(i);
                m_recipientFragments.remove(fragment);
                fragmentTransaction.remove(fragment);
            }
        } if (cur_cnt < count) {
            for (int i = cur_cnt; i < count; i ++) {
                RecipientFragment fragment = new RecipientFragment();
                fragment.setParent(this);
                m_recipientFragments.add(fragment);
                fragmentTransaction.add(R.id.recipient_container, fragment, "RECIPIENT");
            }
        }

        fragmentTransaction.commit();
    }

    @Override
    public int getRecipientCount() {
        return m_recipientFragments.size();
    }

    @Override
    public String getRecipientAddress(int index) {
        RecipientFragment fragment =  m_recipientFragments.get(index);
        if (fragment == null)
            return "";
        return fragment.getRecipient();
    }

    @Override
    public boolean setTransactionHash(int index, String hash) {
        RecipientFragment fragment =  m_recipientFragments.get(index);
        if (fragment == null)
            return false;
        fragment.setTransactionHash(hash);
        return true;
    }

    @Override
    public String getTransactionHash(int index) {
        RecipientFragment fragment =  m_recipientFragments.get(index);
        if (fragment == null)
            return "";
        return fragment.getTransactionHash();
    }

    @Override
    public boolean setStateIdel(int index) {
        RecipientFragment fragment =  m_recipientFragments.get(index);
        if (fragment == null)
            return false;
        fragment.setStateIdel();
        return true;
    }

    @Override
    public boolean setStateSending(int index) {
        RecipientFragment fragment =  m_recipientFragments.get(index);
        if (fragment == null)
            return false;
        fragment.setStateSending();
        return true;
    }

    @Override
    public boolean setStateSuccess(int index) {
        RecipientFragment fragment =  m_recipientFragments.get(index);
        if (fragment == null)
            return false;
        fragment.setStateSuccess();
        return true;
    }

    @Override
    public boolean setStateFail(int index) {
        RecipientFragment fragment =  m_recipientFragments.get(index);
        if (fragment == null)
            return false;
        fragment.setStateFail();
        return true;
    }

    @Override
    public boolean isStateSending(int index) {
        RecipientFragment fragment = m_recipientFragments.get(index);
        if (fragment == null)
            return false;
        return fragment.isStateSending();
    }

    @Override
    public void onSendCompleted(String hash) {
        for (int i = 0; i < m_recipientFragments.size(); i ++) {
            RecipientFragment fragment = m_recipientFragments.get(i);
            if (!fragment.isStateSending())
                continue;

            if (hash.equals(fragment.getTransactionHash())) {
                fragment.setStateSuccess();
                return;
            }
        }
    }
}
