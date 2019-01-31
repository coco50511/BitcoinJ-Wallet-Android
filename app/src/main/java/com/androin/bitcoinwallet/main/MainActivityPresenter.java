package com.androin.bitcoinwallet.main;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.androin.bitcoinwallet.Constants;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * Created by Lynx on 4/11/2017.
 */

public class MainActivityPresenter implements MainActivityContract.MainActivityPresenter {

    private MainActivityContract.MainActivityView view;
    private File walletDir; //Context.getCacheDir();

    private NetworkParameters parameters;
    private WalletAppKit walletAppKit;

    public MainActivityPresenter(MainActivityContract.MainActivityView view, File walletDir) {
        this.view = view;
        this.walletDir = walletDir;

        view.setPresenter(this);
    }

    @Override
    public void subscribe() {
        setBtcSDKThread();
        BriefLogFormatter.init();

        parameters = Constants.IS_PRODUCTION ? MainNetParams.get() : TestNet3Params.get();
        String wallet_name = Constants.IS_PRODUCTION ? Constants.WALLET_NAME : Constants.WALLET_NAME_TESTNET;

        walletAppKit = new WalletAppKit(parameters, walletDir, wallet_name) {
            @Override
            protected void onSetupCompleted() {
                if (wallet().getImportedKeys().size() < 1) wallet().importKey(new ECKey());
                wallet().allowSpendingUnconfirmedTransactions();
                view.displayWalletPath(vWalletFile.getAbsolutePath());
                setupWalletListeners(wallet());

                Log.d("myLogs", "My address = " + wallet().freshReceiveAddress());
            }
        };
        walletAppKit.setDownloadListener(new DownloadProgressTracker() {
            @Override
            protected void progress(double pct, int blocksSoFar, Date date) {
                super.progress(pct, blocksSoFar, date);
                int percentage = (int) pct;
                view.displayPercentage(percentage);
                view.displayProgress(percentage);
            }

            @Override
            protected void doneDownload() {
                super.doneDownload();
                view.displayDownloadContent(false);
                refresh();
            }
        });
        walletAppKit.setBlockingStartup(false);
        walletAppKit.setCheckpoints(getCheckpoints());
        walletAppKit.startAsync();
    }

    private InputStream getCheckpoints() {
        AssetManager assManager = ((Activity)view).getApplicationContext().getAssets();
        InputStream is = null;
        try {
            is = assManager.open(Constants.IS_PRODUCTION ? Constants.CHECKPOINTS : Constants.CHECKPOINTS_TESTNET);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return is;
    }

    @Override
    public void unsubscribe() {

    }

    @Override
    public void refresh() {
        String myAddress = walletAppKit.wallet().freshReceiveAddress().toBase58();

        view.displayMyBalance(walletAppKit.wallet().getBalance().toFriendlyString());
        view.displayMyAddress(myAddress);

    }

    @Override
    public void send() {
        int count = view.getRecipientCount();

        // Get total count of valid recipient address
        int real_count = 0;
        for (int i = 0; i < count; i ++) {
            view.setStateIdel(i);
            view.setTransactionHash(i,"");

            String recipientAddress = view.getRecipientAddress(i);
            if (!TextUtils.isEmpty(recipientAddress)) {
                real_count ++;
            }
        }
        if (real_count <= 0) {
            view.showToastMessage("Input the least one recipient address");
            return;
        }

        // Check the balance of wallet
        String strAmount = view.getAmount();
        if (TextUtils.isEmpty(strAmount) | Float.parseFloat(strAmount) <= 0) {
            view.showToastMessage("Select valid amount");
            return;
        }
        float total_amount = Float.parseFloat(strAmount) * real_count;
        String strTotalAmount = String.format ("%f", total_amount);
        if (walletAppKit.wallet().getBalance().isLessThan(Coin.parseCoin(strTotalAmount))) {
            view.showToastMessage("You got not enough coins");
            view.clearAmount();
            return;
        }

        // Send
        for (int i = 0; i < count; i ++) {
            String recipientAddress = view.getRecipientAddress(i);
            if (TextUtils.isEmpty(recipientAddress)) {
                continue;
            }

            try {
                SendRequest request = SendRequest.to(Address.fromBase58(parameters, recipientAddress), Coin.parseCoin(strAmount));
                walletAppKit.wallet().completeTx(request);
                walletAppKit.wallet().commitTx(request.tx);
                walletAppKit.peerGroup().broadcastTransaction(request.tx).broadcast();

                view.setTransactionHash(i, request.tx.getHashAsString());
                view.setStateSending(i);
            } catch (Exception e) {
                e.printStackTrace();
                view.setStateFail(i);
                view.showToastMessage(e.getMessage());
            }
        }
    }

    @Override
    public void getInfoDialog() {
        view.displayInfoDialog(walletAppKit.wallet().currentReceiveAddress().toBase58());
    }

    private void setBtcSDKThread() {
        final Handler handler = new Handler();
        Threading.USER_THREAD = handler::post;
    }

    private void setupWalletListeners(Wallet wallet) {
        wallet.addCoinsReceivedEventListener((wallet1, tx, prevBalance, newBalance) -> {
            view.displayMyBalance(wallet.getBalance().toFriendlyString());
            if(tx.getPurpose() == Transaction.Purpose.UNKNOWN)
            view.showToastMessage("Receive " + newBalance.minus(prevBalance).toFriendlyString());
        });
        wallet.addCoinsSentEventListener((wallet12, tx, prevBalance, newBalance) -> {
            view.onSendCompleted(tx.getHashAsString());
            view.displayMyBalance(wallet.getBalance().toFriendlyString());
            view.showToastMessage("Sent " + prevBalance.minus(newBalance).minus(tx.getFee()).toFriendlyString());
        });
    }
}
