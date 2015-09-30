/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.smsc.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.jboss.mx.util.MBeanServerLocator;
import org.mobicents.smsc.domain.SmscManagement;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.MProcMessageDestination;
import org.mobicents.smsc.mproc.MProcNewMessage;
import org.mobicents.smsc.mproc.MProcResult;
import org.mobicents.smsc.mproc.MProcRuleFactory;
import org.mobicents.smsc.mproc.MProcRule;
import org.mobicents.smsc.mproc.MProcRuleMBean;
import org.mobicents.smsc.mproc.impl.MProcMessageDestinationImpl;
import org.mobicents.smsc.mproc.impl.MProcMessageImpl;
import org.mobicents.smsc.mproc.impl.MProcNewMessageImpl;
import org.mobicents.smsc.mproc.impl.MProcRuleOamMessages;
import org.mobicents.smsc.mproc.impl.PostArrivalProcessorImpl;
import org.mobicents.smsc.mproc.impl.PostDeliveryProcessorImpl;
import org.mobicents.smsc.mproc.impl.PostImsiProcessorImpl;

/**
*
* @author sergey vetyutnev
*
*/
public class MProcManagement implements MProcManagementMBean {

    private static final Logger logger = Logger.getLogger(MProcManagement.class);

    private static final String MPROC_LIST = "mprocList";
    private static final String TAB_INDENT = "\t";
    private static final String CLASS_ATTRIBUTE = "type";
    private static final XMLBinding binding = new XMLBinding();
    private static final String PERSIST_FILE_NAME = "mproc.xml";

    private final String name;

    private String persistDir = null;

    protected FastList<MProcRule> mprocs = new FastList<MProcRule>();

    private final TextBuilder persistFile = TextBuilder.newInstance();

    private MBeanServer mbeanServer = null;

    private static MProcManagement instance = null;

    private SmscManagement smscManagement = null;
    private SmscPropertiesManagement smscPropertiesManagement = null;

    private MProcManagement(String name) {
        this.name = name;

        binding.setClassAttribute(CLASS_ATTRIBUTE);
    }

    public static MProcManagement getInstance(String name) {
        if (instance == null) {
            instance = new MProcManagement(name);
        }
        return instance;
    }

    public static MProcManagement getInstance() {
        return instance;
    }

    public String getName() {
        return name;
    }

    public String getPersistDir() {
        return persistDir;
    }

    public void setPersistDir(String persistDir) {
        this.persistDir = persistDir;
    }

    public SmscManagement getSmscManagement() {
        return smscManagement;
    }

    public void setSmscManagement(SmscManagement smscManagement) {
        this.smscManagement = smscManagement;
    }

    @Override
    public FastList<MProcRule> getMProcRules() {
        return mprocs;
    }

    @Override
    public MProcRule getMProcRuleById(int id) {
        FastList<MProcRule> cur = mprocs;
        for (FastList.Node<MProcRule> n = cur.head(), end = cur.tail(); (n = n.getNext()) != end;) {
            MProcRule rule = n.getValue();
            if (rule.getId() == id)
                return rule;
        }        
        return null;
    }

    private void resortRules(FastList<MProcRule> lst) {
        SortedMap<Integer, MProcRule> cur = new TreeMap<Integer, MProcRule>();
        for (FastList.Node<MProcRule> n = lst.head(), end = lst.tail(); (n = n.getNext()) != end;) {
            MProcRule rule = n.getValue();
            cur.put(rule.getId(), rule);
        }        
        FastList<MProcRule> res = new FastList<MProcRule>();
        res.addAll(cur.values());

        this.mprocs = res;
    }

