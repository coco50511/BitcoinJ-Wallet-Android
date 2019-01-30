package com.androin.bitcoinwallet.main;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.androin.bitcoinwallet.R;

public class RecipientFragment extends Fragment {
    private MainActivity mParent;
    private RecipientFragment mThis;

    private Button btnRecipientAddress_AM;
    private EditText etRecipientAddress_AM;
    private TextView tvLabel;

    private static final int STATE_IDLE = 0;
    private static final int STATE_SENDING = 1;
    private static final int STATE_SUCCESS = 2;
    private static final int STATE_FAIL = 3;
    private int mState = STATE_IDLE;
    private String mTransactionHash = "";

    private View.OnClickListener listenerQrScan = new View.OnClickListener() {
        public void onClick(View v) {
            displayRecipientAddress(null);
            mParent.startScanQR(mThis);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /** Inflating the layout for this fragment **/
        mThis = this;

        View v = inflater.inflate(R.layout.recipient_fragment, null);

        btnRecipientAddress_AM = ((Button) v.findViewById(R.id.btnRecipientAddress_AM));
        etRecipientAddress_AM = ((EditText) v.findViewById(R.id.etRecipientAddress_AM));
        tvLabel = ((TextView) v.findViewById(R.id.tvLabel));

        btnRecipientAddress_AM.setOnClickListener(listenerQrScan);

        return v;
    }

    public void setParent(MainActivity activity) {
        mParent = activity;
    }

    public void displayRecipientAddress(String recipientAddress) {
        etRecipientAddress_AM.setText(TextUtils.isEmpty(recipientAddress) ? "" : recipientAddress);
        etRecipientAddress_AM.setTextColor(TextUtils.isEmpty(recipientAddress) ? mParent.colorGreyDark : mParent.colorGreenDark);
    }

    public String getRecipient() {
        return etRecipientAddress_AM.getText().toString().trim();
    }

    //--------------------------- state -----------------------------------
    public void setStateIdel() {
        mState = STATE_IDLE;
        tvLabel.setTextColor(mParent.colorGreyDark);
    }
    public void setStateSending() {
        mState = STATE_SENDING;
        tvLabel.setTextColor(mParent.colorBlueDark);
    }
    public void setStateSuccess() {
        mState = STATE_SUCCESS;
        tvLabel.setTextColor(mParent.colorGreenDark);
    }
    public void setStateFail() {
        mState = STATE_FAIL;
        tvLabel.setTextColor(mParent.colorRedDark);
    }
    public boolean isStateSending() { return (mState == STATE_SENDING); }

    public String getTransactionHash() { return mTransactionHash; }
    public void setTransactionHash(String hash) { mTransactionHash = hash; }
}