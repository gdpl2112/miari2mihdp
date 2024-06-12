package clinet;

import clinet.mihdp.GeneralData;
import clinet.mihdp.MihdpClient;
import clinet.mihdp.ReqDataPack;
import clinet.mihdp.ResDataPack;
import com.alibaba.fastjson.JSON;
import io.github.kloping.judge.Judge;
import io.github.kloping.url.UrlUtils;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.ListenerHost;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;

import java.util.Base64;
import java.util.Deque;
import java.util.LinkedList;

/**
 * @author github.kloping
 */
public class MihdpConnect implements ListenerHost {
    public static final MihdpConnect INSTANCE = new MihdpConnect();

    @EventHandler
    public void onMessage(GroupMessageEvent event) {
        sendToMihdp(event);
    }

    @EventHandler
    public void onMessage(FriendMessageEvent event) {
        sendToMihdp(event);
    }

    @EventHandler
    public void onMessage(GroupTempMessageEvent event) {
        sendToMihdp(event);
    }

    private void sendToMihdp(MessageEvent event) {
        offer(event);
        if (MihdpClient.INSTANCE == null) return;
        GeneralData.ResDataChain chain = new GeneralData.ResDataChain(new LinkedList<>());
        for (SingleMessage singleMessage : event.getMessage()) {
            if (singleMessage instanceof PlainText) {
                chain.getList().add(new GeneralData.ResDataText(((PlainText) singleMessage).getContent()));
            } else if (singleMessage instanceof Image) {
                chain.getList().add(new GeneralData.ResDataImage(Image.queryUrl((Image) singleMessage), "http", 1, 1));
            } else if (singleMessage instanceof At) {
                chain.getList().add(new GeneralData.ResDataAt(String.valueOf(((At) singleMessage).getTarget())));
            }
        }
        ReqDataPack req = new ReqDataPack();
        req.setAction("msg")
                .setContent(JSON.toJSONString(chain))
                .setId(getMessageEventId(event))
                .setBot_id(String.valueOf(event.getBot().getId()))
                .setTime(System.currentTimeMillis())
                .setEnv_type(event instanceof GroupMessageEvent ? "group" : "friend")
                .setSender_id(String.valueOf(event.getSender().getId()))
                .setEnv_id(String.valueOf(event.getSubject().getId()));
        req.getArgs().put("icon", event.getSender().getAvatarUrl());
        req.getArgs().put("name", event.getSender().getNick());
        req.getArgs().put("draw", "true");
        MihdpClient.INSTANCE.send(req.toString());
    }

    @EventHandler
    public void onBotOnline(BotOnlineEvent event) {
        String bid = String.valueOf(event.getBot().getId());
        if (MihdpClient.INSTANCE == null) return;
        MihdpClient.INSTANCE.listeners.put(bid, new MihdpClient.MihdpClientMessageListener() {
            @Override
            public void onMessage(ResDataPack pack) {
                MessageEvent raw = getMessage(pack.getId());
                MessageChainBuilder builder = new MessageChainBuilder();
                if (raw != null) builder.append(new QuoteReply(raw.getSource()));
                append(pack.getData(), builder, raw.getSubject());
                Contact contact;
                if (raw != null) contact = raw.getSubject();
                else contact = pack.getEnv_type().equals("group") ?
                        event.getBot().getFriend(Long.parseLong(pack.getEnv_id())) :
                        event.getBot().getGroup(Long.parseLong(pack.getEnv_id()));
                contact.sendMessage(builder.build());
            }

            private void append(GeneralData data, MessageChainBuilder builder, Contact contact) {
                if (data instanceof GeneralData.ResDataChain) {
                    GeneralData.ResDataChain chain = (GeneralData.ResDataChain) data;
                    for (GeneralData generalData : chain.getList()) {
                        append(generalData, builder, contact);
                    }
                } else if (data instanceof GeneralData.ResDataText) {
                    builder.append(((GeneralData.ResDataText) data).getContent());
                } else if (data instanceof GeneralData.ResDataAt) {
                    builder.append(new At(Long.valueOf(((GeneralData.ResDataAt) data).getId())));
                } else if (data instanceof GeneralData.ResDataImage) {
                    GeneralData.ResDataImage image = (GeneralData.ResDataImage) data;
                    if (image.getType().equals("http")) {
                        byte[] bytes = UrlUtils.getBytesFromHttpUrl(image.getData());
                        builder.append(contact.uploadImage(ExternalResource.create(bytes)));
                    } else {
                        builder.append(contact.uploadImage(ExternalResource.create(Base64.getDecoder().decode(image.getData()))));
                    }
                } else if (data instanceof GeneralData.ResDataSelect) {
                    String d0 = ((GeneralData.ResDataSelect) data).getS() + "." + ((GeneralData.ResDataSelect) data).getContent();
                    builder.append(d0);
                }
            }
        });
    }

    //=============消息记录start
    public static final Integer MAX_E = 50;
    private MessageEvent temp0 = null;
    private Deque<MessageEvent> QUEUE = new LinkedList<>();


    public void offer(MessageEvent msg) {
        if (QUEUE.contains(msg)) return;
        if (QUEUE.size() >= MAX_E) QUEUE.pollLast();
        QUEUE.offerFirst(msg);
    }

    public MessageEvent getMessage(String id) {
        if (Judge.isEmpty(id)) return null;
        if (temp0 != null && getMessageEventId(temp0).equals(id)) return temp0;
        for (MessageEvent event : QUEUE) {
            if (getMessageEventId(event).equals(id)) return temp0 = event;
        }
        return null;
    }

    public String getMessageEventId(MessageEvent event) {
        if (event.getSource().getIds().length == 0) return "";
        else return String.valueOf(event.getSource().getIds()[0]);
    }
    //=============消息记录end
}
