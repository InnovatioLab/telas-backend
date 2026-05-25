package com.telas.services.monitor;

import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MonitorAdsQuotaRules {

	public void validateMonitorAdsQuotas(Monitor monitor, List<Ad> ads) {
		int totalCap = monitor.getMaxBlocks() != null ? monitor.getMaxBlocks() : SharedConstants.MAX_MONITOR_ADS;
		totalCap = Math.min(totalCap, SharedConstants.MAX_MONITOR_ADS);

		int partnerCap = Math.min(10, totalCap);
		int clientCap = Math.max(0, totalCap - partnerCap);

		long partnerCount = ads.stream().filter(ad -> {
			Client c = ad.getClient();
			if (c == null) {
				return false;
			}
			return c.isPartner() || c.isAdmin() || c.isDeveloper();
		}).count();

		long clientCount = ads.size() - partnerCount;

		if (ads.size() > totalCap) {
			throw new BusinessRuleException(MonitorValidationMessages.ADS_LIMIT_EXCEEDED + totalCap);
		}
		if (partnerCount > partnerCap) {
			throw new BusinessRuleException(MonitorValidationMessages.PARTNER_ADS_LIMIT_EXCEEDED);
		}
		if (clientCount > clientCap) {
			throw new BusinessRuleException(MonitorValidationMessages.CLIENT_ADS_LIMIT_EXCEEDED);
		}
	}
}
