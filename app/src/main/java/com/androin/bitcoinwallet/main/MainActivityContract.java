package com.androin.bitcoinwallet.main;

import com.androin.bitcoinwallet.base.BasePresenter;
import com.androin.bitcoinwallet.base.BaseView;

/**
 * Created by Lynx on 4/11/2017.
 */

public interface MainActivityContract {
    interface MainActivityView extends BaseView<MainActivityPresenter> {
        void displayDownloadContent(boolean isShown);
        void displayProgress(int percent);
        void displayPercentage(int percent);

        void displayMyBalance(String myBalance);
        void displayWalletPath(String walletPath);

        void displayMyAddress(String myAddress);

        void showToastMessage(String message);
        String getAmount();
        void clearAmount();

        void displayInfoDialog(String myAddress);

        int getRecipientCount();
        String getRecipientAddress(int index);
        boolean setTransactionHash(int index, String hash);
        String getTransactionHash(int index);
        boolean setStateIdel(int index);
        boolean setStateSending(int index);
        boolean setStateSuccess(int index);
        boolean setStateFail(int index);
        boolean isStateSending(int index);
        void onSendCompleted(String hash);
    }
    interface MainActivityPresenter extends BasePresenter {
        void refresh();
        void send();

        void getInfoDialog();
    }
}
