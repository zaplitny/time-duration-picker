package mobi.upod.timedurationpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.*;

/**
 * Control that allows the user to easily input a time duration made up of hours, minutes and seconds, like known from
 * the Lollipop stock timer app.
 *
 * See {@link R.styleable#TimeDurationPicker_textAppearanceDisplay},
 *  {@link R.styleable#TimeDurationPicker_textAppearanceUnit},
 *  {@link R.styleable#TimeDurationPicker_textAppearanceButton},
 *  {@link R.styleable#TimeDurationPicker_backspaceIcon},
 *  {@link R.styleable#TimeDurationPicker_clearIcon},
 *  {@link R.styleable#TimeDurationPicker_separatorColor},
 *  {@link R.styleable#TimeDurationPicker_durationDisplayBackground},
 *  {@link R.styleable#TimeDurationPicker_numPadButtonPadding}
 */
public class TimeDurationPicker extends FrameLayout {
    private final TimeDurationString input = new TimeDurationString();
    private final View displayRow;
    private final View durationView;
    private final TextView hoursView;
    private final TextView minutesView;
    private final TextView secondsView;
    private final TextView[] displayViews;
    private final TextView[] unitLabelViews;
    private final ImageButton backspaceButton;
    private final ImageButton clearButton;
    private final View separatorView;
    private final View numPad;
    private final Button[] numPadButtons;
    private final Button numPadMeasureButton;
    private OnDurationChangedListener changeListener = null;

    /**
     * Implement this interface and set it using #setOnDurationChangeListener to get informed about input changes.
     */
    public interface OnDurationChangedListener {
        /**
         * Called whenever the input (the displayed duration string) changes.
         * @param view the view that fired the event
         * @param duration the new duration in milli seconds
         */
        void onDurationChanged(TimeDurationPicker view, long duration);
    }

    public TimeDurationPicker(Context context) {
        this(context, null);
    }

