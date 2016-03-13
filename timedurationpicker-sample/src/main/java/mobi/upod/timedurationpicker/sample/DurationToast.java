package mobi.upod.timedurationpicker.sample;

import android.content.Context;
import android.widget.Toast;
import mobi.upod.timedurationpicker.TimeDurationUtil;

final class DurationToast {

    public static void show(Context context, long duration) {
        final String formattedDuration = TimeDurationUtil.formatHoursMinutesSeconds(duration);
        Toast.makeText(context, formattedDuration, Toast.LENGTH_LONG).show();
    }
}
