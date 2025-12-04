package com.telas.helpers;

import com.stripe.exception.StripeException;
import com.stripe.model.Coupon;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.telas.entities.*;
import com.telas.enums.NotificationReference;
import com.telas.enums.PaymentStatus;
import com.telas.enums.Recurrence;
import com.telas.repositories.ClientRepository;
import com.telas.services.NotificationService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.utils.DateUtils;
import com.telas.shared.utils.MoneyUtils;
import com.telas.shared.utils.ValidateDataUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

@Component
public class PaymentHelper {
    private final Logger log = LoggerFactory.getLogger(PaymentHelper.class);
    private final SubscriptionHelper subscriptionHelper;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;

    @Value("${stripe.product.id}")
    private String productId;

    @Autowired
    public PaymentHelper(@Lazy SubscriptionHelper subscriptionHelper,
                         ClientRepository clientRepository,
                         NotificationService notificationService) {
        this.subscriptionHelper = subscriptionHelper;
        this.clientRepository = clientRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void updateSubscriptionPeriod(Invoice invoice, Subscription subscription) {
        List<Long> periods = invoice.getLines().getData().stream()
                .map(line -> line.getPeriod().getStart())
                .toList();

        if (!periods.isEmpty()) {
            subscription.setStartedAt(Instant.ofEpochSecond(Collections.min(periods)));
        }
    }

    @Transactional
    public Map<String, String> createMetaData(Subscription subscription, Payment payment, Recurrence recurrence) {
        Map<String, String> metaData = new HashMap<>();
        metaData.put("subscriptionId", subscription.getId().toString());
        metaData.put("clientId", subscription.getClient().getId().toString());
        metaData.put("paymentId", payment.getId().toString());
        metaData.put("billing_address_locked", "true");
        metaData.put("allowed_billing_countries", "US,CA,MX");

        if (recurrence != null) {
            metaData.put("recurrence", recurrence.name());
        }
        return metaData;
    }

    @Transactional
    public void configureSubscriptionParams(SessionCreateParams.Builder paramsBuilder, Subscription subscription, Map<String, String> metaData, Recurrence recurrence) throws StripeException {
        paramsBuilder.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                .putAllMetadata(metaData)
                .setDescription("Invoice payment for your tela's subscription for monitors: " + subscription.getMonitorAddresses())
                .build());

        if (subscription.isUpgrade() && subscription.getEndsAt() != null) {
            addDiscountForUpgrade(paramsBuilder, subscription, recurrence);
        }

        addLineItems(paramsBuilder, subscription.getSubscriptionMonitors(), recurrence, true);
    }

    @Transactional
    public void configurePaymentParams(SessionCreateParams.Builder paramsBuilder, Subscription subscription, Customer customer, Map<String, String> metaData, Recurrence recurrence) {
        paramsBuilder.setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                .setSetupFutureUsage(SessionCreateParams.PaymentIntentData.SetupFutureUsage.OFF_SESSION)
                .setReceiptEmail(customer.getEmail())
                .putAllMetadata(metaData)
                .build());

        addLineItems(paramsBuilder, subscription.getSubscriptionMonitors(), recurrence, false);
    }

