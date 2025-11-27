package com.telas.shared.utils;

import com.telas.entities.SubscriptionMonitor;
import com.telas.shared.constants.SharedConstants;

import java.util.List;
import java.util.Objects;

public final class MonitorBlocksUtils {
  private MonitorBlocksUtils() {
  }

    public static int sumSubscriptionBlocks(List<SubscriptionMonitor> subscriptionMonitors) {
        if (subscriptionMonitors == null || subscriptionMonitors.isEmpty()) return 0;
        return subscriptionMonitors.stream()
                .filter(Objects::nonNull)
                .mapToInt(SubscriptionMonitor::getSlotsQuantity)
                .sum();
    }

    public static int computeRemainingBlocks(Integer maxBlocks, int totalSubscriptionBlocks) {
        int mb = maxBlocks != null ? maxBlocks : SharedConstants.MAX_MONITOR_ADS;
        int remaining = mb - totalSubscriptionBlocks;
        return Math.max(0, remaining);
    }

    /**
     * Calcula quantos blocos cada ad "admin" (sem subscription) deve receber.
     * Retorna 0 quando não há ads correspondentes ou não há blocos restantes.
     * Quando houver distribuição válida, garante pelo menos 1 bloco por ad.
     */
    public static int blocksPerAdminAd(Integer maxBlocks, int totalSubscriptionBlocks, long unmatchedAdsCount) {
        if (unmatchedAdsCount <= 0) {
            return 0;
        }
        int remaining = computeRemainingBlocks(maxBlocks, totalSubscriptionBlocks);

        if (remaining <= 0) {
            return 0;
        }

        int per = remaining / (int) unmatchedAdsCount;
        return Math.max(1, per);
    }

    /**
     * Calcula os minutos diários de exibição para um ad admin baseado na distribuição de blocos.
     * Fórmula: minutos_por_dia = round(blocks_do_ad * 1440 / total_blocos_considerados)
     */
    public static int calculateAdsDailyDisplayTimeInMinutes(Integer maxBlocks, int totalSubscriptionBlocks, long unmatchedAdsCount) {
        int perAdmin = blocksPerAdminAd(maxBlocks, totalSubscriptionBlocks, unmatchedAdsCount);

        if (perAdmin <= 0) {
            return 0;
        }

        int totalBlocksConsidered = totalSubscriptionBlocks + perAdmin * (int) unmatchedAdsCount;

        if (totalBlocksConsidered <= 0) {
            return 0;
        }
        double minutes = (double) perAdmin * (double) SharedConstants.TOTAL_MINUTES_IN_A_DAY / (double) totalBlocksConsidered;
        return (int) Math.round(minutes);
    }


}
