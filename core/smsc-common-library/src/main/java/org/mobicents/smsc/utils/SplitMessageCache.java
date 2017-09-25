package org.mobicents.smsc.utils;

import org.mobicents.smsc.library.Sms;

import java.util.*;

/**
 * Created by Stanis?aw Leja on 31.08.17.
 */
public class SplitMessageCache implements SplitMessageCacheMBean {

    private static SplitMessageCache instance = null;
    private static int removeOlderThanXSeconds;
    private static Map<String,Long> referenceNumberMessageIdA;
    private static Map<String,Long> referenceNumberMessageIdB;
    private static boolean balanceFlag = false;
    private static long lastChangeOfBalanceFlag;

    public SplitMessageCache(){
        referenceNumberMessageIdA = new HashMap<String, Long>();
        referenceNumberMessageIdB = new HashMap<String, Long>();
        removeOlderThanXSeconds = 600;
        balanceFlag = false;
        lastChangeOfBalanceFlag = System.currentTimeMillis();
    }

    public static SplitMessageCache getInstance() {
        if (instance == null) {
            instance = new SplitMessageCache();
        }
        return instance;
    }

    public void addReferenceNumber(int reference_number, Sms smsEvent, long message_id){
        if(balanceFlag){
            referenceNumberMessageIdA.put(createStringReferenceNumber(reference_number,smsEvent),message_id);
        }else{
            referenceNumberMessageIdB.put(createStringReferenceNumber(reference_number,smsEvent),message_id);
        }
    }

    private String createStringReferenceNumber(int reference_number, Sms smsEvent){
        StringBuilder sb = new StringBuilder();
        sb.append(reference_number);
        sb.append(";");
        sb.append(smsEvent.getSourceAddr());
        return sb.toString();
    }

    private int getReferenceNumber(String reference_number){
        String[] split = reference_number.split(";");
        return Integer.parseInt(split[0]);
    }

    private String getSourceAddress(String reference_number){
        String[] split = reference_number.split(";");
        return split[1];
    }

    public synchronized void removeOldReferenceNumbers(){
        if ((System.currentTimeMillis() -lastChangeOfBalanceFlag) > removeOlderThanXSeconds/2*1000){
            lastChangeOfBalanceFlag = System.currentTimeMillis();
            balanceFlag = !balanceFlag;
            if(balanceFlag == true){
                referenceNumberMessageIdA.clear();
            } else{
                referenceNumberMessageIdB.clear();
            }
        }
    }

    public int checkExistenceOfReferenceNumberInCache(int reference_number, Sms smsEvent){//0 don't exist //1 exisit in A //2 exist in B
        if(referenceNumberMessageIdA.get(createStringReferenceNumber(reference_number, smsEvent)) != null){
            return 1;
        }else if(referenceNumberMessageIdB.get(createStringReferenceNumber(reference_number, smsEvent)) != null){
            return 2;
        } else return 0;
    }

    public long getMessageIdByReferenceNumber(int reference_number, Sms smsEvent,boolean queueFlag) {//queueFlag 0->A //1->B
        if(queueFlag) return referenceNumberMessageIdB.get(createStringReferenceNumber(reference_number, smsEvent));
        else return referenceNumberMessageIdA.get(createStringReferenceNumber(reference_number, smsEvent));
    }

    public void setRemoveOlderThanXSeconds(int numberOfSeconds){
        this.removeOlderThanXSeconds = numberOfSeconds;
    }

    public int getRemoveOlderThanXSeconds(){
        return this.removeOlderThanXSeconds;
    }
}