//    private void addLineItems(SessionCreateParams.Builder paramsBuilder, Set<SubscriptionMonitor> subscriptionMonitors, Recurrence recurrence, boolean isMonthly) {
//        subscriptionMonitors.stream()
//                .sorted(Comparator.comparing(sm -> sm.getMonitor().getId()))
//                .filter(sm -> sm.getSlotsQuantity() != null && sm.getSlotsQuantity() > SharedConstants.ZERO)
//                .forEachOrdered(sm -> {
//                    BigDecimal unitPricePerSlot = getUnitPricePerSlot(new ArrayList<>(subscriptionMonitors).indexOf(sm), getMultiplier(sm.getSubscription(), recurrence));
//                    paramsBuilder.addLineItem(createLineItem(sm.getSlotsQuantity(), unitPricePerSlot, isMonthly));
//                });
//    }
//
//    private SessionCreateParams.LineItem createLineItem(Integer slotsQuantity, BigDecimal unitPricePerSlot, boolean isMonthly) {
//        SessionCreateParams.LineItem.PriceData.Builder priceDataBuilder =
//                SessionCreateParams.LineItem.PriceData.builder()
//                        .setCurrency(SharedConstants.USD)
//                        .setUnitAmount(unitPricePerSlot.multiply(BigDecimal.valueOf(100)).longValue())
//                        .setProduct(productId);
//
//        if (isMonthly) {
//            priceDataBuilder.setRecurring(
//                    SessionCreateParams.LineItem.PriceData.Recurring.builder()
//                            .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
//                            .build()
//            );
//        }
//
//        return SessionCreateParams.LineItem.builder()
//                .setQuantity(slotsQuantity.longValue())
//                .setPriceData(priceDataBuilder.build())
//                .build();
//    }


    private void addLineItems(SessionCreateParams.Builder paramsBuilder, Set<SubscriptionMonitor> subscriptionMonitors, Recurrence recurrence, boolean isMonthly) {
        List<SubscriptionMonitor> monitors = subscriptionMonitors.stream().toList();

        BigDecimal baseSum = getBaseMonitorsSum(monitors);
        int totalSlots = monitors.stream().mapToInt(SubscriptionMonitor::getSlotsQuantity).sum();

        BigDecimal adjustedTotal = MoneyUtils.divide(MoneyUtils.multiply(baseSum, BigDecimal.valueOf(totalSlots)), BigDecimal.valueOf(monitors.size()));

        BigDecimal finalTotal = MoneyUtils.multiply(adjustedTotal, getMultiplier(monitors.get(0).getSubscription(), recurrence));

        long totalCents = finalTotal.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
        distributeCents(paramsBuilder, monitors, totalSlots, totalCents, isMonthly);
    }

    private BigDecimal getBaseMonitorsSum(List<SubscriptionMonitor> monitors) {
        return IntStream.range(0, monitors.size())
                .mapToObj(this::getUnitPricePerSlot)
                .reduce(BigDecimal.ZERO, MoneyUtils::add);
    }

    private SessionCreateParams.LineItem createLineItem(long unitAmountCents, int slotsQuantity, boolean isMonthly) {
        SessionCreateParams.LineItem.PriceData.Builder priceDataBuilder =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(SharedConstants.USD)
                        .setUnitAmount(unitAmountCents)
                        .setProduct(productId);

        if (isMonthly) {
            priceDataBuilder.setRecurring(
                    SessionCreateParams.LineItem.PriceData.Recurring.builder()
                            .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                            .build()
            );
        }

        return SessionCreateParams.LineItem.builder()
                .setQuantity((long) Math.max(1, slotsQuantity))
                .setPriceData(priceDataBuilder.build())
                .build();
    }

    private BigDecimal getUnitPricePerSlot(int index) {
        return switch (index) {
            case 0 -> BigDecimal.valueOf(700);
            case 1 -> BigDecimal.valueOf(600);
            default -> BigDecimal.valueOf(500);
        };
    }

    private void distributeCents(SessionCreateParams.Builder paramsBuilder, List<SubscriptionMonitor> monitors, int totalSlots, long totalCents, boolean isMonthly) {
        long centsPerSlot = totalCents / totalSlots;
        long remainder = totalCents % totalSlots;

        for (SubscriptionMonitor sm : monitors) {
            int slots = sm.getSlotsQuantity();
            long extra = Math.min(remainder, slots);
            long unitAmountCents = centsPerSlot + (extra > 0 ? 1 : 0);
            remainder -= extra;

            paramsBuilder.addLineItem(createLineItem(unitAmountCents, slots, isMonthly));
        }
    }

    private BigDecimal calculatePrice(Subscription subscription, Recurrence recurrence) {
        List<SubscriptionMonitor> monitors = subscription.getSubscriptionMonitors().stream().toList();

        BigDecimal baseSum = getBaseMonitorsSum(monitors);
        int totalSlots = monitors.stream().mapToInt(SubscriptionMonitor::getSlotsQuantity).sum();

        BigDecimal adjustedTotal = MoneyUtils.divide(MoneyUtils.multiply(baseSum, BigDecimal.valueOf(totalSlots)), BigDecimal.valueOf(monitors.size()));
        return MoneyUtils.multiply(adjustedTotal, getMultiplier(monitors.get(0).getSubscription(), recurrence));
    }

    private void addDiscountForUpgrade(SessionCreateParams.Builder paramsBuilder, Subscription subscription, Recurrence recurrence) throws StripeException {
        long now = Instant.now().getEpochSecond();
        long endsAt = subscription.getEndsAt().getEpochSecond();
        long remainingSeconds = endsAt - now;

        if (remainingSeconds > 0) {
            long totalSecondsInMonth = SharedConstants.MAX_BILLING_CYCLE_ANCHOR;
            double proportion = (double) remainingSeconds / totalSecondsInMonth;

            BigDecimal totalPrice = calculatePrice(subscription, recurrence);
            BigDecimal adjustedPrice = MoneyUtils.multiply(totalPrice, BigDecimal.valueOf(proportion));

            CouponCreateParams couponParams = CouponCreateParams.builder()
                    .setAmountOff(adjustedPrice.multiply(BigDecimal.valueOf(100)).longValue())
                    .setCurrency(SharedConstants.USD)
                    .setDuration(CouponCreateParams.Duration.ONCE)
                    .build();

            Coupon coupon = Coupon.create(couponParams);

            paramsBuilder.addDiscount(SessionCreateParams.Discount.builder()
                    .setCoupon(coupon.getId())
                    .build());
        }
    }

    private BigDecimal getMultiplier(Subscription subscription, Recurrence recurrence) {
        if (!subscription.isUpgrade() || recurrence == null) {
            return subscription.getRecurrence().getMultiplier();
        }
        return Recurrence.MONTHLY.equals(recurrence)
                ? recurrence.getMultiplier()
                : MoneyUtils.subtract(recurrence.getMultiplier(), subscription.getRecurrence().getMultiplier());
    }

