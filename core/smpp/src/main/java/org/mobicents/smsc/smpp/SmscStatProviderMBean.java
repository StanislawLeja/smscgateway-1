/**
 * 
 */
package org.mobicents.smsc.smpp;

import java.util.Date;

/**
 * @author Amit Bhayani
 *
 */
public interface SmscStatProviderMBean {
	
	int getMessageInProcess();
	
	int getDueSlotProcessingLag();
	
	long getMessageScheduledTotal();
	
	long getCurrentMessageId();
	
	Date getSmscStartTime();
	
}
