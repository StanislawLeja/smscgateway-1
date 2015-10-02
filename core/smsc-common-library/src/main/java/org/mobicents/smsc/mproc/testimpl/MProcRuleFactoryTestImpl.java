package org.mobicents.smsc.mproc.testimpl;

import org.mobicents.smsc.mproc.MProcRule;
import org.mobicents.smsc.mproc.MProcRuleFactory;

public class MProcRuleFactoryTestImpl implements MProcRuleFactory {

    @Override
    public String getRuleClassName() {
        return "testrule";
    }

    @Override
    public MProcRule createMProcRuleInstance() {
        return new MProcRuleTestImpl();
    }

}
