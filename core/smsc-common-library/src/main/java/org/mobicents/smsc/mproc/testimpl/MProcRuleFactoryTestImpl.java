package org.mobicents.smsc.mproc.testimpl;

import org.mobicents.smsc.mproc.MProcRule;
import org.mobicents.smsc.mproc.MProcRuleFactory;

public class MProcRuleFactoryTestImpl implements MProcRuleFactory {
    public static final String CLASS_NAME = "testrule";

    @Override
    public String getRuleClassName() {
        return CLASS_NAME;
    }

    @Override
    public MProcRule createMProcRuleInstance() {
        return new MProcRuleTestImpl();
    }

}
