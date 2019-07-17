package com.app.wifipassword;

import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.app.wifipassword.zxing.encode.CodeCreator;
import com.noober.menu.FloatMenu;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.view.View.VISIBLE;

public class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.ViewHolder> {
    private Context mContext;
    private List<Wifi> wifiList;
    private int opened = -1;
    private Map<Integer, Boolean> map = new LinkedHashMap<>();
    private SharedPreferences pref;
    private onItemClickListener listener;

    static class ViewHolder extends RecyclerView.ViewHolder {
        View listView;
        LinearLayout root_view;
        ImageView imageView;
        TextView wifiNameTextView;
        TextView wifiPasswordTextView;
        TextView wifiTypeTextView;
        TextView wifiRemarkTextView;
        FrameLayout detailQRCode;
        ImageView QRCodeView;
        LayoutInflater layoutInflater;
        View remarkInputView;

        public ViewHolder(View view) {
            super(view);
            listView = view;
            root_view = (LinearLayout) view.findViewById(R.id.root_view);
            imageView = (ImageView) view.findViewById(R.id.wifi_image);
            wifiNameTextView = (TextView) view.findViewById(R.id.wifi_name);
            wifiPasswordTextView = (TextView) view.findViewById(R.id.wifi_password);
            wifiTypeTextView = (TextView) view.findViewById(R.id.wifi_type);
            wifiRemarkTextView = (TextView) view.findViewById(R.id.wifi_remark);
            detailQRCode = (FrameLayout) view.findViewById(R.id.detail_qr_code);
            QRCodeView = (ImageView) view.findViewById(R.id.qr_code);
        }
    }