    public TimeDurationPicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.timeDurationPickerStyle);
    }

    public TimeDurationPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.time_duration_picker, this);

        //
        // find views
        //
        displayRow = findViewById(R.id.displayRow);
        durationView = findViewById(R.id.duration);
        hoursView = (TextView) findViewById(R.id.hours);
        minutesView = (TextView) findViewById(R.id.minutes);
        secondsView = (TextView) findViewById(R.id.seconds);
        displayViews = new TextView[] { hoursView, minutesView, secondsView };

        final TextView hoursLabel = (TextView) findViewById(R.id.hoursLabel);
        final TextView minutesLabel = (TextView) findViewById(R.id.minutesLabel);
        final TextView secondsLabel = (TextView) findViewById(R.id.secondsLabel);
        unitLabelViews = new TextView[] { hoursLabel, minutesLabel, secondsLabel };

        backspaceButton = (ImageButton) findViewById(R.id.backspace);
        clearButton = (ImageButton) findViewById(R.id.clear);

        separatorView = findViewById(R.id.separator);

        numPad = findViewById(R.id.numPad);
        numPadMeasureButton = (Button) findViewById(R.id.numPadMeasure);
        numPadButtons = new Button[] {
                (Button) findViewById(R.id.numPad1), (Button) findViewById(R.id.numPad2), (Button) findViewById(R.id.numPad3),
                (Button) findViewById(R.id.numPad4), (Button) findViewById(R.id.numPad5), (Button) findViewById(R.id.numPad6),
                (Button) findViewById(R.id.numPad7), (Button) findViewById(R.id.numPad8), (Button) findViewById(R.id.numPad9),
                (Button) findViewById(R.id.numPad0), (Button) findViewById(R.id.numPad00)
        };

        //
        // apply style
        //
        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TimeDurationPicker, defStyleAttr, 0);
        try {
            applyPadding(attributes, R.styleable.TimeDurationPicker_numPadButtonPadding, numPadButtons);

            applyTextAppearance(context, attributes, R.styleable.TimeDurationPicker_textAppearanceDisplay, displayViews);
            applyTextAppearance(context, attributes, R.styleable.TimeDurationPicker_textAppearanceButton, numPadButtons);
            applyTextAppearance(context, attributes, R.styleable.TimeDurationPicker_textAppearanceUnit, unitLabelViews);

            applyIcon(attributes, R.styleable.TimeDurationPicker_backspaceIcon, backspaceButton);
            applyIcon(attributes, R.styleable.TimeDurationPicker_clearIcon, clearButton);

            applyBackgroundColor(attributes, R.styleable.TimeDurationPicker_separatorColor, separatorView);
            applyBackgroundColor(attributes, R.styleable.TimeDurationPicker_durationDisplayBackground, displayRow);
        } finally {
            attributes.recycle();
        }

        //
        // init actions
        //
        backspaceButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackspace();
            }
        });
        clearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClear();
            }
        });

        final OnClickListener numberClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                onNumberClick(((Button) v).getText());
            }
        };
        for (Button button : numPadButtons) {
            button.setOnClickListener(numberClickListener);
        }

        //
        // init default value
        //
        updateHoursMinutesSeconds();
    }

    //
    // public interface
    //

    /**
     * Gets the current duration entered by the user.
     * @return the duration entered by the user in milliseconds.
     */
    public long getDuration() {
        return input.getDuration();
    }

    /**
     * Sets the current duration.
     * @param millis the duration in milliseconds
     */
    public void setDuration(long millis) {
        input.setDuration(millis);
        updateHoursMinutesSeconds();
    }

    /**
     * Sets a listener to be informed of updates to the entered duration.
     * @param listener the listener to be informed or {@code null} if no one should be informed.
     */
    public void setOnDurationChangeListener(OnDurationChangedListener listener) {
        changeListener = listener;
    }

    /**
     * Sets the text appearance for the entered duration (the large numbers in the upper area).
     * @param resId resource id of the style describing the text appearance.
     */
    public void setDisplayTextAppearance(int resId) {
        applyTextAppearance(getContext(), resId, displayViews);
    }

    /**
     * Sets the text appearance for the small unit lables ("h", "m", "s") in the upper display area.
     * @param resId resource id of the style describing the text appearance.
     */
    public void setUnitTextAppearance(int resId) {
        applyTextAppearance(getContext(), resId, unitLabelViews);
    }

    /**
     * Sets the text appearance for the number pad buttons.
     * @param resId resource id of the style describing the text appearance.
     */
    public void setButtonTextAppearance(int resId) {
        applyTextAppearance(getContext(), resId, numPadButtons);
    }

    /**
     * Sets the icon to be shown on the backspace button.
     * @param icon backspace drawable
     */
    public void setBackspaceIcon(Drawable icon) {
        backspaceButton.setImageDrawable(icon);
    }

    /**
     * Sets the icon for the clear button.
     * @param icon clear drawable
     */
    public void setClearIcon(Drawable icon) {
        clearButton.setImageDrawable(icon);
    }

    /**
     * Sets the color of the separator line between the duration display and the number pad.
     * @param color color value
     */
    public void setSeparatorColor(int color) {
        separatorView.setBackgroundColor(color);
    }

    /**
     * Sets the background color of the upper duration display area.
     * @param color color value
     */
    public void setDurationDisplayBackgroundColor(int color) {
        displayRow.setBackgroundColor(color);
    }

    /**
     * Sets the padding to be used for the number pad buttons.
     * @param padding padding in pixels
     */
    public void setNumPadButtonPadding(int padding) {
        applyPadding(padding, numPadButtons);
    }

    //
    // style helpers
    //

    private void applyPadding(TypedArray attrs, int attributeIndex, final View[] targetViews) {
        final int padding = attrs.getDimensionPixelSize(attributeIndex, -1);
        if (padding > -1) {
            applyPadding(padding, targetViews);
        }
    }

    private void applyPadding(int padding, final View[] targetViews) {
        for (View view : targetViews) view.setPadding(padding, padding, padding, padding);
    }

    private void applyTextAppearance(Context context, TypedArray attrs, int attributeIndex, final TextView[] targetViews) {
        final int id = attrs.getResourceId(attributeIndex, 0);
        if (id != 0) {
            applyTextAppearance(context, id, targetViews);
        }
    }

    private void applyTextAppearance(Context context, int resId, final TextView[] targetViews) {
        for (TextView view : targetViews) view.setTextAppearance(context, resId);
    }

    private void applyIcon(TypedArray attrs, int attributeIndex, ImageView targetView) {
        final Drawable icon = attrs.getDrawable(attributeIndex);
        if (icon != null) {
            targetView.setImageDrawable(icon);
        }
    }

    private void applyBackgroundColor(TypedArray attrs, int attributeIndex, View targetView) {
        if (attrs.hasValue(attributeIndex)) {
            final int color = attrs.getColor(attributeIndex, 0);
            targetView.setBackgroundColor(color);
        }
    }

    private void applyLeftMargin(int margin, View... targetViews) {
        for (View view : targetViews) {
            final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
            params.setMargins(margin, params.topMargin, params.rightMargin, params.bottomMargin);
            view.setLayoutParams(params);
        }
    }

    //
    // event helpers
    //

    private void onBackspace() {
        input.popDigit();
        updateHoursMinutesSeconds();
    }

    private void onClear() {
        input.clear();
        updateHoursMinutesSeconds();
    }

    private void onNumberClick(final CharSequence digits) {
        input.pushNumber(digits);
        updateHoursMinutesSeconds();
    }

    private void updateHoursMinutesSeconds() {
        hoursView.setText(input.getHoursString());
        minutesView.setText(input.getMinutesString());
        secondsView.setText(input.getSecondsString());
        fireDurationChangeListener();
    }

    private void fireDurationChangeListener() {
        if (changeListener != null) {
            changeListener.onDurationChanged(this, input.getDuration());
        }
    }

    //
    // layouting
    //

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int touchableSize = getContext().getResources().getDimensionPixelSize(R.dimen.touchable);
        final int dummyMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        // set spacing between units
        hoursView.measure(dummyMeasureSpec, dummyMeasureSpec);
        final TextView unitLabelView = unitLabelViews[2];
        unitLabelView.measure(dummyMeasureSpec, dummyMeasureSpec);
        final int unitSpacing = Math.max(hoursView.getMeasuredWidth() / 3, (int) (1.2f * unitLabelView.getMeasuredWidth()));
        applyLeftMargin(unitSpacing, minutesView, secondsView);

        // calculate size for display row
        durationView.measure(dummyMeasureSpec, dummyMeasureSpec);
        final int minDisplayWidth = durationView.getMeasuredWidth() + 2 * touchableSize;
        final int minDisplayHeight = Math.max(durationView.getMeasuredHeight(), touchableSize);

        // calculate size for num pad
        numPadMeasureButton.measure(dummyMeasureSpec, dummyMeasureSpec);
        final int minNumPadButtonSize = Math.max(Math.max(numPadMeasureButton.getMeasuredHeight(), numPadMeasureButton.getMeasuredWidth()), touchableSize);
        final int minNumPadWidth = 3 * minNumPadButtonSize;
        final int minNumPadHeight = 4 * minNumPadButtonSize;

        // calculate overall size
        final int minWidth = Math.max(minDisplayWidth, minNumPadWidth);
        final int minHeight = minDisplayHeight + minNumPadHeight;

        // respect measure spec
        final int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int availableHeight = MeasureSpec.getSize(heightMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        final int preferredWidth = widthMode == MeasureSpec.EXACTLY ? availableWidth : minWidth;
        final int preferredHeight = heightMode == MeasureSpec.EXACTLY ? availableHeight : minHeight;

        // measure the display
        final int displayRowWidth = Math.max(minDisplayWidth, preferredWidth);
        final int displayRowHeight = minDisplayHeight;
        displayRow.measure(MeasureSpec.makeMeasureSpec(displayRowWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(displayRowHeight, MeasureSpec.EXACTLY));

        // measure the numPad
        // if we have more space available, we can try to grow the num pad
        final int numPadWidth = Math.max(minNumPadHeight, displayRowWidth);
        final int numPadHeight = Math.max(minNumPadHeight, preferredHeight - displayRowHeight);
        numPad.measure(MeasureSpec.makeMeasureSpec(numPadWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(numPadHeight, MeasureSpec.EXACTLY));

        // forward calculated size to super implementation
        final int width = Math.max(displayRowWidth, numPadWidth);
        final int height = displayRowHeight + numPadHeight;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = right - left;

        // layout display row
        final int displayRowWidth = displayRow.getMeasuredWidth();
        final int displayRowHeight = displayRow.getMeasuredHeight();
        final int displayRowX = (width - displayRowWidth) / 2;
        displayRow.layout(displayRowX, 0, displayRowX + displayRowWidth, displayRowHeight);

        // layout num pad
        final int numPadWidth = numPad.getMeasuredWidth();
        final int numPadHeight = numPad.getMeasuredHeight();
        final int numPadX = (width - numPadWidth) / 2;
        final int numPadY = displayRowHeight;
        numPad.layout(numPadX, numPadY, numPadX + numPadWidth, numPadY + numPadHeight);
    }

    //
    // state handling
    //

    @Override
    protected Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), input.getInputString());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState))
            throw new IllegalArgumentException("Expected state of class " + SavedState.class.getName() + " but received state of class " + state.getClass().getName());

        final SavedState savedStated = (SavedState) state;
        super.onRestoreInstanceState(savedStated.getSuperState());
        input.clear();
        input.pushNumber(savedStated.durationInput);
        updateHoursMinutesSeconds();
    }

    /**
     * Encapsulates the digit input logic and text to duration conversion logic.
     */
    private static class TimeDurationString {
        private final int MAX_DIGITS = 6;
        private final StringBuilder input = new StringBuilder(MAX_DIGITS);

        public TimeDurationString() {
            padWithZeros();
        }

        public void pushNumber(final CharSequence digits) {
            for (int i = 0; i < digits.length(); ++i)
                pushDigit(digits.charAt(i));
        }

        public void pushDigit(final char digit) {
            if (!Character.isDigit(digit))
                throw new IllegalArgumentException("Only numbers are allowed");

            removeLeadingZeros();
            if (input.length() < MAX_DIGITS && (input.length() > 0 || digit != '0')) {
                input.append(digit);
            }
            padWithZeros();
        }

        public void popDigit() {
            if (input.length() > 0)
                input.deleteCharAt(input.length() - 1);
            padWithZeros();
        }

        public void clear() {
            input.setLength(0);
            padWithZeros();
        }

        public String getHoursString() {
            return input.substring(0, 2);
        }

        public String getMinutesString() {
            return input.substring(2, 4);
        }

        public String getSecondsString() {
            return input.substring(4, 6);
        }

        public String getInputString() {
            return input.toString();
        }

        public long getDuration() {
            final int hours = Integer.parseInt(getHoursString());
            final int minutes = Integer.parseInt(getMinutesString());
            final int seconds = Integer.parseInt(getSecondsString());
            return TimeDurationUtil.durationOf(hours, minutes, seconds);
        }

        public void setDuration(long millis) {
            final long hours = TimeDurationUtil.hoursOf(millis);
            final long minutes = TimeDurationUtil.minutesInHourOf(millis);
            final long seconds = TimeDurationUtil.secondsInMinuteOf(millis);
            input.setLength(0);
            if (hours > 99) {
                input.append("999999");
            } else {
                input
                        .append(stringFragment(hours))
                        .append(stringFragment(minutes))
                        .append(stringFragment(seconds));
            }
        }

        private void removeLeadingZeros() {
            while (input.length() > 0 && input.charAt(0) == '0')
                input.deleteCharAt(0);
        }

        private void padWithZeros() {
            while (input.length() < MAX_DIGITS)
                input.insert(0, '0');
        }

        private String stringFragment(long value) {
            return (value < 10 ? "0" : "") + Long.toString(value);
        }
    }

    /**
     * User interface state that is stored by this view for implementing
     * {@link View#onSaveInstanceState}.
     */
    public static class SavedState extends BaseSavedState {
        final String durationInput;

        public SavedState(Parcelable superState, String durationInput) {
            super(superState);
            this.durationInput = durationInput;
        }

        @SuppressWarnings("unused")
        public SavedState(Parcel source) {
            super(source);
            durationInput = source.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(durationInput);
        }
    }
}