//    private String getProductPriceIdMonthly() {
//        try {
//            PriceListParams params = PriceListParams.builder()
//                    .setProduct(productId)
//                    .addAllLookupKey(List.of("subscription"))
//                    .build();
//
//            List<Price> prices = Price.list(params).getData();
//
//            return prices.stream()
//                    .filter(price -> price.getRecurring() != null && "month".equals(price.getRecurring().getInterval()))
//                    .map(Price::getId)
//                    .findFirst()
//                    .orElseThrow(() -> new ResourceNotFoundException(PaymentValidationMessages.PAYMENT_PRODUCT_PRICES_NOT_FOUND));
//        } catch (StripeException e) {
//            throw new BusinessRuleException(PaymentValidationMessages.PAYMENT_PRODUCT_PRICES_NOT_FOUND);
//        }
//    }

    @Transactional
    public boolean isRecurringPayment(Subscription subscription, PaymentIntent paymentIntent) {
        String recurrenceStr = paymentIntent.getMetadata().get("recurrence");
        Recurrence recurrence = !ValidateDataUtils.isNullOrEmptyString(recurrenceStr)
                ? Recurrence.valueOf(recurrenceStr)
                : null;

        return Recurrence.MONTHLY.equals(subscription.getRecurrence()) || Recurrence.MONTHLY.equals(recurrence);
    }

    @Transactional
    public Subscription getSubscriptionFromInvoice(Invoice invoice) {
        UUID subscriptionId = UUID.fromString(invoice.getParent().getSubscriptionDetails().getMetadata().get("subscriptionId"));
        return subscriptionHelper.findEntityById(subscriptionId);
    }

    @Transactional
    public Payment getOrCreatePayment(Invoice invoice, Subscription subscription) {
        boolean isRecurringPayment = "subscription_cycle".equals(invoice.getBillingReason());

        return (!isRecurringPayment || subscription.isUpgrade())
                ? subscription.getPayments().stream()
                .filter(p -> PaymentStatus.PENDING.equals(p.getStatus()) && p.getStripeId() == null)
                .findFirst()
                .orElseGet(() -> new Payment(subscription))
                : new Payment(subscription);
    }

    @Transactional
    public void updatePaymentDetails(Payment payment, PaymentIntent paymentIntent) {
        payment.setStatus(PaymentStatus.fromStripeStatus(paymentIntent.getStatus(), null, payment));
        payment.setStripeId(paymentIntent.getId());

        BigDecimal amountCharged = paymentIntent.getAmount() != null
                ? MoneyUtils.divide(BigDecimal.valueOf(paymentIntent.getAmount()), BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        payment.setAmount(amountCharged);
    }

    @Transactional
    public void updatePaymentDetailsFromInvoice(Payment payment, Invoice invoice) {
        payment.setStatus(PaymentStatus.fromStripeStatus(null, invoice.getStatus(), payment));
        payment.setStripeId(invoice.getId());

        BigDecimal amountDue = invoice.getAmountDue() != null
                ? MoneyUtils.divide(BigDecimal.valueOf(invoice.getAmountDue()), BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        payment.setAmount(amountDue);
    }

    @Transactional
    public void handleCompletedPayment(Subscription subscription, Payment payment, PaymentIntent paymentIntent) {
        String recurrenceStr = paymentIntent.getMetadata().get("recurrence");
        Recurrence recurrence = ObjectUtils.isNotEmpty(recurrenceStr)
                ? Recurrence.valueOf(recurrenceStr)
                : null;

        if (recurrence == null) {
            subscription.initialize();
            handleSuccessfulPayment(payment, false);
            return;
        }


        if (recurrence.equals(subscription.getRecurrence())) {
            subscription.setEndsAt(recurrence.calculateEndsAtRenew(subscription));
            createRenewSubscriptionNotification(subscription);
        } else {
            subscription.setEndsAt(recurrence.calculateEndsAtUpgrade(subscription.getEndsAt(), subscription.getRecurrence()));
            subscription.setRecurrence(recurrence);
            subscription.setUpgrade(false);
            createUpgradeSubscriptionNotification(subscription);
        }
    }

    @Transactional
    public void handleCompletedPaymentFromInvoice(Subscription subscription, Payment payment, Invoice invoice) {
        boolean isRecurringPayment = invoice.getBillingReason() != null && invoice.getBillingReason().equals("subscription_cycle");

        if (subscription.getStartedAt() == null) {
            updateSubscriptionPeriod(invoice, subscription);
        }

        if (ValidateDataUtils.isNullOrEmptyString(subscription.getStripeId()) && !ValidateDataUtils.isNullOrEmptyString(invoice.getParent().getSubscriptionDetails().getSubscription())) {
            subscription.setStripeId(invoice.getParent().getSubscriptionDetails().getSubscription());
        }


        String recurrenceStr = invoice.getParent().getSubscriptionDetails().getMetadata().get("recurrence");

        if (!ValidateDataUtils.isNullOrEmptyString(recurrenceStr) && subscription.isUpgrade()) {
            Recurrence recurrence = Recurrence.valueOf(recurrenceStr);
            if (Recurrence.MONTHLY.equals(recurrence)) {
                subscription.setRecurrence(recurrence);
                subscription.setUpgrade(false);
                subscription.setEndsAt(null);
                createUpgradeSubscriptionNotification(subscription);
            }
        } else {
            handleSuccessfulPayment(payment, isRecurringPayment);
        }
    }

    private void handleSuccessfulPayment(Payment payment, boolean isRecurringPayment) {
        log.info("Payment succeeded with id: {}", payment.getId());
        UUID subscriptionId = payment.getSubscription().getId();
        Subscription subscription = subscriptionHelper.findEntityById(subscriptionId);

        if (!isRecurringPayment) {
            subscriptionHelper.handleNonRecurringPayment(subscription);
            updateClientWishlist(subscription);
        }
    }

    private void updateClientWishlist(Subscription subscription) {
        Set<Monitor> monitors = subscription.getMonitors();
        Client client = subscription.getClient();

        Set<Monitor> wishlistMonitors = client.getWishlist().getMonitors();

        int before = wishlistMonitors.size();

        wishlistMonitors.removeIf(monitors::contains);

        if (wishlistMonitors.size() < before) {
            clientRepository.save(client);
        }
    }

    @Transactional
    public void updateAuditInfo(Subscription subscription) {
        CustomRevisionListener.setUsername("Stripe Webhook");
        subscription.setUsernameUpdate("Stripe Webhook");
        subscription.getPayments().forEach(payment -> payment.setUsernameUpdate("Stripe Webhook"));
    }

    @Transactional
    public com.stripe.model.Subscription getStripeSubscription(Subscription subscription) throws StripeException {
        return subscriptionHelper.getStripeSubscription(subscription);
    }

    private void createUpgradeSubscriptionNotification(Subscription subscription) {
        Map<String, String> params = buildNotificationParams(subscription);
        notificationService.save(NotificationReference.SUBSCRIPTION_UPGRADE, subscription.getClient(), params, false);
    }

    private void createRenewSubscriptionNotification(Subscription subscription) {
        Map<String, String> params = buildNotificationParams(subscription);
        notificationService.save(NotificationReference.SUBSCRIPTION_RENEWAL, subscription.getClient(), params, false);
    }

    private Map<String, String> buildNotificationParams(Subscription subscription) {
        Map<String, String> params = new HashMap<>();
        params.put("locations", subscription.getMonitorAddressesFormated());
        params.put("link", "/client/subscriptions/" + subscription.getId());

        if (subscription.getEndsAt() != null) {
            params.put("endDate", DateUtils.formatInstantToString(subscription.getEndsAt()));
        }
        return params;
    }

    public String getSuccessUrl(Client client) {
        return subscriptionHelper.getRedirectUrlAfterCreatingNewSubscription(client);
    }
}