    @Override
    public MProcRule createMProcRule(int id, String ruleFactoryName, String parametersString) throws Exception {
        if (ruleFactoryName == null) {
            throw new Exception(String.format(MProcRuleOamMessages.CREATE_MPROC_RULE_FAIL_RULE_CLASS_NAME_NULL_VALUE));
        }

        MProcRuleFactory ruleClass = null;
        if (this.smscManagement != null) {
            ruleClass = this.smscManagement.getRuleFactory(ruleFactoryName);
        }
        if (ruleClass == null) {
            throw new Exception(String.format(MProcRuleOamMessages.CREATE_MPROC_RULE_FAIL_RULE_CLASS_NOT_FOUND, ruleFactoryName));
        }

        if (this.getMProcRuleById(id) != null) {
            throw new Exception(String.format(MProcRuleOamMessages.CREATE_MPROC_RULE_FAIL_ALREADY_EXIST, id));
        }

        MProcRule mProcRule = ruleClass.createMProcRuleInstance();
        mProcRule.setId(id);
        mProcRule.setInitialRuleParameters(parametersString);

        FastList<MProcRule> lstTag = new FastList<MProcRule>(this.mprocs);
        lstTag.add(mProcRule);
        this.resortRules(lstTag);
        this.store();

        this.registerMProcRuleMbean(mProcRule);

        return mProcRule;
    }

    @Override
    public MProcRule modifyMProcRule(int mProcRuleId, String parametersString) throws Exception {
        MProcRule mProcRule = this.getMProcRuleById(mProcRuleId);
        if (mProcRule == null) {
            throw new Exception(String.format(MProcRuleOamMessages.MODIFY_MPROC_RULE_FAIL_NOT_EXIST, mProcRuleId));
        }

        mProcRule.updateRuleParameters(parametersString);
        this.store();

        return mProcRule;
    }

    @Override
    public MProcRule destroyMProcRule(int mProcRuleId) throws Exception {
        MProcRule mProcRule = this.getMProcRuleById(mProcRuleId);
        if (mProcRule == null) {
            throw new Exception(String.format(MProcRuleOamMessages.DESTROY_MPROC_RULE_FAIL_NOT_EXIST, mProcRuleId));
        }

        FastList<MProcRule> lstTag = new FastList<MProcRule>(this.mprocs);
        lstTag.remove(mProcRule);
        this.resortRules(lstTag);
        this.store();

        this.unregisterMProcRuleMbean(mProcRule.getId());

        return mProcRule;
    }

    public MProcResult applyMProcArrival(Sms sms) {
        if (this.mprocs.size() == 0) {
            FastList<Sms> res0 = new FastList<Sms>();
            res0.add(sms);
            return new MProcResult(res0, false, false);
        }

        FastList<MProcRule> cur = this.mprocs;
        PostArrivalProcessorImpl pap = new PostArrivalProcessorImpl(
                this.smscPropertiesManagement.getDefaultValidityPeriodHours(),
                this.smscPropertiesManagement.getMaxValidityPeriodHours());
        MProcMessage message = new MProcMessageImpl(sms);

        try {
            for (FastList.Node<MProcRule> n = cur.head(), end = cur.tail(); (n = n.getNext()) != end;) {
                MProcRule rule = n.getValue();
                if (rule.isForPostArrivalState() && rule.matches(message)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("MRule matches at Arrival phase to a message:\nrule: " + rule + "\nmessage: " + sms);
                    }
                    rule.onPostArrival(pap, message);
                }
            }
        } catch (Throwable e) {
            logger.error(
                    "Exception when invoking rule.matches(message) or applyMProcArrival: " + e.getMessage(), e);
            return new MProcResult(null, false, true);
        }

        if (pap.isNeedDropMessage()) {
            return new MProcResult(null, false, true);
        }

        if (pap.isNeedRejectMessage()) {
            return new MProcResult(null, true, false);
        }

        FastList<MProcNewMessage> newMsgs = pap.getPostedMessages();
        if (newMsgs == null || newMsgs.size() == 0) {
            FastList<Sms> res0 = new FastList<Sms>();
            res0.add(sms);
            return new MProcResult(res0, false, false);
        }

