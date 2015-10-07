package org.mobicents.smsc.mproc.testimpl;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.PostArrivalProcessor;
import org.mobicents.smsc.mproc.impl.MProcRuleBaseImpl;

public class MProcRuleTestImpl extends MProcRuleBaseImpl {

    @Override
    public String getRuleClassName() {
        return MProcRuleFactoryTestImpl.CLASS_NAME;
    }


    private static final String PAR1 = "par1";
    private static final String PAR2 = "par2";
    private String par1, par2;

    @Override
    public void setInitialRuleParameters(String parametersString) throws Exception {
        String[] args = splitParametersString(parametersString);
        if (args.length != 2) {
            throw new Exception("parametersString must contains 2 parameters");
        }
        par1 = args[0];
        par2 = args[1];
    }

    @Override
    public void updateRuleParameters(String parametersString) throws Exception {
        String[] args = splitParametersString(parametersString);
        if (args.length != 2) {
            throw new Exception("parametersString must contains 2 parameters");
        }
        par1 = args[0];
        par2 = args[1];
    }

    @Override
    public String getRuleParameters() {
        return par1 + " " + par2;
    }

    @Override
    public boolean matches(MProcMessage message) {
        if (message.getDestAddr().startsWith(par1))
            return true;
        else
            return false;
    }

    @Override
    public void onPostArrival(PostArrivalProcessor factory, MProcMessage message) throws Exception {
        String destAddr = this.par2 + message.getDestAddr();
        factory.updateMessageDestAddr(message, destAddr);
    }

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<MProcRuleTestImpl> M_PROC_RULE_TEST_XML = new XMLFormat<MProcRuleTestImpl>(
            MProcRuleTestImpl.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, MProcRuleTestImpl mProcRule) throws XMLStreamException {
            M_PROC_RULE_BASE_XML.read(xml, mProcRule);

            mProcRule.par1 = xml.getAttribute(PAR1, "");
            mProcRule.par2 = xml.getAttribute(PAR2, "");
        }

        @Override
        public void write(MProcRuleTestImpl mProcRule, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            M_PROC_RULE_BASE_XML.write(mProcRule, xml);

            xml.setAttribute(PAR1, mProcRule.par1);
            xml.setAttribute(PAR2, mProcRule.par2);
        }
    };

}
