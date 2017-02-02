package com.example.songjian.logintest;

import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;
import android.os.Handler;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.xdata.Form;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by songjian on 25/09/2016.
 */
public class FriendsList extends ExpandableListActivity {
    private static ConnectionManager mConnectionManager = ConnectionManager.shareInstance();

    /*
    * create level 1 container
    * */
    private List<Map<String, String>> groups = new ArrayList<Map<String, String>>();

    /*
    * store content, in order to show content in the list
    * */
    private List<List<Map<String, String>>> childs = new ArrayList<List<Map<String, String>>>();
    private Roster roster = mConnectionManager.getRoster();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list_expandable);
        setListData();

        Button bBack = (Button) findViewById(R.id.bBack);
        bBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent userAreaActivityIntent = new Intent(FriendsList.this, UserAreaActivity.class);
                FriendsList.this.startActivity(userAreaActivityIntent);
                FriendsList.this.finish();
            }
        });

        //添加好友按钮
        Button bAddFriends = (Button) findViewById(R.id.bAddFriends);
        bAddFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFriendDialog();
            }
        });
    }
    //930新加
    //    public Chat getFriendChat (String friend, MessageListener listener) {
    //        if(mConnectionManager.connection == null) {
    //            return null;
    //        }
    //        for (String fristr: chatManage.keySet()){
    //            if(fristr.equals(friend)){
    //                return chatManage.get(fristr);
    //            }
    //        }
    //        ChatManager chatManager = ChatManager.getInstanceFor(mConnectionManager.connection);
    //
    //        Chat chat = chatManager.createChat(friend+"@"+mConnectionManager.connection.getServiceName(), (ChatMessageListener) listener);
    //
    //        chatManage.put(friend,chat);
    //        return chat;
    //    }

    public void setListData() {
        //这个函数是创建折叠好友列表的核心函数,不过可以不仔细看
        groups.clear();
        childs.clear();
        try {
            roster.reload();
        } catch (SmackException.NotLoggedInException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        String pattern1 = "\\[(.*)\\]";

        // Create a Pattern object
        Pattern r1 = Pattern.compile(pattern1);

        //获得所有用户(userName, group)
        Map<String, String> users = new HashMap<String, String>(); //("stan", "friends" )
        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry entry : entries) {
            System.out.print(entry.toString() + '\n');
            // Create matcher object.
            Matcher m1 = r1.matcher(entry.toString());
            String groupName = "";
            if (m1.find())
                groupName = m1.group(1);
            else
                groupName = "DefaultGroup";
            String userName = entry.toString().split(":")[0];

            users.put(userName, groupName);
        }

        //根据组信息,来创建一级条目
        Collection collectionGroups = users.values();
        Set setGroups = new HashSet(collectionGroups);
        Iterator iteratorGroups = setGroups.iterator();
        while (iteratorGroups.hasNext()) {
            String group = (String) iteratorGroups.next();
            //根据组信息,来创建一级条目
            Map<String, String> groupItem = new HashMap<String, String>();
            groupItem.put("group", group);
            groups.add(groupItem);

            //创建二级条目
            List<Map<String, String>> childItem = new ArrayList<Map<String, String>>();
            Iterator iterator = users.entrySet().iterator(); //users: ("stan", "friends" )
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String groupName = (String) entry.getValue();
                String userName = (String) entry.getKey();
                if (group.equals(groupName)) {
                    Map<String, String> childItemContent = new HashMap<String, String>();
                    childItemContent.put("child", userName);
                    childItem.add(childItemContent);
                }
            }
            childs.add(childItem);
        }

        SimpleExpandableListAdapter sela = new SimpleExpandableListAdapter(
                this, groups, R.layout.friend_list_group, new String[]{"group"},
                new int[]{R.id.textGroup}, childs, R.layout.friend_list_child,
                new String[]{"child"}, new int[]{R.id.textChild});
        setListAdapter(sela);
    }

    //这个函数就是点击了某个好友后的响应函数
    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
                                int groupPosition, int childPosition, long id) {
        Toast.makeText(
                FriendsList.this,
                "您选择了"
                        + groups.get(groupPosition).toString()
                        + "子编号"
                        + childs.get(groupPosition).get(childPosition)
                        .toString(), Toast.LENGTH_SHORT).show();
        Intent chatIntent = new Intent(FriendsList.this, ChatActivity.class);
        String JID = "";
        String pattern2 = "\\{.*=(.*)\\}";
        Pattern r2 = Pattern.compile(pattern2);
        Matcher m2 = r2.matcher(childs.get(groupPosition).get(childPosition).toString());
        if (m2.find())
            JID = m2.group(1) + "@" + mConnectionManager.connection.getServiceName();
        chatIntent.putExtra("JID", JID); //这是activity之间传递参数的一种方式,很简单

        FriendsList.this.startActivity(chatIntent);
        FriendsList.this.finish();
        return super.onChildClick(parent, v, groupPosition, childPosition, id);
    }

    //不用管
    @Override
    public boolean setSelectedChild(int groupPosition, int childPosition,
                                    boolean shouldExpandGroup) {
        return super.setSelectedChild(groupPosition, childPosition,
                shouldExpandGroup);
    }
    //不用管
    @Override
    public void setSelectedGroup(int groupPosition) {
        super.setSelectedGroup(groupPosition);
    }

    //添加好友那个窗口
    private void addFriendDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(FriendsList.this);
        final View viewAddFriend = layoutInflater.inflate(R.layout.add_friend, null);
        Dialog dialog = new AlertDialog.Builder(FriendsList.this)
                .setTitle("Add friend")
                .setView(viewAddFriend)
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText etAddFriend = (EditText) viewAddFriend.findViewById(R.id.etAddFriend);
                        EditText etAddFriendGroup = (EditText) viewAddFriend.findViewById(R.id.etAddFriendGroup);
                        String userName = etAddFriend.getText().toString();
                        String nickName = etAddFriend.getText().toString();
                        String groupName = etAddFriendGroup.getText().toString();
                        if ("".equals(groupName))
                            groupName = null;
                        if (mConnectionManager.addFriend(userName, nickName, groupName)) {//调用添加好友函数,如果返回true,那么用handler异步刷新UI
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    setListData();
                                }
                            });
                            setListData();
                            Toast.makeText(FriendsList.this, "adding friend succeeded", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(FriendsList.this, "User doesn't exist", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).create();
        dialog.show();
    }

    //
    //下面的函数可以不看,网上抄的,按需求调用即可
    //

    //查询所有分组
    public static List<RosterGroup> getGroups(Roster roster) {
        List<RosterGroup> grouplist = new ArrayList<RosterGroup>();
        Collection<RosterGroup> rosterGroup = roster.getGroups();
        Iterator<RosterGroup> i = rosterGroup.iterator();
        while (i.hasNext()) {
            grouplist.add(i.next());
        }
        return grouplist;
    }

    //添加分组
    public static boolean addGroup(Roster roster, String groupName) {
        try {
            roster.createGroup(groupName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //按组查询好友
    public static List<RosterEntry> getEntriesByGroup(Roster roster,
                                                      String groupName) {
        List<RosterEntry> Entrieslist = new ArrayList<RosterEntry>();
        RosterGroup rosterGroup = roster.getGroup(groupName);
        Collection<RosterEntry> rosterEntry = rosterGroup.getEntries();
        Iterator<RosterEntry> i = rosterEntry.iterator();
        while (i.hasNext()) {
            Entrieslist.add(i.next());
        }
        return Entrieslist;
    }

    //查询所有好友
    public List<RosterEntry> getAllEntries(Roster roster) {
        List<RosterEntry> Entrieslist = new ArrayList<RosterEntry>();
        Collection<RosterEntry> rosterEntry = roster.getEntries();
        Iterator<RosterEntry> i = rosterEntry.iterator();
        while (i.hasNext()) {
            Entrieslist.add(i.next());
        }
        return Entrieslist;
    }


}
