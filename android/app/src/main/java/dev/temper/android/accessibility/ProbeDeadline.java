package dev.temper.android.accessibility;

/** Leading-edge scheduling: another event can accelerate, never postpone, pending work. */
public final class ProbeDeadline {
    private long due = Long.MAX_VALUE;
    public long schedule(long now, long delay) {
        due = Math.min(due, now + Math.max(0, delay));
        return Math.max(0, due - now);
    }
    public void reset() { due = Long.MAX_VALUE; }
}
