package su.nightexpress.excellenteconomy.user;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;

import su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency;

public class UserBalance {

    private final Map<String, Double> balanceMap;

    public UserBalance() {
        this(new ConcurrentHashMap<>());
    }

    public UserBalance(@NonNull Map<String, Double> balanceMap) {
        this.balanceMap = new ConcurrentHashMap<>(balanceMap);
    }

    /**
     * Returns a map of all currency balances.
     * 
     * @return An unmodifiable copy of the balance map.
     */
    @NonNull
    public Map<String, Double> getBalanceMap() {
        return Map.copyOf(this.balanceMap);
    }

    public void clear() {
        this.balanceMap.clear();
    }

    public void clear(@NonNull ExcellentCurrency currency) {
        this.clear(currency.getId());
    }

    public void clear(@NonNull String currencyId) {
        this.balanceMap.remove(currencyId);
    }

    public boolean has(@NonNull ExcellentCurrency currency, double amount) {
        return this.get(currency) >= amount;
    }

    public double get(@NonNull ExcellentCurrency currency) {
        return this.get(currency.getId());
    }

    public double get(@NonNull String currencyId) {
        return sanitize(this.balanceMap.get(currencyId));
    }

    public void add(@NonNull ExcellentCurrency currency, double amount) {
        this.add(currency.getId(), amount);
    }

    public void add(@NonNull String currencyId, double amount) {
        this.adjust(currencyId, Math.abs(amount));
    }

    public void remove(@NonNull ExcellentCurrency currency, double amount) {
        this.remove(currency.getId(), amount);
    }

    public void remove(@NonNull String currencyId, double amount) {
        this.adjust(currencyId, -Math.abs(amount));
    }

    /**
     * Atomically removes the amount only if the balance covers it, so concurrent withdrawals cannot overdraw.
     */
    public boolean tryRemove(@NonNull ExcellentCurrency currency, double amount) {
        if (!Double.isFinite(amount) || amount < 0) return false;

        boolean[] removed = {false};
        this.balanceMap.compute(currency.getId(), (k, v) -> {
            double current = sanitize(v);
            if (current < amount) return v;

            removed[0] = true;
            return current - amount;
        });
        return removed[0];
    }

    public void set(@NonNull ExcellentCurrency currency, double amount) {
        this.set(currency.getId(), currency.floorAndLimit(amount));
    }

    public void set(@NonNull String currencyId, double amount) {
        this.balanceMap.put(currencyId, sanitize(amount));
    }

    void adjust(@NonNull String currencyId, double delta) {
        if (!Double.isFinite(delta)) return;

        this.balanceMap.compute(currencyId, (k, v) -> Math.max(0, sanitize(v) + delta));
    }

    private static double sanitize(Double value) {
        return value == null || !Double.isFinite(value) ? 0D : Math.max(0, value);
    }
}