        FastList<Sms> res = new FastList<Sms>();
        res.add(sms);
        for (FastList.Node<MProcNewMessage> n = newMsgs.head(), end = newMsgs.tail(); (n = n.getNext()) != end;) {
            MProcNewMessageImpl newMsg = (MProcNewMessageImpl) n.getValue();
            res.add(newMsg.getSmsContent());
        }
        return new MProcResult(res, false, false);
    }

    public MProcResult applyMProcImsiRequest(SmsSet smsSet, String imsi, String nnnDigits, int nnnNumberingPlan,
            int nnnAddressNature) {
        if (this.mprocs.size() == 0)
            return new MProcResult(null, false, false);

        FastList<MProcRule> cur = this.mprocs;
        PostImsiProcessorImpl pap = new PostImsiProcessorImpl();
        MProcMessageDestination messageDest = new MProcMessageDestinationImpl(smsSet);

        try {
            for (FastList.Node<MProcRule> n = cur.head(), end = cur.tail(); (n = n.getNext()) != end;) {
                MProcRule rule = n.getValue();
                if (rule.isForPostImsiRequestState() && rule.matches(messageDest)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("MRule matches at ImsiRequest phase to a message:\nrule: " + rule + "\nmessage: " + smsSet);
                    }
                    rule.onPostImsiRequest(pap, messageDest, imsi, nnnDigits, nnnNumberingPlan, nnnAddressNature);
                }
            }
        } catch (Throwable e) {
            logger.error(
                    "Exception when invoking rule.matches(message) or applyMProcImsiRequest(): " + e.getMessage(), e);
            return new MProcResult(null, false, false);
        }

        if (pap.isNeedDropMessages()) {
            return new MProcResult(null, false, true);
        }
        return new MProcResult(null, false, false);
    }

    public MProcResult applyMProcDelivery(Sms sms, boolean isDeliveryFailure) {
        if (this.mprocs.size() == 0) {
            return new MProcResult(null, false, false);
        }

        FastList<MProcRule> cur = this.mprocs;
        PostDeliveryProcessorImpl pap = new PostDeliveryProcessorImpl(
                this.smscPropertiesManagement.getDefaultValidityPeriodHours(),
                this.smscPropertiesManagement.getMaxValidityPeriodHours());
        MProcMessage message = new MProcMessageImpl(sms);

        try {
            for (FastList.Node<MProcRule> n = cur.head(), end = cur.tail(); (n = n.getNext()) != end;) {
                MProcRule rule = n.getValue();
                if (rule.isForPostDeliveryState() && rule.matches(message)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("MRule matches at Delivery phase to a message:\nrule: " + rule + "\nmessage: " + sms);
                    }
                    rule.onPostDelivery(pap, message, isDeliveryFailure);
                }
            }
        } catch (Throwable e) {
            logger.error(
                    "Exception when invoking rule.matches(message) or onPostDelivery(): " + e.getMessage(), e);
            return new MProcResult(null, false, true);
        }

        FastList<MProcNewMessage> newMsgs = pap.getPostedMessages();
        if (newMsgs == null || newMsgs.size() == 0) {
            return new MProcResult(null, false, false);
        }

        FastList<Sms> res = new FastList<Sms>();
        for (FastList.Node<MProcNewMessage> n = newMsgs.head(), end = newMsgs.tail(); (n = n.getNext()) != end;) {
            MProcNewMessageImpl newMsg = (MProcNewMessageImpl) n.getValue();
            res.add(newMsg.getSmsContent());
        }
        return new MProcResult(res, false, false);
    }

    public void start() throws Exception {

        this.smscPropertiesManagement = SmscPropertiesManagement.getInstance();

        if (this.smscManagement != null) {
            for (MProcRuleFactory ruleFactory : this.smscManagement.getMProcRuleFactories2()) {
                this.bindAlias(ruleFactory);
            }
        }

        try {
            this.mbeanServer = MBeanServerLocator.locateJBoss();
        } catch (Exception e) {
        }

        this.persistFile.clear();

        if (persistDir != null) {
            this.persistFile.append(persistDir).append(File.separator).append(this.name).append("_")
                    .append(PERSIST_FILE_NAME);
        } else {
            persistFile
                    .append(System.getProperty(SmscManagement.SMSC_PERSIST_DIR_KEY,
                            System.getProperty(SmscManagement.USER_DIR_KEY))).append(File.separator).append(this.name)
                    .append("_").append(PERSIST_FILE_NAME);
        }

        logger.info(String.format("Loading MProcRule configuration from %s", persistFile.toString()));

        try {
            this.load();
        } catch (FileNotFoundException e) {
            logger.warn(String.format("Failed to load the ProcRule configuration file. \n%s", e.getMessage()));
        }

        this.resortRules(this.mprocs);
        this.store();

        for (MProcRule rule : this.mprocs) {
            this.registerMProcRuleMbean(rule);
        }
    }

    public void stop() throws Exception {
        this.store();

        for (MProcRule rule : this.mprocs) {
            this.unregisterMProcRuleMbean(rule.getId());
        }
    }

    public void store() {

        try {
            XMLObjectWriter writer = XMLObjectWriter.newInstance(new FileOutputStream(persistFile.toString()));
            writer.setBinding(binding);
            writer.setIndentation(TAB_INDENT);
            writer.write(mprocs, MPROC_LIST, FastList.class);

            writer.close();
        } catch (Exception e) {
            logger.error("Error while persisting the MProcRule state in file", e);
        }
    }

    public void load() throws FileNotFoundException {
        XMLObjectReader reader = null;
        try {
            reader = XMLObjectReader.newInstance(new FileInputStream(persistFile.toString()));

            reader.setBinding(binding);
            this.mprocs = reader.read(MPROC_LIST, FastList.class);

            reader.close();
        } catch (XMLStreamException ex) {
            logger.info("Error while re-creating MProcRule from persisted file", ex);
        }
    }

    private void registerMProcRuleMbean(MProcRuleMBean mProcRule) {
        try {
            ObjectName esmeObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=MProcRule,name=" + mProcRule.getId());
            StandardMBean esmeMxBean = new StandardMBean(mProcRule, MProcRuleMBean.class, true);

            if (this.mbeanServer != null)
                this.mbeanServer.registerMBean(esmeMxBean, esmeObjNname);
        } catch (InstanceAlreadyExistsException e) {
            logger.error(String.format("Error while registering MBean for MProcRule %d", mProcRule.getId()), e);
        } catch (MBeanRegistrationException e) {
            logger.error(String.format("Error while registering MBean for MProcRule %d", mProcRule.getId()), e);
        } catch (NotCompliantMBeanException e) {
            logger.error(String.format("Error while registering MBean for MProcRule %d", mProcRule.getId()), e);
        } catch (MalformedObjectNameException e) {
            logger.error(String.format("Error while registering MBean for MProcRule %d", mProcRule.getId()), e);
        }
    }

    private void unregisterMProcRuleMbean(int mProcRuleId) {

        try {
            ObjectName esmeObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=MProcRule,name=" + mProcRuleId);
            if (this.mbeanServer != null)
                this.mbeanServer.unregisterMBean(esmeObjNname);
        } catch (MBeanRegistrationException e) {
            logger.error(String.format("Error while unregistering MBean for MProcRule %d", mProcRuleId), e);
        } catch (InstanceNotFoundException e) {
            logger.error(String.format("Error while unregistering MBean for MProcRule %d", mProcRuleId), e);
        } catch (MalformedObjectNameException e) {
            logger.error(String.format("Error while unregistering MBean for MProcRule %d", mProcRuleId), e);
        }
    }

    protected void bindAlias(MProcRuleFactory ruleFactory) {
        Class cls = ruleFactory.createMProcRuleInstance().getClass();
        String alias = ruleFactory.getRuleClassName();
        binding.setAlias(cls, alias);
    }
}