    public WifiAdapter(List<Wifi> wifiList) {
        this.wifiList = wifiList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        final View view = LayoutInflater.from(mContext).inflate(R.layout.wifi_item, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        map = listener.getMap();
        final Wifi wifi = wifiList.get(position);
        String wifiName = "";
        String wifiPassword = "";
        String wifiTypeDetail = "";
        String wifiRemark = wifi.getRemark();
        for (int i = 0; i < wifi.getWifiName().length(); i++) {
            String current = wifi.getWifiName().substring(i, i + 1);
            if (!current.equals("\"")) {
                wifiName += current;
            }
        }
        for (int i = 0; i < wifi.getWifiPassword().length(); i++) {
            String current = wifi.getWifiPassword().substring(i, i + 1);
            if (!current.equals("\"")) {
                wifiPassword += current;
            }
        }
        for (int i = 0; i < wifi.getWifiType().length(); i++) {
            String current = wifi.getWifiType().substring(i, i + 1);
            if (!current.equals("\"")) {
                wifiTypeDetail += current;
            }
        }
        wifiTypeDetail = wifiTypeDetail.substring(wifiName.length());
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo currentWifi = wifiManager.getConnectionInfo();
        String currentWifiName = currentWifi.getSSID();
        holder.imageView.setImageResource(currentWifiName.equals(wifi.getWifiName()) ? R.drawable.ic_network_connect : R.drawable.ic_network);
        holder.wifiNameTextView.setText(wifiName);
        holder.wifiPasswordTextView.setText(pref.getBoolean("is_clear_display", false) && !wifiPassword.equals("") ? "******" : wifiPassword);
        holder.wifiTypeTextView.setText(wifiTypeDetail.equals("NONE") ? null : wifiTypeDetail);
        holder.wifiRemarkTextView.setText(wifiRemark == null ? null : "(" + wifiRemark + ")");
        String wifiType = "";
        wifiType = wifiTypeDetail.contains("WEP") ? "WEP" : "WPA";
        String contentText = "WIFI:T:" + wifiType + ";P:" + wifiPassword + ";S:" + wifiName + ";";
        Bitmap bitmap = CodeCreator.createQRCode(contentText, 300, 300, null);
        if (bitmap != null) {
            holder.QRCodeView.setImageBitmap(bitmap);
        }
        holder.detailQRCode.measure(0, 0);
        final int height = holder.detailQRCode.getMeasuredHeight();
        if (position == opened) {
            show(holder.detailQRCode, height);
        } else {
            dismiss(holder.detailQRCode, height);
        }

        holder.root_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.detailQRCode.measure(0, 0);
                final int height = holder.detailQRCode.getMeasuredHeight();
                if (holder.detailQRCode.getVisibility() == View.VISIBLE) {
                    opened = -1;
                    notifyItemChanged(holder.getAdapterPosition());
                } else {
                    int openedTemp = opened;
                    opened = position;
                    notifyItemChanged(openedTemp);
                    notifyItemChanged(opened);
                }
            }
        });

        holder.root_view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final FloatMenu floatMenu = new FloatMenu(mContext, v);

                final String originalWifiName = wifiList.get(holder.getAdapterPosition()).getWifiName();
                List<Wifi> currentWifi = LitePal.where("wifiName = ?", originalWifiName).find(Wifi.class);
                final Boolean isHighlight = currentWifi.get(0).getHighLight();
                final String originalWifiPassword = wifiList.get(holder.getAdapterPosition()).getWifiPassword();
                final String remark = currentWifi.get(0).getRemark();
                List<String> itemList = new ArrayList<>();
                if (!isHighlight && remark == null && !originalWifiPassword.equals("")) {
                    floatMenu.items(400, mContext.getString(R.string.setRemark), mContext.getString(R.string.copyName),
                            mContext.getString(R.string.copyPassword), mContext.getString(R.string.enableHighlight));
                    itemList.add("setRemark");
                    itemList.add("copyName");
                    itemList.add("copyPassword");
                    itemList.add("enableHighlight");
                } else if (!isHighlight && remark == null && originalWifiPassword.equals("")) {
                    floatMenu.items(400, mContext.getString(R.string.setRemark), mContext.getString(R.string.copyName),
                            mContext.getString(R.string.enableHighlight));
                    itemList.add("setRemark");
                    itemList.add("copyName");
                    itemList.add("enableHighlight");
                } else if (!isHighlight && remark != null && !originalWifiPassword.equals("")) {
                    floatMenu.items(400, mContext.getString(R.string.editRemark), mContext.getString(R.string.deleteRemark),
                            mContext.getString(R.string.copyName), mContext.getString(R.string.copyPassword), mContext.getString(R.string.enableHighlight));
                    itemList.add("editRemark");
                    itemList.add("deleteRemark");
                    itemList.add("copyName");
                    itemList.add("copyPassword");
                    itemList.add("enableHighlight");
                } else if (!isHighlight && remark != null && originalWifiPassword.equals("")) {
                    floatMenu.items(400, mContext.getString(R.string.editRemark), mContext.getString(R.string.deleteRemark),
                            mContext.getString(R.string.copyName), mContext.getString(R.string.enableHighlight));
                    itemList.add("editRemark");
                    itemList.add("deleteRemark");
                    itemList.add("copyName");
                    itemList.add("enableHighlight");
                } else if (isHighlight && remark != null && !originalWifiPassword.equals("")) {
                    floatMenu.items(400, mContext.getString(R.string.editRemark), mContext.getString(R.string.deleteRemark),
                            mContext.getString(R.string.copyName), mContext.getString(R.string.copyPassword), mContext.getString(R.string.disableHighlight));
                    itemList.add("editRemark");
                    itemList.add("deleteRemark");
                    itemList.add("copyName");
                    itemList.add("copyPassword");
                    itemList.add("disableHighlight");
                } else if (isHighlight && remark != null && originalWifiPassword.equals("")) {
                    floatMenu.items(400, mContext.getString(R.string.editRemark), mContext.getString(R.string.deleteRemark),
                            mContext.getString(R.string.copyName), mContext.getString(R.string.disableHighlight));
                    itemList.add("editRemark");
                    itemList.add("deleteRemark");
                    itemList.add("copyName");
                    itemList.add("disableHighlight");
                } else if (isHighlight && remark == null && !originalWifiPassword.equals("")) {
                    floatMenu.items(400, mContext.getString(R.string.setRemark), mContext.getString(R.string.copyName),
                            mContext.getString(R.string.copyPassword), mContext.getString(R.string.disableHighlight));
                    itemList.add("setRemark");
                    itemList.add("copyName");
                    itemList.add("copyPassword");
                    itemList.add("disableHighlight");
                } else if (isHighlight && remark == null && originalWifiPassword.equals("")) {
                    floatMenu.items(400, mContext.getString(R.string.setRemark), mContext.getString(R.string.copyName),
                            mContext.getString(R.string.disableHighlight));
                    itemList.add("setRemark");
                    itemList.add("copyName");
                    itemList.add("disableHighlight");
                }
                final List<String> finalItemList = itemList;
                floatMenu.setOnItemClickListener(new FloatMenu.OnItemClickListener() {
                    @Override
                    public void onClick(View v, int position) {
                        if (position == 0) {
                            final Wifi wifi = new Wifi();
                            if (finalItemList.get(position).equals("editRemark")) {
                                holder.layoutInflater = LayoutInflater.from(mContext);
                                holder.remarkInputView = holder.layoutInflater.inflate(R.layout.remark_input, null);
                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                                alertDialogBuilder.setView(holder.remarkInputView);
                                final EditText remark = (EditText) holder.remarkInputView.findViewById(R.id.remark_message);
                                remark.setText(LitePal.where("wifiName = ?", originalWifiName).find(Wifi.class).get(0).getRemark());
                                alertDialogBuilder
                                        .setCancelable(false)
                                        .setPositiveButton(mContext.getString(R.string.dialog_ok),
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        String remarkMessage = remark.getText().toString().trim();
                                                        wifi.setRemark(remarkMessage);
                                                        holder.wifiRemarkTextView.setText("(" + remarkMessage + ")");
                                                        wifi.updateAll("wifiName = ?", originalWifiName);
                                                    }
                                                })
                                        .setNegativeButton(mContext.getString(R.string.dialog_cancel),
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        dialog.cancel();
                                                    }
                                                });
                                AlertDialog alertDialog = alertDialogBuilder.create();
                                alertDialog.show();
                            } else {
                                holder.layoutInflater = LayoutInflater.from(mContext);
                                holder.remarkInputView = holder.layoutInflater.inflate(R.layout.remark_input, null);
                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                                alertDialogBuilder.setView(holder.remarkInputView);
                                final EditText remark = (EditText) holder.remarkInputView.findViewById(R.id.remark_message);
                                alertDialogBuilder
                                        .setCancelable(false)
                                        .setPositiveButton(mContext.getString(R.string.dialog_ok),
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        String remarkMessage = remark.getText().toString().trim();
                                                        wifi.setRemark(remarkMessage);
                                                        holder.wifiRemarkTextView.setText("(" + remarkMessage + ")");
                                                        wifi.updateAll("wifiName = ?", originalWifiName);
                                                    }
                                                })
                                        .setNegativeButton(mContext.getString(R.string.dialog_cancel),
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        dialog.cancel();
                                                    }
                                                });
                                AlertDialog alertDialog = alertDialogBuilder.create();
                                alertDialog.show();
                            }
                        } else if (position == 1) {
                            if (finalItemList.get(position).equals("deleteRemark")) {
                                wifi.setToDefault("remark");
                                holder.wifiRemarkTextView.setText(null);
                                wifi.updateAll("wifiName = ?", originalWifiName);
                            } else {
                                ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                String wifiName = "";
                                for (int i = 0; i < originalWifiName.length(); i++) {
                                    String current = originalWifiName.substring(i, i + 1);
                                    if (!current.equals("\"")) {
                                        wifiName += current;
                                    }
                                }
                                ClipData clipData = ClipData.newPlainText(null, wifiName);
                                clipboard.setPrimaryClip(clipData);
                                Toast.makeText(mContext, mContext.getString(R.string.copy_name_done), Toast.LENGTH_SHORT).show();
                            }
                        } else if (position == 2) {
                            if (finalItemList.get(position).equals("copyName")) {
                                ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                String wifiName = "";
                                for (int i = 0; i < originalWifiName.length(); i++) {
                                    String current = originalWifiName.substring(i, i + 1);
                                    if (!current.equals("\"")) {
                                        wifiName += current;
                                    }
                                }
                                ClipData clipData = ClipData.newPlainText(null, wifiName);
                                clipboard.setPrimaryClip(clipData);
                                Toast.makeText(mContext, mContext.getString(R.string.copy_name_done), Toast.LENGTH_SHORT).show();
                            } else if (finalItemList.get(position).equals("copyPassword")) {
                                ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                String originalWifiPassword = wifiList.get(holder.getAdapterPosition()).getWifiPassword();
                                String wifiPassword = "";
                                for (int i = 0; i < originalWifiPassword.length(); i++) {
                                    String current = originalWifiPassword.substring(i, i + 1);
                                    if (!current.equals("\"")) {
                                        wifiPassword += current;
                                    }
                                }
                                ClipData clipData = ClipData.newPlainText(null, wifiPassword);
                                clipboard.setPrimaryClip(clipData);
                                Toast.makeText(mContext, mContext.getString(R.string.copy_password_done), Toast.LENGTH_SHORT).show();
                            } else {
                                Wifi wifi = new Wifi();
                                wifi.setHighLight(!isHighlight);
                                wifi.updateAll("wifiName = ?", originalWifiName);
                                map.put(holder.getAdapterPosition(), !isHighlight);
                                holder.root_view.setBackgroundColor(!isHighlight ? mContext.getResources().getColor(R.color.colorHighlight) : mContext.getResources().getColor(R.color.grayPrimary));
                            }
                        } else if (position == 3) {
                            if (finalItemList.get(position).equals("copyPassword")) {
                                ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                String originalWifiPassword = wifiList.get(holder.getAdapterPosition()).getWifiPassword();
                                String wifiPassword = "";
                                for (int i = 0; i < originalWifiPassword.length(); i++) {
                                    String current = originalWifiPassword.substring(i, i + 1);
                                    if (!current.equals("\"")) {
                                        wifiPassword += current;
                                    }
                                }
                                ClipData clipData = ClipData.newPlainText(null, wifiPassword);
                                clipboard.setPrimaryClip(clipData);
                                Toast.makeText(mContext, mContext.getString(R.string.copy_password_done), Toast.LENGTH_SHORT).show();
                            } else {
                                Wifi wifi = new Wifi();
                                wifi.setHighLight(!isHighlight);
                                wifi.updateAll("wifiName = ?", originalWifiName);
                                map.put(holder.getAdapterPosition(), !isHighlight);
                                holder.root_view.setBackgroundColor(!isHighlight ? mContext.getResources().getColor(R.color.colorHighlight) : mContext.getResources().getColor(R.color.grayPrimary));
                            }
                        } else if (position == 4) {
                            Wifi wifi = new Wifi();
                            wifi.setHighLight(!isHighlight);
                            wifi.updateAll("wifiName = ?", originalWifiName);
                            map.put(holder.getAdapterPosition(), !isHighlight);
                            holder.root_view.setBackgroundColor(!isHighlight ? mContext.getResources().getColor(R.color.colorHighlight) : mContext.getResources().getColor(R.color.grayPrimary));
                        }
                    }
                });
                floatMenu.show(listener.getPoint());
                return false;
            }
        });

        if (map.containsKey(position)) {
            if (map.get(position)) {
                holder.root_view.setBackgroundColor(mContext.getResources().getColor(R.color.colorHighlight));
            }
        } else {
            holder.root_view.setBackgroundColor(mContext.getResources().getColor(R.color.grayPrimary));
        }

    }

    @Override
    public int getItemCount() {
        return wifiList.size();
    }

    public void show(final View v, int height) {
        v.setVisibility(VISIBLE);
        ValueAnimator animator = ValueAnimator.ofInt(0, height);
        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (Integer) animation.getAnimatedValue();
                v.getLayoutParams().height = value;
                v.setLayoutParams(v.getLayoutParams());
            }
        });
        animator.start();
    }

    public void dismiss(final View v, int height) {
        ValueAnimator animator = ValueAnimator.ofInt(height, 0);
        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (Integer) animation.getAnimatedValue();
                if (value == 0) {
                    v.setVisibility(View.GONE);
                }
                v.getLayoutParams().height = value;
                v.setLayoutParams(v.getLayoutParams());
            }
        });
        animator.start();
    }

    public void setListener(onItemClickListener listener) {
        this.listener = listener;
    }

    public interface onItemClickListener {

        Point getPoint();

        Map<Integer, Boolean> getMap();

    }

}
