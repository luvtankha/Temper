package dev.temper.android;

/** A new process never resumes capture from an old ON preference without a new user tap. */
public final class TemperApplication extends android.app.Application {
    @Override public void onCreate(){super.onCreate();new dev.temper.android.privacy.PowerStore(this).setEnabled(false);}
}
