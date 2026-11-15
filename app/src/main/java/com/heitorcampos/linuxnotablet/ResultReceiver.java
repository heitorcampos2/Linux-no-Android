package com.heitorcampos.linuxnotablet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/** Recebe a resposta do Termux (saída, código de saída e erro) e entrega ao Termux.run(). */
public class ResultReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        int id = intent.getIntExtra("id", -1);
        Bundle r = intent.getBundleExtra("result");
        Termux.deliver(id, r);
    }
}
