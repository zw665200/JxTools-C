package com.hyphenate.helpdesk.easeui.widget;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ListView;
import android.widget.RelativeLayout;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.hyphenate.chat.ChatClient;
import com.hyphenate.chat.Conversation;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.chat.Message;
import com.hyphenate.helpdesk.R;
import com.hyphenate.helpdesk.easeui.adapter.MessageAdapter;
import com.hyphenate.helpdesk.easeui.provider.CustomChatRowProvider;
import com.hyphenate.helpdesk.util.Log;

import java.util.List;

public class MessageList extends RelativeLayout {
    protected static final String TAG = MessageList.class.getSimpleName();
    protected ListView listView;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected Context context;
    protected Conversation conversation;
    protected String toChatUsername;
    protected MessageAdapter messageAdapter;
    protected boolean showUserNick;
    protected boolean showAvatar;
    protected Drawable myBubbleBg;
    protected Drawable otherBuddleBg;
    public static long defaultDelay = 200;

    public MessageList(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs);
    }

    public MessageList(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseStyle(context, attrs);
        init(context);
    }

    public MessageList(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.hd_chat_message_list, this);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.chat_swipe_layout);
        listView = (ListView) findViewById(R.id.list);
    }

    /**
     * init widget
     *
     * @param toChatUsername
     * @param customChatRowProvider
     */
    public void init(String toChatUsername, CustomChatRowProvider customChatRowProvider) {
        this.toChatUsername = toChatUsername;

        conversation = ChatClient.getInstance().chatManager().getConversation(toChatUsername);
        messageAdapter = new MessageAdapter(context, toChatUsername, listView);
        messageAdapter.setShowAvatar(showAvatar);
        messageAdapter.setShowUserNick(showUserNick);
        messageAdapter.setMyBubbleBg(myBubbleBg);
        messageAdapter.setOtherBuddleBg(otherBuddleBg);
        messageAdapter.setCustomChatRowProvider(customChatRowProvider);
        // 设置adapter显示消息
        listView.setAdapter(messageAdapter);

        refreshSelectLast();
    }

    protected void parseStyle(Context context, AttributeSet attrs) {
        @SuppressLint("CustomViewStyleable") TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.EaseChatMessageList);
        showAvatar = ta.getBoolean(R.styleable.EaseChatMessageList_msgListShowUserAvatar, true);
        myBubbleBg = ta.getDrawable(R.styleable.EaseChatMessageList_msgListMyBubbleBackground);
        otherBuddleBg = ta.getDrawable(R.styleable.EaseChatMessageList_msgListMyBubbleBackground);
        showUserNick = ta.getBoolean(R.styleable.EaseChatMessageList_msgListShowUserNick, false);
        ta.recycle();
    }


    /**
     * 刷新列表
     */
    public void refresh() {
        if (messageAdapter != null) {
            messageAdapter.refresh();
        }
    }

    public void clearMessages(){
        if(messageAdapter != null){
            messageAdapter.isAdd = false;
            messageAdapter.isClear = true;
        }
    }

    public void addNewMessage(String content, Message message) {
        if (messageAdapter != null) {
            if (!messageAdapter.isNewSession) {
                ChatClient.getInstance().chatManager().sendMessage(message);
            } else {
                messageAdapter.addAndRefresh(message);
                String cont = getContent(content);
                if (cont != null) {
                    Message msg = Message.createReceiveMessage(Message.Type.TXT);
                    EMTextMessageBody body = new EMTextMessageBody(cont);
                    msg.setBody(body);
                    messageAdapter.addAndRefresh(msg);
                } else {
                    if (content.equals("9")) {
                        ChatClient.getInstance().chatManager().sendMessage(message);
                    }
                }
            }
            messageAdapter.refreshSelectLast();
        }
    }

    private String getContent(String key) {
        ArrayMap<String, String> map = new ArrayMap<>();
        map.put("1", "点击首页的【微信聊天恢复】，根据教程提示操作（部分机型需要配合电脑端的官方备份助手）");
        map.put("2", "点击首页的【微信聊天恢复】，根据教程提示操作（部分机型需要配合电脑端的官方备份助手）");
        map.put("3", "本软件是对微信备份文件进行数据分析的，和微信本身也没有任何的交互。恢复的聊天记录只能查看，恢复的好友需要通过恢复的微信号去微信上添加");
        map.put("4", "软件扫描数据，和是否登录微信账户或者是否打开微信没有关系。软件是通过扫描备份文件中的数据碎片来查找记录的。");
        map.put("5", "本软件能恢复出来多少的聊天记录和文件，和用户删除之后的具体操作有很大关系。新的数据会不断覆盖掉之前被删除的数据。时间越久或者数据写入频繁，恢复的几率越低。");
        map.put("6", "微信卸载之后，文字记录恢复几率相对很低，有机会找到卸载之前的图片、语音、视频，文档等文件，具体可以体验首页各个功能。");
        map.put("7", "点击首页的【微信图片恢复】，软件会对微信的目录深层扫描（提示：手机内的文件碎片越多，扫描的时间就会越长，请耐心等待）");
        map.put("8", "付完款之后，在聊天恢复页面查看到完整的教程，请根据教程提示操作即可");
        return map.get(key);
    }

    /**
     * 刷新列表，并且跳至最后一个item
     */
    public void refreshSelectLast() {
        if (messageAdapter != null) {
            messageAdapter.refreshSelectLast();
        }
    }

    public void refreshSelectLastDelay(long delay) {
        new Handler().postDelayed(() -> {
            if (messageAdapter != null) {
                messageAdapter.refreshSelectLast();
            }
        }, delay);
    }

    /**
     * 刷新页面,并跳至给定position
     *
     * @param position
     */
    public void refreshSeekTo(int position) {
        if (messageAdapter != null) {
            messageAdapter.refreshSeekTo(position);
        }
    }

    /**
     * 获取listview
     *
     * @return
     */
    public ListView getListView() {
        return listView;
    }

    /**
     * 获取SwipeRefreshLayout
     *
     * @return
     */
    public SwipeRefreshLayout getSwipeRefreshLayout() {
        return swipeRefreshLayout;
    }

    public Message getItem(int position) {
        return messageAdapter.getItem(position);
    }

    /**
     * 设置是否显示用户昵称
     *
     * @param showUserNick
     */
    public void setShowUserNick(boolean showUserNick) {
        this.showUserNick = showUserNick;
    }

    public boolean isShowUserNick() {
        return showUserNick;
    }

    public enum ItemAction {
        ITEM_TO_NOTE,//跳转到留言页面
        ITEM_RESOLVED, //问题已经解决
        ITEM_UNSOLVED //问题未解决
    }

    public interface MessageListItemClickListener {
        void onResendClick(Message message);

        /**
         * 控件有对气泡做点击事件默认实现，如果需要自己实现，return true。
         * 当然也可以在相应的chatrow的onBubbleClick()方法里实现点击事件
         *
         * @param message
         * @return
         */
        boolean onBubbleClick(Message message);

        void onBubbleLongClick(Message message);

        void onUserAvatarClick(String username);

        void onMessageItemClick(Message message, ItemAction action);
    }

    /**
     * 设置list item里控件的点击事件
     *
     * @param listener
     */
    public void setItemClickListener(MessageListItemClickListener listener) {
        if (messageAdapter != null) {
            messageAdapter.setItemClickListener(listener);
        }
    }

    /**
     * 设置自定义chatrow提供者
     *
     * @param rowProvider
     */
    public void setCustomChatRowProvider(CustomChatRowProvider rowProvider) {
        if (messageAdapter != null) {
            messageAdapter.setCustomChatRowProvider(rowProvider);
        }
    }
}