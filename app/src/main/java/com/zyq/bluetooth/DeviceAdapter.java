package com.zyq.bluetooth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Administrator on 2017/10/20 0020.
 */

public class DeviceAdapter extends BaseAdapter {

    private Context mContext;
    private List<DeviceItem> mItems;
    public DeviceAdapter(Context context, List<DeviceItem> items){
        this.mContext = context;
        this.mItems = items;
    }
    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_layout,null);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.list_item_name);
            holder.address = (TextView) convertView.findViewById(R.id.list_item_address);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        holder.name.setText(mItems.get(position).getDeviceName());
        holder.address.setText(mItems.get(position).getDeviceAddress());
        return convertView;
    }

    class ViewHolder{
        TextView name;
        TextView address;
    }
}
